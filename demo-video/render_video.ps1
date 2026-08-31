param(
    [Parameter(Mandatory = $true)][string]$HeroImage,
    [string]$OutputRoot = (Join-Path $PSScriptRoot 'output'),
    [string]$Python = 'python'
)

$ErrorActionPreference = 'Stop'
$renderScript = Join-Path $PSScriptRoot 'render_demo.py'
$slidesDir = Join-Path $OutputRoot 'slides'
$narrationDir = Join-Path $OutputRoot 'narration'
$audioDir = Join-Path $OutputRoot 'audio'
$assetDir = Join-Path $OutputRoot 'assets'
$deckPath = Join-Path $OutputRoot 'LOG-A-TRON-client-demo.pptx'
$videoPath = Join-Path $OutputRoot 'LOG-A-TRON-client-demo.mp4'
$captionPath = Join-Path $OutputRoot 'LOG-A-TRON-client-demo.srt'
$heroCopy = Join-Path $assetDir 'telemetry-hero.png'

New-Item -ItemType Directory -Force -Path $OutputRoot, $slidesDir, $narrationDir, $audioDir, $assetDir | Out-Null
Copy-Item -LiteralPath $HeroImage -Destination $heroCopy -Force
& $Python $renderScript $OutputRoot $heroCopy
if ($LASTEXITCODE -ne 0) { throw 'Slide rendering failed.' }

Add-Type -AssemblyName System.Speech
$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
$speaker.SelectVoice('Microsoft Zira Desktop')
$speaker.Rate = 0
$speaker.Volume = 100

try {
    1..9 | ForEach-Object {
        $scene = $_
        $textPath = Join-Path $narrationDir ('scene-{0:d2}.txt' -f $scene)
        $wavPath = Join-Path $audioDir ('scene-{0:d2}.wav' -f $scene)
        $spokenText = Get-Content -LiteralPath $textPath -Raw
        $speaker.SetOutputToWaveFile($wavPath)
        $speaker.Speak($spokenText)
        $speaker.SetOutputToNull()
    }
}
finally {
    $speaker.Dispose()
}

function Get-WavDurationSeconds([string]$Path) {
    $stream = [System.IO.File]::OpenRead($Path)
    $reader = New-Object System.IO.BinaryReader($stream)
    try {
        if ((-join $reader.ReadChars(4)) -ne 'RIFF') { throw "Invalid WAV: $Path" }
        [void]$reader.ReadUInt32()
        if ((-join $reader.ReadChars(4)) -ne 'WAVE') { throw "Invalid WAV: $Path" }
        $byteRate = 0
        $dataSize = 0
        while ($reader.BaseStream.Position -lt $reader.BaseStream.Length) {
            $chunk = -join $reader.ReadChars(4)
            $size = $reader.ReadUInt32()
            if ($chunk -eq 'fmt ') {
                [void]$reader.ReadUInt16()
                [void]$reader.ReadUInt16()
                [void]$reader.ReadUInt32()
                $byteRate = $reader.ReadUInt32()
                $reader.BaseStream.Position += ($size - 12)
            }
            elseif ($chunk -eq 'data') {
                $dataSize = $size
                break
            }
            else {
                $reader.BaseStream.Position += $size
            }
            if (($size % 2) -eq 1) { $reader.BaseStream.Position++ }
        }
        if ($byteRate -le 0 -or $dataSize -le 0) { throw "WAV duration unavailable: $Path" }
        return [math]::Round($dataSize / $byteRate, 3)
    }
    finally {
        $reader.Dispose()
        $stream.Dispose()
    }
}

function Format-SrtTime([double]$Seconds) {
    $span = [TimeSpan]::FromSeconds($Seconds)
    return ('{0:00}:{1:00}:{2:00},{3:000}' -f [math]::Floor($span.TotalHours), $span.Minutes, $span.Seconds, $span.Milliseconds)
}

$durations = @()
$cursor = 0.0
$srt = New-Object System.Collections.Generic.List[string]
1..9 | ForEach-Object {
    $scene = $_
    $wavPath = Join-Path $audioDir ('scene-{0:d2}.wav' -f $scene)
    $duration = Get-WavDurationSeconds $wavPath
    $slideDuration = $duration + 0.75
    $durations += $slideDuration
    $spokenText = (Get-Content -LiteralPath (Join-Path $narrationDir ('scene-{0:d2}.txt' -f $scene)) -Raw).Trim()
    $srt.Add([string]$scene)
    $srt.Add(('{0} --> {1}' -f (Format-SrtTime $cursor), (Format-SrtTime ($cursor + $duration))))
    $srt.Add($spokenText)
    $srt.Add('')
    $cursor += $slideDuration
}
$srt | Set-Content -LiteralPath $captionPath -Encoding utf8

$powerPoint = New-Object -ComObject PowerPoint.Application
$presentation = $null
try {
    $powerPoint.Visible = -1
    $presentation = $powerPoint.Presentations.Add()
    $presentation.PageSetup.SlideWidth = 13.333333 * 72
    $presentation.PageSetup.SlideHeight = 7.5 * 72

    1..9 | ForEach-Object {
        $scene = $_
        $slide = $presentation.Slides.Add($scene, 12)
        $slideImage = Join-Path $slidesDir ('scene-{0:d2}.png' -f $scene)
        [void]$slide.Shapes.AddPicture($slideImage, 0, -1, 0, 0, $presentation.PageSetup.SlideWidth, $presentation.PageSetup.SlideHeight)

        $wavPath = Join-Path $audioDir ('scene-{0:d2}.wav' -f $scene)
        $audioShape = $slide.Shapes.AddMediaObject2($wavPath, 0, -1, -40, -40, 1, 1)
        $audioShape.AnimationSettings.Animate = -1
        $audioShape.AnimationSettings.PlaySettings.PlayOnEntry = -1
        $audioShape.AnimationSettings.PlaySettings.HideWhileNotPlaying = -1
        $audioShape.AnimationSettings.PlaySettings.StopAfterSlides = 1

        $slide.SlideShowTransition.AdvanceOnClick = 0
        $slide.SlideShowTransition.AdvanceOnTime = -1
        $slide.SlideShowTransition.AdvanceTime = $durations[$scene - 1]
    }

    $presentation.SlideShowSettings.AdvanceMode = 2
    $presentation.SaveAs($deckPath, 24)
    if (Test-Path -LiteralPath $videoPath) { Remove-Item -LiteralPath $videoPath -Force }

    $presentation.CreateVideo($videoPath, $true, 4, 1080, 30, 85)
    do {
        Start-Sleep -Seconds 10
        $status = $presentation.CreateVideoStatus
        Write-Output ("VIDEO_RENDER_STATUS={0}" -f $status)
    } while ($status -eq 1 -or $status -eq 2)

    if ($status -ne 3) { throw "PowerPoint video render failed with status $status" }
}
finally {
    if ($null -ne $presentation) { $presentation.Close() }
    $powerPoint.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($powerPoint) | Out-Null
}

$totalSeconds = [math]::Round(($durations | Measure-Object -Sum).Sum, 3)
$report = [ordered]@{
    video = $videoPath
    presentation = $deckPath
    captions = $captionPath
    voice = 'Microsoft Zira Desktop'
    resolution = '1920x1080'
    frame_rate = 30
    duration_seconds = $totalSeconds
    duration_clock = [TimeSpan]::FromSeconds($totalSeconds).ToString('mm\:ss')
    scenes = 9
}
$report | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutputRoot 'render-report.json') -Encoding utf8
$report | ConvertTo-Json

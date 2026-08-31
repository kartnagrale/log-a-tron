# LOG-A-TRON Client Demo Video

This package renders a narrated, nine-scene product overview for LOG-A-TRON v0.7.0.

## Generated deliverables

The ignored `output/` directory contains generated artifacts such as:

- `LOG-A-TRON-client-demo.mp4` — final 1080p, 30 fps client-demo video.
- `LOG-A-TRON-client-demo.pptx` — editable timed presentation with embedded narration.
- `LOG-A-TRON-client-demo.srt` — narration captions.
- `narration-script.txt` — complete voice-over script.
- `storyboard.md` — scene outline.

## Re-render

The renderer requires Python with Pillow and python-pptx, Windows speech synthesis, and PowerPoint's native video encoder. Pass a Python executable available on your machine or leave the default `python` command.

```powershell
.\render_video.ps1 -HeroImage '.\assets\telemetry-hero.png'
```

The hero visual used for the local render was generated from a generic observability prompt and contains no company branding, logos, words, or watermarks.

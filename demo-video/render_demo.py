from __future__ import annotations

import json
import math
import sys
import textwrap
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont


W, H = 1920, 1080
NAVY = "#07111f"
PANEL = "#0d1d31"
PANEL_2 = "#122842"
CYAN = "#2dd4ff"
BLUE = "#377dff"
AMBER = "#ffb347"
MINT = "#39e6b0"
WHITE = "#f5f9ff"
MUTED = "#9fb3c8"
RED = "#ff6b79"


SCENES = [
    {
        "title": "Evidence before assumptions",
        "eyebrow": "LOG-A-TRON  /  CLIENT DEMO",
        "subtitle": "Observability and deterministic investigation across logs, traces, and metrics",
        "narration": "Welcome to LOG-A-TRON: an open-source observability and investigation platform built around a simple idea. During an incident, teams need evidence before assumptions. LOG-A-TRON brings logs, distributed traces, and metrics into one bounded workflow, then reconstructs what happened, why it happened, and which signals are consequences rather than causes.",
    },
    {
        "title": "The signal is everywhere. The answer is not.",
        "eyebrow": "THE OPERATIONAL GAP",
        "subtitle": "Fragmented tools turn incident response into manual correlation",
        "narration": "Most incidents do not arrive as a neat root cause. An engineer sees a timeout in one tool, a retry in another, a trace with a slow dependency, and a metric spike several minutes later. The first visible error is often only a downstream symptom. LOG-A-TRON reduces that manual correlation work by preserving identifiers, timestamps, scope, and provenance across every piece of evidence.",
    },
    {
        "title": "A durable telemetry path",
        "eyebrow": "INGEST  →  ENRICH  →  STORE  →  INVESTIGATE",
        "subtitle": "OpenTelemetry, Kafka, ClickHouse, Tempo, Prometheus, and a Spring control plane",
        "narration": "The platform starts with a durable telemetry path. OpenTelemetry agents and a gateway collect and redact application signals. Kafka provides at-least-once log delivery. A processor canonicalizes metadata, masks sensitive values, validates events, and creates stable fingerprints before ClickHouse persistence. Traces flow to Tempo and metrics to Prometheus. A Spring Boot control plane applies server-derived authorization before the Angular interface can search or investigate anything.",
    },
    {
        "title": "Explore with context intact",
        "eyebrow": "LOG EXPLORER",
        "subtitle": "Structured filters, saved searches, surrounding context, live tail, and trace navigation",
        "narration": "In the Log Explorer, users work with structured filters instead of arbitrary query text. Searches are bounded by project, environment, service, severity, and time. From one event, an investigator can open surrounding context, correlate identifiers, follow a trace, or use a bounded live tail. Saved searches make repeatable operational views easy, while the authorization boundary remains enforced on the server rather than trusted to the browser.",
    },
    {
        "title": "Follow the evidence chain",
        "eyebrow": "SANITIZED DEMO INCIDENT",
        "subtitle": "The earliest supported failure is separated from everything it triggered",
        "narration": "Consider a sanitized demo incident. The database connection pool reaches fifty out of fifty. That saturation is followed by a settlement database timeout. Kafka retries the work, a duplicate delivery is rejected, and consumer lag rises. A conventional alert stream may present five competing problems. LOG-A-TRON orders the evidence by time and dependency, identifies pool saturation as the earliest supported failure, and keeps every later event as a downstream symptom with its own evidence reference.",
    },
    {
        "title": "Deterministic RCA you can inspect",
        "eyebrow": "INVESTIGATION ENGINE",
        "subtitle": "Timeline, dependency flow, exception groups, confidence, provenance, and uncertainty",
        "narration": "The investigation engine is deliberately deterministic. It reconstructs the timeline, groups related exceptions, evaluates dependency flow, and selects the earliest root-cause candidate supported by the available signals. The result includes confidence states, uncertainty warnings, and citations back to bounded evidence. That means reviewers can inspect how a conclusion was formed instead of receiving an opaque answer that cannot be reproduced.",
    },
    {
        "title": "Security is part of the query plan",
        "eyebrow": "SCOPE  /  MASKING  /  LIMITS  /  AUDIT",
        "subtitle": "Authorization is derived server-side before any telemetry query executes",
        "narration": "Security is not a presentation-layer filter. Effective scope is derived on the server from memberships, roles, and environment grants before ClickHouse, Tempo, or Prometheus is queried. Sensitive data is masked before persistence. Query windows, result size, concurrency, live-tail duration, and evidence packages are bounded. Unauthorized lookups are non-enumerating, and investigation actions produce audit metadata. Even prompt-injection-like text inside logs remains untrusted evidence data.",
    },
    {
        "title": "A clear, replaceable AI boundary",
        "eyebrow": "AI STATUS — HONEST BY DESIGN",
        "subtitle": "The included provider is a deterministic stub; no external LLM call is made",
        "narration": "LOG-A-TRON also defines a replaceable AI provider boundary, but the included provider is intentionally a deterministic stub. It makes no external LLM or AI service call. It operates only on a bounded evidence package and validates that every cited reference exists. A future provider can be integrated behind this contract, but it cannot broaden authorization scope, execute raw SQL or PromQL, or turn untrusted log content into instructions.",
    },
    {
        "title": "One investigation surface. Evidence you can defend.",
        "eyebrow": "LOG-A-TRON  v0.7.0",
        "subtitle": "Open source • Apache 2.0 • Local Docker Compose stack • Phases 1–7 verified",
        "narration": "LOG-A-TRON version zero point seven brings durable ingestion, secure search, trace and metric correlation, deterministic root-cause analysis, and an Angular investigation experience into one local Docker Compose stack. Phases one through seven are verified with backend tests, frontend unit tests, and a production build. It is an open-source development and evaluation foundation, ready for teams to explore, challenge, and extend. LOG-A-TRON: evidence you can defend.",
    },
]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    return ImageFont.truetype(name, size)


def rounded(draw, box, radius=28, fill=PANEL, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def text(draw, xy, value, size, color=WHITE, bold=False, anchor=None):
    draw.text(xy, value, font=font(size, bold), fill=color, anchor=anchor)


def wrap(draw, value, x, y, max_width, size, color=MUTED, bold=False, spacing=14):
    f = font(size, bold)
    words = value.split()
    lines, line = [], ""
    for word in words:
        trial = f"{line} {word}".strip()
        if draw.textlength(trial, font=f) <= max_width:
            line = trial
        else:
            lines.append(line)
            line = word
    if line:
        lines.append(line)
    draw.multiline_text((x, y), "\n".join(lines), font=f, fill=color, spacing=spacing)
    return y + len(lines) * (size + spacing)


def base(scene_no, eyebrow, title_value, subtitle):
    img = Image.new("RGB", (W, H), NAVY)
    draw = ImageDraw.Draw(img)
    for i in range(7):
        draw.ellipse((1440+i*30, -140+i*18, 2050+i*30, 470+i*18), outline=(20, 92, 145), width=2)
    text(draw, (92, 62), "LOG-A-TRON", 28, CYAN, True)
    text(draw, (1825, 67), f"0{scene_no}", 22, MUTED, True, "ra")
    text(draw, (92, 160), eyebrow, 20, AMBER, True)
    text(draw, (92, 205), title_value, 58, WHITE, True)
    wrap(draw, subtitle, 94, 288, 1450, 28, MUTED)
    draw.line((92, 1000, 1828, 1000), fill="#1b3855", width=2)
    text(draw, (92, 1024), "OPEN-SOURCE OBSERVABILITY + INVESTIGATION", 17, "#65829e", True)
    return img, draw


def glow_dot(img, xy, color, radius=12):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    for r, a in [(radius*4, 18), (radius*2, 40), (radius, 255)]:
        ld.ellipse((xy[0]-r, xy[1]-r, xy[0]+r, xy[1]+r), fill=(*color, a))
    img.paste(Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB"))


def scene_1(hero):
    img = hero.resize((W, H), Image.Resampling.LANCZOS).convert("RGB")
    overlay = Image.new("RGBA", img.size, (3, 12, 24, 28))
    grad = Image.new("L", (W, 1))
    for x in range(W):
        grad.putpixel((x, 0), max(0, 230-int(230*x/1200)))
    grad = grad.resize((W, H))
    dark = Image.new("RGBA", img.size, (3, 10, 20, 245))
    dark.putalpha(grad)
    img = Image.alpha_composite(img.convert("RGBA"), overlay)
    img = Image.alpha_composite(img, dark).convert("RGB")
    d = ImageDraw.Draw(img)
    text(d, (106, 118), SCENES[0]["eyebrow"], 21, AMBER, True)
    text(d, (106, 230), "LOG-A-TRON", 82, WHITE, True)
    text(d, (108, 350), SCENES[0]["title"], 48, CYAN, True)
    wrap(d, SCENES[0]["subtitle"], 110, 430, 730, 30, "#c7d7e8", spacing=16)
    rounded(d, (108, 650, 560, 714), 32, "#10283b", CYAN, 2)
    text(d, (334, 682), "LOGS  •  TRACES  •  METRICS", 20, WHITE, True, "mm")
    text(d, (110, 928), "v0.7.0  /  APACHE 2.0", 19, MUTED, True)
    return img


def scene_2():
    img, d = base(2, SCENES[1]["eyebrow"], SCENES[1]["title"], SCENES[1]["subtitle"])
    cards = [(100, 430, 560, 850, "LOGS", "Timeout", RED), (730, 430, 1190, 850, "TRACES", "Slow dependency", CYAN), (1360, 430, 1820, 850, "METRICS", "Lag spike", AMBER)]
    for x1,y1,x2,y2,label,value,c in cards:
        rounded(d,(x1,y1,x2,y2),28,PANEL,"#1d4262",2)
        text(d,(x1+38,y1+42),label,20,c,True)
        text(d,(x1+38,y1+115),value,32,WHITE,True)
        for j in range(4):
            yy=y1+215+j*38
            d.rounded_rectangle((x1+38,yy,x2-38,yy+12),6,fill="#17314c")
            d.rounded_rectangle((x1+38,yy,x1+120+(j*61)%250,yy+12),6,fill=c)
    text(d,(960,914),"MANUAL CORRELATION",22,RED,True,"mm")
    d.line((505,884,1415,884),fill=RED,width=3)
    return img


def scene_3():
    img, d = base(3, SCENES[2]["eyebrow"], SCENES[2]["title"], SCENES[2]["subtitle"])
    nodes=[("APPLICATIONS",135),("OTEL",410),("KAFKA",685),("PROCESSOR",960),("CLICKHOUSE",1235),("INVESTIGATE",1510)]
    y=540
    for idx,(label,x) in enumerate(nodes):
        rounded(d,(x,y,x+230,y+110),22,PANEL_2,CYAN if idx in (1,5) else "#2c5271",2)
        text(d,(x+115,y+55),label,20,WHITE,True,"mm")
        if idx<len(nodes)-1:
            d.line((x+230,y+55,nodes[idx+1][1]-18,y+55),fill="#477da6",width=5)
            d.polygon([(nodes[idx+1][1]-18,y+45),(nodes[idx+1][1],y+55),(nodes[idx+1][1]-18,y+65)],fill=CYAN)
    for label,x,c in [("TEMPO",760,CYAN),("PROMETHEUS",1080,AMBER),("POSTGRES",1390,MINT)]:
        rounded(d,(x,745,x+260,825),20,"#0a192a",c,2)
        text(d,(x+130,785),label,18,c,True,"mm")
    d.line((875,650,875,745),fill="#365e7c",width=3)
    d.line((1140,650,1210,745),fill="#365e7c",width=3)
    d.line((1625,650,1520,745),fill="#365e7c",width=3)
    return img


def scene_4():
    img, d = base(4, SCENES[3]["eyebrow"], SCENES[3]["title"], SCENES[3]["subtitle"])
    rounded(d,(90,400,1830,920),30,"#091827","#234762",2)
    rounded(d,(118,430,400,890),20,"#0c2035")
    text(d,(150,468),"FILTERS",18,CYAN,True)
    for i,(label,val) in enumerate([("PROJECT","demo-platform"),("ENVIRONMENT","staging"),("SERVICE","settlement-api"),("SEVERITY","ERROR + WARN")]):
        yy=525+i*82
        text(d,(150,yy),label,14,MUTED,True)
        rounded(d,(150,yy+25,368,yy+65),10,"#142e48")
        text(d,(166,yy+45),val,15,WHITE,False,"lm")
    rounded(d,(430,430,1798,500),15,"#10273e")
    text(d,(465,465),"Search logs, trace IDs, order IDs, exception fingerprints…",20,"#7590a8",False,"lm")
    cols=[("TIME",470),("SEVERITY",700),("SERVICE",905),("MESSAGE",1160)]
    for label,x in cols: text(d,(x,550),label,15,MUTED,True)
    rows=[("14:03:18.204","ERROR","settlement-api","Database connection timed out"),("14:03:18.031","WARN","db-pool","Pool capacity reached 50 / 50"),("14:03:17.842","INFO","gateway","Trace context propagated"),("14:03:17.510","INFO","orders-api","Request accepted • demo-order-1042")]
    for i,row in enumerate(rows):
        yy=600+i*67
        if i==0: rounded(d,(445,575,1775,635),10,"#172d42",RED,1)
        for val,x in zip(row,[470,700,905,1160]): text(d,(x,yy),val,17,RED if (i==0 and x==700) else WHITE, x==700)
    rounded(d,(1460,842,1765,888),22,"#123b4e",CYAN,1)
    text(d,(1612,865),"OPEN TRACE  →",16,CYAN,True,"mm")
    return img


def scene_5():
    img,d=base(5,SCENES[4]["eyebrow"],SCENES[4]["title"],SCENES[4]["subtitle"])
    events=[("14:03:18.031","DB pool reaches 50 / 50","INITIATING FAILURE",AMBER),("14:03:18.204","Settlement database timeout","DOWNSTREAM",RED),("14:03:18.520","Kafka retry scheduled","DOWNSTREAM",CYAN),("14:03:18.844","Duplicate delivery rejected","DOWNSTREAM",CYAN),("14:03:20.100","Kafka lag increases","DOWNSTREAM",CYAN)]
    x=230
    d.line((x,445,x,885),fill="#2b668e",width=5)
    for i,(tm,label,badge,c) in enumerate(events):
        y=470+i*88
        d.ellipse((x-14,y-14,x+14,y+14),fill=c)
        text(d,(100,y),tm,17,MUTED,True,"lm")
        rounded(d,(285,y-34,1305,y+34),16,PANEL_2,"#254b68",1)
        text(d,(320,y),label,22,WHITE,True if i==0 else False,"lm")
        rounded(d,(1370,y-25,1750,y+25),25,"#3a2b1d" if i==0 else "#102d43",c,1)
        text(d,(1560,y),badge,15,c,True,"mm")
    return img


def scene_6():
    img,d=base(6,SCENES[5]["eyebrow"],SCENES[5]["title"],SCENES[5]["subtitle"])
    rounded(d,(90,405,1120,910),28,PANEL,"#244967",2)
    text(d,(130,448),"ROOT-CAUSE CANDIDATE",18,AMBER,True)
    text(d,(130,500),"Database pool saturation",38,WHITE,True)
    rounded(d,(130,575,495,635),28,"#18394b",MINT,2)
    text(d,(312,605),"HIGH CONFIDENCE",17,MINT,True,"mm")
    wrap(d,"Earliest supported failure with correlated log, trace, and metric evidence.",130,685,850,25,MUTED,spacing=12)
    text(d,(130,825),"EVIDENCE  3 LOGS  •  2 SPANS  •  1 METRIC WINDOW",16,CYAN,True)
    panels=[("TIMELINE","Ordered causal events",CYAN),("DEPENDENCIES","Service-to-service flow",BLUE),("EXCEPTIONS","Grouped fingerprints",RED),("UNCERTAINTY","Explicit coverage gaps",AMBER)]
    for i,(a,b,c) in enumerate(panels):
        xx=1170+(i%2)*330; yy=405+(i//2)*250
        rounded(d,(xx,yy,xx+295,yy+215),24,"#0c1d30",c,2)
        text(d,(xx+28,yy+40),a,17,c,True)
        wrap(d,b,xx+28,yy+92,235,21,WHITE,True,spacing=9)
    return img


def scene_7():
    img,d=base(7,SCENES[6]["eyebrow"],SCENES[6]["title"],SCENES[6]["subtitle"])
    center=(960,655)
    d.ellipse((790,485,1130,825),fill="#0d263b",outline=CYAN,width=4)
    text(d,center,"SERVER-DERIVED\nSCOPE",26,WHITE,True,"mm")
    items=[("MEMBERSHIP",(420,475),MINT),("ROLE",(1500,475),CYAN),("MASKING",(350,790),AMBER),("BOUNDED QUERY",(1570,790),BLUE),("AUDIT",(960,910),RED)]
    for label,pos,c in items:
        d.line((center[0],center[1],pos[0],pos[1]),fill="#315a76",width=3)
        rounded(d,(pos[0]-150,pos[1]-48,pos[0]+150,pos[1]+48),24,PANEL_2,c,2)
        text(d,pos,label,18,c,True,"mm")
    return img


def scene_8():
    img,d=base(8,SCENES[7]["eyebrow"],SCENES[7]["title"],SCENES[7]["subtitle"])
    boxes=[(100,"DETERMINISTIC\nEVIDENCE",CYAN),(625,"AIPROVIDER\nBOUNDARY",AMBER),(1150,"DETERMINISTIC\nSTUB RESULT",MINT)]
    for x,label,c in boxes:
        rounded(d,(x,495,x+410,715),28,PANEL_2,c,3)
        text(d,(x+205,605),label,28,WHITE,True,"mm")
    for x in (510,1035):
        d.line((x,605,x+100,605),fill="#547b97",width=5)
        d.polygon([(x+100,605),(x+78,592),(x+78,618)],fill=CYAN)
    rounded(d,(390,805,1530,875),32,"#2b1721",RED,2)
    text(d,(960,840),"NO EXTERNAL LLM CALL IN v0.7.0",22,RED,True,"mm")
    return img


def scene_9(hero):
    img=hero.resize((W,H),Image.Resampling.LANCZOS).convert("RGB")
    img=ImageEnhance.Brightness(img).enhance(.6)
    layer=Image.new("RGBA",img.size,(3,10,20,100)); img=Image.alpha_composite(img.convert("RGBA"),layer).convert("RGB")
    d=ImageDraw.Draw(img)
    text(d,(110,165),SCENES[8]["eyebrow"],21,AMBER,True)
    wrap(d,SCENES[8]["title"],110,245,950,58,WHITE,True,spacing=18)
    wrap(d,SCENES[8]["subtitle"],112,430,900,27,"#c4d5e5",False,spacing=14)
    rounded(d,(110,650,560,720),35,"#10324a",CYAN,2)
    text(d,(335,685),"EXPLORE THE PROJECT",19,CYAN,True,"mm")
    text(d,(112,925),"github.com/kartnagrale/log-a-tron",22,WHITE,True)
    return img


def main():
    if len(sys.argv) != 3:
        raise SystemExit("usage: render_demo.py OUTPUT_ROOT HERO_IMAGE")
    root=Path(sys.argv[1]); hero_path=Path(sys.argv[2])
    slides=root/"slides"; scripts=root/"narration"
    slides.mkdir(parents=True,exist_ok=True); scripts.mkdir(parents=True,exist_ok=True)
    hero=Image.open(hero_path)
    makers=[lambda:scene_1(hero),scene_2,scene_3,scene_4,scene_5,scene_6,scene_7,scene_8,lambda:scene_9(hero)]
    for i,(scene,maker) in enumerate(zip(SCENES,makers),1):
        maker().save(slides/f"scene-{i:02d}.png",quality=95)
        (scripts/f"scene-{i:02d}.txt").write_text(scene["narration"],encoding="utf-8")
    (root/"scene-manifest.json").write_text(json.dumps(SCENES,indent=2),encoding="utf-8")
    (root/"narration-script.txt").write_text("\n\n".join(f"SCENE {i}\n{s['narration']}" for i,s in enumerate(SCENES,1)),encoding="utf-8")
    storyboard=["# LOG-A-TRON Client Demo Storyboard","","Target: polished 3–5 minute narrated product overview.",""]
    for i,s in enumerate(SCENES,1): storyboard += [f"## {i}. {s['title']}","",s["subtitle"],""]
    (root/"storyboard.md").write_text("\n".join(storyboard),encoding="utf-8")


if __name__ == "__main__":
    main()

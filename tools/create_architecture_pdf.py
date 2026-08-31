from __future__ import annotations

from pathlib import Path

from reportlab.graphics.shapes import Drawing, Line, Polygon, Rect, String
from reportlab.lib import colors
from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    HRFlowable,
    Image,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.pdfbase.pdfmetrics import stringWidth


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "pdf" / "LOG-A-TRON-Architecture-Brief.pdf"
HERO = ROOT / "demo-video" / "output" / "assets" / "telemetry-hero.png"
PAGE_W, PAGE_H = A4

NAVY = HexColor("#07111F")
INK = HexColor("#112235")
SLATE = HexColor("#4D647A")
MUTED = HexColor("#70879A")
CYAN = HexColor("#00AEEB")
BLUE = HexColor("#377DFF")
AMBER = HexColor("#F3A847")
MINT = HexColor("#17B890")
RED = HexColor("#E85C6A")
PANEL = HexColor("#F4F8FB")
LINE = HexColor("#D8E4EC")
WHITE = colors.white


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="Kicker", parent=styles["Normal"], fontName="Helvetica-Bold", fontSize=8.4, leading=10, textColor=CYAN, spaceAfter=5, tracking=1.2))
styles.add(ParagraphStyle(name="PageTitle", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=23, leading=27, textColor=INK, spaceAfter=8))
styles.add(ParagraphStyle(name="Lead", parent=styles["Normal"], fontName="Helvetica", fontSize=11.2, leading=16.2, textColor=SLATE, spaceAfter=10))
styles.add(ParagraphStyle(name="H2x", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=14, leading=17, textColor=INK, spaceBefore=4, spaceAfter=6))
styles.add(ParagraphStyle(name="Bodyx", parent=styles["BodyText"], fontName="Helvetica", fontSize=9.2, leading=13.2, textColor=SLATE, spaceAfter=6))
styles.add(ParagraphStyle(name="Small", parent=styles["BodyText"], fontName="Helvetica", fontSize=7.7, leading=10.5, textColor=SLATE))
styles.add(ParagraphStyle(name="Tiny", parent=styles["BodyText"], fontName="Helvetica", fontSize=6.8, leading=8.5, textColor=SLATE))
styles.add(ParagraphStyle(name="CardTitle", parent=styles["Normal"], fontName="Helvetica-Bold", fontSize=10, leading=12, textColor=INK, spaceAfter=4))
styles.add(ParagraphStyle(name="CardBody", parent=styles["Normal"], fontName="Helvetica", fontSize=8.1, leading=11, textColor=SLATE))
styles.add(ParagraphStyle(name="WhiteTitle", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=30, leading=34, textColor=WHITE))
styles.add(ParagraphStyle(name="WhiteLead", parent=styles["Normal"], fontName="Helvetica", fontSize=13, leading=18, textColor=HexColor("#D4E6F2")))
styles.add(ParagraphStyle(name="Quote", parent=styles["Normal"], fontName="Helvetica-Bold", fontSize=12, leading=17, textColor=INK, leftIndent=14, rightIndent=14, borderColor=CYAN, borderWidth=0, borderPadding=8, backColor=HexColor("#EAF8FD"), spaceBefore=4, spaceAfter=10))


def P(text: str, style: str = "Bodyx") -> Paragraph:
    return Paragraph(text, styles[style])


def bullet(text: str) -> Paragraph:
    s = ParagraphStyle("bullet-local", parent=styles["Bodyx"], leftIndent=12, firstLineIndent=-8, bulletIndent=0, spaceAfter=4)
    return Paragraph("- " + text, s)


class SectionRule(Flowable):
    def __init__(self, width=18 * mm, color=CYAN):
        super().__init__()
        self.width = width
        self.height = 4
        self.color = color

    def draw(self):
        self.canv.setFillColor(self.color)
        self.canv.roundRect(0, 0, self.width, 3, 1.5, stroke=0, fill=1)


def title_block(kicker: str, title: str, lead: str):
    return [P(kicker.upper(), "Kicker"), P(title, "PageTitle"), SectionRule(), Spacer(1, 5), P(lead, "Lead")]


def card(title: str, body: str, accent=CYAN):
    data = [[P(title, "CardTitle")], [P(body, "CardBody")]]
    t = Table(data, colWidths=[79 * mm], hAlign="LEFT")
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), PANEL),
        ("BOX", (0, 0), (-1, -1), 0.7, LINE),
        ("LINEBEFORE", (0, 0), (0, -1), 3.2, accent),
        ("LEFTPADDING", (0, 0), (-1, -1), 9),
        ("RIGHTPADDING", (0, 0), (-1, -1), 9),
        ("TOPPADDING", (0, 0), (0, 0), 8),
        ("BOTTOMPADDING", (0, -1), (0, -1), 9),
    ]))
    return t


def arrow(d: Drawing, x1, y1, x2, y2, color=CYAN, width=1.8, dashed=False):
    line = Line(x1, y1, x2, y2, strokeColor=color, strokeWidth=width)
    if dashed:
        line.strokeDashArray = [4, 3]
    d.add(line)
    a = 5
    if abs(x2 - x1) >= abs(y2 - y1):
        sign = 1 if x2 > x1 else -1
        d.add(Polygon([x2, y2, x2-sign*a, y2+a/1.5, x2-sign*a, y2-a/1.5], fillColor=color, strokeColor=color))
    else:
        sign = 1 if y2 > y1 else -1
        d.add(Polygon([x2, y2, x2-a/1.5, y2-sign*a, x2+a/1.5, y2-sign*a], fillColor=color, strokeColor=color))


def node(d, x, y, w, h, label, fill=PANEL, stroke=LINE, color=INK, size=7.5):
    d.add(Rect(x, y, w, h, rx=6, ry=6, fillColor=fill, strokeColor=stroke, strokeWidth=1))
    lines = label.split("\n")
    for i, line in enumerate(lines):
        d.add(String(x+w/2, y+h/2 + (len(lines)-1)*4 - i*9, line, fontName="Helvetica-Bold", fontSize=size, fillColor=color, textAnchor="middle"))


def system_context_diagram():
    d = Drawing(480, 255)
    d.add(Rect(0, 0, 480, 255, rx=12, ry=12, fillColor=HexColor("#F8FBFD"), strokeColor=LINE))
    node(d, 180, 85, 120, 80, "LOG-A-TRON\nPLATFORM", HexColor("#0E2C44"), CYAN, WHITE, 10)
    node(d, 18, 165, 105, 45, "Applications\nand log files")
    node(d, 18, 40, 105, 45, "Engineer or\nsupport user")
    node(d, 355, 165, 105, 45, "OIDC identity\nprovider")
    node(d, 355, 40, 105, 45, "Platform or\nproject admin")
    arrow(d, 123, 187, 180, 140, BLUE)
    arrow(d, 123, 62, 180, 105, MINT)
    arrow(d, 355, 187, 300, 140, AMBER)
    arrow(d, 355, 62, 300, 105, CYAN)
    d.add(String(240, 31, "Search, correlate, reconstruct, and explain bounded evidence", fontName="Helvetica", fontSize=8, fillColor=SLATE, textAnchor="middle"))
    return d


def runtime_architecture_diagram():
    d = Drawing(480, 310)
    bands = [(0, 236, "COLLECTION", HexColor("#EAF8FD")), (0, 158, "INGESTION", HexColor("#EEF3FF")), (0, 80, "DATA", HexColor("#EFFAF7")), (0, 2, "PRODUCT + INVESTIGATION", HexColor("#FFF7EA"))]
    for x,y,label,c in bands:
        d.add(Rect(x, y, 480, 68, rx=9, ry=9, fillColor=c, strokeColor=LINE))
        d.add(String(10, y+54, label, fontName="Helvetica-Bold", fontSize=7, fillColor=MUTED))
    node(d, 35, 247, 90, 38, "Applications\n+ files", WHITE, LINE)
    node(d, 195, 247, 90, 38, "OTel agent", WHITE, CYAN)
    node(d, 355, 247, 90, 38, "OTel gateway", WHITE, CYAN)
    arrow(d, 125, 266, 195, 266); arrow(d, 285, 266, 355, 266)
    node(d, 75, 169, 100, 38, "Kafka\nlogs.raw.v1", WHITE, BLUE)
    node(d, 305, 169, 100, 38, "Log processor", WHITE, BLUE)
    arrow(d, 355, 247, 125, 207, BLUE); arrow(d, 175, 188, 305, 188, BLUE)
    node(d, 15, 91, 90, 38, "ClickHouse\nlogs", WHITE, MINT)
    node(d, 130, 91, 90, 38, "Tempo\ntraces", WHITE, MINT)
    node(d, 245, 91, 90, 38, "Prometheus\nmetrics", WHITE, MINT)
    node(d, 360, 91, 105, 38, "PostgreSQL\ncontrol plane", WHITE, MINT)
    arrow(d, 355, 169, 60, 129, MINT); arrow(d, 400, 247, 175, 129, CYAN, dashed=True); arrow(d, 420, 247, 290, 129, AMBER, dashed=True)
    node(d, 20, 13, 100, 38, "Angular UI", WHITE, AMBER)
    node(d, 155, 13, 115, 38, "Spring Boot API", WHITE, AMBER)
    node(d, 310, 13, 150, 38, "Deterministic\ninvestigation engine", WHITE, AMBER)
    arrow(d, 120, 32, 155, 32, AMBER); arrow(d, 270, 32, 310, 32, AMBER)
    for x in (60,175,290,412): arrow(d, x, 91, 220 if x != 412 else 235, 51, SLATE, 1, dashed=True)
    return d


def ingestion_flow_diagram():
    d = Drawing(480, 220)
    items = [
        (5, "1", "COLLECT", "File checkpoints\nOTLP signals", CYAN),
        (102, "2", "BUFFER", "Kafka\nat-least-once", BLUE),
        (199, "3", "PROTECT", "Parse, enrich\nmask, validate", AMBER),
        (296, "4", "STORE", "Batched\nClickHouse insert", MINT),
        (393, "5", "QUERY", "Authorized\nbounded search", RED),
    ]
    for i,(x,n,label,body,c) in enumerate(items):
        d.add(Rect(x, 56, 82, 125, rx=9, ry=9, fillColor=PANEL, strokeColor=c, strokeWidth=1.3))
        d.add(Rect(x, 153, 82, 28, rx=9, ry=9, fillColor=c, strokeColor=c))
        d.add(String(x+41, 162, n + "  " + label, fontName="Helvetica-Bold", fontSize=7.2, fillColor=WHITE, textAnchor="middle"))
        for j,line in enumerate(body.split("\n")):
            d.add(String(x+41, 122-j*13, line, fontName="Helvetica", fontSize=7.5, fillColor=SLATE, textAnchor="middle"))
        if i < len(items)-1: arrow(d, x+82, 118, items[i+1][0]-6, 118, c)
    d.add(String(240, 24, "Failure path: retry -> redacted dead-letter record -> auditable replay", fontName="Helvetica-Bold", fontSize=8.5, fillColor=RED, textAnchor="middle"))
    return d


def investigation_diagram():
    d = Drawing(480, 250)
    events=[("DB POOL", "50 / 50", AMBER), ("DB TIMEOUT", "settlement", RED), ("KAFKA RETRY", "scheduled", CYAN), ("DUPLICATE", "rejected", CYAN), ("LAG", "125", CYAN)]
    y=150
    for i,(a,b,c) in enumerate(events):
        x=8+i*95
        d.add(Rect(x,y,82,60,rx=8,ry=8,fillColor=PANEL,strokeColor=c,strokeWidth=1.2))
        d.add(String(x+41,y+38,a,fontName="Helvetica-Bold",fontSize=6.8,fillColor=c,textAnchor="middle"))
        d.add(String(x+41,y+20,b,fontName="Helvetica",fontSize=7.2,fillColor=SLATE,textAnchor="middle"))
        if i<4: arrow(d,x+82,y+30,x+94,y+30,c)
    d.add(Rect(8,35,210,78,rx=10,ry=10,fillColor=HexColor("#FFF5E8"),strokeColor=AMBER,strokeWidth=1.4))
    d.add(String(22,92,"INITIATING FAILURE",fontName="Helvetica-Bold",fontSize=8,fillColor=AMBER))
    d.add(String(22,70,"Database pool saturation",fontName="Helvetica-Bold",fontSize=11,fillColor=INK))
    d.add(String(22,51,"Earliest supported by ordered evidence",fontName="Helvetica",fontSize=7.5,fillColor=SLATE))
    d.add(Rect(248,35,224,78,rx=10,ry=10,fillColor=HexColor("#EEF8FC"),strokeColor=CYAN,strokeWidth=1.2))
    d.add(String(262,92,"DOWNSTREAM SYMPTOMS",fontName="Helvetica-Bold",fontSize=8,fillColor=CYAN))
    d.add(String(262,70,"Timeout, retry, duplicate, lag",fontName="Helvetica-Bold",fontSize=10,fillColor=INK))
    d.add(String(262,51,"Retained and cited, not promoted to root cause",fontName="Helvetica",fontSize=7.2,fillColor=SLATE))
    arrow(d,49,150,95,113,AMBER,1.5)
    return d


def security_diagram():
    d=Drawing(480,255)
    cx,cy=240,128
    rings=[(218,112,HexColor("#EFF4F8"),"CLIENT REQUEST"),(168,86,HexColor("#E8F7FC"),"SERVER-DERIVED EFFECTIVE SCOPE"),(112,58,HexColor("#EAF9F4"),"BOUNDED QUERY ADAPTERS")]
    for rx,ry,c,label in rings:
        d.add(Rect(cx-rx,cy-ry,2*rx,2*ry,rx=18,ry=18,fillColor=c,strokeColor=LINE))
        d.add(String(cx,cy+ry-17,label,fontName="Helvetica-Bold",fontSize=7,fillColor=SLATE,textAnchor="middle"))
    d.add(Rect(190,98,100,60,rx=12,ry=12,fillColor=NAVY,strokeColor=CYAN,strokeWidth=1.4))
    d.add(String(cx,135,"AUTHORIZED",fontName="Helvetica-Bold",fontSize=9,fillColor=WHITE,textAnchor="middle"))
    d.add(String(cx,118,"EVIDENCE",fontName="Helvetica-Bold",fontSize=9,fillColor=WHITE,textAnchor="middle"))
    labels=[(26,224,"Non-enumerating\n404 responses",RED),(350,224,"Parameterized SQL\nallowlisted PromQL",BLUE),(26,20,"Mask before\npersistence",AMBER),(350,20,"Audit metadata\n+ hard limits",MINT)]
    for x,y,label,c in labels:
        node(d,x,y,104,42,label,WHITE,c,c,7)
    return d


def ai_boundary_diagram():
    d=Drawing(480,190)
    node(d,15,70,125,70,"Bounded deterministic\nevidence package",HexColor("#EAF8FD"),CYAN,INK,8)
    node(d,178,70,125,70,"Replaceable\nAiProvider contract",HexColor("#FFF5E8"),AMBER,INK,8)
    node(d,340,70,125,70,"Structured result\nwith valid citations",HexColor("#EAF9F4"),MINT,INK,8)
    arrow(d,140,105,178,105,CYAN); arrow(d,303,105,340,105,AMBER)
    d.add(Rect(100,15,280,30,rx=14,ry=14,fillColor=HexColor("#FCECEF"),strokeColor=RED))
    d.add(String(240,26,"v0.7.0: deterministic stub - no external LLM call",fontName="Helvetica-Bold",fontSize=8.5,fillColor=RED,textAnchor="middle"))
    return d


def table(data, widths, header=True, font_size=7.6):
    rows=[]
    for r,row in enumerate(data):
        rows.append([P(str(v), "Small" if r or not header else "CardTitle") for v in row])
    t=Table(rows,colWidths=widths,repeatRows=1 if header else 0,hAlign="LEFT")
    cmds=[("GRID",(0,0),(-1,-1),0.45,LINE),("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),6),("RIGHTPADDING",(0,0),(-1,-1),6),("TOPPADDING",(0,0),(-1,-1),6),("BOTTOMPADDING",(0,0),(-1,-1),6)]
    if header:
        cmds += [("BACKGROUND",(0,0),(-1,0),NAVY),("TEXTCOLOR",(0,0),(-1,0),WHITE)]
        for c in range(len(data[0])): rows[0][c].style.textColor=WHITE
    for r in range(1 if header else 0,len(rows)):
        if r%2==0: cmds.append(("BACKGROUND",(0,r),(-1,r),HexColor("#F8FBFD")))
    t.setStyle(TableStyle(cmds)); return t


def footer(canvas, doc):
    if doc.page == 1:
        return
    canvas.saveState()
    canvas.setStrokeColor(LINE); canvas.setLineWidth(0.5); canvas.line(20*mm,14*mm,PAGE_W-20*mm,14*mm)
    canvas.setFont("Helvetica-Bold",7); canvas.setFillColor(MUTED); canvas.drawString(20*mm,9*mm,"LOG-A-TRON  /  ARCHITECTURE BRIEF  /  v0.7.0")
    canvas.setFont("Helvetica",7); canvas.drawRightString(PAGE_W-20*mm,9*mm,str(doc.page-1))
    canvas.restoreState()


def cover(canvas, doc):
    canvas.saveState()
    if HERO.exists():
        canvas.drawImage(str(HERO),0,0,width=PAGE_W,height=PAGE_H,mask="auto",preserveAspectRatio=False)
    else:
        canvas.setFillColor(NAVY); canvas.rect(0,0,PAGE_W,PAGE_H,fill=1,stroke=0)
    canvas.setFillColor(colors.Color(0.015,0.04,0.08,alpha=0.74)); canvas.rect(0,0,PAGE_W,PAGE_H,fill=1,stroke=0)
    canvas.setFillColor(CYAN); canvas.roundRect(24*mm,244*mm,42*mm,7*mm,3.5*mm,fill=1,stroke=0)
    canvas.setFillColor(NAVY); canvas.setFont("Helvetica-Bold",8); canvas.drawCentredString(45*mm,246.4*mm,"ARCHITECTURE BRIEF")
    canvas.setFillColor(WHITE); canvas.setFont("Helvetica-Bold",31); canvas.drawString(24*mm,211*mm,"LOG-A-TRON")
    canvas.setFillColor(CYAN); canvas.setFont("Helvetica-Bold",19); canvas.drawString(24*mm,193*mm,"Evidence before assumptions")
    canvas.setFillColor(HexColor("#D4E6F2")); canvas.setFont("Helvetica",12)
    lines=["An architectural guide to durable telemetry ingestion,", "authorization-scoped exploration, and deterministic", "root-cause investigation across logs, traces, and metrics."]
    for i,line in enumerate(lines): canvas.drawString(24*mm,(170-i*8)*mm,line)
    canvas.setStrokeColor(HexColor("#3B637F")); canvas.line(24*mm,111*mm,115*mm,111*mm)
    canvas.setFillColor(WHITE); canvas.setFont("Helvetica-Bold",9); canvas.drawString(24*mm,98*mm,"OPEN SOURCE  /  APACHE 2.0  /  v0.7.0")
    canvas.setFillColor(HexColor("#A9C0D2")); canvas.setFont("Helvetica",8.5); canvas.drawString(24*mm,88*mm,"Phases 1-7 verified  |  Local Docker Compose evaluation stack")
    canvas.setFillColor(HexColor("#8BA4B8")); canvas.setFont("Helvetica",8); canvas.drawString(24*mm,24*mm,"github.com/kartnagrale/log-a-tron")
    canvas.restoreState()


def build_story():
    story=[Spacer(1,250*mm),PageBreak()]

    # 1 Executive overview
    story += title_block("01 / Executive overview","A trustworthy path from telemetry to cause","LOG-A-TRON is an open-source observability and investigation foundation. It centralizes signals, preserves authorization, and reconstructs deterministic evidence so incident responders can distinguish an initiating failure from everything it triggered.")
    story += [P("The architectural stance", "H2x"), P("The platform favors explicit contracts, bounded queries, durable delivery, and inspectable results over hidden automation. Logs are canonicalized and masked before storage. Searches are authorization-scoped before execution. Investigations correlate only bounded evidence and retain provenance, confidence, and uncertainty.")]
    cards=[[card("Evidence-driven", "Every conclusion links back to concrete log, span, or metric references.", CYAN), card("Deterministic first", "The timeline and root-cause candidate remain available even if an AI provider fails.", AMBER)], [card("Secure by construction", "Server-derived scope is applied before querying ClickHouse, Tempo, or Prometheus.", MINT), card("Replaceable boundaries", "Storage, telemetry, identity, and AI providers sit behind explicit adapters and contracts.", BLUE)]]
    story += [Table(cards,colWidths=[84*mm,84*mm],hAlign="LEFT",style=[("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(-1,-1),5),("TOPPADDING",(0,0),(-1,-1),4),("BOTTOMPADDING",(0,0),(-1,-1),4)]),Spacer(1,8),P("A permitted engineer can select project, environment, service, and time; search telemetry across distributed services; follow identifiers and traces; and build a defensible investigation without knowing physical log paths.","Quote"),P("Current status", "H2x"),P("The v0.7.0 repository is a development and evaluation foundation. It includes durable ingestion, scoped search, distributed tracing, metrics correlation, deterministic root-cause analysis, and an Angular investigation UI. It does not claim production readiness, a hosted service, RAG, incident workflows, alerting, or a real external AI integration."),PageBreak()]

    # 2 Context
    story += title_block("02 / System context","Who interacts with the platform","LOG-A-TRON sits between distributed applications, authorized operational users, identity infrastructure, and platform administration. Its job is to turn many telemetry sources into one governed investigation surface.")
    story += [system_context_diagram(),Spacer(1,8),P("Primary actors", "H2x")]
    actor_data=[["Actor","What they contribute","What they receive"],["Applications and hosts","Structured or plaintext logs plus OTLP logs, traces, and metrics.","A standard collection path with host and service context."],["Engineer or support user","A bounded scope, time range, identifier, or structured filter.","Search results, trace waterfalls, metric context, and evidence-backed investigations."],["Platform or project administrator","Catalog, sources, memberships, environment grants, and policy.","Audited administration with clear resource ownership."],["OIDC provider","Signed identity tokens in non-development environments.","A standard OAuth2/OIDC resource-server boundary."]]
    story += [table(actor_data,[34*mm,72*mm,62*mm]),Spacer(1,8),P("Design principle", "H2x"),P("User-supplied project, environment, service, and server identifiers are filters - never proof of access. The control plane resolves the authenticated principal into an immutable effective scope and intersects every request with that scope before any telemetry backend is queried."),PageBreak()]

    # 3 runtime
    story += title_block("03 / Runtime architecture","Four planes with explicit responsibilities","The local stack is deliberately composed from specialized systems. Each plane owns one kind of responsibility, which keeps analytical data, control metadata, ingestion pressure, and user workflows from collapsing into a single database or service.")
    story += [runtime_architecture_diagram(),Spacer(1,4)]
    plane_data=[["Plane","Core components","Responsibility"],["Collection","OTel agent and gateway","Tail files, accept OTLP, checkpoint, batch, redact, enrich, and route."],["Ingestion","Kafka and log processor","Buffer durably; parse, normalize, mask, validate, fingerprint, and batch."],["Data","ClickHouse, Tempo, Prometheus, PostgreSQL","Store logs, traces, metrics, and transactional control-plane metadata in purpose-built systems."],["Product","Angular UI and Spring Boot API","Authenticate, authorize, orchestrate bounded queries, audit, and present investigations."],["Investigation","Deterministic engine and AiProvider boundary","Order evidence, identify earliest supported failure, classify symptoms, and create a bounded result."]]
    story += [table(plane_data,[26*mm,55*mm,87*mm]),Spacer(1,6),P("Why a modular monolith?", "H2x"),P("The Spring Boot control plane deploys catalog, authorization, administration, audit, search, trace, and investigation modules together. Package and architecture rules preserve boundaries without paying premature microservice coordination costs. The log processor remains separately deployable because Kafka consumption, backpressure, failure semantics, and scaling differ materially from API traffic."),PageBreak()]

    # 4 ingestion
    story += title_block("04 / Telemetry ingestion","Durable, protected, and replayable","The ingestion path is at-least-once by design. It explicitly acknowledges that duplicates can occur and handles them with stable identifiers, fingerprints, source offsets, and eventual collapse rather than claiming end-to-end exactly-once delivery.")
    story += [ingestion_flow_diagram(),Spacer(1,4),P("Canonical processing order", "H2x")]
    steps=[["Stage","What happens","Failure behavior"],["Collect","File rotation and multiline handling, persistent checkpoints, host/source attributes, OTLP batching and retry.","Agent and gateway queues absorb temporary outages."],["Buffer","Gateway publishes versioned envelopes to Kafka logs.raw.v1.","Kafka preserves a durable replay window."],["Process","Parse JSON/plaintext, normalize time and severity, resolve metadata, mask, validate, and fingerprint.","Transient failures retry; permanent failures become redacted DLQ records."],["Store","Insert bounded batches into ClickHouse and commit offsets only after success or durable DLQ acknowledgement.","Duplicate event IDs collapse in storage/query semantics."],["Query","Compile structured criteria into parameterized, time-bounded, scope-aware ClickHouse queries.","Limits constrain windows, bytes, rows, and concurrency."]]
    story += [table(steps,[22*mm,88*mm,58*mm]),Spacer(1,6),P("Protection is authoritative before persistence", "H2x"),P("Gateway redaction provides defense in depth. Processor masking is authoritative before ClickHouse persistence or fan-out. Raw unmasked payloads are not retained, and even parsing failures are redacted before entering the dead-letter topic."),PageBreak()]

    # 5 data
    story += title_block("05 / Data and contract architecture","Use each store for what it is good at","The platform separates high-volume analytical events from transactional configuration. Versioned contracts connect the components and make compatibility visible.")
    store_data=[["Store","Owns","Does not own"],["ClickHouse","Canonical structured logs, typed filter columns, timestamps, identifiers, attributes, fingerprints.","User identity, grants, mutable administration, or raw secrets."],["PostgreSQL","Companies, projects, environments, services, sources, users, grants, saved searches, audit and investigation metadata.","Raw logs, trace bodies, or metric samples."],["Grafana Tempo","Distributed traces and span relationships received over OTLP.","Authorization policy or log storage."],["Prometheus","Allowlisted operational and application metric series used in bounded correlation.","Business identifiers as labels or arbitrary user PromQL."],["Kafka","Short-lived durable transport across raw, enriched, error, and dead-letter topics.","Permanent system of record."]]
    story += [table(store_data,[29*mm,76*mm,63*mm]),Spacer(1,9),P("Canonical log event", "H2x"),P("The versioned event contract carries UTC timestamps; authoritative project, environment, service, server, and instance IDs; trace and business-correlation identifiers; severity; logger and thread; message and exception data; controlled attributes; masking status; fingerprint; collector; and source offset."),Spacer(1,4)]
    key_data=[["Contract rule","Architectural effect"],["Metadata UUIDs are authoritative","Readable names may be denormalized, but authorization and joins use stable IDs."],["eventId is retained across retries","Retry does not invent a new logical event."],["Attributes are controlled extension points","Frequently queried fields can graduate into typed columns through migrations."],["Stack traces and event bodies are capped","One oversized event cannot dominate ingestion or evidence packages."],["Schema versions are explicit","Incompatible contracts can coexist during migrations."]]
    story += [table(key_data,[54*mm,114*mm]),Spacer(1,8),P("Storage principle", "Quote"),P("A datastore is selected for its workload, not convenience: ClickHouse for analytical logs, PostgreSQL for constrained metadata, Tempo for traces, Prometheus for metrics, and Kafka for transport."),PageBreak()]

    # 6 exploration
    story += title_block("06 / Exploration and correlation","Keep context intact from search to trace","The Angular interface exposes a governed investigation workflow while the Spring API enforces every security and query boundary. The browser never receives a direct database capability.")
    experience=[["Capability","User experience","Backend control"],["Log Explorer","Project/environment/service/severity/time filters, details, JSON, surrounding context.","Structured criteria, bounded windows, parameterized ClickHouse SQL."],["Correlation","Follow trace, request, correlation, transaction, order token, or auction identifiers.","Supported identifier types only; authorized scope applied before lookup."],["Saved search","Reusable operational views.","Ownership and scope stored in PostgreSQL."],["Live tail","Short-lived Server-Sent Events stream.","Connection count, duration, result size, and scope are bounded."],["Trace detail","Waterfall and log-to-trace navigation.","Tempo is queried through an authorization-aware adapter."],["Investigation","Timeline, root-cause candidate, confidence, symptoms, evidence links, and warnings.","Logs, traces, and metrics are projected into a bounded evidence package."]]
    story += [table(experience,[30*mm,67*mm,71*mm]),Spacer(1,10),P("Identifier strategy", "H2x"),P("TRACE_ID, REQUEST_ID, CORRELATION_ID, TRANSACTION_ID, ORDER_TOKEN, AUCTION_ID, and authorized scope investigations are supported. Business identifiers are useful for correlation but are never promoted into unbounded metric labels. Investigation windows must be positive and no longer than 24 hours."),Spacer(1,6),P("Frontend architecture", "H2x"),P("Angular uses standalone components, strict TypeScript, lazy feature routes, signals for local state, RxJS for streams, and a shared scope picker. Feature areas cover overview, projects, logs, traces, investigations, administration, authentication, and reusable API models."),PageBreak()]

    # 7 investigation
    story += title_block("07 / Deterministic investigation","Reconstruct the causal sequence before explaining it","The engine correlates authorized logs, Tempo spans, and allowlisted Prometheus series. It orders evidence by event time, collapses duplicates, groups exception fingerprints, builds dependency edges, and reports missing identifiers or ordering uncertainty explicitly.")
    story += [investigation_diagram(),Spacer(1,5),P("What the engine produces", "H2x")]
    outputs=[["Output","Meaning"],["Timeline","An ordered, deduplicated sequence across logs, spans, and metric windows."],["Root-cause candidate","The earliest supported failure, not merely the loudest or latest alert."],["Downstream symptoms","Retries, duplicate rejections, lag, or secondary exceptions retained as evidence."],["Evidence references","Stable citations linking every claim to items in the package."],["Quality metadata","Confidence states, warnings, coverage gaps, and uncertainty."],["Dependency and exception views","Service flow plus normalized exception grouping for pattern clarity."]]
    story += [table(outputs,[43*mm,125*mm]),Spacer(1,7),P("Why deterministic first?", "Quote"),P("A deterministic result is reproducible, testable, inspectable, and available even if an optional AI provider is unavailable or rejects malformed output."),PageBreak()]

    # 8 security
    story += title_block("08 / Security and trust boundaries","Authorization is part of the query plan","Security controls operate before data access, during ingestion, at query compilation, and when evidence leaves the deterministic engine. The design assumes that browsers, identifiers, and log text are untrusted.")
    story += [security_diagram(),Spacer(1,4)]
    controls=[["Boundary","Control"],["Identity","OIDC/JWT resource-server security; profile-isolated signed development tokens only for local use."],["Authorization","Memberships, roles, and environment grants resolve to server-derived EffectiveScope."],["Enumeration resistance","Existing but unauthorized resource IDs use the same 404 contract as missing resources."],["Query safety","No arbitrary SQL; PromQL comes only from sealed, allowlisted, project-scoped operations."],["Data protection","Baseline gateway redaction plus authoritative processor masking before persistence."],["Resource governance","Time windows, bytes, rows, concurrency, live-tail duration, and evidence package size are bounded."],["Auditability","Search, administration, denials, and investigation actions produce metadata without copying raw telemetry."],["Prompt injection","Instruction-like log text remains untrusted evidence data and cannot become executable intent."]]
    story += [table(controls,[39*mm,129*mm]),Spacer(1,6),P("Production note", "H2x"),P("The Compose deployment is for local evaluation. Production still requires external secrets, TLS and Kafka SASL, replicated stores, production OIDC, capacity planning, organization-specific masking, backup/restore, and operational ownership.", "Small"),PageBreak()]

    # 9 AI
    story += title_block("09 / AI boundary","Replaceable, bounded, and non-authoritative","The AI contract exists to explain already-constructed evidence. It is not allowed to expand scope, choose arbitrary queries, or replace the deterministic result.")
    story += [ai_boundary_diagram(),Spacer(1,8)]
    ai_data=[["Guarantee","Behavior"],["Bounded input","Only configured top-ranked evidence references and a deterministic projection enter the provider."],["Structured output","The provider must return the RCA contract, not free-form operational commands."],["Citation validation","Every cited evidence ID must exist in the supplied package."],["Failure isolation","Exceptions, outages, malformed output, or unsupported citations become UNAVAILABLE_OR_REJECTED."],["Deterministic continuity","The deterministic investigation remains returned and persisted even when the provider fails."],["No privilege expansion","The provider cannot broaden authorization, execute SQL/PromQL, or reinterpret log text as instructions."]]
    story += [table(ai_data,[45*mm,123*mm]),Spacer(1,10),P("What ships today", "H2x"),P("v0.7.0 includes DeterministicStubAiProvider, a deterministic template renderer for development and contract testing. It does not call an external LLM or AI service and must not be presented as model-generated analysis."),Spacer(1,6),P("What is deferred", "H2x"),P("A real external provider, RAG, governed knowledge retrieval, and provider credential workflows are not implemented. Adding credentials alone does not activate an external model."),PageBreak()]

    # 10 deployment
    story += title_block("10 / Deployment and operations","A complete local evaluation stack","Docker Compose assembles the platform and its dependencies using public images or local builds. Persistent volumes retain state for PostgreSQL, Kafka, ClickHouse, Tempo, Prometheus, and OpenTelemetry checkpoints.")
    deploy=[["Service","Role","Scaling or production consideration"],["web-ui","Angular application served through its container.","CDN or web replicas; production TLS and CSP."],["platform-api","REST/SSE control plane and investigation orchestration.","Stateless horizontal replicas; external OIDC and secrets."],["log-processor","Kafka consumer and ClickHouse writer.","Scale by partitions; monitor batch latency, retries, and DLQ."],["otel-agent / gateway","Host collection and central routing/policy.","One agent per host/node; load-balanced gateways with durable queues."],["Kafka","Durable ingestion buffer.","Replication, TLS/SASL, partition planning, and retention."],["ClickHouse","Structured log analytics.","Replicated storage, retention policy, capacity and query governance."],["Tempo / Prometheus","Trace and metric correlation.","Retention, object storage where applicable, and HA strategy."],["PostgreSQL","Control metadata, authorization, audit, investigations.","Backups, HA, schema migration, and secret rotation."]]
    story += [table(deploy,[35*mm,64*mm,69*mm]),Spacer(1,8),P("Local evaluation path", "H2x"),bullet("Copy .env.example to .env and replace placeholder passwords plus DEV_JWT_SECRET."),bullet("Run docker compose config --quiet, then docker compose up --build -d."),bullet("Use the deterministic generator to emit sanitized log, trace, and metric scenarios."),bullet("Open the Angular UI at localhost:4200 and the investigation view at /investigations."),bullet("Inspect readiness endpoints, processor metrics, Prometheus, Kafka topics, and ClickHouse rows."),Spacer(1,4),P("Operational signals", "H2x"),P("The ingestion runbook emphasizes health probes, consumer lag, processing outcomes, batch behavior, dead-letter counts, and persistent OTel queue state. These signals are central to operating the observability platform itself."),PageBreak()]

    # 11 rationale
    story += title_block("11 / Technology and decision rationale","Specialized tools, conservative boundaries","The stack uses mature components for their strongest workloads and keeps extraction paths open without prematurely distributing the control plane.")
    decisions=[["Decision","Why it fits","Trade-off accepted"],["Spring Boot modular monolith","Transactional control plane with explicit package boundaries and simple deployment.","Modules scale together until measured needs justify extraction."],["Separate log processor","Ingestion has independent Kafka, scaling, and failure semantics.","Shared versioned contracts must stay compatible."],["OpenTelemetry Collector","One standard agent/gateway path for logs, traces, and metrics.","Collector configuration and extension versions require operational discipline."],["Kafka buffer","Durable at-least-once delivery and backpressure isolation.","Duplicates and partition-order limits are handled explicitly."],["ClickHouse for logs","Columnar compression and bounded analytical search.","Not used for transactional metadata."],["PostgreSQL control plane","Constraints, transactions, Flyway migrations, and auditable relationships.","Not used for raw event analytics."],["Tempo and Prometheus","OTLP-compatible trace storage and allowlisted metric correlation.","Authorization is enforced by API adapters, not delegated to these stores."],["Deterministic RCA first","Reproducible conclusions and testable evidence selection.","Novel inference is intentionally constrained until governed providers exist."]]
    story += [table(decisions,[43*mm,70*mm,55*mm]),Spacer(1,9),P("Accepted architecture decisions", "H2x"),P("The repository records four explicit ADRs: modular-monolith control plane, authorization-derived query scope, PostgreSQL for the control plane, and profile-isolated development authentication. Together they express the project's recurring preference for strong boundaries over accidental convenience."),PageBreak()]

    # 12 quality and status
    story += title_block("12 / Quality, limits, and next steps","Know what is verified - and what is not","The current checkpoint is intentionally candid. It demonstrates the architecture and core behavior locally, while leaving production hardening and later product phases explicit.")
    status=[["Verified in v0.7.0","Planned - not implemented"],["Durable log ingestion and canonicalization","Real external AI provider integration"],["Authorization-scoped search and saved searches","RAG and governed knowledge retrieval"],["Trace lookup and waterfall","Incident workflow and collaboration"],["Prometheus metrics correlation","Alert evaluation and notification providers"],["Deterministic timeline and root-cause analysis","Retention archive and restore enhancements"],["Bounded replaceable AiProvider with deterministic stub","Production deployment and operational hardening"],["61 backend tests, frontend unit tests, Angular production build","Performance, scale, availability, and production security validation"]]
    story += [table(status,[84*mm,84*mm]),Spacer(1,10),P("Evaluation checklist", "H2x"),bullet("Verify a permitted user can search only authorized project and environment combinations."),bullet("Follow a synthetic trace from log detail into a trace waterfall and related logs."),bullet("Investigate demo-order-42 and confirm database-pool saturation precedes timeout, retry, duplicate rejection, and lag."),bullet("Confirm downstream symptoms retain evidence links without replacing the initiating failure."),bullet("Exercise an unauthorized scope and confirm a non-enumerating response before any telemetry query."),bullet("Inspect an AI-stub result and confirm every citation exists in the bounded evidence package."),bullet("Review masking, queue persistence, DLQ behavior, query limits, and audit metadata."),Spacer(1,8),P("The measure of success", "Quote"),P("LOG-A-TRON succeeds when a responder can move from fragmented telemetry to an inspectable causal account - without sacrificing authorization, provenance, or operational honesty."),PageBreak()]

    # 13 appendix
    story += title_block("Appendix / Repository guide","Where to go deeper","This brief synthesizes the current public repository. The following files are the authoritative starting points for implementation detail and operational verification.")
    refs=[["Path","Purpose"],["README.md","Public overview, supported features, AI status, quick start, verification, and roadmap."],["ARCHITECTURE.md","Long-form architecture, data contracts, module boundaries, schemas, query model, and operating assumptions."],["docs/INGESTION.md","Pipeline runbook, Kafka topics, failure semantics, ClickHouse verification, metrics, and probes."],["docs/INVESTIGATIONS.md","Runtime investigation flow, supported identifiers, metric boundary, deterministic scenario, and AI contract."],["docs/adr/*.md","Accepted decisions for modularity, authorization scope, PostgreSQL ownership, and development authentication."],["SETUP.md","Compose startup, synthetic scenarios, inspection commands, outage drill, and reset workflow."],["DEVELOPMENT.md","Local development, builds, tests, and contribution-oriented workflows."],["backend/","Spring Boot API, processor, shared contracts, migrations, adapters, and tests."],["frontend/","Angular UI for overview, projects, logs, traces, investigations, administration, and login."],["infrastructure/","OpenTelemetry, ClickHouse, Tempo, and Prometheus configuration."],["tools/log-generator/","Deterministic synthetic telemetry generator."]]
    story += [table(refs,[43*mm,125*mm]),Spacer(1,12),P("Project", "H2x"),P('<link href="https://github.com/kartnagrale/log-a-tron" color="#00AEEB">github.com/kartnagrale/log-a-tron</link>'),Spacer(1,6),P("Document scope", "H2x"),P("Prepared from the v0.7.0 public repository state. This is an architectural explanation, not a production-readiness certification or performance benchmark."),Spacer(1,20),HRFlowable(width="100%",thickness=0.7,color=LINE),Spacer(1,9),P("LOG-A-TRON", "PageTitle"),P("Evidence-driven observability and root-cause investigation across logs, traces, and metrics.", "Lead")]
    return story


def main():
    OUT.parent.mkdir(parents=True,exist_ok=True)
    frame=Frame(20*mm,18*mm,PAGE_W-40*mm,PAGE_H-34*mm,id="normal")
    doc=BaseDocTemplate(str(OUT),pagesize=A4,leftMargin=20*mm,rightMargin=20*mm,topMargin=18*mm,bottomMargin=18*mm,title="LOG-A-TRON Architecture Brief",author="LOG-A-TRON Project")
    doc.addPageTemplates(PageTemplate(id="main",frames=[frame],onPage=footer))
    # The first page uses the same frame but its canvas is painted as a full-bleed cover.
    original_before=doc.beforePage
    def before_page():
        original_before()
        if doc.page==1:
            cover(doc.canv,doc)
    doc.beforePage=before_page
    doc.build(build_story())
    print(OUT)


if __name__=="__main__":
    main()

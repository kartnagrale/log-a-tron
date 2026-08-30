import argparse, datetime as dt, json, os, random, time, uuid, urllib.request

SOURCES = [
 {"project":"Demo Marketplace","projectId":"10000000-0000-0000-0000-000000000001","environmentId":"20000000-0000-0000-0000-000000000003","serverId":"30000000-0000-0000-0000-000000000001","serviceId":"40000000-0000-0000-0000-000000000001","instanceId":"50000000-0000-0000-0000-000000000001","sourceId":"60000000-0000-0000-0000-000000000001","service":"order","profile":"JSON"},
 {"project":"Demo Marketplace","projectId":"10000000-0000-0000-0000-000000000001","environmentId":"20000000-0000-0000-0000-000000000003","serverId":"30000000-0000-0000-0000-000000000002","serviceId":"40000000-0000-0000-0000-000000000005","instanceId":"50000000-0000-0000-0000-000000000005","sourceId":"60000000-0000-0000-0000-000000000005","service":"notification","profile":"PLAINTEXT"},
 {"project":"Demo Marketplace","projectId":"10000000-0000-0000-0000-000000000001","environmentId":"20000000-0000-0000-0000-000000000003","serverId":"30000000-0000-0000-0000-000000000003","serviceId":"40000000-0000-0000-0000-000000000008","instanceId":"50000000-0000-0000-0000-000000000008","sourceId":"60000000-0000-0000-0000-000000000003","service":"settlement","profile":"JSON"},
 {"project":"Demo Payments","projectId":"10000000-0000-0000-0000-000000000002","environmentId":"20000000-0000-0000-0000-000000000005","serverId":"30000000-0000-0000-0000-000000000006","serviceId":"40000000-0000-0000-0000-000000000010","instanceId":"50000000-0000-0000-0000-000000000011","sourceId":"60000000-0000-0000-0000-000000000006","service":"fulfillment","profile":"JSON"},
 {"project":"Demo Analytics","projectId":"10000000-0000-0000-0000-000000000003","environmentId":"20000000-0000-0000-0000-000000000007","serverId":"30000000-0000-0000-0000-000000000007","serviceId":"40000000-0000-0000-0000-000000000011","instanceId":"50000000-0000-0000-0000-000000000012","sourceId":"60000000-0000-0000-0000-000000000007","service":"analytics","profile":"JSON"},
 {"project":"Demo Support","projectId":"10000000-0000-0000-0000-000000000004","environmentId":"20000000-0000-0000-0000-000000000008","serverId":"30000000-0000-0000-0000-000000000008","serviceId":"40000000-0000-0000-0000-000000000012","instanceId":"50000000-0000-0000-0000-000000000013","sourceId":"60000000-0000-0000-0000-000000000008","service":"case","profile":"PLAINTEXT"},
]
SCENARIOS=["normal","db-pool-exhaustion","kafka-retry","http-timeout","null-pointer","malformed","missing-identifier","duplicate","out-of-order","sensitive"]

def iso(t): return t.astimezone(dt.timezone.utc).isoformat(timespec="milliseconds").replace("+00:00","Z")
def payload(source, scenario, timestamp, i):
    level="INFO"; message=f"{scenario} event {i} completed"; stack=None
    if scenario=="db-pool-exhaustion": level="ERROR"; message="HikariPool connection timeout during settlement"; stack="java.sql.SQLTimeoutException: connection timeout\n    at com.example.SettlementService.settle(SettlementService.java:84)\nCaused by: java.util.concurrent.TimeoutException: pool exhausted"
    elif scenario=="kafka-retry": level="ERROR"; message="Kafka retry caused duplicate key for orderToken=demo-order-42"
    elif scenario=="http-timeout": level="WARN"; message="HTTP call to downstream service timed out requestId=req-timeout"
    elif scenario=="null-pointer": level="ERROR"; message="Failed to process auction"; stack="java.lang.NullPointerException: auction was null\n    at com.example.AuctionService.process(AuctionService.java:42)\n    at com.example.Handler.handle(Handler.java:18)"
    elif scenario=="malformed": return "{not-json Authorization: Bearer DLQ_SECRET password=dlq-secret"
    elif scenario=="missing-identifier": message="Event intentionally has no correlation identifiers"
    elif scenario=="out-of-order": timestamp-=dt.timedelta(minutes=5); message="Delayed event arrived out of order"
    elif scenario=="sensitive": level="WARN"; message="Authorization: Bearer SECRET_TOKEN password=supersecret apiKey=SECRET123 jdbc:postgresql://user:dbpass@db.local.test/app"
    trace=None if scenario=="missing-identifier" else f"{i+1:032x}"[-32:]
    values={"timestamp":iso(timestamp),"level":level,"logger":f"com.example.{source['service'].title()}Service","thread":"worker-1","message":message,"traceId":trace,"spanId":f"{i+1:016x}"[-16:],"requestId":None if not trace else f"req-{i}","correlationId":None if not trace else f"corr-{i//4}","transactionId":f"txn-{i}","orderToken":f"demo-order-{i}","auctionId":f"auction-{i%7}","stackTrace":stack,"exceptionType":stack.split(':',1)[0] if stack else None}
    values={k:v for k,v in values.items() if v is not None}
    if source["profile"]=="JSON": return json.dumps(values,separators=(",",":"),ensure_ascii=False)
    text=f"{timestamp.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]} {level} [worker-1] com.example.{source['service'].title()}Service - {message}"
    return text+("\n"+stack if stack else "")

def envelope(source,scenario,seed,i,event_id=None):
    now=dt.datetime.now(dt.timezone.utc); event_id=event_id or str(uuid.uuid5(uuid.NAMESPACE_URL,f"logatron:{seed}:{scenario}:{source['sourceId']}:{i}"))
    return {"schemaVersion":1,"eventId":event_id,"observedTimestamp":iso(now),"collector":{"collectorId":"otel-agent-local","sourceOffset":f"seed={seed};scenario={scenario};index={i}"},"resource":{"projectId":source["projectId"],"environmentId":source["environmentId"],"serverId":source["serverId"],"serviceId":source["serviceId"],"serviceInstanceId":source["instanceId"]},"source":{"logSourceId":source["sourceId"],"logFile":f"/logs/{source['service']}/application.log","logType":"application","parserProfile":source["profile"]},"payload":payload(source,scenario,now,i),"attributes":{"scenario":scenario,"seed":seed}}

def otlp_attr(key,value):
    return {"key":key,"value":{"stringValue":str(value)}}

def correlated_trace_data(seed,failed):
    suffix="failed" if failed else "success";trace_id=uuid.uuid5(uuid.NAMESPACE_URL,f"logatron:trace:{seed}:{suffix}").hex
    base=time.time_ns()-8_000_000_000 if failed else time.time_ns()-1_000_000_000
    if failed:
        spans=[("api-gateway","POST /api/bids",0,5_500_000_000,"SPAN_KIND_SERVER",False,{}),("bid-service","placeBid",10_000_000,5_400_000_000,"SPAN_KIND_SERVER",False,{}),("kafka-producer","publish auction.bid",50_000_000,100_000_000,"SPAN_KIND_PRODUCER",False,{"messaging.system":"kafka","messaging.destination.name":"auction.bid","messaging.operation":"publish"}),("kafka-consumer","process auction.bid",110_000_000,5_300_000_000,"SPAN_KIND_CONSUMER",False,{"messaging.system":"kafka","messaging.destination.name":"auction.bid","messaging.operation":"process"}),("settlement-service","settleBid",200_000_000,5_200_000_000,"SPAN_KIND_INTERNAL",True,{"error.type":"java.sql.SQLTimeoutException"}),("postgresql","INSERT settlement",300_000_000,5_100_000_000,"SPAN_KIND_CLIENT",True,{"db.system":"postgresql","db.operation.name":"INSERT","error.type":"java.sql.SQLTimeoutException"})]
    else:
        spans=[("api-gateway","POST /api/bids",0,300_000_000,"SPAN_KIND_SERVER",False,{}),("bid-service","placeBid",10_000_000,260_000_000,"SPAN_KIND_SERVER",False,{}),("kafka-producer","publish auction.bid",40_000_000,75_000_000,"SPAN_KIND_PRODUCER",False,{"messaging.system":"kafka","messaging.destination.name":"auction.bid","messaging.operation":"publish"}),("kafka-consumer","process auction.bid",80_000_000,240_000_000,"SPAN_KIND_CONSUMER",False,{"messaging.system":"kafka","messaging.destination.name":"auction.bid","messaging.operation":"process"}),("settlement-service","settleBid",100_000_000,220_000_000,"SPAN_KIND_INTERNAL",False,{}),("postgresql","INSERT settlement",130_000_000,190_000_000,"SPAN_KIND_CLIENT",False,{"db.system":"postgresql","db.operation.name":"INSERT"})]
    resource_spans=[];span_ids=[f"{seed*100+i+1:016x}"[-16:] for i in range(len(spans))]
    for i,(service,operation,start,end,kind,error,attrs) in enumerate(spans):
        span={"traceId":trace_id,"spanId":span_ids[i],"name":operation,"kind":kind,"startTimeUnixNano":str(base+start),"endTimeUnixNano":str(base+end),"attributes":[otlp_attr(k,v) for k,v in attrs.items()],"status":{"code":"STATUS_CODE_ERROR" if error else "STATUS_CODE_OK"}}
        if i: span["parentSpanId"]=span_ids[i-1]
        resource={"attributes":[otlp_attr("service.name",service),otlp_attr("service.instance.id",f"{service}-generator"),otlp_attr("deployment.environment.name","PROD"),otlp_attr("logatron.project.id",SOURCES[0]["projectId"])]}
        resource_spans.append({"resource":resource,"scopeSpans":[{"scope":{"name":"logatron.generator","version":"1.0"},"spans":[span]}]})
    return trace_id,span_ids,base,resource_spans

def emit_correlated_traces(seed,output,endpoint):
    all_resources=[];trace_ids=[]
    for failed in (False,True):
        trace_id,span_ids,base,resources=correlated_trace_data(seed,failed);all_resources.extend(resources);trace_ids.append(trace_id)
        for index,source in enumerate((SOURCES[0],SOURCES[2])):
            record=envelope(source,"db-pool-exhaustion" if failed and index else "normal",seed,10_000+(100 if failed else 0)+index)
            body=json.loads(record["payload"]);body.update({"timestamp":iso(dt.datetime.fromtimestamp((base+(200_000_000 if index else 10_000_000))/1_000_000_000,dt.timezone.utc)),"traceId":trace_id,"spanId":span_ids[4 if index else 1],"requestId":f"req-trace-{seed}","correlationId":f"corr-trace-{seed}","transactionId":f"txn-trace-{seed}","orderToken":f"order-{seed}","auctionId":f"auction-{seed}"})
            if failed and index: body.update({"level":"ERROR","message":"Settlement PostgreSQL operation timed out","exceptionType":"java.sql.SQLTimeoutException","stackTrace":"java.sql.SQLTimeoutException: statement timed out\n    at com.example.SettlementService.settle(SettlementService.java:84)"})
            record["payload"]=json.dumps(body,separators=(",",":"));record["collector"]["sourceOffset"]+=f";trace={trace_id}"
            directory=os.path.join(output,source["service"]);os.makedirs(directory,exist_ok=True)
            with open(os.path.join(directory,"application.log"),"a",encoding="utf-8",buffering=1) as f:f.write(json.dumps(record,separators=(",",":"),ensure_ascii=False)+"\n")
        if failed:
            for extra,scenario,message,level in ((0,"kafka-retry","Kafka retry scheduled after settlement timeout","WARN"),(1,"duplicate","Duplicate settlement delivery rejected after retry","ERROR")):
                source=SOURCES[0];record=envelope(source,scenario,seed,10_200+extra);body=json.loads(record["payload"]);body.update({"timestamp":iso(dt.datetime.fromtimestamp((base+5_300_000_000+extra*50_000_000)/1_000_000_000,dt.timezone.utc)),"level":level,"message":message,"traceId":trace_id,"spanId":span_ids[3],"requestId":f"req-trace-{seed}","correlationId":f"corr-trace-{seed}","transactionId":f"txn-trace-{seed}","orderToken":f"order-{seed}","auctionId":f"auction-{seed}"});record["payload"]=json.dumps(body,separators=(",",":"));directory=os.path.join(output,source["service"]);os.makedirs(directory,exist_ok=True)
                with open(os.path.join(directory,"application.log"),"a",encoding="utf-8",buffering=1) as f:f.write(json.dumps(record,separators=(",",":"),ensure_ascii=False)+"\n")
    request=urllib.request.Request(endpoint.rstrip("/")+"/v1/traces",data=json.dumps({"resourceSpans":all_resources},separators=(",",":")).encode(),headers={"Content-Type":"application/json"},method="POST")
    with urllib.request.urlopen(request,timeout=10) as response:
        if response.status not in (200,202): raise RuntimeError(f"OTLP trace export failed with HTTP {response.status}")
    return trace_ids

def emit_correlated_metrics(seed,endpoint):
    now=time.time_ns();project=SOURCES[0]["projectId"];environment=SOURCES[0]["environmentId"]
    def point(offset,value,service): return {"timeUnixNano":str(now+offset),"asDouble":value,"attributes":[otlp_attr("project_id",project),otlp_attr("environment_id",environment),otlp_attr("service",service),otlp_attr("scenario","db-pool-exhaustion")]}
    metrics=[
      {"name":"hikaricp.connections.active","unit":"connections","gauge":{"dataPoints":[point(-30_000_000_000,50.0,"settlement-service")] }},
      {"name":"hikaricp.connections.max","unit":"connections","gauge":{"dataPoints":[point(-30_000_000_000,50.0,"settlement-service")] }},
      {"name":"kafka.consumer.fetch.manager.records.lag","unit":"records","gauge":{"dataPoints":[point(-2_000_000_000,125.0,"bid-service")] }}]
    body={"resourceMetrics":[{"resource":{"attributes":[otlp_attr("service.name","logatron-scenario-generator"),otlp_attr("logatron.project.id",project),otlp_attr("deployment.environment.name","PROD")]},"scopeMetrics":[{"scope":{"name":"logatron.generator","version":"1.0"},"metrics":metrics}]}]}
    request=urllib.request.Request(endpoint.rstrip("/")+"/v1/metrics",data=json.dumps(body,separators=(",",":")).encode(),headers={"Content-Type":"application/json"},method="POST")
    with urllib.request.urlopen(request,timeout=10) as response:
        if response.status not in (200,202): raise RuntimeError(f"OTLP metric export failed with HTTP {response.status}")

def main():
    ap=argparse.ArgumentParser();ap.add_argument("--seed",type=int,default=42);ap.add_argument("--scenario",choices=SCENARIOS+["all"],default="all");ap.add_argument("--rate",type=float,default=20);ap.add_argument("--count",type=int,default=80);ap.add_argument("--project");ap.add_argument("--service");ap.add_argument("--output",default="/logs");ap.add_argument("--rotate-every",type=int,default=0,help="rename each active file before every Nth generated event");ap.add_argument("--no-traces",action="store_true");ap.add_argument("--no-metrics",action="store_true");args=ap.parse_args();rng=random.Random(args.seed)
    sources=[s for s in SOURCES if (not args.project or s["project"].lower()==args.project.lower()) and (not args.service or s["service"].lower()==args.service.lower())]
    if not sources: raise SystemExit("No source matches project/service")
    scenarios=SCENARIOS if args.scenario=="all" else [args.scenario]; written=[]
    for i in range(args.count):
        source=sources[i%len(sources)];scenario=scenarios[i%len(scenarios)];record=envelope(source,scenario,args.seed,i)
        if scenario=="duplicate" and written: record=dict(written[-1])
        directory=os.path.join(args.output,source["service"]);os.makedirs(directory,exist_ok=True);path=os.path.join(directory,"application.log")
        if args.rotate_every>0 and i>0 and i%args.rotate_every==0 and os.path.exists(path):
            os.replace(path,os.path.join(directory,f"application.{args.seed}.{i}.log"))
        with open(path,"a",encoding="utf-8",buffering=1) as f:f.write(json.dumps(record,separators=(",",":"),ensure_ascii=False)+"\n")
        written.append(record)
        if args.rate>0:time.sleep(1/args.rate)
    trace_ids=[] if args.no_traces else emit_correlated_traces(args.seed,args.output,os.environ.get("OTEL_EXPORTER_OTLP_ENDPOINT","http://localhost:4318"))
    if not args.no_metrics: emit_correlated_metrics(args.seed,os.environ.get("OTEL_EXPORTER_OTLP_ENDPOINT","http://localhost:4318"))
    correlated_event_count = len(trace_ids) * 2 + (2 if trace_ids else 0)
    print(json.dumps({"generated":len(written)+correlated_event_count,"seed":args.seed,"scenario":args.scenario,"sources":len(sources),"traceIds":trace_ids}))
if __name__=="__main__": main()

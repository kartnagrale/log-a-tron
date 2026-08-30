package com.logatron.tracequery.web;
import java.time.Instant;import java.util.*;
public final class TraceDtos{private TraceDtos(){}public record TraceView(String traceId,List<SpanView> spans,Set<UUID> projectIds){}public record SpanView(String spanId,String parentSpanId,String service,String operation,Instant start,Instant end,long durationMicros,String status,Map<String,String> attributes){}}

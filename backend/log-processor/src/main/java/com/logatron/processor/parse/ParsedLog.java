package com.logatron.processor.parse;
import java.time.Instant;import java.util.Map;
public record ParsedLog(Instant timestamp,String level,String logger,String thread,String message,String exceptionType,String stackTrace,String traceId,String spanId,String traceFlags,String requestId,String correlationId,String transactionId,String orderToken,String auctionId,Map<String,String> attributes){}
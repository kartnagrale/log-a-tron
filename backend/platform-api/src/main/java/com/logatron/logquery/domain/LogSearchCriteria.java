package com.logatron.logquery.domain;

import com.logatron.common.error.ApiException;
import java.time.*;import java.util.*;import java.util.regex.Pattern;

public record LogSearchCriteria(UUID projectId,UUID environmentId,UUID serverId,UUID serviceId,String level,Instant from,Instant to,
 String traceId,String spanId,String requestId,String correlationId,String transactionId,String orderToken,String auctionId,
 String exceptionType,String text,Integer pageSize,Cursor cursor){
 private static final Pattern TRACE=Pattern.compile("[0-9a-fA-F]{32}"),SPAN=Pattern.compile("[0-9a-fA-F]{16}"),LEVEL=Pattern.compile("TRACE|DEBUG|INFO|WARN|ERROR|FATAL|UNKNOWN");
 public Normalized normalize(Clock clock){
  Instant end=to==null?clock.instant():to,start=from==null?end.minus(Duration.ofMinutes(30)):from;
  if(!start.isBefore(end))throw ApiException.invalid("from must be before to.");
  if(Duration.between(start,end).compareTo(Duration.ofHours(24))>0)throw ApiException.invalid("Interactive log searches are limited to 24 hours.");
  int size=pageSize==null?100:pageSize;if(size<1||size>500)throw ApiException.invalid("pageSize must be between 1 and 500.");
  String normalizedLevel=blank(level);if(normalizedLevel!=null){normalizedLevel=normalizedLevel.toUpperCase(Locale.ROOT);if(!LEVEL.matcher(normalizedLevel).matches())throw ApiException.invalid("level is not supported.");}
  String normalizedTrace=bounded(traceId,"traceId",32);if(normalizedTrace!=null&&!TRACE.matcher(normalizedTrace).matches())throw ApiException.invalid("traceId must contain 32 hexadecimal characters.");
  String normalizedSpan=bounded(spanId,"spanId",16);if(normalizedSpan!=null&&!SPAN.matcher(normalizedSpan).matches())throw ApiException.invalid("spanId must contain 16 hexadecimal characters.");
  String term=bounded(text,"text",256);if(term!=null&&term.chars().anyMatch(Character::isISOControl))throw ApiException.invalid("text contains unsupported control characters.");
  return new Normalized(projectId,environmentId,serverId,serviceId,normalizedLevel,start,end,lower(normalizedTrace),lower(normalizedSpan),bounded(requestId,"requestId",200),bounded(correlationId,"correlationId",200),bounded(transactionId,"transactionId",200),bounded(orderToken,"orderToken",200),bounded(auctionId,"auctionId",200),bounded(exceptionType,"exceptionType",300),term,size,cursor);
 }
 private static String lower(String value){return value==null?null:value.toLowerCase(Locale.ROOT);}private static String blank(String value){return value==null||value.isBlank()?null:value.trim();}
 private static String bounded(String value,String name,int max){String v=blank(value);if(v!=null&&v.length()>max)throw ApiException.invalid(name+" is too long.");return v;}
 public record Cursor(Instant timestamp,UUID eventId){public Cursor{if(timestamp==null||eventId==null)throw ApiException.invalid("A cursor requires timestamp and eventId.");}}
 public record Normalized(UUID projectId,UUID environmentId,UUID serverId,UUID serviceId,String level,Instant from,Instant to,String traceId,String spanId,String requestId,String correlationId,String transactionId,String orderToken,String auctionId,String exceptionType,String text,int pageSize,Cursor cursor){}
}

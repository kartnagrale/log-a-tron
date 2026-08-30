package com.logatron.logquery.web;
import com.fasterxml.jackson.databind.JsonNode;import com.logatron.logquery.domain.LogSearchCriteria;import java.time.Instant;import java.util.*;
public final class LogQueryDtos{private LogQueryDtos(){}
 public record LogRow(UUID eventId,Instant timestamp,UUID projectId,String project,UUID environmentId,String environment,UUID serverId,String server,UUID serviceId,String service,String level,String message,String exceptionType,String traceId,String spanId,String requestId,String correlationId,String transactionId,String orderToken,String auctionId){}
 public record LogDetail(UUID eventId,Instant timestamp,Instant observedTimestamp,Instant ingestedAt,UUID projectId,String project,UUID environmentId,String environment,UUID serverId,String server,String hostname,UUID serviceId,String service,String serviceInstance,String level,String logger,String thread,String message,String exceptionType,String stackTrace,String traceId,String spanId,String requestId,String correlationId,String transactionId,String orderToken,String auctionId,Map<String,String> attributes,Map<String,String> resourceAttributes,String logFile,String logType,String dataClassification,boolean maskingApplied){}
 public record SearchPage(List<LogRow> items,LogSearchCriteria.Cursor nextCursor,boolean hasMore){}
 public record ContextRequest(UUID eventId,Integer before,Integer after){}
 public record ContextResponse(LogDetail anchor,List<LogRow> before,List<LogRow> after){}
 public record CorrelateRequest(UUID projectId,UUID environmentId,String identifierType,String value,Instant from,Instant to,Integer pageSize,LogSearchCriteria.Cursor cursor){}
 public record OverviewRequest(UUID projectId,UUID environmentId,UUID serviceId,Instant from,Instant to){}
 public record TrendPoint(Instant bucket,long total,long warn,long error){}
 public record ServiceCount(UUID serviceId,String service,long errors){}
 public record Overview(long total,long warn,long error,List<TrendPoint> trend,List<ServiceCount> topServicesByErrors){}
}

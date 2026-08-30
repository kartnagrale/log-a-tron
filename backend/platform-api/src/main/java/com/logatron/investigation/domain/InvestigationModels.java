package com.logatron.investigation.domain;

import java.time.Instant;
import java.util.*;

public final class InvestigationModels {
 private InvestigationModels() {}
 public enum IdentifierType { TRACE_ID, REQUEST_ID, CORRELATION_ID, TRANSACTION_ID, ORDER_TOKEN, AUCTION_ID, SCOPE }
 public enum EvidenceKind { LOG, TRACE, METRIC }
 public enum EvidenceStatus { CONFIRMED_FACT, STRONG_EVIDENCE, PROBABLE_INFERENCE, POSSIBLE_HYPOTHESIS }
 public enum MetricOperation { DB_POOL_USAGE_RATIO, KAFKA_CONSUMER_LAG, HTTP_ERROR_RATE, HTTP_LATENCY_P95, JVM_HEAP_USAGE_RATIO, HOST_CPU_USAGE, PROCESSOR_FAILURE_RATE, CLICKHOUSE_QUERY_FAILURE_RATE, PLATFORM_API_ERROR_RATE, COLLECTOR_EXPORT_FAILURES }

 public record InvestigationQuery(UUID projectId, UUID environmentId, Instant from, Instant to, IdentifierType identifierType, String identifierValue) {
  public InvestigationQuery {
   Objects.requireNonNull(projectId,"projectId");Objects.requireNonNull(from,"from");Objects.requireNonNull(to,"to");Objects.requireNonNull(identifierType,"identifierType");
   if(!from.isBefore(to)||java.time.Duration.between(from,to).compareTo(java.time.Duration.ofHours(24))>0)throw new IllegalArgumentException("Investigation window must be positive and at most 24 hours.");
   identifierValue=identifierValue==null?null:identifierValue.trim();if(identifierType!=IdentifierType.SCOPE&&(identifierValue==null||identifierValue.isBlank()))throw new IllegalArgumentException("The selected identifier requires a value.");if(identifierValue!=null&&identifierValue.length()>200)throw new IllegalArgumentException("Identifier is too long.");
  }
 }
 public record EvidenceRef(String id,EvidenceKind kind,String sourceId,Instant timestamp,String service,String label,String link,EvidenceStatus status,Map<String,String> provenance) { public EvidenceRef { provenance=provenance==null?Map.of():Map.copyOf(provenance); } }
 public record TimelineEvent(Instant timestamp,Instant uncertaintyEnd,String service,String host,String summary,String evidenceId,EvidenceStatus status,boolean firstFailure,boolean downstreamSymptom) {}
 public record ExceptionPattern(String fingerprint,String exceptionType,String normalizedMessage,int occurrences,Set<String> services,List<String> evidenceIds) {}
 public record DependencyEdge(String parentService,String childService,String operation,String evidenceId) {}
 public record MetricPoint(Instant timestamp,double value) {}
 public record MetricSeries(MetricOperation operation,String service,String unit,List<MetricPoint> points,String queryReference) {}
 public record DeterministicResult(String summary,EvidenceStatus confidence,List<TimelineEvent> timeline,List<EvidenceRef> evidence,List<ExceptionPattern> exceptionPatterns,List<DependencyEdge> dependencyFlow,Set<String> affectedServices,Set<String> affectedHosts,String firstFailureEvidenceId,List<String> downstreamSymptoms,List<String> qualityWarnings) {}
 public record EvidencePackage(UUID investigationId,UUID projectId,UUID environmentId,Instant from,Instant to,DeterministicResult deterministic,List<EvidenceRef> rankedEvidence,int maxEvidenceItems,String promptTemplateVersion) {}
 public record AiRca(String summary,String mostLikelyRootCause,EvidenceStatus confidence,List<String> evidenceReferences,List<TimelineEvent> timeline,Set<String> affectedServices,List<String> downstreamSymptoms,String whyThisLikelyHappened,List<String> suggestedInvestigationSteps,List<String> possibleRemediation,List<String> uncertainties) {}
 public record AiResult(AiRca rca,String provider,String model,long latencyMs,String outcome) {}
 public record InvestigationResult(UUID id,InvestigationQuery query,DeterministicResult deterministic,AiResult ai) {}
}


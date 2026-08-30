package com.logatron.investigation.application;
import com.logatron.auth.application.EffectiveScope;import com.logatron.investigation.domain.InvestigationModels.*;import org.springframework.security.oauth2.jwt.Jwt;import java.util.*;
public final class InvestigationPorts { private InvestigationPorts(){}
 public interface LogEvidencePort { List<com.logatron.logquery.web.LogQueryDtos.LogRow> query(EffectiveScope scope,InvestigationQuery query); }
 public interface TraceEvidencePort { List<DependencyEdge> dependencies(Jwt jwt,List<com.logatron.logquery.web.LogQueryDtos.LogRow> logs); }
 public interface MetricQueryPort { List<MetricSeries> query(EffectiveScope scope,InvestigationQuery query,Set<MetricOperation> operations); }
 public interface AiProvider { AiResult explain(EvidencePackage evidence); }
}


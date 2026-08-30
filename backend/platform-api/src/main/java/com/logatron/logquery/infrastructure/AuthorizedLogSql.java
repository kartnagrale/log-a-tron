package com.logatron.logquery.infrastructure;

import com.logatron.auth.application.EffectiveScope;import com.logatron.common.error.ApiException;import com.logatron.logquery.domain.LogSearchCriteria;import org.springframework.stereotype.Component;
import java.time.ZoneOffset;import java.time.format.DateTimeFormatter;import java.time.temporal.ChronoUnit;import java.util.*;

@Component
public class AuthorizedLogSql{
 private static final DateTimeFormatter CLICKHOUSE_DATETIME64=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);
 public ScopeClause scope(EffectiveScope effective,LogSearchCriteria.Normalized criteria){
  if(effective.projects().isEmpty())throw ApiException.denied();
  Map<String,String> params=new LinkedHashMap<>();List<String> projects=new ArrayList<>();int pi=0,ei=0,si=0;
  for(var entry:effective.projects().entrySet()){
   UUID projectId=entry.getKey();if(criteria.projectId()!=null&&!criteria.projectId().equals(projectId))continue;
   var project=entry.getValue();String pp="p"+pi++;params.put(pp,projectId.toString());List<String> environments=new ArrayList<>();
   for(UUID environmentId:project.environmentIds()){
    if(criteria.environmentId()!=null&&!criteria.environmentId().equals(environmentId))continue;
    String ep="e"+ei++;params.put(ep,environmentId.toString());Set<UUID> allowed=project.serviceIdsByEnvironment().get(environmentId);
    if(allowed!=null&&!allowed.isEmpty()){
     List<String> serviceParams=new ArrayList<>();for(UUID serviceId:allowed){String sp="s"+si++;params.put(sp,serviceId.toString());serviceParams.add("{"+sp+":UUID}");}
     environments.add("(environment_id={"+ep+":UUID} AND service_id IN ("+String.join(",",serviceParams)+"))");
    }else environments.add("environment_id={"+ep+":UUID}");
   }
   if(!environments.isEmpty())projects.add("(project_id={"+pp+":UUID} AND ("+String.join(" OR ",environments)+"))");
  }
  if(projects.isEmpty())throw ApiException.denied();return new ScopeClause("("+String.join(" OR ",projects)+")",params);
 }
 public QueryParts filtered(EffectiveScope scope,LogSearchCriteria.Normalized c){
  ScopeClause authorized=scope(scope,c);List<String> where=new ArrayList<>();where.add(authorized.sql());Map<String,String> p=new LinkedHashMap<>(authorized.parameters());
  where.add("timestamp >= {from:DateTime64(3,'UTC')} AND timestamp < {to:DateTime64(3,'UTC')}");p.put("from",dateTime64(c.from()));p.put("to",dateTime64(c.to()));
  equal(where,p,"project_id","project",c.projectId());equal(where,p,"environment_id","environment",c.environmentId());equal(where,p,"server_id","server",c.serverId());equal(where,p,"service_id","service",c.serviceId());equal(where,p,"level","level",c.level());equal(where,p,"trace_id","trace",c.traceId());equal(where,p,"span_id","span",c.spanId());equal(where,p,"request_id","request",c.requestId());equal(where,p,"correlation_id","correlation",c.correlationId());equal(where,p,"transaction_id","transaction",c.transactionId());equal(where,p,"order_token","order",c.orderToken());equal(where,p,"auction_id","auction",c.auctionId());equal(where,p,"exception_type","exception",c.exceptionType());
  if(c.text()!=null){where.add("positionCaseInsensitiveUTF8(message,{text:String})>0");p.put("text",c.text());}
  if(c.cursor()!=null){where.add("(timestamp < {cursorTimestamp:DateTime64(3,'UTC')} OR (timestamp = {cursorTimestamp:DateTime64(3,'UTC')} AND event_id < {cursorEvent:UUID}))");p.put("cursorTimestamp",dateTime64(c.cursor().timestamp()));p.put("cursorEvent",c.cursor().eventId().toString());}
  return new QueryParts(String.join(" AND ",where),p);
 }
 public static String dateTime64(java.time.Instant value){return CLICKHOUSE_DATETIME64.format(value.truncatedTo(ChronoUnit.MILLIS));}
 private void equal(List<String>w,Map<String,String>p,String column,String name,Object value){if(value!=null){w.add(column+"={"+name+":"+(value instanceof UUID?"UUID":"String")+"}");p.put(name,value.toString());}}
 public record ScopeClause(String sql,Map<String,String> parameters){}public record QueryParts(String where,Map<String,String> parameters){}
}

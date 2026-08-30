package com.logatron.logquery.configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;import java.time.Duration;
@ConfigurationProperties("logatron.log-query")
public record LogQueryProperties(String clickhouseUrl,String database,String username,String password,Duration queryTimeout,long maxResultBytes,int maxConcurrentQueries,Duration liveMaxDuration,Duration livePollInterval,int liveBufferCap,int maxLiveConnections){
 public LogQueryProperties{if(queryTimeout==null)queryTimeout=Duration.ofSeconds(5);if(maxResultBytes<=0)maxResultBytes=8_388_608;if(maxConcurrentQueries<=0)maxConcurrentQueries=8;if(liveMaxDuration==null)liveMaxDuration=Duration.ofMinutes(5);if(livePollInterval==null)livePollInterval=Duration.ofSeconds(1);if(liveBufferCap<=0)liveBufferCap=200;if(maxLiveConnections<=0)maxLiveConnections=4;}
}

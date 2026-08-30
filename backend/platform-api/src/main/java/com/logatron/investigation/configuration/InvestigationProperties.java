package com.logatron.investigation.configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;import java.time.Duration;
@ConfigurationProperties("logatron.investigation") public record InvestigationProperties(String prometheusUrl,Duration metricTimeout,int maxEvidenceItems,boolean aiEnabled,String aiProvider,String aiModel){public InvestigationProperties{if(prometheusUrl==null)prometheusUrl="http://localhost:9090";if(metricTimeout==null)metricTimeout=Duration.ofSeconds(5);if(maxEvidenceItems<1)maxEvidenceItems=60;if(aiProvider==null)aiProvider="deterministic-stub";if(aiModel==null)aiModel="grounded-template-v1";}}


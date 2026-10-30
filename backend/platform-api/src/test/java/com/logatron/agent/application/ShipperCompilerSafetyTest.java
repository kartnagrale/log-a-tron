package com.logatron.agent.application;
import com.logatron.agent.configuration.AgentManagementProperties;
import com.logatron.catalog.persistence.*;
import com.logatron.catalog.domain.*;
import com.logatron.common.error.ApiException;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ShipperCompilerSafetyTest {
 final LogSourceRepository sources=mock(LogSourceRepository.class);
 final CollectorConfigCompiler compiler=new CollectorConfigCompiler(sources,new AgentManagementProperties(15,"/tmp/logatron-test"));
 @Test void persistedIdsIndependentMetadataAndDeterminism(){var f=CollectorConfigCompilerTest.fixture("JAVA_PIPE_V1",null);var second=new LogSourceEntity(UUID.randomUUID(),f.source().getServiceInstance(),"/logs/root/*.log","system","PLAINTEXT","",true);when(sources.findEnabledDetailedByServerId(f.server().getId())).thenReturn(List.of(f.source(),second));var result=compiler.compile(f.agent());Map<?,?> doc=new Yaml().load(result.yaml());assertThat((Map<?,?>)doc.get("receivers")).hasSize(3);assertThat(result.sourceCount()).isEqualTo(2);assertThat(result.yaml()).contains(f.server().getId().toString(),f.source().getId().toString(),second.getId().toString(),"include_file_record_offset: true","SHA256(body)","error_mode: propagate");assertThat(result.sha256()).isEqualTo(compiler.compile(f.agent()).sha256());}
 @Test void roundTripsQuotedPathsAndOptions(){var f=CollectorConfigCompilerTest.fixture("PLAINTEXT","");f.source().update("/logs/it's app/*.log","application","PLAINTEXT","",true);f.source().configureShipper(new LogSourceOptions("App","beginning","512KiB",List.of("/logs/archive/*/*.log"),List.of("/logs/archive/*/secret.log")));when(sources.findEnabledDetailedByServerId(f.server().getId())).thenReturn(List.of(f.source()));Map<?,?> doc=new Yaml().load(compiler.compile(f.agent()).yaml());var receivers=(Map<?,?>)doc.get("receivers");var receiver=(Map<?,?>)receivers.get("filelog/source_"+f.source().getId().toString().replace("-",""));assertThat(receiver.get("include")).isEqualTo(List.of("/logs/it's app/*.log","/logs/archive/*/*.log"));assertThat(receiver.get("start_at")).isEqualTo("beginning");assertThat(receiver.get("max_log_size")).isEqualTo("512KiB");assertThat(receiver.containsKey("multiline")).isFalse();}
 @Test void overlappingReceiversCannotReadTheSameFiles(){var f=CollectorConfigCompilerTest.fixture("JAVA_PIPE_V1",null);var second=new LogSourceEntity(UUID.randomUUID(),f.source().getServiceInstance(),"/srv/apps/*/logs/one.log","application","PLAINTEXT","",true);when(sources.findEnabledDetailedByServerId(f.server().getId())).thenReturn(List.of(f.source(),second));var result=compiler.compile(f.agent());assertThat(result.warnings()).hasSize(1);assertThat(result.yaml()).contains("exclude:",f.source().getPathPattern());}
 @Test void directoryBoundariesAndGlobs(){assertThat(LogSourceRules.overlaps("/logs/*/*.log","/logs/*.log")).isFalse();assertThat(LogSourceRules.overlaps("/logs/*/*.log","/logs/28-Oct-2026/KARTIK.log")).isTrue();assertThat(LogSourceRules.overlaps("/logs/**","/logs/a/b/c.log")).isTrue();assertThat(LogSourceRules.overlaps("/logs/a?.log","/logs/b*.log")).isFalse();}
 @Test void rejectsUnsafePaths(){for(String p:List.of("relative/*.log","/logs/../*.log","/logs/\nfile","/logs/${env:SECRET}","/logs/[ab].log"))assertThatThrownBy(()->LogSourceRules.path(p)).isInstanceOf(ApiException.class);}
 @Test void recursiveGlobIncludesZeroDirectoryDepth(){assertThat(LogSourceRules.overlaps("/logs/**/*.log","/logs/*.log")).isTrue();assertThat(LogSourceRules.overlaps("/logs/**/KARTIK.log","/logs/KARTIK.log")).isTrue();}
}

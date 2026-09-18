package com.logatron.agent.application;

import com.logatron.agent.configuration.AgentManagementProperties;
import com.logatron.agent.persistence.CollectorAgentEntity;
import com.logatron.catalog.persistence.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.logatron.catalog.domain.CatalogEnums.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CollectorConfigCompilerTest {
    private final LogSourceRepository sources=mock(LogSourceRepository.class);
    private final CollectorConfigCompiler compiler=new CollectorConfigCompiler(sources,new AgentManagementProperties(15,"/var/lib/logatron-agent"));

    @Test void compilesWildcardPathCanonicalProfileAndProfileDefaultMultiline(){
        Fixture f=fixture("JAVA_PIPE_V1",null);
        String yaml=compile(f);
        assertThat(yaml).contains("'/srv/apps/*/logs/*.log'")
                .contains("\\\"parserProfile\\\":\\\"JAVA_PIPE_V1\\\"")
                .contains("\\\\x22")
                .contains("multiline:")
                .contains("TRACE|DEBUG|INFO");
    }

    @Test void customMultilineOverridesTheProfileDefault(){
        Fixture f=fixture("PLAINTEXT","^CUSTOM-START");
        assertThat(compile(f)).contains("line_start_pattern: '^CUSTOM-START'")
                .doesNotContain("^\\d{4}-\\d{2}");
    }

    @Test void blankMultilineExplicitlyDisablesProfileMultiline(){
        Fixture f=fixture("JAVA_PIPE_LEVEL_FIRST_V1","");
        assertThat(compile(f)).doesNotContain("multiline:");
    }

    private String compile(Fixture f){
        when(sources.findEnabledDetailedByServerId(f.server().getId())).thenReturn(List.of(f.source()));
        return compiler.compile(f.agent()).yaml();
    }

    private static Fixture fixture(String profile,String multiline){
        CompanyEntity company=new CompanyEntity(UUID.randomUUID(),"acme","Acme",Status.ACTIVE);
        ProjectEntity project=new ProjectEntity(UUID.randomUUID(),company,"platform","Platform",Classification.INTERNAL,Status.ACTIVE);
        EnvironmentEntity environment=new EnvironmentEntity(UUID.randomUUID(),project,"UAT","UAT",EnvironmentType.UAT,Sensitivity.INTERNAL,Status.ACTIVE);
        ServerEntity server=new ServerEntity(UUID.randomUUID(),environment,"uat-01","uat-01.internal",null,Status.ONLINE,Map.of());
        ServiceEntity service=new ServiceEntity(UUID.randomUUID(),project,"orders","Orders","Platform",Criticality.HIGH,Status.ACTIVE,Map.of());
        ServiceInstanceEntity instance=new ServiceInstanceEntity(UUID.randomUUID(),service,server,"orders-1","1.0",Status.RUNNING);
        LogSourceEntity source=new LogSourceEntity(UUID.randomUUID(),instance,"/srv/apps/*/logs/*.log","application",profile,multiline,"Asia/Kolkata",true);
        CollectorAgentEntity agent=new CollectorAgentEntity(UUID.randomUUID(),server,"uat-01","hash","gateway.internal:4317",false);
        return new Fixture(server,source,agent);
    }

    private record Fixture(ServerEntity server,LogSourceEntity source,CollectorAgentEntity agent){}
}

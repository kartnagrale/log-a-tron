package com.logatron.integration;
import com.fasterxml.jackson.databind.*;
import com.logatron.agent.application.*;
import com.logatron.agent.persistence.*;
import com.logatron.catalog.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers(disabledWithoutDocker=true)
@SpringBootTest(properties={"spring.profiles.active=dev-auth","logatron.shipper.destinations[0].id=test","logatron.shipper.destinations[0].name=Disposable gateway","logatron.shipper.destinations[0].endpoint=gateway.test:4317","logatron.shipper.destinations[0].insecure=true"})
@AutoConfigureMockMvc
class ShipperApiIntegrationTest {
 @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:17.6-alpine").withDatabaseName("logatron").withUsername("logatron").withPassword("test-password");
 @DynamicPropertySource static void props(DynamicPropertyRegistry p){p.add("spring.datasource.url",POSTGRES::getJdbcUrl);p.add("spring.datasource.username",POSTGRES::getUsername);p.add("spring.datasource.password",POSTGRES::getPassword);}
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired ShipperConfigurationService shipper;@Autowired ProjectRepository projects;@Autowired CollectorConfigRevisionRepository revisions;
 @MockitoBean CollectorConfigurationValidator validator;
 String base="/api/v1/admin",agent,token,project,env,server,service,instance,source;
 RequestPostProcessor admin(){return jwt().jwt(j->j.issuer("http://localhost:8080/dev-auth").subject("admin"));}
 RequestPostProcessor developer(){return jwt().jwt(j->j.issuer("http://localhost:8080/dev-auth").subject("developer"));}
 @BeforeEach void register()throws Exception{
  when(validator.validate(anyString())).thenReturn(new CollectorConfigurationValidator.Result("VALID","Runtime validation is mocked only in API transaction tests; real contrib is covered separately."));
  project=id("projects",Map.of("companyId","00000000-0000-0000-0000-000000000001","slug","test-"+UUID.randomUUID(),"name","Test","classification","INTERNAL"));
  env=id("environments",Map.of("projectId",project,"code","UAT","name","UAT","type","UAT","sensitivity","INTERNAL"));
  server=id("servers",Map.of("environmentId",env,"hostId","test-01","hostname","test.internal"));
  service=id("services",Map.of("projectId",project,"serviceKey","test-service","displayName","Test Service","criticality","MEDIUM"));
  instance=id("service-instances",Map.of("serviceId",service,"serverId",server,"instanceKey","test-1"));
  // The first file scan may happen after the sample append on a fast runner.
  // Beginning is intentional for this disposable replay fixture; production defaults stay end.
  source=id("log-sources",Map.of("serviceInstanceId",instance,"pathPattern","/logs/*/*.log","logType","application","parserProfile","JAVA_PIPE_V1","enabled",true,"shipperOptions",Map.of("startAt","beginning")));
  JsonNode registration=postJson(base+"/collector-agents",Map.of("serverId",server,"agentKey","test-"+UUID.randomUUID(),"gatewayEndpoint","gateway.test:4317","gatewayInsecure",true));agent=registration.path("agent").path("id").asText();token=registration.path("token").asText();
 }
 @org.junit.jupiter.api.io.TempDir java.nio.file.Path runtimeDirectory;
 @Test void registeredResourcesGenerateConfigurationThatIngestsMultilineLogsAcrossRestartAndRollback()throws Exception{
  var config=generate();java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/shipper-example"));java.nio.file.Files.writeString(java.nio.file.Path.of("target/shipper-example/config.yml"),config.path("yaml").asText());CollectorRuntimeIntegrationTest.verifyPersistedConfiguration(config.path("yaml").asText(),runtimeDirectory,Map.of("projectId",UUID.fromString(project),"environmentId",UUID.fromString(env),"serverId",UUID.fromString(server),"serviceId",UUID.fromString(service),"serviceInstanceId",UUID.fromString(instance)),UUID.fromString(source));
 }
 @Test void generationResolvesPersistedIdsWithoutDeployingAndDownloadIsAuthenticated()throws Exception{
  JsonNode generated=generate();String yaml=generated.path("yaml").asText();assertThat(yaml).contains(project,env,server,service,instance,source);assertThat(generate().path("id")).isEqualTo(generated.path("id"));
  mvc.perform(get(base+"/collector-agents/"+agent+"/shipper").with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.selectedVersion").isEmpty()).andExpect(jsonPath("$.deployments.length()").value(0));
  mvc.perform(get("/api/internal/v1/agents/"+agent+"/config").header("X-Logatron-Agent-Token",token)).andExpect(status().isNoContent());
  mvc.perform(get(base+"/collector-agents/"+agent+"/config-revisions/1/download")).andExpect(status().isUnauthorized());
  mvc.perform(get(base+"/collector-agents/"+agent+"/config-revisions/1/download").with(developer())).andExpect(status().isNotFound());
  mvc.perform(get(base+"/collector-agents/"+agent+"/config-revisions/1/download").with(admin())).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=\"config.yml\""));
 }
 @Test void concurrentRequestsShareOneImmutableVersion()throws Exception{
  Jwt identity=Jwt.withTokenValue("test").header("alg","none").issuer("http://localhost:8080/dev-auth").subject("admin").build();
  try(var pool=Executors.newFixedThreadPool(4)){var futures=new ArrayList<Future<UUID>>();for(int n=0;n<8;n++)futures.add(pool.submit(()->shipper.generate(identity,UUID.fromString(agent),null).id()));Set<UUID> ids=new HashSet<>();for(var future:futures)ids.add(future.get(30,TimeUnit.SECONDS));assertThat(ids).hasSize(1);assertThat(revisions.findByAgent_IdOrderByConfigVersionDesc(UUID.fromString(agent))).hasSize(1);}
 }
 @Test void unauthorizedAndInvalidDeploymentCannotRestartCollector()throws Exception{
  generate();mvc.perform(post(base+"/collector-agents/"+agent+"/deployments").with(developer()).contentType("application/json").content("{\"version\":1,\"confirmed\":true}")).andExpect(status().isNotFound());
  mvc.perform(post(base+"/collector-agents/"+agent+"/deployments").with(admin()).contentType("application/json").content("{\"version\":1,\"confirmed\":true}")).andExpect(status().isBadRequest());
  mvc.perform(post(base+"/shipper-configurations/generate").with(admin()).contentType("application/json").content("{\"agentId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isNotFound());
 }
 @Test void missingAndInvalidAgentTokensAreRejectedWithoutExposingIdentity()throws Exception{
  String internal="/api/internal/v1/agents/"+agent;
  mvc.perform(get(internal+"/config")).andExpect(status().isNotFound());
  mvc.perform(get(internal+"/config").header("X-Logatron-Agent-Token","invalid-token")).andExpect(status().isNotFound());
  mvc.perform(post(internal+"/heartbeat").header("X-Logatron-Agent-Token","invalid-token").contentType("application/json").content("{\"protocol\":\"managed-v2\",\"status\":\"ONLINE\"}")).andExpect(status().isNotFound());
 }
 @Test void deploymentFailureRestoresSelectionAndExplicitRollbackIsVersioned()throws Exception{
  JsonNode v1=generate();postJson(base+"/collector-agents/"+agent+"/config-revisions/1/validate",Map.of());
  heartbeat(Map.of("collectorVersion","otelcol-contrib version 0.135.0","protocol","managed-v2","status","DEGRADED"));
  mvc.perform(put(base+"/collector-agents/"+agent+"/deployment-settings").with(admin()).contentType("application/json").content("{\"enabled\":true,\"confirmed\":true}")).andExpect(status().isNoContent());
  var first=postJson(base+"/collector-agents/"+agent+"/deployments",Map.of("version",1,"confirmed",true));
  heartbeat(Map.of("collectorVersion","otelcol-contrib version 0.135.0","protocol","managed-v2","status","ONLINE","healthVerified",true,"deploymentId",first.path("id").asText(),"appliedConfigVersion",1,"appliedConfigHash",v1.path("sha256").asText()));
  mvc.perform(put(base+"/log-sources/"+source).with(admin()).contentType("application/json").content(json.writeValueAsString(Map.of("pathPattern","/logs/*/*.log","logType","application","parserProfile","PLAINTEXT","enabled",true)))).andExpect(status().isOk());
  JsonNode v2=generate();assertThat(v2.path("version").asInt()).isEqualTo(2);postJson(base+"/collector-agents/"+agent+"/config-revisions/2/validate",Map.of());
  var second=postJson(base+"/collector-agents/"+agent+"/deployments",Map.of("version",2,"expectedSelectedVersion",1,"confirmed",true));
  heartbeat(Map.of("collectorVersion","otelcol-contrib version 0.135.0","protocol","managed-v2","status","DEGRADED","deploymentId",second.path("id").asText(),"lastError","Target validation failed; retained previous configuration","appliedConfigVersion",1,"appliedConfigHash",v1.path("sha256").asText()));
  mvc.perform(get(base+"/collector-agents/"+agent+"/shipper").with(admin())).andExpect(jsonPath("$.selectedVersion").value(1)).andExpect(jsonPath("$.deployments[0].status").value("FAILED"));
  var retry=postJson(base+"/collector-agents/"+agent+"/deployments",Map.of("version",2,"expectedSelectedVersion",1,"confirmed",true));
  heartbeat(Map.of("collectorVersion","otelcol-contrib version 0.135.0","protocol","managed-v2","status","ONLINE","healthVerified",true,"deploymentId",retry.path("id").asText(),"appliedConfigVersion",2,"appliedConfigHash",v2.path("sha256").asText()));
  postJson(base+"/collector-agents/"+agent+"/rollback",Map.of("version",1,"expectedSelectedVersion",2,"confirmed",true));
  mvc.perform(get(base+"/collector-agents/"+agent+"/shipper").with(admin())).andExpect(jsonPath("$.selectedVersion").value(1)).andExpect(jsonPath("$.deployments[0].action").value("ROLLBACK"));
 }
 @Test void legacyNaturalKeysAreAdoptedAndNeverReplaced()throws Exception{
  long before=projects.count();String adopted=id("projects",Map.of("companyId","00000000-0000-0000-0000-000000000001","slug","demo-marketplace","name","Different label","classification","INTERNAL"));assertThat(adopted).isEqualTo("10000000-0000-0000-0000-000000000001");assertThat(projects.count()).isEqualTo(before);assertThat(projects.findById(UUID.fromString(adopted)).orElseThrow().getName()).isEqualTo("Demo Marketplace");
  assertThat(id("log-sources",Map.of("serviceInstanceId",instance,"pathPattern","/logs/*/*.log","logType","application","parserProfile","JAVA_PIPE_V1","enabled",true))).isEqualTo(source);
 }
 @Test void rejectsUnsafePathsAndUnmanagedDestinations()throws Exception{
  mvc.perform(post(base+"/log-sources").with(admin()).contentType("application/json").content(json.writeValueAsString(Map.of("serviceInstanceId",instance,"pathPattern","/logs/../secret","logType","application","parserProfile","PLAINTEXT","enabled",true)))).andExpect(status().isBadRequest());
  mvc.perform(put(base+"/collector-agents/"+agent+"/gateway").with(admin()).contentType("application/json").content("{\"gatewayEndpoint\":\"untrusted.test:4317\",\"gatewayInsecure\":true}")).andExpect(status().isBadRequest());
 }
 JsonNode generate()throws Exception{return postJson(base+"/shipper-configurations/generate",Map.of("agentId",agent));}
 String id(String route,Map<String,Object> body)throws Exception{return postJson(base+"/"+route,body).path("id").asText();}
 JsonNode postJson(String route,Object body)throws Exception{String response=mvc.perform(post(route).with(admin()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(response);}
 void heartbeat(Object body)throws Exception{mvc.perform(post("/api/internal/v1/agents/"+agent+"/heartbeat").header("X-Logatron-Agent-Token",token).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isNoContent());}
}

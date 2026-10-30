package com.logatron.agent.application;

import com.fasterxml.jackson.databind.*;
import com.logatron.agent.configuration.AgentManagementProperties;
import com.logatron.catalog.domain.LogSourceOptions;
import com.logatron.catalog.persistence.LogSourceRepository;
import com.logatron.contracts.ingestion.RawLogEnvelope;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.*;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Disposable real contrib runtime. Never contacts application servers or configured production endpoints. */
@Testcontainers(disabledWithoutDocker=true)
public class CollectorRuntimeIntegrationTest {
 @TempDir Path directory;
 final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
 @Test void validatesMultilineEnvelopeAndCheckpointedRestart()throws Exception{
  var f=CollectorConfigCompilerTest.fixture("JAVA_PIPE_V1",null);f.agent().updateGateway("gateway:4317",true);
  f.source().update("/logs/*/*.log","application","JAVA_PIPE_V1",null,true);
  f.source().configureShipper(new LogSourceOptions("Application","beginning","200KiB",null,null));
  var repo=mock(LogSourceRepository.class);when(repo.findEnabledDetailedByServerId(f.server().getId())).thenReturn(List.of(f.source()));
  String yaml=new CollectorConfigCompiler(repo,new AgentManagementProperties(15,"/state")).compile(f.agent()).yaml();
  verifyPersistedConfiguration(yaml,directory,Map.of("projectId",f.server().getEnvironment().getProject().getId(),"environmentId",f.server().getEnvironment().getId(),"serverId",f.server().getId(),"serviceId",f.source().getServiceInstance().getService().getId(),"serviceInstanceId",f.source().getServiceInstance().getId()),f.source().getId());
 }
 public static void verifyPersistedConfiguration(String yaml,Path directory,Map<String,UUID> expected,UUID sourceId)throws Exception{new CollectorRuntimeIntegrationTest().exercise(yaml,directory,expected,sourceId);}
 private void exercise(String yaml,Path directory,Map<String,UUID> expected,UUID sourceId)throws Exception{
  Map<?,?> doc=new org.yaml.snakeyaml.Yaml().load(yaml);String storage=((Map<?,?>)((Map<?,?>)doc.get("extensions")).get("file_storage")).get("directory").toString();String stateMount=storage.substring(0,storage.lastIndexOf("/"));
  Path logs=Files.createDirectories(directory.resolve("logs/day")),state=Files.createDirectories(directory.resolve("state")),out=Files.createDirectories(directory.resolve("out"));
  Path config=directory.resolve("config.yml");Files.writeString(config,yaml);Files.writeString(logs.resolve("app.log"),"");
  Path gatewayConfig=directory.resolve("gateway.yml");Files.writeString(gatewayConfig,"receivers:\n  otlp:\n    protocols:\n      grpc:\n        endpoint: 0.0.0.0:4317\nexporters:\n  file:\n    path: /out/events.json\n    flush_interval: 100ms\nservice:\n  pipelines:\n    logs:\n      receivers: [otlp]\n      exporters: [file]\n");
  String image=System.getProperty("test.otel.image","otel/opentelemetry-collector-contrib:0.135.0");
  try(Network network=Network.newNetwork();var gateway=collector(image,gatewayConfig).withNetwork(network).withNetworkAliases("gateway","gateway.test").withFileSystemBind(out.toString(),"/out",BindMode.READ_WRITE)){
   gateway.start();
   var agent=collector(image,config).withNetwork(network).withFileSystemBind(directory.resolve("logs").toString(),"/logs",BindMode.READ_ONLY).withFileSystemBind(state.toString(),stateMount,BindMode.READ_WRITE);
   try{
    agent.start();var check=agent.execInContainer("/otelcol-contrib","validate","--config=/etc/config.yml");assertThat(check.getExitCode()).withFailMessage(check.getStderr()).isZero();
    String first="http-nio-8080-exec-36 | ERROR | 28 Oct 2026 10:47:23,862 | LoginController.java:113:checkLogin | failed\njava.lang.IllegalStateException: boom\n\tat LoginController.checkLogin(LoginController.java:113)\n";
    Files.writeString(logs.resolve("app.log"),first,StandardOpenOption.APPEND);
    var envelopes=awaitEnvelopes(out.resolve("events.json"),1);var event=envelopes.get(0);
    assertThat(event.resource().projectId()).isEqualTo(expected.get("projectId"));assertThat(event.resource().environmentId()).isEqualTo(expected.get("environmentId"));assertThat(event.resource().serverId()).isEqualTo(expected.get("serverId"));assertThat(event.resource().serviceId()).isEqualTo(expected.get("serviceId"));assertThat(event.resource().serviceInstanceId()).isEqualTo(expected.get("serviceInstanceId"));assertThat(event.source().logSourceId()).isEqualTo(sourceId);
    assertThat(event.payload()).contains("IllegalStateException","\tat LoginController");assertThat(event.eventId()).isNotNull();assertThat(event.observedTimestamp()).isNotNull();assertThat(event.collector().sourceOffset()).contains("|/logs/day/app.log|0|");
    agent.stop();agent.start();
    Files.writeString(logs.resolve("app.log"),"worker | INFO | 28 Oct 2026 10:48:23,862 | App.java:1:run | after restart\n",StandardOpenOption.APPEND);
    envelopes=awaitEnvelopes(out.resolve("events.json"),2);assertThat(envelopes).hasSize(2);assertThat(envelopes.get(1).payload()).contains("after restart");assertThat(envelopes.get(1).collector().sourceOffset()).isNotEqualTo(event.collector().sourceOffset());
    agent.stop();Files.writeString(config,yaml.replace("timeout: 1s","timeout: 2s"));agent.start();agent.stop();Files.writeString(config,yaml);agent.start();
    Files.writeString(logs.resolve("app.log"),"worker | INFO | 28 Oct 2026 10:49:23,862 | App.java:1:run | after rollback\n",StandardOpenOption.APPEND);
    assertThat(awaitEnvelopes(out.resolve("events.json"),3)).hasSize(3);
   }finally{agent.close();}
  }
 }
 private GenericContainer<?> collector(String image,Path config){
  // Match bind-mount ownership on Unix so state/queue files remain readable and
  // removable by JUnit. Docker Desktop's Windows mounts use the root fallback.
  String owner="0";
  try{owner=Files.getAttribute(config,"unix:uid")+":"+Files.getAttribute(config,"unix:gid");}
  catch(UnsupportedOperationException|java.io.IOException ignored){}
  final String user=owner;
  return new GenericContainer<>(image).withCreateContainerCmdModifier(c->c.withUser(user)).withCopyFileToContainer(MountableFile.forHostPath(config),"/etc/config.yml").withCommand("--config=/etc/config.yml").waitingFor(Wait.forLogMessage(".*Everything is ready.*",1)).withStartupTimeout(Duration.ofSeconds(45));
 }
 private List<RawLogEnvelope> awaitEnvelopes(Path file,int count)throws Exception{long until=System.nanoTime()+Duration.ofSeconds(30).toNanos();List<RawLogEnvelope> events=List.of();while(System.nanoTime()<until){events=readEnvelopes(file);if(events.size()>=count)return events;Thread.sleep(200);}throw new AssertionError("Expected "+count+" envelopes, got "+events.size());}
 private List<RawLogEnvelope> readEnvelopes(Path file)throws Exception{var result=new ArrayList<RawLogEnvelope>();if(!Files.exists(file))return result;for(String line:Files.readAllLines(file)){if(line.isBlank())continue;JsonNode doc;try{doc=json.readTree(line);}catch(Exception incomplete){continue;}for(var resource:doc.path("resourceLogs"))for(var scope:resource.path("scopeLogs"))for(var record:scope.path("logRecords"))result.add(json.convertValue(decode(record.path("body")),RawLogEnvelope.class));}return result;}
 private Object decode(JsonNode value){if(value.has("stringValue"))return value.get("stringValue").asText();if(value.has("intValue"))return value.get("intValue").asInt();if(value.has("kvlistValue")){var map=new LinkedHashMap<String,Object>();for(var item:value.path("kvlistValue").path("values"))map.put(item.path("key").asText(),decode(item.path("value")));return map;}return null;}
}

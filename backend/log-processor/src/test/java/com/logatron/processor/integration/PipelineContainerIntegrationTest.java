package com.logatron.processor.integration;

import com.fasterxml.jackson.databind.*;
import com.logatron.contracts.ingestion.*;
import com.logatron.processor.TestFixtures;
import com.logatron.processor.consume.*;
import com.logatron.processor.deduplicate.*;
import com.logatron.processor.dlq.DeadLetterFactory;
import com.logatron.processor.enrich.*;
import com.logatron.processor.observability.ProcessorMetrics;
import com.logatron.processor.parse.*;
import com.logatron.processor.protect.SensitiveDataMasker;
import com.logatron.processor.store.ClickHouseStore;
import com.logatron.processor.validate.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import java.net.*;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.time.*;import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker=true)
class PipelineContainerIntegrationTest {
 @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:17.6-alpine").withDatabaseName("logatron").withUsername("logatron").withPassword("test-password");
 @Container static final KafkaContainer KAFKA=new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));
 @Container static final ClickHouseContainer CLICKHOUSE=new ClickHouseContainer(DockerImageName.parse("clickhouse/clickhouse-server:25.8-alpine")).withDatabaseName("logatron").withUsername("logatron_ingest").withPassword("test-password");
 static ObjectMapper json;static JdbcTemplate jdbc;static String clickUrl;
 @BeforeAll static void setup()throws Exception{json=new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);jdbc=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));seedMetadata();clickUrl="http://"+CLICKHOUSE.getHost()+":"+CLICKHOUSE.getMappedPort(8123);String ddl=Files.readString(Path.of("..","..","infrastructure","clickhouse","migrations","001_create_logs.sql"));for(String statement:ddl.split("(?m);\\s*(?=CREATE)"))http("POST","/",statement);try(Admin admin=Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,KAFKA.getBootstrapServers()))){admin.createTopics(List.of(new NewTopic("logs.raw.v1",1,(short)1))).all().get();}}
 @Test void kafkaRawEnvelopeBecomesMaskedCanonicalClickHouseRow()throws Exception{RawLogEnvelope raw=TestFixtures.raw(ParserProfile.JSON,"{\"timestamp\":\"2026-08-27T10:30:01Z\",\"level\":\"ERROR\",\"message\":\"Authorization: Bearer TOPSECRET password=myPassword123\",\"traceId\":\"12345678901234567890123456789012\"}");String wire=json.writeValueAsString(raw);Properties pp=new Properties();pp.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,KAFKA.getBootstrapServers());try(var producer=new KafkaProducer<String,String>(pp,new StringSerializer(),new StringSerializer())){producer.send(new ProducerRecord<>("logs.raw.v1","project:service",wire)).get();}Properties cp=new Properties();cp.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,KAFKA.getBootstrapServers());cp.put(ConsumerConfig.GROUP_ID_CONFIG,"pipeline-test");cp.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");cp.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"false");String consumed=null;try(var consumer=new KafkaConsumer<String,String>(cp,new StringDeserializer(),new StringDeserializer())){consumer.subscribe(List.of("logs.raw.v1"));long end=System.nanoTime()+Duration.ofSeconds(20).toNanos();while(consumed==null&&System.nanoTime()<end){for(var r:consumer.poll(Duration.ofMillis(500)))consumed=r.value();}}assertThat(consumed).isEqualTo(wire);var properties=properties();var metrics=new ProcessorMetrics(new SimpleMeterRegistry());var masker=new SensitiveDataMasker(properties);var resolver=new MetadataResolver(jdbc,properties);var pipeline=new ProcessorPipeline(json,new EnvelopeValidator(properties),resolver,List.of(new JsonLogParser(json,new TimestampNormalizer()),new PlaintextLogParser(new TimestampNormalizer())),masker,new CanonicalEventFactory(new SeverityNormalizer(),masker,new Fingerprinter(),properties),new CanonicalValidator(),new DuplicateTracker(properties),new DeadLetterFactory(masker));ProcessingOutcome outcome=pipeline.process(consumed);assertThat(outcome.event()).isNotNull();new ClickHouseStore(json,properties,metrics).store(List.of(outcome.event()));String result=http("GET","/?query="+URLEncoder.encode("SELECT count(),any(message) FROM logatron.logs_local",StandardCharsets.UTF_8),null);assertThat(result).startsWith("1\t").doesNotContain("TOPSECRET","myPassword123").contains("[REDACTED]");}
 static com.logatron.processor.configuration.IngestionProperties properties(){var p=TestFixtures.properties();return new com.logatron.processor.configuration.IngestionProperties(p.ingestion(),p.masking(),p.metadataCache(),p.kafka(),new com.logatron.processor.configuration.IngestionProperties.ClickHouse(clickUrl,"logatron","logatron_ingest","test-password",Duration.ofSeconds(10)));}
 static String http(String method,String path,String body)throws Exception{String auth=Base64.getEncoder().encodeToString("logatron_ingest:test-password".getBytes(StandardCharsets.UTF_8));var b=HttpRequest.newBuilder(URI.create(clickUrl+path)).header("Authorization","Basic "+auth);HttpRequest req="POST".equals(method)?b.POST(HttpRequest.BodyPublishers.ofString(body)).build():b.GET().build();var res=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.ofString());if(res.statusCode()/100!=2)throw new IllegalStateException(res.statusCode()+" "+res.body());return res.body();}
 static void seedMetadata(){for(String sql:List.of("create table company(id uuid primary key,name text)","create table project(id uuid primary key,company_id uuid,name text,classification text)","create table environment(id uuid primary key,project_id uuid,code text)","create table server_node(id uuid primary key,environment_id uuid,host_id text,hostname text,ip_address text)","create table service_definition(id uuid primary key,project_id uuid,service_key text)","create table service_instance(id uuid primary key,service_id uuid,server_id uuid,instance_key text)","create table log_source(id uuid primary key,service_instance_id uuid,path_pattern text,log_type text,parser_profile text,enabled boolean)"))jdbc.execute(sql);jdbc.update("insert into company values(?::uuid,?)","00000000-0000-0000-0000-000000000001","Example");jdbc.update("insert into project values(?::uuid,?::uuid,?,?)","10000000-0000-0000-0000-000000000001","00000000-0000-0000-0000-000000000001","Demo Marketplace","CONFIDENTIAL");jdbc.update("insert into environment values(?::uuid,?::uuid,?)","20000000-0000-0000-0000-000000000003","10000000-0000-0000-0000-000000000001","PROD");jdbc.update("insert into server_node values(?::uuid,?::uuid,?,?,?)","30000000-0000-0000-0000-000000000001","20000000-0000-0000-0000-000000000003","prod-1","prod-1.local.test","192.0.2.1");jdbc.update("insert into service_definition values(?::uuid,?::uuid,?)","40000000-0000-0000-0000-000000000001","10000000-0000-0000-0000-000000000001","order");jdbc.update("insert into service_instance values(?::uuid,?::uuid,?::uuid,?)","50000000-0000-0000-0000-000000000001","40000000-0000-0000-0000-000000000001","30000000-0000-0000-0000-000000000001","order-1");jdbc.update("insert into log_source values(?::uuid,?::uuid,?,?,?,true)","60000000-0000-0000-0000-000000000001","50000000-0000-0000-0000-000000000001","/logs/order/*.log","application","logback-json");}
}
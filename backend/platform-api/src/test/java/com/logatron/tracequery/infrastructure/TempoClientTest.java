package com.logatron.tracequery.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logatron.tracequery.configuration.TraceQueryProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TempoClientTest{
 @Test void successfulTraceMayContainTimeoutErrorAttributes()throws Exception{
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  byte[] body="{\"batches\":[],\"error.type\":\"java.sql.SQLTimeoutException\"}".getBytes(StandardCharsets.UTF_8);
  server.createContext("/api/traces/0123456789abcdef0123456789abcdef",exchange->{exchange.sendResponseHeaders(200,body.length);try(var output=exchange.getResponseBody()){output.write(body);}});
  server.start();
  try{var client=new TempoClient(new TraceQueryProperties("http://127.0.0.1:"+server.getAddress().getPort(),Duration.ofSeconds(2)),new ObjectMapper());assertThat(client.trace("0123456789abcdef0123456789abcdef").path("error.type").asText()).isEqualTo("java.sql.SQLTimeoutException");}
  finally{server.stop(0);}
 }
}

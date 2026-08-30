package com.logatron.logquery.infrastructure;

import com.fasterxml.jackson.databind.*;import com.logatron.common.error.ApiException;import com.logatron.logquery.configuration.LogQueryProperties;import org.springframework.stereotype.Component;
import java.io.*;import java.net.*;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.time.Duration;import java.util.*;import java.util.concurrent.Semaphore;

@Component
public class ClickHouseQueryClient{
 private final LogQueryProperties properties;private final ObjectMapper mapper;private final HttpClient client;private final Semaphore permits;
 public ClickHouseQueryClient(LogQueryProperties properties,ObjectMapper mapper){this.properties=properties;this.mapper=mapper;this.client=HttpClient.newBuilder().connectTimeout(properties.queryTimeout()).build();this.permits=new Semaphore(properties.maxConcurrentQueries(),true);}
 public List<JsonNode> query(String sql,Map<String,String> parameters){
  if(!permits.tryAcquire())throw ApiException.rateLimited();
  try{
   StringBuilder url=new StringBuilder(strip(properties.clickhouseUrl())).append("/?database=").append(enc(properties.database())).append("&default_format=JSONEachRow&prefer_column_name_to_alias=1&max_execution_time=").append(Math.max(1,properties.queryTimeout().toSeconds())).append("&max_result_bytes=").append(properties.maxResultBytes()).append("&result_overflow_mode=throw");
   parameters.forEach((k,v)->url.append("&param_").append(enc(k)).append('=').append(enc(v)));
   String auth=Base64.getEncoder().encodeToString((properties.username()+":"+properties.password()).getBytes(StandardCharsets.UTF_8));
   HttpRequest request=HttpRequest.newBuilder(URI.create(url.toString())).timeout(properties.queryTimeout()).header("Authorization","Basic "+auth).header("Content-Type","text/plain; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(sql+" FORMAT JSONEachRow",StandardCharsets.UTF_8)).build();
   HttpResponse<InputStream> response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
   if(response.statusCode()<200||response.statusCode()>=300){String body=new String(response.body().readNBytes(2048),StandardCharsets.UTF_8);if(response.statusCode()==408||response.statusCode()==504||body.toLowerCase(Locale.ROOT).contains("timeout"))throw ApiException.timeout();throw ApiException.unavailable("ClickHouse");}
   List<JsonNode> rows=new ArrayList<>();try(BufferedReader reader=new BufferedReader(new InputStreamReader(new BoundedInputStream(response.body(),properties.maxResultBytes()),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)if(!line.isBlank())rows.add(mapper.readTree(line));}
   return rows;
  }catch(HttpTimeoutException ex){throw ApiException.timeout();}catch(InterruptedException ex){Thread.currentThread().interrupt();throw ApiException.unavailable("ClickHouse");}catch(IOException|IllegalArgumentException ex){throw ApiException.unavailable("ClickHouse");}finally{permits.release();}
 }
 private static String strip(String value){return value.endsWith("/")?value.substring(0,value.length()-1):value;}private static String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
 private static final class BoundedInputStream extends FilterInputStream{private long remaining;BoundedInputStream(InputStream in,long max){super(in);remaining=max;}@Override public int read()throws IOException{if(remaining<=0)throw new IOException("ClickHouse result exceeded byte limit");int value=super.read();if(value>=0)remaining--;return value;}@Override public int read(byte[] b,int off,int len)throws IOException{if(remaining<=0)throw new IOException("ClickHouse result exceeded byte limit");int read=super.read(b,off,(int)Math.min(len,remaining));if(read>0)remaining-=read;return read;}}
}

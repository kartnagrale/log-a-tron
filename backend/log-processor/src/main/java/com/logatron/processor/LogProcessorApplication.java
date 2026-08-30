package com.logatron.processor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
@SpringBootApplication @ConfigurationPropertiesScan
public class LogProcessorApplication { public static void main(String[] args){SpringApplication.run(LogProcessorApplication.class,args);} }
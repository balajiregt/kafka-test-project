package com.balaji.kafkaeventlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KafkaEventLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaEventLabApplication.class, args);
    }
}

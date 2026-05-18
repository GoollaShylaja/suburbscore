package com.suburbscore.suburb;

import org.springdoc.core.configuration.SpringDocSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = {SpringDocSecurityConfiguration.class})
@EnableDiscoveryClient
public class SuburbServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuburbServiceApplication.class, args);
    }
}

package com.cssms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CssmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CssmsApplication.class, args);
    }
}

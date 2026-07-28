package com.example.xapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class XAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(XAppApplication.class, args);
    }
}

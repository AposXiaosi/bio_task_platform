package com.bioinfo.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BioinfoPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(BioinfoPlatformApplication.class, args);
    }
}

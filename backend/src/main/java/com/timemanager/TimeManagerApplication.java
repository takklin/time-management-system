package com.timemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TimeManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeManagerApplication.class, args);
    }
}

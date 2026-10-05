package com.h8.ems.redeployment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RedeploymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedeploymentServiceApplication.class, args);
    }
}

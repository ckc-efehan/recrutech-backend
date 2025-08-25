package com.recrutech.recrutechauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecrutechAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecrutechAuthApplication.class, args);
    }

}

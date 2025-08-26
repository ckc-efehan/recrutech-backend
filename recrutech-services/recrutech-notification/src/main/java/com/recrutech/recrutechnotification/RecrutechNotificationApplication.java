package com.recrutech.recrutechnotification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class RecrutechNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecrutechNotificationApplication.class, args);
    }

}

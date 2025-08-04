package com.recrutech.recrutechnotification;

import org.springframework.boot.SpringApplication;

public class TestRecrutechNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.from(RecrutechNotificationApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

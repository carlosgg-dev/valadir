package com.valadir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ValadirApplication {

    public static void main(String[] args) {

        SpringApplication.run(ValadirApplication.class, args);
    }
}

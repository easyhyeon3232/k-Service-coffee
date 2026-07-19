package com.example.coffee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoffeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeApplication.class, args);
    }

}

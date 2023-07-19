package com.example.postandreplyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients({"com.example.postandreplyservice.service.remote"})
public class PostAndReplyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostAndReplyServiceApplication.class, args);
    }

}

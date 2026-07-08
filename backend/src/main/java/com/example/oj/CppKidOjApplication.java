package com.example.oj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.example.oj.mapper")
@EnableScheduling
@SpringBootApplication
public class CppKidOjApplication {

    public static void main(String[] args) {
        SpringApplication.run(CppKidOjApplication.class, args);
    }
}

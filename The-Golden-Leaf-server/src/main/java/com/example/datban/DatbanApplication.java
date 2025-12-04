package com.example.datban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatbanApplication {
    public static void main(String[] args) {
        SpringApplication.run(DatbanApplication.class, args);
        System.out.println("🚀 Server đang chạy tại http://localhost:8080");
    }
    
}

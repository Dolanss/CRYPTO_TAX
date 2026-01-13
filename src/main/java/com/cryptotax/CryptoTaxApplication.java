package com.cryptotax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CryptoTaxApplication {
    public static void main(String[] args) {
        SpringApplication.run(CryptoTaxApplication.class, args);
    }
}

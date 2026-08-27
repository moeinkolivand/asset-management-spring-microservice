package com.tutorial.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;


@SpringBootApplication
@EnableKafka
@EnableJpaRepositories(basePackages = "com.tutorial.transaction")
@EntityScan(basePackages = "com.tutorial.transaction")
public class TransactionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionApplication.class, args);
    }

}

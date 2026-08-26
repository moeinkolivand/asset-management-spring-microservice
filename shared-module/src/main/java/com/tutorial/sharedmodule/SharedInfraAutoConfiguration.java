package com.tutorial.sharedmodule;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@EnableJpaRepositories(basePackages = "com.tutorial.sharedmodule")
@EntityScan(basePackages = "com.tutorial.sharedmodule")
public class SharedInfraAutoConfiguration {
}
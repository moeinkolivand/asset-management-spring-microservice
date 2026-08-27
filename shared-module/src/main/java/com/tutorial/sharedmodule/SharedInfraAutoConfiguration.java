package com.tutorial.sharedmodule;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@ComponentScan(basePackages = "com.tutorial.sharedmodule")
@EnableJpaRepositories(basePackages = "com.tutorial.sharedmodule")
@EntityScan(basePackages = "com.tutorial.sharedmodule")
public class SharedInfraAutoConfiguration {
}
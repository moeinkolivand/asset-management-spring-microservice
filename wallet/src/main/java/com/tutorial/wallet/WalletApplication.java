package com.tutorial.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

import java.util.Map;

@SpringBootApplication
@EnableKafka
@EnableJpaRepositories(basePackages = "com.tutorial.wallet")
@EntityScan(basePackages = "com.tutorial.wallet")
public class WalletApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(WalletApplication.class);
        boolean commandMode = WalletCommandDispatcher.hasCommand(args);
        if (commandMode) {
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setDefaultProperties(Map.of("spring.kafka.listener.auto-startup", "false"));
        }

        ConfigurableApplicationContext context = application.run(args);
        if (commandMode) {
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
        }
    }

}

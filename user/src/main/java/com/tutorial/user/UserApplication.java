package com.tutorial.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

@SpringBootApplication
public class UserApplication {

  public static void main(String[] args) {
    System.out.println("The User Service Is Up");
    SpringApplication application = new SpringApplication(UserApplication.class);
    boolean commandMode = UserCommandDispatcher.hasCommand(args);
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

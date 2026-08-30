package com.tutorial.user;

import com.tutorial.user.user.AdminUserSeeder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class UserCommandDispatcher implements ApplicationRunner {
    private static final String SEED_ADMIN = "seed-admin";

    private final AdminUserSeeder adminUserSeeder;

    public UserCommandDispatcher(AdminUserSeeder adminUserSeeder) {
        this.adminUserSeeder = adminUserSeeder;
    }

    public static boolean hasCommand(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--command=")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("command")) {
            return;
        }

        if (args.getOptionValues("command") == null || args.getOptionValues("command").size() != 1) {
            throw new IllegalArgumentException("The --command option must have exactly one value");
        }
        String command = args.getOptionValues("command").get(0);
        if (SEED_ADMIN.equals(command)) {
            adminUserSeeder.seed();
            return;
        }

        throw new IllegalArgumentException(
                "Unknown command '" + command + "'. Supported commands: " + SEED_ADMIN);
    }
}

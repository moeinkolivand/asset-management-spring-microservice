package com.tutorial.wallet;

import com.tutorial.wallet.currency.CurrencyDataSeeder;
import com.tutorial.wallet.wallet.SystemWalletSeeder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WalletCommandDispatcher implements ApplicationRunner {
    private static final String SEED_CURRENCY = "seed-currency";
    private static final String SEED_SYSTEM_WALLET = "seed-system-wallet";

    private final CurrencyDataSeeder currencyDataSeeder;
    private final SystemWalletSeeder systemWalletSeeder;

    public WalletCommandDispatcher(
            CurrencyDataSeeder currencyDataSeeder,
            SystemWalletSeeder systemWalletSeeder) {
        this.currencyDataSeeder = currencyDataSeeder;
        this.systemWalletSeeder = systemWalletSeeder;
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
        switch (command) {
            case SEED_CURRENCY -> currencyDataSeeder.seed();
            case SEED_SYSTEM_WALLET -> systemWalletSeeder.seed();
            default -> throw new IllegalArgumentException(
                    "Unknown command '" + command + "'. Supported commands: "
                            + SEED_CURRENCY + ", " + SEED_SYSTEM_WALLET);
        }
    }
}

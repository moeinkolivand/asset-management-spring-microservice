package com.tutorial.wallet.wallet;
import com.tutorial.wallet.currency.Currency;
import com.tutorial.wallet.currency.CurrencyApiImpl;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(2)
public class SystemWalletSeeder implements CommandLineRunner {
    private final WalletRepository walletRepository;
    private final CurrencyApiImpl currencyApi;

    public SystemWalletSeeder(WalletRepository walletRepository, CurrencyApiImpl currencyApi) {
        this.walletRepository = walletRepository;
        this.currencyApi = currencyApi;
    }


    @Override
    @Transactional
    public void run(String... args) {

        Long adminUserId = 10203048859L;
        String currencyName = "USDT";
        Currency usdtCurrency = currencyApi.getCurrencyByName(currencyName)
                .orElseThrow(() -> new IllegalStateException(
                        "USDT currency not found! Please ensure CurrencySeeder (@Order(2)) ran first."
                ));

        if (walletRepository.findByCurrencyIdAndUserId(usdtCurrency.getId(), adminUserId).isEmpty()) {
            Wallet systemWallet = new Wallet(
                    "SYSTEM_USDT_WALLET",
                    BigDecimal.valueOf(1_000_000.00),
                    adminUserId,
                    usdtCurrency.getId()
            );
            systemWallet.setWalletStatus(WalletStatus.ACTIVE);

            walletRepository.save(systemWallet);

            System.out.println("System wallet created successfully for Admin with 1,000,000 USDT!");
        } else {
            System.out.println("System wallet already exists for Admin + USDT. Skipping.");
        }
    }
}
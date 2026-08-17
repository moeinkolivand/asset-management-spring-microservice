package com.tutorial.wallet.currency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CurrencyDataSeeder implements CommandLineRunner {
    private final CurrencyRepository currencyRepository;

    @Autowired
    public CurrencyDataSeeder(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }


    @Override
    public void run(String... args) throws Exception {
        String currencyName = "USDT";
        if (currencyRepository.findByCurrencyName(currencyName).isEmpty())
            currencyRepository.save(new Currency(currencyName));
    }
}

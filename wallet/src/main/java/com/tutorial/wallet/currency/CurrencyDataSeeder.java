package com.tutorial.wallet.currency;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class CurrencyDataSeeder implements CommandLineRunner {
  private final CurrencyRepository currencyRepository;

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

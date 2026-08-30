package com.tutorial.wallet.currency;

import org.springframework.stereotype.Component;

@Component
public class CurrencyDataSeeder {
  private final CurrencyRepository currencyRepository;

  public CurrencyDataSeeder(CurrencyRepository currencyRepository) {
    this.currencyRepository = currencyRepository;
  }

  public void seed() {
    String currencyName = "USDT";
    if (currencyRepository.findByCurrencyName(currencyName).isEmpty())
      currencyRepository.save(new Currency(currencyName));
  }
}

package com.tutorial.wallet.currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CurrencyApiImpl {
    private final CurrencyRepository currencyRepository;

    @Autowired
    public CurrencyApiImpl(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public boolean currencyExistsById(Long id) {
        return currencyRepository.existsById(id);
    }

}

package com.tutorial.wallet.wallet;

import com.tutorial.wallet.wallet.dto.*;
import com.tutorial.wallet.currency.CurrencyApiImpl;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final CurrencyApiImpl currencyApi;

    @Autowired
    public WalletService(WalletRepository walletRepository, CurrencyApiImpl currencyApi) {
        this.walletRepository = walletRepository;
        this.currencyApi = currencyApi;
    }

    public WalletResponseDto createWallet(WalletRequestDto walletRequestDto, Long userId) {
        if (currencyApi.currencyExistsById(walletRequestDto.currencyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency Not Found");
        }
        if (walletRepository.existsByUserIdAndCurrencyId(userId, walletRequestDto.currencyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet With Currency Already exists");
        }
        Wallet wallet = new Wallet(walletRequestDto.name(), BigDecimal.ZERO, userId, walletRequestDto.currencyId());
        walletRepository.save(wallet);
        return returnWalletResponse(wallet);
    }

    public WalletResponseDto getWalletWithId(Long id) {
        Wallet wallet = getWalletById(id);
        return returnWalletResponse(wallet);
    }


    public WalletResponseDto updateWallet(Long id, WalletRequestDto walletRequestDto) {
        Wallet wallet = getWalletById(id);
        wallet.setName(walletRequestDto.name());
        Wallet updatedWallet = walletRepository.save(wallet);
        return returnWalletResponse(updatedWallet);
    }

    private Wallet getWalletById(Long id) {
        return walletRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Wallet Not Found"));
    }

    private WalletResponseDto returnWalletResponse(Wallet wallet) {
        return new WalletResponseDto(
                wallet.getName(),
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCurrencyId(),
                wallet.getCreatedAt()
        );
    }

    public @Nullable List<WalletResponseDto> getUserWallets(Long userId) {
        return walletRepository.findByUserId(userId).stream().map(this::returnWalletResponse).toList();
    }

}

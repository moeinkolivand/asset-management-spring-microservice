package com.tutorial.wallet.wallet.consumer;

import com.tutorial.shared.wallet.events.WithdrawDtoEvent;
import com.tutorial.wallet.wallet.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WithdrawConsumer {
    private final WalletService walletService;

    @Autowired
    public WithdrawConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(topics = "wallet-withdraw", groupId = "wallet-group")
    public void withdrawHandler(WithdrawDtoEvent withdrawDtoEvent) {
        walletService.withdraw(withdrawDtoEvent);
    }
}

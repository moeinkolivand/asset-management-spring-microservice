package com.tutorial.transaction.transaction.consumer;

import com.tutorial.shared.wallet.events.WithdrawResultDtoEvent;
import com.tutorial.sharedmodule.infra.KafkaTopics;
import com.tutorial.transaction.transaction.TransactionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WithdrawConsumer {
    private final TransactionService transactionService;

    public WithdrawConsumer(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @KafkaListener(topics = KafkaTopics.WALLET_TRANSFER_RESPONSE, groupId = "transaction-group")
    private void handleWalletTransferResponseEvent(WithdrawResultDtoEvent dtoEvent) {
        transactionService.processWalletTransferEvent(dtoEvent);
    }
}

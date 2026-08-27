package com.tutorial.transaction.transaction.consumer;

import com.tutorial.shared.wallet.events.WithdrawFailedDtoEvent;
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

    @KafkaListener(topics = KafkaTopics.WITHDRAW_FAILED, groupId = "transaction-group")
    private void failedWithdrawConsumer(WithdrawFailedDtoEvent dtoEvent) {
        transactionService.failedWithdraw(dtoEvent);
    }
}

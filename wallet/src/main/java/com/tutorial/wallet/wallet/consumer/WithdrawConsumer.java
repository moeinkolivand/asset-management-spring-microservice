package com.tutorial.wallet.wallet.consumer;

import com.tutorial.shared.wallet.events.TransferDtoEvent;
import com.tutorial.sharedmodule.infra.KafkaTopics;
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

  @KafkaListener(topics = KafkaTopics.WALLET_TRANSFER, groupId = "wallet-group")
  public void withdrawHandler(TransferDtoEvent withdrawDtoEvent) {
    walletService.withdraw(withdrawDtoEvent);
  }
}

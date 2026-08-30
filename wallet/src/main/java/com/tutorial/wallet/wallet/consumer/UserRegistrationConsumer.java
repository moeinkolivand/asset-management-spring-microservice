package com.tutorial.wallet.wallet.consumer;


import com.tutorial.shared.user.events.UserRegisteredEvent;
import com.tutorial.wallet.wallet.WalletService;
import com.tutorial.sharedmodule.infra.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationConsumer.class);
    private final WalletService walletService;

    @Autowired
    public UserRegistrationConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "wallet-group")
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("Received event: User ID {} registered. Creating default wallet...", event.getUserId());

        try {
             if ("ADMIN".equals(event.getRole())) {
                 walletService.createSystemWallet(event.getUserId());
             } else {
                 walletService.createDefaultWallet(event.getUserId());
             }
            log.info("Default wallet created for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create wallet for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}
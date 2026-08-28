package com.tutorial.sharedmodule.infra;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String WALLET_TRANSFER = "wallet-transfer";

    public static final String WALLET_TRANSFER_RESPONSE = "wallet-transfer-response";

    public static final String WITHDRAW_FAILED =  "withdraw-failed";


    public static final String DEPOSIT_REQUESTED = "wallet-deposit";

    public static final String DEPOSIT_SUCCESS = "deposit-success";

    public static final String DEPOSIT_FAILED =  "deposit-failed";

}
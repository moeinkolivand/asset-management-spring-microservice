package com.tutorial.sharedmodule.infra;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String WITHDRAW_REQUESTED = "wallet-withdraw";

    public static final String WITHDRAW_SUCCESS = "withdraw-success";

    public static final String WITHDRAW_FAILED =  "withdraw-failed";
}
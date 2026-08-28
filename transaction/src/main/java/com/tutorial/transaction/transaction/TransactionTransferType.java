package com.tutorial.transaction.transaction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionTransferType {
    WITHDRAW(1),
    DEPOSIT(2);

    private final int value;

    TransactionTransferType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static TransactionTransferType fromValue(int value) {
        for (TransactionTransferType type : values()) {
            if (type.value == value) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid transferType: " + value
        );
    }
    }

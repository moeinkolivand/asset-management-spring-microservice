package com.tutorial.transaction.transaction;

public class TransactionAlreadyProccesedException extends RuntimeException {
    public TransactionAlreadyProccesedException(String message) {
        super(message);
    }
}

package com.tutorial.wallet.wallet;

public class ForbiddenActionOnFreezeWalletException extends RuntimeException {
    public ForbiddenActionOnFreezeWalletException(String message) {
        super(message);
    }
}

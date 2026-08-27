package com.tutorial.wallet.wallet;

import java.io.IOException;

public class OutboxSerializationException extends Exception {
  public OutboxSerializationException(String s, IOException e) {}
}

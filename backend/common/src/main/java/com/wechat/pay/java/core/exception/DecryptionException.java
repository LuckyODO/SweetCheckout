package com.wechat.pay.java.core.exception;

public class DecryptionException extends WechatPayException {
  public DecryptionException(String message, Throwable throwable) {
    super(message, throwable);
  }
}

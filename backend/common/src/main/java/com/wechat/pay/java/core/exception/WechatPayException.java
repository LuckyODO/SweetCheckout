package com.wechat.pay.java.core.exception;

/**
 * 微信支付异常基类
 */
public abstract class WechatPayException extends RuntimeException {
    public WechatPayException(String message) {
        super(message);
    }

    public WechatPayException(String message, Throwable cause) {
        super(message, cause);
    }
}

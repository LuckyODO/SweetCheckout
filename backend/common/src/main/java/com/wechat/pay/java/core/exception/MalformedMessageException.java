package com.wechat.pay.java.core.exception;

/**
 * 解析微信支付应答或回调报文异常时抛出，例如回调通知参数不正确、应答类型错误。
 */
public class MalformedMessageException extends WechatPayException {

    public MalformedMessageException(String message) {
        super(message);
    }

    public MalformedMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.wechat.pay.api;

import com.wechat.pay.utils.WXPayUtility;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

/**
 * 微信支付接口配置
 */
public class WXPay {
    private final String host;
    private final String appId;
    private final String mchId;
    private final String certificateSerialNo;
    private final PrivateKey privateKey;
    private final String wechatPayPublicKeyId;
    private final PublicKey wechatPayPublicKey;

    public WXPay(String host, String appId, String mchId, String certificateSerialNo, PrivateKey privateKey, String wechatPayPublicKeyId, PublicKey wechatPayPublicKey) {
        this.host = host;
        this.appId = appId;
        this.mchId = mchId;
        this.certificateSerialNo = certificateSerialNo;
        this.privateKey = privateKey;
        this.wechatPayPublicKeyId = wechatPayPublicKeyId;
        this.wechatPayPublicKey = wechatPayPublicKey;
    }

    public String getHost() {
        return host;
    }

    public String getAppId() {
        return appId;
    }

    public String getMerchantId() {
        return mchId;
    }

    public String getPublicKeyId() {
        return wechatPayPublicKeyId;
    }

    /**
     * @see WXPayUtility#buildAuthorization(String, String, PrivateKey, String, String, String)
     */
    public String buildAuthorization(String method, String uri, String requestBody) {
        return WXPayUtility.buildAuthorization(mchId, certificateSerialNo, privateKey, method, uri, requestBody);
    }

    public boolean isInvalidPublicKey(String serialNumber) {
        return !Objects.equals(serialNumber, wechatPayPublicKeyId);
    }

    /**
     * @see WXPayUtility#verify(String, String, String, PublicKey)
     */
    public boolean verify(String message, String signature, String algorithm) {
        return WXPayUtility.verify(message, signature, algorithm, wechatPayPublicKey);
    }
}

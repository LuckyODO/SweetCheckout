package com.wechat.pay.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.wechat.pay.utils.WXPayUtility;
import okhttp3.*;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 关闭订单，修改自<a href="https://pay.weixin.qq.com/doc/v3/merchant/4012791881">官方文档</a>
 */
public class CloseOrder {
    private static final String METHOD = "POST";
    private static final String PATH = "/v3/pay/transactions/out-trade-no/{out_trade_no}/close";

    public CloseOrder(WXPay config) {
        this.config = config;
    }

    @SuppressWarnings({"deprecation"})
    public void run(OrderRequest request) {
        request.mchid = config.getMerchantId();
        String uri = PATH;
        uri = uri.replace("{out_trade_no}", WXPayUtility.urlEncode(request.outTradeNo));
        String reqBody = WXPayUtility.toJson(request);

        Request.Builder reqBuilder = new Request.Builder().url(config.getHost() + uri);
        reqBuilder.addHeader("Accept", "application/json");
        reqBuilder.addHeader("Wechatpay-Serial", config.getPublicKeyId());
        reqBuilder.addHeader("Authorization", config.buildAuthorization(METHOD, uri, reqBody));
        reqBuilder.addHeader("Content-Type", "application/json");
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), reqBody);
        reqBuilder.method(METHOD, requestBody);
        Request httpRequest = reqBuilder.build();

        // 发送HTTP请求
        OkHttpClient client = new OkHttpClient.Builder().build();
        try (Response httpResponse = client.newCall(httpRequest).execute()) {
            String respBody = WXPayUtility.extractBody(httpResponse);
            if (httpResponse.code() >= 200 && httpResponse.code() < 300) {
                // 2XX 成功，验证应答签名
                WXPayUtility.validateResponse(config, httpResponse.headers(), respBody);
            } else {
                throw new WXPayUtility.ApiException(httpResponse.code(), respBody, httpResponse.headers());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Sending request to " + uri + " failed.", e);
        }
    }

    private final WXPay config;

    public static class OrderRequest {
        @SerializedName("mchid")
        public String mchid;

        @SerializedName("out_trade_no")
        @Expose(serialize = false)
        public String outTradeNo;
    }

}

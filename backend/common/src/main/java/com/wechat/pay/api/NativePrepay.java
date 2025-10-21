package com.wechat.pay.api;

import com.wechat.pay.utils.WXPayUtility;

import com.google.gson.annotations.SerializedName;
import okhttp3.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Native下单，修改自<a href="https://pay.weixin.qq.com/doc/v3/merchant/4012791877">官方文档</a>
 */
public class NativePrepay {
    private static final String METHOD = "POST";
    private static final String PATH = "/v3/pay/transactions/native";

    public NativePrepay(WXPay config) {
        this.config = config;
    }

    public Response run(CommonPrepayRequest request) {
        request.appid = config.getAppId();
        request.mchid = config.getMerchantId();
        String uri = PATH;
        String reqBody = WXPayUtility.toJson(request);

        Request.Builder reqBuilder = new Request.Builder().url(config.getHost() + uri);
        reqBuilder.addHeader("Accept", "application/json");
        reqBuilder.addHeader("Wechatpay-Serial", config.getPublicKeyId());
        reqBuilder.addHeader("Authorization", config.buildAuthorization(METHOD, uri, reqBody));
        reqBuilder.addHeader("Content-Type", "application/json");
        RequestBody requestBody = RequestBody.create(reqBody, MediaType.parse("application/json; charset=utf-8"));
        reqBuilder.method(METHOD, requestBody);
        Request httpRequest = reqBuilder.build();

        // 发送HTTP请求
        OkHttpClient client = new OkHttpClient.Builder().build();
        try (okhttp3.Response httpResponse = client.newCall(httpRequest).execute()) {
            String respBody = WXPayUtility.extractBody(httpResponse);
            if (httpResponse.code() >= 200 && httpResponse.code() < 300) {
                // 2XX 成功，验证应答签名
                WXPayUtility.validateResponse(config, httpResponse.headers(), respBody);

                // 从HTTP应答报文构建返回数据
                return WXPayUtility.fromJson(respBody, Response.class);
            } else {
                throw new WXPayUtility.ApiException(httpResponse.code(), respBody, httpResponse.headers());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Sending request to " + uri + " failed.", e);
        }
    }

    private final WXPay config;

    public static class CommonPrepayRequest {
        @SerializedName("appid")
        public String appid;

        @SerializedName("mchid")
        public String mchid;

        @SerializedName("description")
        public String description;

        @SerializedName("out_trade_no")
        public String outTradeNo;

        @SerializedName("time_expire")
        public String timeExpire;

        @SerializedName("attach")
        public String attach;

        @SerializedName("notify_url")
        public String notifyUrl;

        @SerializedName("goods_tag")
        public String goodsTag;

        @SerializedName("support_fapiao")
        public Boolean supportFapiao;

        @SerializedName("amount")
        public CommonAmountInfo amount;

        @SerializedName("detail")
        public CouponInfo detail;

        @SerializedName("scene_info")
        public CommonSceneInfo sceneInfo;

        @SerializedName("settle_info")
        public SettleInfo settleInfo;
    }

    public static class Response {
        @SerializedName("code_url")
        public String codeUrl;

        public String getCodeUrl() {
            return codeUrl;
        }
    }

    public static class CommonAmountInfo {
        @SerializedName("total")
        public Long total;

        @SerializedName("currency")
        public String currency;
    }

    public static class CouponInfo {
        @SerializedName("cost_price")
        public Long costPrice;

        @SerializedName("invoice_id")
        public String invoiceId;

        @SerializedName("goods_detail")
        public List<GoodsDetail> goodsDetail;
    }

    public static class CommonSceneInfo {
        @SerializedName("payer_client_ip")
        public String payerClientIp;

        @SerializedName("device_id")
        public String deviceId;

        @SerializedName("store_info")
        public StoreInfo storeInfo;
    }

    public static class SettleInfo {
        @SerializedName("profit_sharing")
        public Boolean profitSharing;
    }

    public static class GoodsDetail {
        @SerializedName("merchant_goods_id")
        public String merchantGoodsId;

        @SerializedName("wechatpay_goods_id")
        public String wechatpayGoodsId;

        @SerializedName("goods_name")
        public String goodsName;

        @SerializedName("quantity")
        public Long quantity;

        @SerializedName("unit_price")
        public Long unitPrice;
    }

    public static class StoreInfo {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;

        @SerializedName("area_code")
        public String areaCode;

        @SerializedName("address")
        public String address;
    }

}

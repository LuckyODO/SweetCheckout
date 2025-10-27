package com.wechat.pay.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.wechat.pay.utils.WXPayUtility;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信支付商户订单号查询订单，修改自<a href="https://pay.weixin.qq.com/doc/v3/merchant/4012791880">官方文档</a>
 */
public class QueryByOutTradeNo {
    private static final String METHOD = "GET";
    private static final String PATH = "/v3/pay/transactions/out-trade-no/{out_trade_no}";

    public QueryByOutTradeNo(WXPay config) {
        this.config = config;
    }

    public Response run(QueryByOutTradeNoRequest request) {
        request.mchid = config.getMerchantId();
        String uri = PATH.replace("{out_trade_no}", WXPayUtility.urlEncode(request.outTradeNo));
        Map<String, Object> args = new HashMap<>();
        args.put("mchid", request.mchid);
        String queryString = WXPayUtility.urlEncode(args);
        if (!queryString.isEmpty()) {
            uri = uri + "?" + queryString;
        }

        Request.Builder reqBuilder = new Request.Builder().url(config.getHost() + uri);
        reqBuilder.addHeader("Accept", "application/json");
        reqBuilder.addHeader("Wechatpay-Serial", config.getPublicKeyId());
        reqBuilder.addHeader("Authorization", config.buildAuthorization(METHOD, uri, null));
        reqBuilder.method(METHOD, null);
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

    public static class QueryByOutTradeNoRequest {
        @SerializedName("mchid")
        @Expose(serialize = false)
        public String mchid;

        @SerializedName("out_trade_no")
        @Expose(serialize = false)
        public String outTradeNo;
    }

    public static class Response {
        @SerializedName("appid")
        public String appid;

        @SerializedName("mchid")
        public String mchid;

        @SerializedName("out_trade_no")
        public String outTradeNo;

        @SerializedName("transaction_id")
        public String transactionId;

        @SerializedName("trade_type")
        public String tradeType;

        @SerializedName("trade_state")
        public String tradeState;

        @SerializedName("trade_state_desc")
        public String tradeStateDesc;

        @SerializedName("bank_type")
        public String bankType;

        @SerializedName("attach")
        public String attach;

        @SerializedName("success_time")
        public String successTime;

        @SerializedName("payer")
        public CommRespPayerInfo payer;

        @SerializedName("amount")
        public CommRespAmountInfo amount;

        @SerializedName("scene_info")
        public CommRespSceneInfo sceneInfo;

        @SerializedName("promotion_detail")
        public List<PromotionDetail> promotionDetail;
    }

    public static class CommRespPayerInfo {
        @SerializedName("openid")
        public String openid;
    }

    public static class CommRespAmountInfo {
        @SerializedName("total")
        public Long total;

        @SerializedName("payer_total")
        public Long payerTotal;

        @SerializedName("currency")
        public String currency;

        @SerializedName("payer_currency")
        public String payerCurrency;
    }

    public static class CommRespSceneInfo {
        @SerializedName("device_id")
        public String deviceId;
    }

    public static class PromotionDetail {
        @SerializedName("coupon_id")
        public String couponId;

        @SerializedName("name")
        public String name;

        @SerializedName("scope")
        public String scope;

        @SerializedName("type")
        public String type;

        @SerializedName("amount")
        public Long amount;

        @SerializedName("stock_id")
        public String stockId;

        @SerializedName("wechatpay_contribute")
        public Long wechatpayContribute;

        @SerializedName("merchant_contribute")
        public Long merchantContribute;

        @SerializedName("other_contribute")
        public Long otherContribute;

        @SerializedName("currency")
        public String currency;

        @SerializedName("goods_detail")
        public List<GoodsDetailInPromotion> goodsDetail;
    }

    public static class GoodsDetailInPromotion {
        @SerializedName("goods_id")
        public String goodsId;

        @SerializedName("quantity")
        public Long quantity;

        @SerializedName("unit_price")
        public Long unitPrice;

        @SerializedName("discount_amount")
        public Long discountAmount;

        @SerializedName("goods_remark")
        public String goodsRemark;
    }

}

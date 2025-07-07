/**
 * Alipay.com Inc. Copyright (c) 2004-2012 All Rights Reserved.
 */
package com.alipay.api;

import java.util.Map;

/**
 * @author runzhi
 */
public interface AlipayClient {

    <T extends AlipayResponse> T execute(AlipayRequest<T> request) throws AlipayApiException;

    <T extends AlipayResponse> T execute(AlipayRequest<T> request,
                                         String authToken) throws AlipayApiException;

    <T extends AlipayResponse> T execute(AlipayRequest<T> request, String accessToken,
                                         String appAuthToken) throws AlipayApiException;

    <T extends AlipayResponse> T execute(AlipayRequest<T> request, String accessToken,
                                         String appAuthToken, String targetAppId) throws AlipayApiException;

    <T extends AlipayResponse> T pageExecute(AlipayRequest<T> request) throws AlipayApiException;

    /**
     * SDK客户端调用生成sdk字符串
     */
    <T extends AlipayResponse> T sdkExecute(AlipayRequest<T> request) throws AlipayApiException;

    <T extends AlipayResponse> T pageExecute(AlipayRequest<T> request,
                                             String method) throws AlipayApiException;

    /**
     * 移动客户端同步结果返回解析的参考工具方法
     *
     * @param result       移动客户端SDK同步返回的结果map，一般包含resultStatus，result和memo三个key
     * @param requestClazz 接口请求request类，如App支付传入 AlipayTradeAppPayRequest.class
     * @return 同步返回结果的response对象
     */
    <TR extends AlipayResponse, T extends AlipayRequest<TR>> TR parseAppSyncResult(Map<String, String> result,
                                                                                   Class<T> requestClazz) throws AlipayApiException;

    /**
     * 批量调用
     */
    BatchAlipayResponse execute(BatchAlipayRequest request) throws AlipayApiException;

    /**
     * 证书类型调用
     */
    <T extends AlipayResponse> T certificateExecute(AlipayRequest<T> request) throws AlipayApiException;

    <T extends AlipayResponse> T certificateExecute(AlipayRequest<T> request,
                                                    String authToken) throws AlipayApiException;

    <T extends AlipayResponse> T certificateExecute(AlipayRequest<T> request, String accessToken,
                                                    String appAuthToken) throws AlipayApiException;

    <T extends AlipayResponse> T certificateExecute(AlipayRequest<T> request, String accessToken,
                                                    String appAuthToken, String targetAppId) throws AlipayApiException;
}

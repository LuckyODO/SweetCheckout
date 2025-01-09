package top.mrxiaom.sweet.checkout.backend.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import top.mrxiaom.sweet.checkout.backend.Configuration;
import top.mrxiaom.sweet.checkout.backend.ConsoleMain;
import top.mrxiaom.sweet.checkout.backend.PaymentServer;
import top.mrxiaom.sweet.checkout.backend.data.ClientInfo;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentCancel;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentConfirm;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginRequestOrder;

import java.util.*;

public class PaymentAlipay {
    PaymentServer server;
    public Map<String, ClientInfo.Order> moneyLocked = new HashMap<>();
    public PaymentAlipay(PaymentServer server) {
        this.server = server;
    }

    // TODO: 暂无Hook实现计划，先把接口摆在这
    public PacketPluginRequestOrder.Response handleHook(PacketPluginRequestOrder packet, ClientInfo client, Configuration config) {
        Configuration.AlipayHook hook = config.getHook().getAlipay();
        if (moneyLocked.containsKey(packet.getPrice())) {
            return new PacketPluginRequestOrder.Response("payment.hook-price-locked");
        }
        String orderId = client.nextOrderId();
        String paymentUrl = hook.getPaymentUrls().getOrDefault(packet.getPrice(), hook.getPaymentUrl());
        ClientInfo.Order order = client.createOrder(orderId, "alipay", packet.getPlayerName(), packet.getPrice());
        moneyLocked.put(packet.getPrice(), order);
        return new PacketPluginRequestOrder.Response("hook", orderId, paymentUrl);
    }

    public PacketPluginRequestOrder.Response handleFaceToFace(PacketPluginRequestOrder packet, ClientInfo client, Configuration config) {
        String orderId = client.nextOrderId();
        if (orderId == null) {
            return new PacketPluginRequestOrder.Response("payment.can-not-create-id");
        }
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(config.getAlipayFaceToFace().getConfig());

            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setOutTradeNo(orderId);
            model.setTotalAmount(packet.getPrice());
            model.setSubject(packet.getProductName());
            model.setProductCode(config.getAlipayFaceToFace().getProduceCode());
            model.setBody(packet.getProductName());

            request.setBizModel(model);

            AlipayTradePrecreateResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                ClientInfo.Order order = client.createOrder(orderId, "alipay", packet.getPlayerName(), packet.getPrice());
                String outTradeNo = response.getOutTradeNo();
                order.setCancelAction(() -> cancelAlipayFaceToFace(outTradeNo));
                // 轮询检查是否交易成功
                order.setTask(new TimerTask() {
                    @Override
                    public void run() {
                        checkAlipayFaceToFace(client, this, orderId, outTradeNo);
                    }
                });
                // 每3秒检查一次是否支付成功
                server.getTimer().schedule(order.getTask(), 1000L, 3000L);
                server.getLogger().info("支付宝 订单码支付 下单成功 {} : {}", response.getMsg(), response.getOutTradeNo());
                return new PacketPluginRequestOrder.Response("face2face", orderId, response.getQrCode());
            } else {
                client.removeOrder(orderId);
                server.getLogger().warn("支付宝 订单码支付 调用失败 {}, {} {}", response.getMsg(), response.getSubCode(), response.getSubMsg());
                return new PacketPluginRequestOrder.Response("payment.internal-error");
            }
        } catch (AlipayApiException e) {
            client.removeOrder(orderId);
            server.getLogger().warn("支付宝 订单码支付 API执行错误", e);
            return new PacketPluginRequestOrder.Response("payment.internal-error");
        }
    }

    private void checkAlipayFaceToFace(ClientInfo client, TimerTask task, String orderId, String outTradeNo) {
        Configuration config = ConsoleMain.getConfig();
        ClientInfo.Order order = client.getOrder(orderId);
        if (order == null || !client.getWebSocket().isOpen()) { // 插件连接断开时、任务不存在时取消任务，并关闭交易
            task.cancel();
            if (order != null) {
                order.setTask(null);
                client.removeOrder(order);
            }
            cancelAlipayFaceToFace(outTradeNo);
            return;
        }
        try {
            // 统一收款订单交易查询
            AlipayClient alipayClient = new DefaultAlipayClient(config.getAlipayFaceToFace().getConfig());

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();

            model.setOutTradeNo(outTradeNo);
            List<String> queryOptions = new ArrayList<>();
            queryOptions.add("trade_settle_info");
            model.setQueryOptions(queryOptions);

            request.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String status = response.getTradeStatus();
                switch (status.toUpperCase()) {
                    case "WAIT_BUYER_PAY": // 等待买家付款
                        break;
                    case "TRADE_CLOSED": // 超时未付款，交易关闭
                        client.removeOrder(order);
                        server.send(client.getWebSocket(), new PacketBackendPaymentCancel(orderId, "payment.timeout"));
                        break;
                    case "TRADE_SUCCESS": // 交易支付成功
                    case "TRADE_FINISHED": {// 交易结束，不可退款
                        client.removeOrder(order);
                        // 买家支付宝账号，通常是打码的手机号
                        String buyerLogonId = response.getBuyerLogonId();
                        String money = response.getReceiptAmount();
                        server.getLogger().info("[收款] 从支付宝 订单码支付 收款，来自 {} 的 ￥{}", buyerLogonId, money);
                        server.send(client.getWebSocket(), new PacketBackendPaymentConfirm(orderId, money));
                        break;
                    }
                }
            } else {
                if (response.getSubCode().equals("ACQ.TRADE_NOT_EXIST")) {
                    // 忽略交易不存在的情况
                    // precreate 之后，只要用户不扫码，交易就不存在
                    // 订单一预创建就开始轮询，不应该处理交易不存在的情况
                    return;
                }
                server.getLogger().warn("支付宝 订单码支付 检查订单失败 {}, {} {}，查询的订单号 {}\n    {}", response.getMsg(), response.getSubCode(), response.getSubMsg(), outTradeNo, response.getBody());
            }
        } catch (AlipayApiException e) {
            server.getLogger().warn("支付宝 订单码支付 API检查订单时执行错误", e);
        }
    }

    private void cancelAlipayFaceToFace(String outTradeNo) {
        try {
            Configuration config = ConsoleMain.getConfig();
            AlipayClient alipayClient = new DefaultAlipayClient(config.getAlipayFaceToFace().getConfig());
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            AlipayTradeCloseModel model = new AlipayTradeCloseModel();
            model.setOutTradeNo(outTradeNo);
            request.setBizModel(model);
            AlipayTradeCloseResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                if (!response.getSubCode().equals("ACQ.TRADE_NOT_EXIST")) {
                    server.getLogger().warn("支付宝 订单码支付 关闭订单失败 {}, {} {}，要关闭的订单号 {}\n    {}", response.getMsg(), response.getSubCode(), response.getSubMsg(), outTradeNo, response.getBody());
                }
            }
        } catch (AlipayApiException e) {
            server.getLogger().warn("支付宝 订单码支付 API关闭交易时执行错误", e);
        }
    }
}

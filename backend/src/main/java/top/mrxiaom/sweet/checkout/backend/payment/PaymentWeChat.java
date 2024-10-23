package top.mrxiaom.sweet.checkout.backend.payment;

import com.wechat.pay.java.service.partnerpayments.nativepay.NativePayService;
import com.wechat.pay.java.service.partnerpayments.nativepay.model.*;
import top.mrxiaom.sweet.checkout.backend.Configuration;
import top.mrxiaom.sweet.checkout.backend.ConsoleMain;
import top.mrxiaom.sweet.checkout.backend.PaymentServer;
import top.mrxiaom.sweet.checkout.backend.data.ClientInfo;
import top.mrxiaom.sweet.checkout.backend.util.Util;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentCancel;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentConfirm;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginRequestOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

public class PaymentWeChat {
    PaymentServer server;
    public Map<String, ClientInfo.Order> moneyLocked = new HashMap<>();
    public PaymentWeChat(PaymentServer server) {
        this.server = server;
    }

    public PacketPluginRequestOrder.Response handleHook(PacketPluginRequestOrder packet, ClientInfo client, Configuration config) {
        Configuration.WeChatHook hook = config.getHook().getWeChat();
        String requireProcess = hook.getRequireProcess();
        if (!requireProcess.isEmpty()) {
            if (ProcessHandle.allProcesses().noneMatch(it -> it.info().command().map(name -> name.equals(requireProcess)).orElse(false))) {
                return new PacketPluginRequestOrder.Response("payment.hook-not-running");
            }
        }
        if (moneyLocked.containsKey(packet.getPrice())) {
            return new PacketPluginRequestOrder.Response("payment.hook-price-locked");
        }
        String orderId = client.nextOrderId();
        String paymentUrl = hook.getPaymentUrls().getOrDefault(packet.getPrice(), hook.getPaymentUrl());
        ClientInfo.Order order = client.createOrder(orderId, "wechat", packet.getPlayerName(), packet.getPrice());
        moneyLocked.put(packet.getPrice(), order);
        return new PacketPluginRequestOrder.Response("hook", orderId, paymentUrl);
    }

    public PacketPluginRequestOrder.Response handleNative(PacketPluginRequestOrder packet, ClientInfo client, Configuration config) {
        // 微信支付的订单总金额单位为「分」，保留两位小数的结果去掉小数点，再转整数完事
        Integer priceWeChat = Util.parseInt(packet.getPrice().replace(".", "")).orElse(null);
        if (priceWeChat == null) {
            return new PacketPluginRequestOrder.Response("payment.not-a-number");
        }
        String orderId = client.nextOrderId();
        if (orderId == null) {
            return new PacketPluginRequestOrder.Response("payment.can-not-create-id");
        }
        NativePayService service = new NativePayService.Builder()
                .config(config.getWeChatNative().getConfig())
                .build();

        PrepayRequest request = new PrepayRequest();
        request.setSpAppid(config.getWeChatNative().getSpAppId());
        request.setSpMchid(config.getWeChatNative().getSpMerchantId());
        request.setSubMchid(config.getWeChatNative().getSubMerchantId());
        Amount amount = new Amount();
        amount.setTotal(priceWeChat);
        request.setAmount(amount);
        request.setDescription(packet.getProductName());
        request.setNotifyUrl(config.getWeChatNative().getNotifyUrl());
        request.setOutTradeNo(orderId);
        // 调用下单方法，得到应答
        PrepayResponse response = service.prepay(request);

        ClientInfo.Order order = client.createOrder(orderId, "wechat", packet.getPlayerName(), packet.getPrice());
        order.setCancelAction(() -> cancelWeChatNative(orderId));
        // 轮询检查是否交易成功
        order.setTask(new TimerTask() {
            @Override
            public void run() {
                checkWeChatNative(client, this, orderId);
            }
        });
        // 每3秒检查一次是否支付成功
        server.getTimer().schedule(order.getTask(), 1000L, 3000L);
        return new PacketPluginRequestOrder.Response("native", orderId, response.getCodeUrl());
    }

    private void checkWeChatNative(ClientInfo client, TimerTask task, String orderId) {
        Configuration config = ConsoleMain.getConfig();
        ClientInfo.Order order = client.getOrder(orderId);
        if (order == null || !client.getWebSocket().isOpen()) { // 插件连接断开时、任务不存在时取消任务，并关闭交易
            task.cancel();
            if (order != null) {
                order.setTask(null);
                client.removeOrder(order);
            }
            cancelWeChatNative(orderId);
            return;
        }
        NativePayService service = new NativePayService.Builder()
                .config(config.getWeChatNative().getConfig())
                .build();

        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setSpMchid(config.getWeChatNative().getSpMerchantId());
        request.setSubMchid(config.getWeChatNative().getSubMerchantId());
        request.setOutTradeNo(orderId);

        Transaction response = service.queryOrderByOutTradeNo(request);
        switch (response.getTradeState()) {
            case SUCCESS: // 支付成功
                client.removeOrder(order);
                String openId = response.getPayer().getSpOpenid();
                String money;
                if (response.getAmount().getPayerTotal() != null) {
                    money = String.format("%.2f", response.getAmount().getPayerTotal() / 100.0);
                } else {
                    money = order.getMoney();
                }
                server.getLogger().info("[收款] 从微信Native收款，来自 {} 的 ￥{}", openId, money);
                server.send(client.getWebSocket(), new PacketBackendPaymentConfirm(orderId, "微信用户", money));
                break;
            case REFUND: // 转入退款
            case CLOSED: // 已关闭
                client.removeOrder(order);
                server.send(client.getWebSocket(), new PacketBackendPaymentCancel(orderId, "payment.native." + response.getTradeState().name().toLowerCase()));
                break;
            case NOTPAY: // 未支付，忽略
            case ACCEPT: // ACCEPT 在文档中未定义，忽略
                break;
            case REVOKED: // 已撤销 (仅付款码，忽略)
            case PAYERROR: // 支付失败 (仅付款码，忽略)
            case USERPAYING: // 用户支付中 (仅付款码，忽略)
                break;
        }
    }

    private void cancelWeChatNative(String orderId) {
        Configuration config = ConsoleMain.getConfig();

        NativePayService service = new NativePayService.Builder()
                .config(config.getWeChatNative().getConfig())
                .build();

        CloseOrderRequest request = new CloseOrderRequest();
        request.setSpMchid(config.getWeChatNative().getSpMerchantId());
        request.setSubMchid(config.getWeChatNative().getSubMerchantId());
        request.setOutTradeNo(orderId);
        service.closeOrder(request);
    }
}

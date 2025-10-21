package top.mrxiaom.sweet.checkout.backend.payment;

import com.wechat.pay.api.CloseOrder;
import com.wechat.pay.api.NativePrepay;
import com.wechat.pay.api.QueryByOutTradeNo;
import top.mrxiaom.sweet.checkout.backend.AbstractPaymentServer;
import top.mrxiaom.sweet.checkout.backend.Configuration;
import top.mrxiaom.sweet.checkout.backend.data.ClientInfo;
import top.mrxiaom.sweet.checkout.backend.util.Util;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentCancel;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentConfirm;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginRequestOrder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

public class PaymentWeChat<C extends ClientInfo<C>> {
    AbstractPaymentServer<C> server;
    public Map<String, ClientInfo.Order<C>> moneyLocked = new HashMap<>();

    public PaymentWeChat(AbstractPaymentServer<C> server) {
        this.server = server;
    }

    public PacketPluginRequestOrder.Response handleHook(PacketPluginRequestOrder packet, C client, Configuration config) {
        Configuration.WeChatHook hook = config.getHook().getWeChat();
        String requireProcess = hook.getRequireProcess();
        if (!requireProcess.isEmpty()) {
            String target = File.separator + requireProcess;
            boolean notRunning = true;
            for (String command : server.getAllProcess()) {
                if (command.endsWith(target)) {
                    notRunning = false;
                    break;
                }
            }
            if (notRunning) {
                return new PacketPluginRequestOrder.Response("payment.hook-not-running");
            }
        }
        if (moneyLocked.containsKey(packet.getPrice())) {
            return new PacketPluginRequestOrder.Response("payment.hook-price-locked");
        }
        String orderId = client.nextOrderId();
        String paymentUrl = hook.getPaymentUrl(packet.getPrice());
        ClientInfo.Order<C> order = client.createOrder(orderId, "wechat", packet.getPlayerName(), packet.getPrice());
        moneyLocked.put(packet.getPrice(), order);
        return new PacketPluginRequestOrder.Response("hook", orderId, paymentUrl);
    }

    public PacketPluginRequestOrder.Response handleNative(PacketPluginRequestOrder packet, C client, Configuration config) {
        // 微信支付的订单总金额单位为「分」，保留两位小数的结果去掉小数点，再转整数完事
        Long priceWeChat = Util.parseLong(packet.getPrice().replace(".", "")).orElse(null);
        if (priceWeChat == null) {
            return new PacketPluginRequestOrder.Response("payment.not-a-number");
        }
        String orderId = client.nextOrderId();
        if (orderId == null) {
            return new PacketPluginRequestOrder.Response("payment.can-not-create-id");
        }
        NativePrepay service = new NativePrepay(config.getWeChatNative().getConfig());

        NativePrepay.CommonPrepayRequest request = new NativePrepay.CommonPrepayRequest();
        request.description = packet.getProductName();
        request.outTradeNo = orderId;
        request.notifyUrl = config.getWeChatNative().getNotifyUrl();
        request.amount = new NativePrepay.CommonAmountInfo();
        request.amount.total = priceWeChat;
        request.amount.currency = "CNY";

        // 调用下单方法，得到应答
        NativePrepay.Response response;
        try {
            response = service.run(request);
        } catch (RuntimeException e) {
            client.removeOrder(orderId);
            server.getLogger().warn("微信 Native支付 API执行错误", e);
            return new PacketPluginRequestOrder.Response("payment.internal-error");
        }

        ClientInfo.Order<C> order = client.createOrder(orderId, "wechat", packet.getPlayerName(), packet.getPrice());
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

    private void checkWeChatNative(C client, TimerTask task, String orderId) {
        Configuration config = server.getConfig();
        ClientInfo.Order<C> order = client.getOrder(orderId);
        if (order == null || !client.isOpen()) { // 插件连接断开时、任务不存在时取消任务，并关闭交易
            task.cancel();
            if (order != null) {
                order.setTask(null);
                client.removeOrder(order);
            }
            cancelWeChatNative(orderId);
            return;
        }
        QueryByOutTradeNo service = new QueryByOutTradeNo(config.getWeChatNative().getConfig());

        QueryByOutTradeNo.QueryByOutTradeNoRequest request = new QueryByOutTradeNo.QueryByOutTradeNoRequest();
        request.outTradeNo = orderId;

        QueryByOutTradeNo.Response response;
        try {
            response = service.run(request);
        } catch (RuntimeException e) {
            server.getLogger().warn("微信 Native支付 API检查订单时执行错误", e);
            return;
        }
        switch (response.tradeState) {
            case "SUCCESS": // 支付成功
                client.removeOrder(order);
                String openId = response.payer.openid;
                String money;
                if (response.amount.payerTotal != null) {
                    money = String.format("%.2f", response.amount.payerTotal / 100.0);
                } else {
                    money = order.getMoney();
                }
                server.getLogger().info("[收款] 从微信Native收款，来自 {} 的 ￥{}", openId, money);
                server.send(client, new PacketBackendPaymentConfirm(orderId, money));
                break;
            case "REFUND": // 转入退款
            case "CLOSED": // 已关闭
                client.removeOrder(order);
                server.send(client, new PacketBackendPaymentCancel(orderId, "payment.native." + response.tradeState.toLowerCase()));
                break;
            case "NOTPAY": // 未支付，忽略
                break;
            case "REVOKED": // 已撤销 (仅付款码，忽略)
            case "USERPAYING": // 用户支付中 (仅付款码，忽略)
            case "PAYERROR": // 支付失败 (仅付款码，忽略)
            default:
                break;
        }
    }

    private void cancelWeChatNative(String orderId) {
        Configuration config = server.getConfig();

        CloseOrder service = new CloseOrder(config.getWeChatNative().getConfig());

        CloseOrder.OrderRequest request = new CloseOrder.OrderRequest();
        request.outTradeNo = orderId;

        try {
            service.run(request);
        } catch (RuntimeException e) {
            server.getLogger().warn("微信 Native支付 API关闭交易时执行错误", e);
        }
    }
}

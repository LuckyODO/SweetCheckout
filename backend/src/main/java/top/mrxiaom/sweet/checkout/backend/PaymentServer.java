package top.mrxiaom.sweet.checkout.backend;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.*;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.google.gson.*;
import com.wechat.pay.java.service.partnerpayments.nativepay.NativePayService;
import com.wechat.pay.java.service.partnerpayments.nativepay.model.*;
import org.java_websocket.IDecodeInjector;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.util.Charsetfunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import top.mrxiaom.sweet.checkout.backend.data.ClientInfo;
import top.mrxiaom.sweet.checkout.backend.data.HookReceive;
import top.mrxiaom.sweet.checkout.backend.util.Util;
import top.mrxiaom.sweet.checkout.packets.PacketSerializer;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentCancel;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentConfirm;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginCancelOrder;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginRequestOrder;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@SuppressWarnings({"rawtypes", "unchecked"})
public class PaymentServer extends WebSocketServer implements IDecodeInjector {
    Logger logger;
    Map<String, List<BiFunction>> executors = new HashMap<>();
    Gson gson = new GsonBuilder().setLenient().create();
    Timer timer = new Timer();
    Map<String, ClientInfo.Order> moneyLocked = new HashMap<>();
    public PaymentServer(Logger logger, int port) {
        super(new InetSocketAddress(port));
        this.logger = logger;
        this.registerExecutor(PacketPluginRequestOrder.class, this::handleRequest);
        this.registerExecutor(PacketPluginCancelOrder.class, this::handleCancel);
    }

    public <T extends IPacket> void registerExecutor(Class<T> type, BiConsumer<T, WebSocket> executor) {
        registerExecutor(type, (packet, client) -> {
            executor.accept(packet, client);
            return null;
        });
    }
    public <S extends IPacket, T extends IPacket<S>> void registerExecutor(Class<T> type, BiFunction<T, WebSocket, S> executor) {
        String key = type.getName();
        List<BiFunction> list = executors.get(key);
        if (list == null) list = new ArrayList<>();
        list.add(executor);
        executors.put(key, list);
    }

    private PacketPluginRequestOrder.Response handleRequest(PacketPluginRequestOrder packet, WebSocket webSocket) {
        // 验证 price 是否符合格式，在可修正时自动修正格式
        Double priceDouble = Util.parseDouble(packet.getPrice()).orElse(null);
        if (priceDouble == null) {
            return new PacketPluginRequestOrder.Response("payment.not-a-number");
        }
        packet.setPrice(String.format("%.2f", priceDouble));

        ClientInfo client = getOrCreateInfo(webSocket);
        Configuration config = ConsoleMain.getConfig();

        if (client.getOrderByPlayer(packet.getPlayerName()) != null) {
            return new PacketPluginRequestOrder.Response("payment.already-requested");
        }

        if (packet.getType().equals("wechat")) {
            // 微信 Hook
            if (config.getHook().isEnable() && config.getHook().getWeChat().isEnable()) {
                Configuration.WeChatHook hook = config.getHook().getWeChat();
                String requireProcess = hook.getRequireProcess();
                if (ProcessHandle.allProcesses().noneMatch(it -> it.info().command().map(name -> name.equals(requireProcess)).orElse(false))) {
                    return new PacketPluginRequestOrder.Response("payment.hook-not-running");
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
            // 微信 Native
            if (config.getWeChatNative().isEnable()) {
                return handleRequestWeChatNative(packet, client, config);
            }
        }
        if (packet.getType().equals("alipay")) {
            // 支付宝 Hook TODO: 暂无实现计划
            if (config.getHook().isEnable()) {

            }
            // 支付宝当面付
            if (config.getAlipayFaceToFace().isEnable()) {
                return handleRequestAlipayFaceToFace(packet, client, config);
            }
        }
        return new PacketPluginRequestOrder.Response("payment.type-unknown");
    }

    private PacketPluginRequestOrder.Response handleRequestAlipayFaceToFace(PacketPluginRequestOrder packet, ClientInfo client, Configuration config) {
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
            model.setProductCode("FACE_TO_FACE_PAYMENT");
            model.setBody(packet.getProductName());

            request.setBizModel(model);

            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            System.out.println(response.getBody());

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
                timer.schedule(order.getTask(), 1000L, 3000L);
                return new PacketPluginRequestOrder.Response("face2face", orderId, response.getQrCode());
            } else {
                client.removeOrder(orderId);
                logger.warn("支付宝当面付调用失败");
                return new PacketPluginRequestOrder.Response("payment.internal-error");
            }
        } catch (AlipayApiException e) {
            client.removeOrder(orderId);
            logger.warn("支付宝当面付API执行错误", e);
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
            request.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            System.out.println(response.getBody());
            if (response.isSuccess()) {
                String status = response.getTradeStatus();
                switch (status.toUpperCase()) {
                    case "WAIT_BUYER_PAY": // 等待买家付款
                        break;
                    case "TRADE_CLOSED": // 超时未付款，交易关闭
                        client.removeOrder(order);
                        send(client.getWebSocket(), new PacketBackendPaymentCancel(orderId, "payment.timeout"));
                        break;
                    case "TRADE_SUCCESS": // 交易支付成功
                    case "TRADE_FINISHED": {// 交易结束，不可退款
                        client.removeOrder(order);
                        // 买家支付宝账号，通常是打码的手机号
                        String buyerLogonId = response.getBuyerLogonId();
                        String money = response.getReceiptAmount();
                        logger.info("[收款] 从支付宝当面付收款，来自 {} 的 ￥{}", buyerLogonId, money);
                        send(client.getWebSocket(), new PacketBackendPaymentConfirm(orderId, "支付宝用户", money));
                        break;
                    }
                }
            } else {
                logger.warn("支付宝当面付检查订单失败");
            }
        } catch (AlipayApiException e) {
            logger.warn("支付宝当面付API检查订单时执行错误", e);
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
            alipayClient.execute(request);
        } catch (AlipayApiException e) {
            logger.warn("支付宝当面付API关闭交易时执行错误", e);
        }
    }

    private PacketPluginRequestOrder.Response handleRequestWeChatNative(PacketPluginRequestOrder packet, ClientInfo client, Configuration config) {
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
        timer.schedule(order.getTask(), 1000L, 3000L);
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
                logger.info("[收款] 从微信Native收款，来自 {} 的 ￥{}", openId, money);
                send(client.getWebSocket(), new PacketBackendPaymentConfirm(orderId, "微信用户", money));
                break;
            case REFUND: // 转入退款
            case CLOSED: // 已关闭
                client.removeOrder(order);
                send(client.getWebSocket(), new PacketBackendPaymentCancel(orderId, "payment.native." + response.getTradeState().name().toLowerCase()));
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

    private PacketPluginCancelOrder.Response handleCancel(PacketPluginCancelOrder packet, WebSocket webSocket) {
        ClientInfo client = getOrCreateInfo(webSocket);
        // 取消订单
        ClientInfo.Order order = client.removeOrder(packet.getOrderId());
        if (order != null) {
            ClientInfo.Order locked = moneyLocked.get(order.getMoney());
            if (locked.getId().equals(order.getId())) {
                moneyLocked.remove(locked.getMoney());
            }
            Runnable action = order.getCancelAction();
            if (action != null) {
                action.run();
            }
            return new PacketPluginCancelOrder.Response();
        }
        return new PacketPluginCancelOrder.Response("payment.cancel.not-found");
    }

    public ClientInfo getOrCreateInfo(WebSocket webSocket) {
        ClientInfo client = webSocket.getAttachment();
        if (client == null) {
            client = new ClientInfo();
            client.setWebSocket(webSocket);
            webSocket.setAttachment(client);
        }
        return client;
    }

    @Override
    public void onOpen(WebSocket client, ClientHandshake clientHandshake) {
        getOrCreateInfo(client);
        logger.info("客户端 {} 已连接.", client.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket client, int i, String s, boolean b) {
        logger.info("客户端 {} 已断开连接 ({}).", client.getRemoteSocketAddress(), i);
    }

    public void send(@NotNull WebSocket client, @NotNull IPacket packet) {
        send(client, packet, null);
    }
    public void send(@NotNull WebSocket client, @NotNull IPacket packet, @Nullable Long echo) {
        JsonObject json = PacketSerializer.serialize(packet);
        if (echo != null) {
            json.addProperty("echo", echo);
        }
        client.send(json.toString());
    }

    @Override
    public void onMessage(WebSocket client, String s) {
        JsonObject json = JsonParser.parseString(s).getAsJsonObject();
        JsonElement echoProperty = json.get("echo");
        Long echo = echoProperty == null ? null : echoProperty.getAsLong();
        IPacket packet = PacketSerializer.deserialize(json);
        if (packet == null) return;
        Object result = null;
        List<BiFunction> list = executors.get(packet.getClass().getName());
        if (list != null && !list.isEmpty()) for (BiFunction executor : list) {
            Object obj = executor.apply(packet, client);
            if (result == null) {
                result = obj;
            }
        }
        if (echo != null && result != null && packet.isResponsePacket(result)) {
            send(client, (IPacket) result, echo);
        }
    }

    @Override
    public void onError(WebSocket client, Exception e) {
        logger.warn("客户端 {} 出现异常", client.getRemoteSocketAddress(), e);
    }

    @Override
    public void onStart() {
        logger.info("服务端已在 {} 端口启动.", getAddress().getPort());
    }

    @Override
    public void stop() throws InterruptedException {
        timer.cancel();
        super.stop();
    }

    private void onHookReceive(HookReceive receive) {
        // 处理接收 hook 收款消息
        Double moneyDouble = Util.parseDouble(receive.getMoney()).orElse(null);
        if (moneyDouble == null) {
            logger.warn("[收款] 收到Hook收款，处理金额时出现错误: 名字[{}]，金额[{}]", receive.getName(), receive.getMoney());
            return;
        }
        String money = String.format("%.2f", moneyDouble);
        logger.info("[收款] 收到Hook收款，来自 {} 的 ￥{}", receive.getName(), money);
        ClientInfo.Order order = moneyLocked.remove(money);
        if (order != null) {
            WebSocket webSocket = order.getClient().getWebSocket();
            if (!webSocket.isOpen()) {
                logger.warn("[Hook] 玩家 {} 的 ￥{} 订单异常，在付款完成之前插件断开了与后端的连接", order.getPlayerName(), money);
                return;
            }
            logger.info("[Hook] 玩家 {} 的 ￥{} 订单已付款完成，回调订单结果", order.getPlayerName(), money);
            order.remove();
            send(webSocket, new PacketBackendPaymentConfirm(order.getId(), receive.getName(), money));
        }
    }

    @Override
    public boolean inject(WebSocketImpl webSocket, ByteBuffer byteBuffer) {
        if (ConsoleMain.getConfig().getHook().isEnable()) {
            String httpLines = new String(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
            if (httpLines.startsWith("POST ")) { // POST /api/hook/receive HTTP/1.1
                String[] lines = httpLines.split("\n");
                String s1 = lines[0].substring(5);
                String path = s1.substring(0, s1.lastIndexOf(' '));
                if (path.equals(ConsoleMain.getConfig().getHook().getEndPoint())) {
                    StringBuilder content = new StringBuilder();
                    boolean flag = false;
                    for (String line : lines) {
                        if (flag) {
                            content.append(line.replace("\r", "")).append("\n");
                        } else if (line.trim().isEmpty()) {
                            flag = true;
                        }
                    }
                    try {
                        HookReceive receive = gson.fromJson(content.toString(), HookReceive.class);
                        onHookReceive(receive);
                    } catch (Throwable t) {
                        logger.warn("解析Hook消息时出现异常", t);
                    }

                    String resp = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nServer: SweetCheckout\r\nContent-Length: 2\r\n\r\nOK";
                    IDecodeInjector.write(this, webSocket, ByteBuffer.wrap(Charsetfunctions.asciiBytes(resp)));
                    webSocket.flushAndClose(1000, "OK", false);
                    return true;
                }
            }
        }
        return false;
    }
}

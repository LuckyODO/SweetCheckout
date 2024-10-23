package top.mrxiaom.sweet.checkout.backend;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.BusinessParams;
import com.alipay.api.domain.ExtendParams;
import com.alipay.api.domain.GoodsDetail;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.google.gson.*;
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
import top.mrxiaom.sweet.checkout.packets.PacketSerializer;
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
        ClientInfo client = getOrCreateInfo(webSocket);
        Configuration config = ConsoleMain.getConfig();
        // TODO: 验证 price 是否符合格式，在可修正时自动修正格式为 %.2f
        if (packet.getType().equals("wechat")) {
            // 微信 Hook TODO: 实现微信Hook
            if (config.getHook().isEnable() && config.getHook().getWeChat().isEnable()) {

            }
            // 微信 Native TODO: 实现微信Native支付
            if (config.getWeChatNative().isEnable()) {

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
        try {
            String orderId = client.nextOrderId();
            // TODO: 支付宝当面付
            AlipayClient alipayClient = new DefaultAlipayClient(config.getAlipayFaceToFace().getConfig());

            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            // TODO: 根据时间生成不会重复的商户订单号
            model.setOutTradeNo(orderId);
            model.setTotalAmount(packet.getPrice());
            model.setSubject(packet.getProductName());
            model.setProductCode("FACE_TO_FACE_PAYMENT");
            model.setBody(packet.getProductName());

            request.setBizModel(model);

            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            System.out.println(response.getBody());

            if (response.isSuccess()) {
                // TODO: 将检查是否交易成功 (AlipayTradeQueryRequest) 加入定时任务
                client.addOrder(new ClientInfo.Order(orderId, "alipay", packet.getPlayerName(), packet.getPrice()));
                return new PacketPluginRequestOrder.Response("", orderId, response.getQrCode());
            } else {
                logger.warn("支付宝当面付调用失败");
                return new PacketPluginRequestOrder.Response("payment.internal-error");
            }
        } catch (AlipayApiException e) {
            logger.warn("支付宝当面付API执行错误", e);
            return new PacketPluginRequestOrder.Response("payment.internal-error");
        }
    }

    private PacketPluginCancelOrder.Response handleCancel(PacketPluginCancelOrder packet, WebSocket webSocket) {
        ClientInfo client = getOrCreateInfo(webSocket);
        // TODO: 取消订单
        return new PacketPluginCancelOrder.Response("payment.cancel.not-found");
    }

    public ClientInfo getOrCreateInfo(WebSocket webSocket) {
        ClientInfo client = webSocket.getAttachment();
        if (client == null) {
            client = new ClientInfo();
            webSocket.setAttachment(client);
        }
        return client;
    }

    @Override
    public void onOpen(WebSocket client, ClientHandshake clientHandshake) {
        client.setAttachment(new ClientInfo());
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
        // TODO: 处理接收 hook 收款消息
        logger.info("[收款] 收到Hook收款，来自 {} 的 ￥{}", receive.getName(), receive.getMoney());
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

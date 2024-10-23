package top.mrxiaom.sweet.checkout.backend;

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
import top.mrxiaom.sweet.checkout.backend.payment.PaymentAlipay;
import top.mrxiaom.sweet.checkout.backend.payment.PaymentWeChat;
import top.mrxiaom.sweet.checkout.backend.util.Util;
import top.mrxiaom.sweet.checkout.packets.PacketSerializer;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentConfirm;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginCancelOrder;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginRequestOrder;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * 后端 WebSocket/Http 路由
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PaymentServer extends WebSocketServer implements IDecodeInjector {
    Gson gson = new GsonBuilder().setLenient().create();
    Logger logger;
    Timer timer = new Timer();
    Map<String, List<BiFunction>> executors = new HashMap<>();
    PaymentWeChat wechat = new PaymentWeChat(this);
    PaymentAlipay alipay = new PaymentAlipay(this);
    public PaymentServer(Logger logger, int port) {
        super(new InetSocketAddress(port));
        this.logger = logger;
        this.registerExecutor(PacketPluginRequestOrder.class, this::handleRequest);
        this.registerExecutor(PacketPluginCancelOrder.class, this::handleCancel);
    }

    public Logger getLogger() {
        return logger;
    }

    public Timer getTimer() {
        return timer;
    }

    /**
     * 注册接收包处理器，无返回值
     * @param type 包类型
     * @param executor 处理器
     * @param <T> 包类型
     */
    public <T extends IPacket> void registerExecutor(Class<T> type, BiConsumer<T, WebSocket> executor) {
        registerExecutor(type, (packet, client) -> {
            executor.accept(packet, client);
            return null;
        });
    }

    /**
     * 注册接收包处理器，有返回值
     * @param type 包类型
     * @param executor 处理器
     * @param <S> 返回类型
     * @param <T> 包类型
     */
    public <S extends IPacket, T extends IPacket<S>> void registerExecutor(Class<T> type, BiFunction<T, WebSocket, S> executor) {
        String key = type.getName();
        List<BiFunction> list = executors.get(key);
        if (list == null) list = new ArrayList<>();
        list.add(executor);
        executors.put(key, list);
    }

    @Nullable
    public Map<String, ClientInfo.Order> getMoneyLockedMap(String type) {
        return switch (type.toLowerCase()) {
            case "alipay" -> alipay.moneyLocked;
            case "wechat" -> wechat.moneyLocked;
            default -> null;
        };
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

        // 防止多次请求订单
        if (client.getOrderByPlayer(packet.getPlayerName()) != null) {
            return new PacketPluginRequestOrder.Response("payment.already-requested");
        }

        if (packet.getType().equals("wechat")) {
            // 微信 Hook
            if (config.getHook().isEnable() && config.getHook().getWeChat().isEnable()) {
                return wechat.handleHook(packet, client, config);
            }
            // 微信 Native
            if (config.getWeChatNative().isEnable()) {
                return wechat.handleNative(packet, client, config);
            }
        }
        if (packet.getType().equals("alipay")) {
            // 支付宝 Hook
            if (config.getHook().isEnable() && config.getHook().getAlipay().isEnable()) {
                return alipay.handleHook(packet, client, config);
            }
            // 支付宝当面付
            if (config.getAlipayFaceToFace().isEnable()) {
                return alipay.handleFaceToFace(packet, client, config);
            }
        }
        return new PacketPluginRequestOrder.Response("payment.type-unknown");
    }

    private PacketPluginCancelOrder.Response handleCancel(PacketPluginCancelOrder packet, WebSocket webSocket) {
        ClientInfo client = getOrCreateInfo(webSocket);
        // 取消订单
        ClientInfo.Order order = client.removeOrder(packet.getOrderId());
        if (order != null) {
            Map<String, ClientInfo.Order> moneyLocked = getMoneyLockedMap(order.getType());
            if (moneyLocked != null) {
                ClientInfo.Order locked = moneyLocked.get(order.getMoney());
                if (locked.getId().equals(order.getId())) {
                    moneyLocked.remove(locked.getMoney());
                }
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
            if (result == null && obj != null) {
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
        logger.info("[收款] 收到Hook收款，来自 {} 渠道，{} 的 ￥{}", receive.getType(), receive.getName(), money);
        Map<String, ClientInfo.Order> moneyLocked = getMoneyLockedMap(receive.getType());
        if (moneyLocked == null) {
            logger.warn("[Hook] 无效的渠道 {}", receive.getType());
            return;
        }
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

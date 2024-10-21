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
import top.mrxiaom.sweet.checkout.packets.PacketSerializer;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@SuppressWarnings({"rawtypes", "unchecked"})
public class PaymentServer extends WebSocketServer implements IDecodeInjector {
    Logger logger;
    Map<String, List<BiFunction>> executors = new HashMap<>();
    Gson gson = new GsonBuilder().create();
    public PaymentServer(Logger logger, int port) {
        super(new InetSocketAddress(port));
        this.logger = logger;
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

    @Override
    public void onOpen(WebSocket client, ClientHandshake clientHandshake) {
        client.setAttachment(new ClientInfo());
        logger.info("客户端 {} 已连接", client.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket client, int i, String s, boolean b) {
        logger.info("客户端 {} 已断开连接 ({})", client.getRemoteSocketAddress(), i);
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
        logger.info("服务端已在 {} 端口启动", getAddress().getPort());
    }

    private void onHookReceive(HookReceive receive) {
        // TODO: 处理接收 hook 收款消息
    }

    @Override
    public boolean inject(WebSocketImpl webSocket, ByteBuffer byteBuffer) {
        String httpLines = new String(byteBuffer.array());
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
                    }
                    else if (line.trim().isEmpty()) {
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
            }
        }
        return false;
    }
}

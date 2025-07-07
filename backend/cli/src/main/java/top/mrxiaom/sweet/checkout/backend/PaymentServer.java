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
import top.mrxiaom.sweet.checkout.backend.data.WebSocketClientInfo;
import top.mrxiaom.sweet.checkout.packets.PacketSerializer;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * 后端 WebSocket/Http 路由
 */
@SuppressWarnings({"rawtypes"})
public class PaymentServer extends AbstractPaymentServer<WebSocketClientInfo> {
    private final ConsoleMain console;
    private final WS webSocketServer;
    public PaymentServer(ConsoleMain console, Logger logger, int port) {
        super(logger);
        this.console = console;
        this.webSocketServer = new WS(new InetSocketAddress(port));
    }

    public WS getWebSocketServer() {
        return webSocketServer;
    }

    public WebSocketClientInfo getOrCreateInfo(WebSocket webSocket) {
        WebSocketClientInfo client = webSocket.getAttachment();
        if (client == null) {
            client = new WebSocketClientInfo();
            client.setWebSocket(webSocket);
            webSocket.setAttachment(client);
        }
        return client;
    }

    @Override
    public List<String> getAllProcess() {
        List<String> list = new ArrayList<>();
        ProcessHandle.allProcesses().forEach(it -> {
            Optional<String> command = it.info().command();
            command.ifPresent(list::add);
        });
        return list;
    }

    @Override
    public Configuration getConfig() {
        return console.getConfig();
    }

    @Override
    public void send(@NotNull WebSocketClientInfo client, @NotNull IPacket packet, @Nullable Long echo) {
        JsonObject json = PacketSerializer.serialize(packet);
        if (echo != null) {
            json.addProperty("echo", echo);
        }
        client.getWebSocket().send(json.toString());
    }

    public class WS extends WebSocketServer implements IDecodeInjector {
        private WS(InetSocketAddress address) {
            super(address);
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

        @Override
        public void onMessage(WebSocket client, String s) {
            WebSocketClientInfo clientInfo = getOrCreateInfo(client);
            PaymentServer.this.onMessage(clientInfo, s);
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

        @Override
        public boolean inject(WebSocketImpl webSocket, ByteBuffer byteBuffer) {
            if (console.getConfig().getHook().isEnable()) {
                String httpLines = new String(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
                if (httpLines.startsWith("POST ")) { // POST /api/hook/receive HTTP/1.1
                    String[] lines = httpLines.split("\n");
                    String s1 = lines[0].substring(5);
                    String path = s1.substring(0, s1.lastIndexOf(' '));
                    if (path.equals(console.getConfig().getHook().getEndPoint())) {
                        StringBuilder content = new StringBuilder();
                        boolean flag = false;
                        for (String line : lines) {
                            if (flag) {
                                content.append(line.replace("\r", "")).append("\n");
                            } else if (line.trim().isEmpty()) {
                                flag = true;
                            }
                        }
                        receiveHook(content);

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
}

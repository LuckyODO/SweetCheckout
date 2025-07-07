package top.mrxiaom.sweet.checkout.api;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketPaymentClient extends PaymentClient {
    private final String url;
    private final Client client;

    public WebSocketPaymentClient(PaymentAPI parent, String url) throws URISyntaxException {
        super(parent);
        this.url = url;
        this.client = new Client();
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public boolean isOpen() {
        return client.isOpen();
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public void connect() {
        client.connect();
    }

    @Override
    public void send(String message) {
        client.send(message);
    }

    public class Client extends WebSocketClient {
        private Client() throws URISyntaxException {
            super(new URI(url));
            addHeader("User-Agent", parent.getUserAgent());
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            parent.info("已连接到后端服务器");
        }

        @Override
        public void onMessage(String s) {
            parent.onMessage(s);
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            parent.info("已与后端服务器断开连接");
        }

        @Override
        public void onError(Exception e) {
            parent.warn("连接出现错误", e);
        }
    }
}

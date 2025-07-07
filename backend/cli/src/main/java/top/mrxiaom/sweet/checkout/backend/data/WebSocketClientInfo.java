package top.mrxiaom.sweet.checkout.backend.data;

import org.java_websocket.WebSocket;

public class WebSocketClientInfo extends ClientInfo<WebSocketClientInfo> {
    WebSocket webSocket;

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public boolean isOpen() {
        return webSocket.isOpen();
    }
}

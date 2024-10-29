package top.mrxiaom.sweet.checkout.func;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.configuration.MemoryConfiguration;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.packets.PacketSerializer;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentCancel;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentConfirm;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.common.NoResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@AutoRegister
@SuppressWarnings({"rawtypes", "unused"})
public class PaymentAPI extends AbstractModule {
    public class PaymentClient extends WebSocketClient {
        public PaymentClient(String url) throws URISyntaxException {
            super(new URI(url));
            addHeader("User-Agent", userAgent);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            info("已连接到后端服务器");
        }

        @Override
        @SuppressWarnings({"deprecation", "unchecked"})
        public void onMessage(String s) {
            JsonObject json = parser.parse(s).getAsJsonObject();
            JsonElement echoProperty = json.get("echo");
            if (echoProperty != null) {
                Consumer resp = responseMap.remove(echoProperty.getAsLong());
                if (resp != null) try {
                    IPacket packet = PacketSerializer.deserialize(json);
                    if (packet != null) resp.accept(packet);
                } catch (Throwable t) {
                    warn("接收数据包时出现错误", t);
                }
            } else {
                IPacket packet = PacketSerializer.deserialize(json);
                if (packet != null) {
                    Consumer consumer = eventMap.get(packet.getClass().getName());
                    if (consumer != null) {
                        consumer.accept(packet);
                    }
                }
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            info("已与后端服务器断开连接");
        }

        @Override
        public void onError(Exception e) {
            warn("连接出现错误", e);
        }
    }

    long echo = 0;
    @SuppressWarnings({"deprecation"}) // 兼容旧版本 gson
    JsonParser parser = new JsonParser();
    Map<Long, Consumer> responseMap = new HashMap<>();
    Map<String, Consumer> eventMap = new HashMap<>();
    PaymentClient client;
    String userAgent;
    public PaymentAPI(SweetCheckout plugin) {
        super(plugin);
        userAgent = "SweetCheckout/" + plugin.getDescription().getVersion() + " Minecraft/" + MinecraftVersion.getVersion().name();
        registerListener(PacketBackendPaymentConfirm.class, this::onReceivePaymentConfirm);
        registerListener(PacketBackendPaymentCancel.class, this::onReceivePaymentCancel);
    }

    private <T extends IPacket<NoResponse>> void registerListener(Class<T> type, Consumer<T> consumer) {
        eventMap.put(type.getName(), consumer);
    }

    public <T extends IPacket> void send(IPacket<T> packet) {
        send(packet, null);
    }

    public <T extends IPacket> void send(IPacket<T> packet, Consumer<T> resp) {
        JsonObject json = PacketSerializer.serialize(packet);
        Class<T> respType = packet.getResponsePacket();
        Long echo = (respType == null || resp == null) ? null : this.echo++;
        if (echo != null) {
            json.addProperty("echo", echo);
            responseMap.put(echo, resp);
        }
        client.send(json.toString());
    }

    private void onReceivePaymentConfirm(PacketBackendPaymentConfirm packet) {

    }

    private void onReceivePaymentCancel(PacketBackendPaymentCancel packet) {

    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        if (client != null && client.isOpen()) {
            client.close();
            client = null;
        }
        String url = config.getString("backend-host");
        if (url != null) try {
            client = new PaymentClient(url);
            client.connect();
        } catch (Throwable t) {
            warn("连接后端服务器时出现异常", t);
        } else {
            warn("未配置后端地址，请在配置文件进行配置");
        }
    }

    @Override
    public void onDisable() {
        if (client != null && client.isOpen()) {
            client.close();
            client = null;
        }
    }

    public static PaymentAPI inst() {
        return instanceOf(PaymentAPI.class);
    }
}

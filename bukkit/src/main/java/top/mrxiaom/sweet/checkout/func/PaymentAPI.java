package top.mrxiaom.sweet.checkout.func;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.checkout.CancelReasons;
import top.mrxiaom.sweet.checkout.Messages;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.func.entry.PaymentInfo;
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
        String url;
        public PaymentClient(String url) throws URISyntaxException {
            super(new URI(url));
            this.url = url;
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
                    if (packet != null) Bukkit.getScheduler().runTask(plugin, () -> resp.accept(packet));
                } catch (Throwable t) {
                    warn("接收数据包时出现错误", t);
                }
            } else {
                IPacket packet = PacketSerializer.deserialize(json);
                if (packet != null) {
                    Consumer consumer = eventMap.get(packet.getClass().getName());
                    if (consumer != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> consumer.accept(packet));
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

    public <T extends IPacket> boolean send(IPacket<T> packet) {
        return send(packet, null);
    }

    public <T extends IPacket> boolean send(IPacket<T> packet, @Nullable Consumer<T> resp) {
        JsonObject json = PacketSerializer.serialize(packet);
        Class<T> respType = packet.getResponsePacket();
        Long echo = (respType == null || resp == null) ? null : this.echo++;
        if (echo != null) {
            json.addProperty("echo", echo);
            responseMap.put(echo, resp);
        }
        if (!isConnected()) {
            warn("请求失败: 未连接到后端");
            return false;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> client.send(json.toString()));
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isConnected() {
        return client != null && client.isOpen();
    }

    private void onReceivePaymentConfirm(PacketBackendPaymentConfirm packet) {
        PaymentsAndQRCodeManager manager = PaymentsAndQRCodeManager.inst();
        manager.markDone(packet.getOrderId(), packet.getMoney());
    }

    private void onReceivePaymentCancel(PacketBackendPaymentCancel packet) {
        PaymentsAndQRCodeManager manager = PaymentsAndQRCodeManager.inst();
        PaymentInfo info = manager.remove(packet.getOrderId());
        if (info == null) {
            warn("收到 " + packet.getOrderId() + " 取消包出错: 没有这个订单号");
        } else {
            if (plugin.processingLogs) {
                info("玩家 " + info.player.getName() + " 因 " + packet.getReason() + " 取消了订单 " + packet.getOrderId());
            }
            CancelReasons reason = CancelReasons.fromString(packet.getReason());
            String reasonString = reason.str(Pair.of("%reason%", packet.getReason()));
            Messages.cancelled.tm(info.player, Pair.of("%reason%", reasonString));
        }
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        String url = config.getString("backend-host");
        reload(url);
    }

    private void reload(@Nullable String url) {
        if (client != null && client.isOpen()) {
            // 已连接地址与配置文件地址相同时，不断开重连
            if (client.url.equals(url)) {
                info("重载时，配置中的后端地址无变动，保持连接");
                return;
            }
            client.close();
            client = null;
        }
        if (url != null) try {
            info("正在连接到后端 " + url);
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

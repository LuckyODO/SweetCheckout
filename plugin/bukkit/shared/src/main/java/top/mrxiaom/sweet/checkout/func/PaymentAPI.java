package top.mrxiaom.sweet.checkout.func;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.checkout.CancelReasons;
import top.mrxiaom.sweet.checkout.Messages;
import top.mrxiaom.sweet.checkout.PluginCommon;
import top.mrxiaom.sweet.checkout.api.PaymentClient;
import top.mrxiaom.sweet.checkout.func.entry.PaymentInfo;
import top.mrxiaom.sweet.checkout.packets.PacketSerializer;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentCancel;
import top.mrxiaom.sweet.checkout.packets.backend.PacketBackendPaymentConfirm;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.common.NoResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@AutoRegister
@SuppressWarnings({"rawtypes", "unused"})
public class PaymentAPI extends AbstractModule {
    private long echo = 0;
    @SuppressWarnings({"deprecation"}) // 兼容旧版本 gson
    private final JsonParser parser = new JsonParser();
    private final Map<Long, Consumer> responseMap = new HashMap<>();
    private final Map<String, Consumer> eventMap = new HashMap<>();
    private final String userAgent;
    private PaymentClient client;
    public PaymentAPI(PluginCommon plugin) {
        super(plugin);
        userAgent = "SweetCheckout/" + plugin.getDescription().getVersion() + " Minecraft/" + MinecraftVersion.getVersion().name();
        registerListener(PacketBackendPaymentConfirm.class, this::onReceivePaymentConfirm);
        registerListener(PacketBackendPaymentCancel.class, this::onReceivePaymentCancel);
    }

    public String getUserAgent() {
        return userAgent;
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
        plugin.getScheduler().runTaskAsync(() -> client.send(json.toString()));
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isConnected() {
        return client != null && client.isOpen();
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    public void onMessage(String s) {
        JsonObject json = parser.parse(s).getAsJsonObject();
        JsonElement echoProperty = json.get("echo");
        if (echoProperty != null) {
            Consumer resp = responseMap.remove(echoProperty.getAsLong());
            if (resp != null) try {
                IPacket packet = PacketSerializer.deserialize(json);
                if (packet != null) plugin.getScheduler().runTask(() -> resp.accept(packet));
            } catch (Throwable t) {
                warn("接收数据包时出现错误", t);
            }
        } else {
            IPacket packet = PacketSerializer.deserialize(json);
            if (packet != null) {
                Consumer consumer = eventMap.get(packet.getClass().getName());
                if (consumer != null) {
                    plugin.getScheduler().runTask(() -> consumer.accept(packet));
                }
            }
        }
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
        try {
            client = plugin.handlePaymentReload(this, url);
        } catch (Throwable t) {
            warn("连接后端服务器时出现异常", t);
        }
    }

    @Nullable
    public PaymentClient getClient() {
        return client;
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

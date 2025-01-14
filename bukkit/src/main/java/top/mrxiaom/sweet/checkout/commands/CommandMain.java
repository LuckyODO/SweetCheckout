package top.mrxiaom.sweet.checkout.commands;
		
import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.qrcode.QRCode;
import top.mrxiaom.qrcode.enums.ErrorCorrectionLevel;
import top.mrxiaom.sweet.checkout.Errors;
import top.mrxiaom.sweet.checkout.Messages;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.func.AbstractModule;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;
import top.mrxiaom.sweet.checkout.func.PaymentsAndQRCodeManager;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginRequestOrder;

import java.util.*;
import java.util.function.Consumer;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    private boolean useWeChat;
    private boolean useAlipay;
    private int paymentTimeout;
    private int pointsScale;
    private List<String> pointsCommands;
    public CommandMain(SweetCheckout plugin) {
        super(plugin);
        registerCommand("sweetcheckout", this);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        useWeChat = config.getBoolean("payment.enable.wechat");
        useAlipay = config.getBoolean("payment.enable.alipay");
        paymentTimeout = config.getInt("payment.timeout");

        pointsScale = config.getInt("points.scale");
        pointsCommands = config.getStringList("points.commands");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 3 && "points".equalsIgnoreCase(args[0])) {
                PaymentsAndQRCodeManager manager = PaymentsAndQRCodeManager.inst();
                String type = args[1];
                String moneyStr = args[2];
                if ("wechat".equalsIgnoreCase(type)) {
                    if (!useWeChat) {
                        return Messages.commands__points__disabled__wechat.tm(player);
                    }
                } else if ("alipay".equalsIgnoreCase(type)) {
                    if (!useAlipay) {
                        return Messages.commands__points__disabled__alipay.tm(player);
                    }
                } else {
                    return Messages.commands__points__unknown_type.tm(player);
                }
                if (manager.isProcess(player)) {
                    return Messages.commands__points__processing.tm(player);
                }
                manager.putProcess(player);
                return send(player, Messages.commands__points__send.str(), new PacketPluginRequestOrder(
                        player.getName(), type, "宝石" /* TODO: 商品名移到配置文件 */, moneyStr
                ), resp -> {
                    String error = resp.getError();
                    if (!error.isEmpty()) {
                        // 下单失败时提示玩家
                        Errors.fromString(error).tm(player, Pair.of("%type%", error));
                        manager.remove(player);
                        return;
                    }
                    // 下单成功操作
                    String orderId = resp.getOrderId();
                    long now = System.currentTimeMillis();
                    long outdateTime = now + (paymentTimeout * 1000L) + 500L;
                    // 向玩家展示二维码地图
                    QRCode code = QRCode.create(resp.getPaymentUrl(), ErrorCorrectionLevel.H);
                    manager.requireScan(player, code, orderId, outdateTime, money -> {
                        // 支付成功操作，给予玩家点券
                        int points = (int) Math.round(money * pointsScale);
                        plugin.run(player, pointsCommands, Pair.of("%points%", points));
                    });
                });
            }
        }
		if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
			plugin.reloadConfig();
			return Messages.commands__reload.tm(sender);
		}
        return true;
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList(
            "points");
    private static final List<String> listOpArg0 = Lists.newArrayList(
            "points", "reload");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listOpArg0 : listArg0, args[0]);
        }
        return emptyList;
    }

    public List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }

    @SuppressWarnings({"rawtypes"})
    public <T extends IPacket> boolean send(Player player, String msg, IPacket<T> packet, Consumer<T> resp) {
        t(player, msg);
        if (!PaymentAPI.inst().send(packet, resp)) {
            Messages.not_connect.tm(player);
        }
        return true;
    }
}

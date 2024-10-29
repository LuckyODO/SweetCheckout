package top.mrxiaom.sweet.checkout.commands;
		
import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.qrcode.QRCode;
import top.mrxiaom.qrcode.enums.ErrorCorrectionLevel;
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
    public CommandMain(SweetCheckout plugin) {
        super(plugin);
        registerCommand("sweetcheckout", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 3 && "points".equalsIgnoreCase(args[0])) {
                PaymentsAndQRCodeManager manager = PaymentsAndQRCodeManager.inst();
                String type = args[1];
                String moneyStr = args[2];
                if (!manager.isProcess(player)) {
                    return t(player, "请先完成你正在进行的订单");
                }
                manager.putProcess(player);
                return send(player, "正在请求…", new PacketPluginRequestOrder(
                        player.getName(), type, "宝石" /* TODO: 商品名移到配置文件 */, moneyStr
                ), resp -> {
                    String error = resp.getError();
                    if (!error.isEmpty()) {
                        // 下单失败时提示玩家
                        t(player, error);
                        return;
                    }
                    // 下单成功操作
                    String orderId = resp.getOrderId();
                    // 向玩家展示二维码地图
                    QRCode code = QRCode.create(resp.getPaymentUrl(), ErrorCorrectionLevel.H);
                    manager.requireScan(player, code, orderId, money -> {
                        // 支付成功操作
                        // TODO: 给予玩家点券
                    });
                });
            }
        }
		if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
			plugin.reloadConfig();
			return t(sender, "&a配置文件已重载");
		}
        return true;
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList(
            "start", "end");
    private static final List<String> listOpArg0 = Lists.newArrayList(
            "start", "end", "reload");
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
        PaymentAPI.inst().send(packet, resp);
        return true;
    }
}

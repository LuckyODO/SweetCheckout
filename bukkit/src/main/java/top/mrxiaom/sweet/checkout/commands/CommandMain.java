package top.mrxiaom.sweet.checkout.commands;
		
import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
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
import top.mrxiaom.sweet.checkout.func.ShopManager;
import top.mrxiaom.sweet.checkout.func.entry.ShopItem;
import top.mrxiaom.sweet.checkout.nms.NMS;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginRequestOrder;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

import static top.mrxiaom.sweet.checkout.utils.Utils.readBase64;
import static top.mrxiaom.sweet.checkout.utils.Utils.writeBase64;
import static top.mrxiaom.sweet.checkout.utils.Utils.random;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    private boolean useWeChat;
    private boolean useAlipay;
    private int paymentTimeout;
    private int pointsScale;
    private List<String> pointsNames;
    private List<String> pointsCommands;
    public CommandMain(SweetCheckout plugin) {
        super(plugin);
        registerCommand("sweetcheckout", this);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        plugin.processingLogs = !config.getBoolean("no-processing-logs");

        useWeChat = config.getBoolean("payment.enable.wechat");
        useAlipay = config.getBoolean("payment.enable.alipay");
        paymentTimeout = config.getInt("payment.timeout");

        pointsScale = config.getInt("points.scale");
        pointsNames = config.getStringList("points.names");
        pointsCommands = config.getStringList("points.commands");
    }

    @Override
    @SuppressWarnings({"deprecation"})
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 3 && "points".equalsIgnoreCase(args[0]) && player.hasPermission("sweet.checkout.points")) {
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
                String productName = random(pointsNames, "商品");
                return send(player, Messages.commands__points__send.str(), new PacketPluginRequestOrder(
                        player.getName(), type, productName, moneyStr
                ), resp -> {
                    String error = resp.getError();
                    if (!error.isEmpty()) {
                        warn("玩家 " + player.getName() + " 通过 " + type + " 下单点券 (￥" + moneyStr + ") 失败: " + error);
                        // 下单失败时提示玩家
                        Errors.fromString(error).tm(player, Pair.of("%type%", error));
                        manager.remove(player);
                        return;
                    }
                    // 下单成功操作
                    String orderId = resp.getOrderId();
                    long now = System.currentTimeMillis();
                    long outdateTime = now + (paymentTimeout * 1000L) + 500L;
                    if (plugin.processingLogs) info("玩家 " + player.getName() + " 通过 " + type + " 下单点券 (￥" + moneyStr + ") 成功，订单号为 " + orderId);
                    // 向玩家展示二维码地图
                    QRCode code = QRCode.create(resp.getPaymentUrl(), ErrorCorrectionLevel.H);
                    manager.requireScan(player, code, orderId, outdateTime, money -> {
                        // 支付成功操作，给予玩家点券
                        int points = (int) Math.round(money * pointsScale);
                        info("玩家 " + player.getName() + " 通过 " + type  + " 支付 ￥" + money + " 获得了 " + points + " 点券 --" + productName + " " + orderId);
                        plugin.run(player, pointsCommands, Pair.of("%points%", points));
                    });
                });
            }
            if (args.length == 3 && "buy".equalsIgnoreCase(args[0])) {
                String shopId = args[1];
                ShopItem shop = ShopManager.inst().get(shopId);
                if (shop == null) {
                    return Messages.commands__buy__not_found.tm(player);
                }
                PaymentsAndQRCodeManager manager = PaymentsAndQRCodeManager.inst();
                String type = args[2];
                if ("wechat".equalsIgnoreCase(type)) {
                    if (!shop.paymentWeChat) {
                        return Messages.commands__buy__disabled__wechat.tm(player);
                    }
                } else if ("alipay".equalsIgnoreCase(type)) {
                    if (!shop.paymentAlipay) {
                        return Messages.commands__buy__disabled__alipay.tm(player);
                    }
                } else {
                    return Messages.commands__buy__unknown_type.tm(player);
                }
                if (manager.isProcess(player)) {
                    return Messages.commands__buy__processing.tm(player);
                }
                manager.putProcess(player);
                String productName = random(shop.names, "商品");
                return send(player, Messages.commands__buy__send.str(), new PacketPluginRequestOrder(
                        player.getName(), type, productName, shop.price
                ), resp -> {
                    String error = resp.getError();
                    if (!error.isEmpty()) {
                        warn("玩家 " + player.getName() + " 通过 " + type + " 下单 " + shop.display + " (" + shop.id + ") 失败: " + error);
                        // 下单失败时提示玩家
                        Errors.fromString(error).tm(player, Pair.of("%type%", error));
                        manager.remove(player);
                        return;
                    }
                    // 下单成功操作
                    String orderId = resp.getOrderId();
                    if (plugin.processingLogs) info("玩家 " + player.getName() + " 通过 " + type + " 下单商品 " + shop.display + " (" + shop.id + ") 成功，订单号为 " + orderId);
                    long now = System.currentTimeMillis();
                    long outdateTime = now + (paymentTimeout * 1000L) + 500L;
                    // 向玩家展示二维码地图
                    QRCode code = QRCode.create(resp.getPaymentUrl(), ErrorCorrectionLevel.H);
                    manager.requireScan(player, code, orderId, outdateTime, money -> {
                        // 支付成功操作，给予玩家奖励
                        info("玩家 " + player.getName() + " 通过 " + type + " 支付 ￥" + money + " 购买了商品 " + shop.display + " (" + shop.id + ") --" + productName + " " + orderId);
                        plugin.run(player, shop.rewards);
                    });
                });
            }
            if (args.length >= 1 && "map".equalsIgnoreCase(args[0]) && sender.isOp()) {
                if (args.length == 2) {
                    byte[] colors = readBase64(new File(plugin.getDataFolder(), args[1]), 16384);
                    if (colors == null) {
                        return Messages.commands__map__invalid.tm(player);
                    }
                    PaymentsAndQRCodeManager manager = PaymentsAndQRCodeManager.inst();
                    long now = System.currentTimeMillis();
                    long outdateTime = now + (paymentTimeout * 1000L) + 500L;
                    manager.requireScan(player, colors, null, outdateTime, null);
                    return Messages.commands__map__given.tm(player);
                }
                ItemStack item = player.getItemInHand();
                ItemMeta meta = item.getItemMeta();
                MapRenderer renderer = null;
                if (meta instanceof MapMeta) {
                    MapMeta map = (MapMeta) meta;
                    MapView mapView = map.getMapView();
                    if (mapView != null) {
                        List<MapRenderer> renderers = mapView.getRenderers();
                        if (!renderers.isEmpty()) {
                            renderer = renderers.get(0);
                        }
                    }
                }
                if (renderer == null) {
                    return Messages.commands__map__not_found.tm(player);
                }
                byte[] colors = NMS.getColors(renderer);
                writeBase64(new File(plugin.getDataFolder(), "output.map"), colors);
                return Messages.commands__map__success.tm(player);
            }
        }
		if (args.length >= 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            if (args.length == 2 && "database".equalsIgnoreCase(args[1])) {
                plugin.options.database().reloadConfig();
                plugin.options.database().reconnect();
                return Messages.commands__reload_database.tm(sender);
            }
			plugin.reloadConfig();
			return Messages.commands__reload.tm(sender);
		}
        return (sender.isOp()
                ? Messages.commands__help__admin
                : Messages.commands__help__normal
        ).tm(sender);
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList(
            "points", "buy");
    private static final List<String> listOpArg0 = Lists.newArrayList(
            "points", "buy", "map", "reload");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listOpArg0 : listArg0, args[0]);
        }
        if (args.length == 2) {
            if ("points".equalsIgnoreCase(args[0])) {
                List<String> list = new ArrayList<>();
                if (useAlipay) list.add("alipay");
                if (useWeChat) list.add("wechat");
                return startsWith(list, args[1]);
            }
            if ("buy".equalsIgnoreCase(args[0])) {
                return startsWith(ShopManager.inst().shops(sender), args[1]);
            }
        }
        if (args.length == 3) {
            if ("buy".equalsIgnoreCase(args[0])) {
                ShopItem shop = ShopManager.inst().get(args[1]);
                if (shop == null) {
                    return emptyList;
                }
                if (shop.permission != null && !sender.hasPermission(shop.permission)) {
                    return emptyList;
                }
                List<String> list = new ArrayList<>();
                if (shop.paymentAlipay) list.add("alipay");
                if (shop.paymentWeChat) list.add("wechat");
                return startsWith(list, args[1]);
            }
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

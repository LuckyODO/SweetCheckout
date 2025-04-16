package top.mrxiaom.sweet.checkout.func.entry;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.sweet.checkout.SweetCheckout;

import java.util.List;

public class ShopItem {
    public final String id;
    public final String display;
    public final @Nullable String permission;
    public final List<String> names;
    public final boolean paymentAlipay, paymentWeChat;
    public final String price;
    public final List<IAction> rewards;

    public ShopItem(String id, String display, @Nullable String permission, List<String> names, boolean paymentAlipay, boolean paymentWeChat, String price, List<IAction> rewards) {
        this.id = id;
        this.display = display;
        this.permission = permission;
        this.names = names;
        this.paymentAlipay = paymentAlipay;
        this.paymentWeChat = paymentWeChat;
        this.price = price;
        this.rewards = rewards;
    }

    @Nullable
    public static ShopItem load(SweetCheckout plugin, ConfigurationSection config, String id) {
        boolean paymentAlipay = config.getBoolean("payment.alipay");
        boolean paymentWeChat = config.getBoolean("payment.wechat");
        String display = config.getString("display", id);
        double price = config.getDouble("price");
        if (price < 0.01) {
            plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 价格输入错误");
            return null;
        }
        List<String> names = config.getStringList("names");
        if (names.isEmpty()) {
            plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 商品名称为空");
            return null;
        }
        List<IAction> rewards = ActionProviders.loadActions(config.getStringList("rewards"));
        if (rewards.isEmpty()) {
            plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 加载后的奖励命令列表为空");
            return null;
        }
        String permission = config.getString("permission", null);
        return new ShopItem(id, display, permission == null ? null : permission.replace("%id%", id),
                names, paymentAlipay, paymentWeChat, String.format("%.2f", price), rewards);
    }
}

package top.mrxiaom.sweet.checkout.func.entry;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.temporary.TemporaryInteger;
import top.mrxiaom.pluginbase.temporary.period.EveryDay;
import top.mrxiaom.pluginbase.temporary.period.EveryMonth;
import top.mrxiaom.pluginbase.temporary.period.EveryWeek;
import top.mrxiaom.pluginbase.temporary.period.Period;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.checkout.Placeholders;
import top.mrxiaom.sweet.checkout.PluginCommon;
import top.mrxiaom.sweet.checkout.func.PaymentsAndQRCodeManager;
import top.mrxiaom.sweet.checkout.func.modifier.Modifiers;
import top.mrxiaom.sweet.checkout.func.modifier.OrderInfo;
import top.mrxiaom.sweet.checkout.func.temporary.NeverReset;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShopItem {
    public final PluginCommon plugin;
    public final String id;
    public final String display;
    public final @Nullable String permission;
    public final Modifiers modifiers;
    public final List<String> names;
    public final boolean paymentAlipay, paymentWeChat;
    public final EnumLimitationMode limitationMode;
    public final int limitationCounts;
    public final List<IAction> limitationDenyActions;
    public final Period limitationReset;
    public final String price;
    public final List<IAction> rewards;
    public final List<IAction> resetActions;

    public ShopItem(
            PluginCommon plugin, String id, String display, @Nullable String permission, Modifiers modifiers,
            List<String> names, boolean paymentAlipay, boolean paymentWeChat,
            EnumLimitationMode limitationMode, int limitationCounts,
            List<IAction> limitationDenyActions, Period limitationReset,
            String price, List<IAction> rewards, List<IAction> resetActions
    ) {
        this.plugin = plugin;
        this.id = id;
        this.display = display;
        this.permission = permission;
        this.modifiers = modifiers;
        this.names = names;
        this.paymentAlipay = paymentAlipay;
        this.paymentWeChat = paymentWeChat;
        this.limitationMode = limitationMode;
        this.limitationCounts = limitationCounts;
        this.limitationDenyActions = limitationDenyActions;
        this.limitationReset = limitationReset;
        this.price = price;
        this.rewards = rewards;
        this.resetActions = resetActions;
    }

    public String getPrice() {
        return price;
    }

    @Nullable
    public String getPrice(Player player) {
        try {
            return getPriceOrError(player);
        } catch (Exception e) {
            return null;
        }
    }

    public String getPriceOrError(Player player) throws Exception {
        double price = Double.parseDouble(this.price);
        OrderInfo order = new OrderInfo(player, price, 0);
        modifiers.modify(order);
        return String.format("%.2f", order.getMoney());
    }

    /**
     * 检查限购情况
     * @return <ul>
     *     <li><code>true</code> 代表不能再购买</li>
     *     <li><code>false</code> 代表可以购买</li>
     * </ul>
     */
    public boolean checkLimitation(PluginCommon plugin, OfflinePlayer player) {
        if (limitationMode.equals(EnumLimitationMode.GLOBAL)) {
            TemporaryInteger cache = plugin.getBuyCountDatabase().getGlobalCount(this, false);
            int count = cache.getValue();
            int processing = PaymentsAndQRCodeManager.inst().getProcessingCount(this);
            return count + processing >= limitationCounts;
        }
        if (limitationMode.equals(EnumLimitationMode.PER_PLAYER)) {
            TemporaryInteger cache = plugin.getBuyCountDatabase().getPlayerCount(player, this, false);
            int count = cache.getValue();
            int processing = PaymentsAndQRCodeManager.inst().getProcessingCount(this);
            return count + processing >= limitationCounts;
        }
        return false;
    }

    public void whenReset() {
        if (limitationMode.equals(EnumLimitationMode.GLOBAL)) {
            ActionProviders.run(plugin, null, resetActions);
        }
    }

    @Nullable
    public static ShopItem load(PluginCommon plugin, ConfigurationSection config, String id) {
        boolean paymentAlipay = config.getBoolean("payment.alipay");
        boolean paymentWeChat = config.getBoolean("payment.wechat");
        String display = config.getString("display", id);
        EnumLimitationMode limitationMode = Util.valueOr(EnumLimitationMode.class, config.getString("limitation.mode"), EnumLimitationMode.NONE);
        int limitationCounts = config.getInt("limitation.counts", 0);
        List<IAction> limitationDenyActions = ActionProviders.loadActions(config, "limitation.deny-actions");
        Period limitationReset;
        if (!limitationMode.equals(EnumLimitationMode.NONE)) {
            List<String> split = Util.split(config.getString("limitation.reset", ""), ' ');
            String type = split.get(0);
            int size = split.size();
            if ("Never".equalsIgnoreCase(type)) {
                limitationReset = NeverReset.INSTANCE;
            } else if ("EveryDay".equalsIgnoreCase(type)) {
                if (size != 2) {
                    plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 重置周期格式错误");
                    return null;
                }
                LocalTime time = parseTime(split.get(1));
                if (time == null) {
                    plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 重置周期格式错误，输入的时间无效");
                    return null;
                }
                limitationReset = EveryDay.at(time);
            } else if ("EveryWeek".equalsIgnoreCase(type)) {
                if (size != 3) {
                    plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 重置周期格式错误");
                    return null;
                }
                Set<DayOfWeek> weeks = new HashSet<>();
                for (String str : Util.split(split.get(1), ',')) {
                    int i = Util.parseInt(str).orElse(0);
                    if (i >= 1 && i <= 7) {
                        weeks.add(DayOfWeek.of(i));
                    } else {
                        DayOfWeek week = Util.valueOr(DayOfWeek.class, str, null);
                        if (week != null) weeks.add(week);
                    }
                }
                if (weeks.isEmpty()) {
                    plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 重置周期格式错误，输入的星期无效");
                    return null;
                }
                LocalTime time = parseTime(split.get(2));
                if (time == null) {
                    plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 重置周期格式错误，输入的时间无效");
                    return null;
                }
                limitationReset = EveryWeek.at(time, weeks);
            } else if ("EveryMonth".equalsIgnoreCase(type)) {
                if (size != 3) {
                    plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 重置周期格式错误");
                    return null;
                }
                List<Integer> days = new ArrayList<>();
                for (String str : Util.split(split.get(1), ',')) {
                    Util.parseInt(str).ifPresent(days::add);
                }
                if (days.isEmpty()) {
                    plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 重置周期格式错误，输入的日期无效");
                    return null;
                }
                LocalTime time = parseTime(split.get(2));
                if (time == null) {
                    plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 重置周期格式错误，输入的时间无效");
                    return null;
                }
                limitationReset = EveryMonth.at(time, days);
            } else {
                plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 无效的重置周期类型 " + type);
                return null;
            }
        } else {
            limitationReset = NeverReset.INSTANCE;
        }
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
        List<IAction> rewards = ActionProviders.loadActions(config, "rewards");
        if (rewards.isEmpty()) {
            plugin.getLogger().warning("[shops] 加载 " + id + " 失败: 加载后的奖励命令列表为空");
            return null;
        }
        List<IAction> resetActions = ActionProviders.loadActions(config, "reset-actions");
        String permission = config.getString("permission", null);
        Modifiers modifiers = Modifiers.load(config, "modifiers");
        return new ShopItem(plugin, id, display, permission == null ? null : permission.replace("%id%", id), modifiers,
                names, paymentAlipay, paymentWeChat,
                limitationMode, limitationCounts,
                limitationDenyActions, limitationReset,
                String.format("%.2f", price), rewards, resetActions);
    }

    @Nullable
    public static LocalTime parseTime(String str) {
        String[] split = str.split(":", 2);
        if (split.length != 2) return null;
        Integer hour = Util.parseInt(split[0]).orElse(null);
        Integer minute = Util.parseInt(split[1]).orElse(null);
        if (hour == null || minute == null) return null;
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
        return LocalTime.of(hour, minute);
    }
}

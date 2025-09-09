package top.mrxiaom.sweet.checkout;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.PlaceholdersExpansion;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.checkout.commands.CommandMain;
import top.mrxiaom.sweet.checkout.func.RankManager;
import top.mrxiaom.sweet.checkout.func.ShopManager;
import top.mrxiaom.sweet.checkout.func.entry.ShopItem;
import top.mrxiaom.sweet.checkout.func.modifier.OrderInfo;

import java.util.Optional;

public class Placeholders extends PlaceholdersExpansion<PluginCommon> {
    public Placeholders(PluginCommon plugin) {
        super(plugin);
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("rank_")) {
            String[] split = params.substring(5).split("_", 2);
            if (split.length != 2) return "WRONG_USAGE";
            RankManager manager = RankManager.inst();
            int top = Util.parseInt(split[0]).orElse(0);
            if (top < 1 || top > manager.getTop()) return "WRONG_USAGE";
            String type = split[1];
            if (type.equals("name")) {
                RankManager.Rank rank = manager.get(top);
                if (rank == null) {
                    return Messages.rank__not_found__player.str();
                } else {
                    return Messages.rank__exist__player.str(Pair.of("%name%", rank.name));
                }
            }
            if (type.equals("money")) {
                RankManager.Rank rank = manager.get(top);
                if (rank == null) {
                    return Messages.rank__not_found__money.str();
                } else {
                    String money = String.format("%.2f", rank.money).replace(".00", "");
                    return Messages.rank__exist__money.str(Pair.of("%money%", money));
                }
            }
            return "WRONG_USAGE";
        }
        if (params.startsWith("points_money_")) {
            double money = Util.parseDouble(params.substring(13)).orElse(0.0);
            return String.valueOf(Math.max(0, CommandMain.inst().getPoints(money)));
        }
        if (params.startsWith("money_shop_")) {
            ShopItem shopItem = ShopManager.inst().get(params.substring(11));
            if (shopItem == null) {
                return "0.00";
            }
            return shopItem.getPrice();
        }
        return super.onRequest(player, params);
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.startsWith("modified_money_")) {
            CommandMain command = CommandMain.inst();
            double money = Util.parseDouble(params.substring(15)).orElse(0.0);
            int point = command.getPoints(money);
            OrderInfo order = new OrderInfo(player, money, point);
            try {
                command.getPointsModifiers().modify(order);
            } catch (Exception ignored) {
            }
            return String.format("%.2f", order.getMoney());
        }
        if (params.startsWith("modified_points_")) {
            CommandMain command = CommandMain.inst();
            double money = Util.parseDouble(params.substring(16)).orElse(0.0);
            int point = command.getPoints(money);
            OrderInfo order = new OrderInfo(player, money, point);
            try {
                command.getPointsModifiers().modify(order);
            } catch (Exception ignored) {
            }
            return String.valueOf(order.getPoint());
        }
        if (params.startsWith("shop_modified_money_")) {
            ShopItem shopItem = ShopManager.inst().get(params.substring(20));
            if (shopItem == null) {
                return "0.00";
            }
            String price = shopItem.getPrice(player);
            return price == null ? "ERROR" : price;
        }
        return null;
    }
}

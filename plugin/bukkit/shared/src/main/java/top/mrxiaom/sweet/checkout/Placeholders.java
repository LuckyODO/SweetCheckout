package top.mrxiaom.sweet.checkout;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.PlaceholdersExpansion;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.checkout.func.RankManager;

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
        return super.onRequest(player, params);
    }
}

package top.mrxiaom.sweet.checkout.func;

import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.checkout.PluginCommon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@AutoRegister
public class RankManager extends AbstractModule {
    public static class Rank {
        public final UUID uuid;
        public final String name;
        public final double money;

        public Rank(UUID uuid, String name, double money) {
            this.uuid = uuid;
            this.name = name;
            this.money = money;
        }
    }
    int top;
    IRunTask task;
    Map<Integer, Rank> rankMap = new HashMap<>();
    public RankManager(PluginCommon plugin) {
        super(plugin);
    }

    public int getTop() {
        return top;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        top = config.getInt("rank.top");
        long refreshInterval = config.getLong("rank.refresh-interval") * 20L;
        cancelTask();
        if (top > 0 && refreshInterval > 0) {
            task = plugin.getScheduler().runTaskTimerAsync(() -> {
                List<Rank> ranks = plugin.getTradeDatabase().calculateRank(top);
                rankMap.clear();
                for (int i = 0; i < ranks.size() && i < top;) {
                    Rank rank = ranks.get(i++);
                    rankMap.put(i, rank);
                }
                ranks.clear();
            }, refreshInterval, refreshInterval);
        }
    }

    public void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void onDisable() {
        cancelTask();
    }

    @Nullable
    public Rank get(int num) {
        return rankMap.get(num);
    }

    public static RankManager inst() {
        return instanceOf(RankManager.class);
    }
}

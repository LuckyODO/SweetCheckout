package top.mrxiaom.sweet.checkout;

import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.func.LanguageManager;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.checkout.nms.NMS;

import java.util.ArrayList;
import java.util.List;

public class SweetCheckout extends BukkitPlugin {
    public static SweetCheckout getInstance() {
        return (SweetCheckout) BukkitPlugin.getInstance();
    }

    public SweetCheckout() {
        super(options()
                .bungee(false)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .vaultEconomy(false)
                .scanIgnore("top.mrxiaom.sweet.checkout.libs")
        );
    }


    @Override
    protected void beforeEnable() {
        if (!NMS.init()) {
            throw new IllegalStateException("不支持的游戏版本 " + MinecraftVersion.getVersion().name());
        }
        LanguageManager.inst()
                .setLangFile("messages.yml")
                .register(Messages.class, Messages::holder)
                .register(Errors.class, Errors::holder);
        options.registerDatabase(
                // TODO: 添加数据库，以记录每一条成功的交易
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetCheckout 加载完毕");
    }

    @SafeVarargs
    public final void run(Player player, List<String> commands, Pair<String, Object>... replacements) {
        List<String> list = new ArrayList<>();
        if (replacements.length == 0) list.addAll(commands);
        else for (String s : commands) {
            list.add(Pair.replace(s, replacements));
        }
        run0(player, PAPI.setPlaceholders(player, list), 0);
    }

    private void run0(Player player, List<String> commands, int startIndex) {
        for (int i = startIndex; i < commands.size(); i++) {
            String s = commands.get(i);
            if (s.startsWith("[console]")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.substring(9));
                continue;
            }
            if (s.startsWith("[player]")) {
                Bukkit.dispatchCommand(player, s.substring(8));
                continue;
            }
            if (s.startsWith("[message]")) {
                AdventureUtil.sendMessage(player, s.substring(9));
                continue;
            }
            if (s.startsWith("[actionbar]")) {
                AdventureUtil.sendActionBar(player, s.substring(11));
                continue;
            }
            if (s.startsWith("[delay]")) {
                Long delay = Util.parseLong(s.substring(7)).orElse(null);
                int index = i + 1;
                if (delay == null) continue;
                Bukkit.getScheduler().runTaskLater(getInstance(), () -> run0(player, commands, index), delay);
            }
        }
    }
}

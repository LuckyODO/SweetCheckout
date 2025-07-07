package top.mrxiaom.sweet.checkout;

import com.google.common.collect.Lists;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.func.LanguageManager;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.sweet.checkout.api.PaymentClient;
import top.mrxiaom.sweet.checkout.database.TradeDatabase;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;
import top.mrxiaom.sweet.checkout.nms.NMS;

import java.net.URISyntaxException;
import java.util.List;

public abstract class PluginCommon extends BukkitPlugin {
    public PluginCommon() {
        super(options()
                .bungee(false)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweet.checkout.libs")
        );
        scheduler = new FoliaLibScheduler(this);
    }
    public boolean processingLogs;
    private TradeDatabase tradeDatabase;

    public TradeDatabase getTradeDatabase() {
        return tradeDatabase;
    }

    public abstract PaymentClient createPaymentClient(PaymentAPI parent, String url) throws URISyntaxException;

    @Override
    protected void beforeEnable() {
        if (!NMS.init()) {
            throw new IllegalStateException("不支持的游戏版本 " + MinecraftVersion.getVersion().name());
        }
        LanguageManager.inst()
                .setLangFile("messages.yml")
                .register(Messages.class, Messages::holder)
                .register(Errors.class, Errors::holder)
                .register(CancelReasons.class, CancelReasons::holder);
        options.registerDatabase(
                tradeDatabase = new TradeDatabase(this)
        );
    }

    @Override
    protected void afterEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new Placeholders(this).register();
        }
        getLogger().info("SweetCheckout 加载完毕");
    }

    @SafeVarargs
    public final void run(Player player, List<IAction> commands, Pair<String, Object>... replacements) {
        List<Pair<String, Object>> list = replacements.length == 0 ? null : Lists.newArrayList(replacements);
        run0(this, player, commands, list, 0);
    }

    private static void run0(BukkitPlugin plugin, Player player, List<IAction> actions, List<Pair<String, Object>> replacements, int startIndex) {
        for (int i = startIndex; i < actions.size(); i++) {
            IAction action = actions.get(i);
            action.run(player, replacements);
            long delay = action.delayAfterRun();
            if (delay > 0) {
                int index = i + 1;
                plugin.getScheduler().runTaskLater(() -> run0(plugin, player, actions, replacements, index), delay);
                return;
            }
        }
    }
}

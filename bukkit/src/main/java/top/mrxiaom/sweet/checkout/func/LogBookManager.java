package top.mrxiaom.sweet.checkout.func;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.database.TradeDatabase;
import top.mrxiaom.sweet.checkout.func.entry.ShopItem;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static top.mrxiaom.pluginbase.utils.AdventureUtil.miniMessage;

@AutoRegister
public class LogBookManager extends AbstractModule {
    private String headerText;
    private String lineText, lineHover;
    private DateTimeFormatter lineTimeFormat;
    private final Map<String, String> paymentTypes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private String reasonPoints, reasonBuy;
    public LogBookManager(SweetCheckout plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        File file = new File(plugin.getDataFolder(), "book.yml");
        if (!file.exists()) {
            plugin.saveResource("book.yml", file);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        headerText = config.getString("header.text");
        lineText = config.getString("line.text");
        lineHover = String.join("\n&r", config.getStringList("line.hover"));
        try {
            lineTimeFormat = DateTimeFormatter.ofPattern(config.getString("line.time-format", ""));
        } catch (IllegalArgumentException e) {
            lineTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        }
        ConfigurationSection section;
        paymentTypes.clear();
        section = config.getConfigurationSection("payment-types");
        if (section != null) for (String key : section.getKeys(false)) {
            paymentTypes.put(key, section.getString(key, key));
        }
        reasonPoints = config.getString("reasons-convert.points");
        reasonBuy = config.getString("reasons-convert.buy");
    }

    public Component header(OfflinePlayer player) {
        String s = PAPI.setPlaceholders(player, headerText);
        return miniMessage(s);
    }

    public Component generateLine(ShopManager manager, TradeDatabase.Log log, OfflinePlayer player) {
        List<Pair<String, Object>> replacements = new ArrayList<>();
        replacements.add(Pair.of("%name%", log.name));
        replacements.add(Pair.of("%money%", log.money));
        replacements.add(Pair.of("%time%", log.time.format(lineTimeFormat)));
        replacements.add(Pair.of("%type%", paymentTypes.getOrDefault(log.type, log.type)));
        String reasonStr = log.reason;
        String reason = reasonStr;
        if (reasonStr.startsWith("points:")) {
            String value = reasonStr.substring(7);
            reason = reasonPoints.replace("%value%", value);
        }
        if (reasonStr.startsWith("buy:")) {
            String id = reasonStr.substring(4);
            ShopItem shopItem = manager.get(id);
            String display = shopItem == null ? id : shopItem.display;
            reason = reasonBuy.replace("%value%", id).replace("%display%", display);
        }
        replacements.add(Pair.of("%reason%", reason));
        Component component = replace(lineText, player, replacements);
        return component.hoverEvent(replace(lineHover, player, replacements));
    }

    private static Component replace(String s, OfflinePlayer player, List<Pair<String, Object>> replacements) {
        String str = Pair.replace(s, replacements);
        return miniMessage(PAPI.setPlaceholders(player, str));
    }

    public static LogBookManager inst() {
        return instanceOf(LogBookManager.class);
    }
}

package top.mrxiaom.sweet.checkout.func;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.func.entry.ShopItem;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@AutoRegister
public class ShopManager extends AbstractModule {
    final Map<String, ShopItem> shops = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public ShopManager(SweetCheckout plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        shops.clear();
        List<String> list = config.getStringList("shops-folders");
        for (String s : list) {
            File folder = s.startsWith("./") ? new File(plugin.getDataFolder(), s.substring(2)) : new File(s);
            if (!folder.exists()) {
                Util.mkdirs(folder);
                if (s.equals("./shops")) {
                    plugin.saveResource("shops/example.yml", new File(folder, "example.yml"));
                }
            }
            Util.reloadFolder(new File(plugin.getDataFolder(), "shops"), false, (id, file) -> {
                if (shops.containsKey(id)) {
                    warn("[shops] 重复的商品ID: " + id);
                    return;
                }
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                ShopItem loaded = ShopItem.load(plugin, cfg, id);
                if (loaded != null) {
                    shops.put(id, loaded);
                }
            });
        }
    }

    public Set<String> shops() {
        return shops.keySet();
    }

    public ShopItem get(String id) {
        return shops.get(id);
    }

    public static ShopManager inst() {
        return instanceOf(ShopManager.class);
    }
}

package top.mrxiaom.sweet.checkout;

import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.sweet.checkout.nms.NMS;

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
        options.registerDatabase(
                // 在这里添加数据库 (如果需要的话)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetCheckout 加载完毕");
    }
}

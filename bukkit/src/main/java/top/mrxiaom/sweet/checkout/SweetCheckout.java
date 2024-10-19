package top.mrxiaom.sweet.checkout;
		
import top.mrxiaom.pluginbase.BukkitPlugin;

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
        options.registerDatabase(
                // 在这里添加数据库 (如果需要的话)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetCheckout 加载完毕");
    }
}

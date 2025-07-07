package top.mrxiaom.sweet.checkout;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginBase;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.sweet.checkout.api.PaymentClient;
import top.mrxiaom.sweet.checkout.backend.BukkitMain;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;

import java.io.File;

public class SweetCheckout extends PluginCommon {
    public static SweetCheckout getInstance() {
        return (SweetCheckout) BukkitPlugin.getInstance();
    }
    private BukkitMain main;

    public BukkitMain getBackend() {
        return main;
    }

    @Override
    protected void beforeEnable() {
        super.beforeEnable();
        main = new BukkitMain(getLogger(), new File(getDataFolder(), "backend"));
    }

    @Override
    protected void beforeReloadConfig(FileConfiguration config) {
        super.beforeReloadConfig(config);
        main.beforePluginReloadConfig();
    }

    @Override
    public PaymentClient handlePaymentReload(PaymentAPI parent, @Nullable String url) {
        return main.getClient();
    }
}

package top.mrxiaom.sweet.checkout;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.checkout.api.PaymentClient;
import top.mrxiaom.sweet.checkout.backend.BukkitMain;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;

public class SweetCheckout extends PluginCommon {
    private BukkitMain main;

    public BukkitMain getBackend() {
        return main;
    }

    @Override
    protected void beforeEnable() {
        super.beforeEnable();
        main = new BukkitMain(getLogger(), getDataFolder());
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

package top.mrxiaom.sweet.checkout.backend;

import top.mrxiaom.sweet.checkout.api.LocalPaymentClient;
import top.mrxiaom.sweet.checkout.backend.data.LocalClientInfo;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;

import java.io.File;

public class BukkitMain extends CommonMain<LocalClientInfo, PluginPaymentServer> {
    private final PluginPaymentServer server;
    private LocalPaymentClient client;
    public BukkitMain(java.util.logging.Logger logger, File dataFolder) {
        super(new LoggerAdapter(logger), dataFolder);
        reloadConfig();
        this.server = new PluginPaymentServer(this, getLogger());
    }

    public void beforePluginReloadConfig() {
        reloadConfig();
        server.restart();
        if (client == null) {
            client = new LocalPaymentClient(this, PaymentAPI.inst());
        }
    }

    public LocalPaymentClient getClient() {
        return client;
    }

    @Override
    public PluginPaymentServer getServer() {
        return server;
    }
}

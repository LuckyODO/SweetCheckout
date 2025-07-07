package top.mrxiaom.sweet.checkout;

import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.sweet.checkout.api.PaymentClient;
import top.mrxiaom.sweet.checkout.api.WebSocketPaymentClient;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;

import java.net.URISyntaxException;

public class SweetCheckout extends PluginCommon {
    public static SweetCheckout getInstance() {
        return (SweetCheckout) BukkitPlugin.getInstance();
    }

    @Override
    public PaymentClient handlePaymentReload(PaymentAPI parent, @Nullable String url) throws URISyntaxException {
        PaymentClient client = parent.getClient();
        if (client != null && client.isOpen()) {
            // 已连接地址与配置文件地址相同时，不断开重连
            if (client.getUrl().equals(url)) {
                info("重载时，配置中的后端地址无变动，保持连接");
                return client;
            }
            client.close();
            client = null;
        }
        if (url != null) {
            info("正在连接到后端 " + url);
            client = new WebSocketPaymentClient(parent, url);
            client.connect();
        } else {
            warn("未配置后端地址，请在配置文件进行配置");
        }
        return client;
    }
}

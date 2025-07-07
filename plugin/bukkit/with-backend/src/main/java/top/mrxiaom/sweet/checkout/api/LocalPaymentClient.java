package top.mrxiaom.sweet.checkout.api;

import top.mrxiaom.sweet.checkout.backend.BukkitMain;
import top.mrxiaom.sweet.checkout.backend.data.LocalClientInfo;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;

public class LocalPaymentClient extends PaymentClient {
    private final BukkitMain main;
    private final LocalClientInfo info;
    public LocalPaymentClient(BukkitMain main, PaymentAPI parent) {
        super(parent);
        this.main = main;
        this.info = new LocalClientInfo();
    }

    @Override
    public String getUrl() {
        return "local";
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public void connect() {
    }

    @Override
    public void send(String message) {
        main.getServer().onMessage(info, message);
    }
}

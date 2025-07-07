package top.mrxiaom.sweet.checkout.api;

import top.mrxiaom.sweet.checkout.func.PaymentAPI;

public abstract class PaymentClient {
    protected final PaymentAPI parent;
    protected PaymentClient(PaymentAPI parent) {
        this.parent = parent;
    }

    public PaymentAPI getParent() {
        return parent;
    }

    public abstract String getUrl();
    public abstract boolean isOpen();
    public abstract void close();
    public abstract void connect();
    public abstract void send(String message);
}

package top.mrxiaom.sweet.checkout;

import top.mrxiaom.sweet.checkout.api.PaymentClient;
import top.mrxiaom.sweet.checkout.api.WebSocketPaymentClient;
import top.mrxiaom.sweet.checkout.func.PaymentAPI;

import java.net.URISyntaxException;

public class SweetCheckout extends PluginCommon {
    @Override
    public PaymentClient createPaymentClient(PaymentAPI parent, String url) throws URISyntaxException {
        return new WebSocketPaymentClient(parent, url);
    }
}

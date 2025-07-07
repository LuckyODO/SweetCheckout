package top.mrxiaom.sweet.checkout.backend.data;

public class LocalClientInfo extends ClientInfo<LocalClientInfo> {
    @Override
    public boolean isOpen() {
        return true;
    }
}

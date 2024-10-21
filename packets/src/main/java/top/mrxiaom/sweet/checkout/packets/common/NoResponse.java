package top.mrxiaom.sweet.checkout.packets.common;

@SuppressWarnings({"rawtypes"})
public class NoResponse implements IPacket {
    @Override
    public Class<?> getResponsePacket() {
        return null;
    }
}

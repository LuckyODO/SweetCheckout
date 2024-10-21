package top.mrxiaom.sweet.checkout.packets.common;

@SuppressWarnings({"rawtypes"})
public interface IPacket<T extends IPacket> {
    Class<T> getResponsePacket();
    default boolean isResponsePacket(IPacket packet) {
        Class<T> type = getResponsePacket();
        return type != null && type.isInstance(packet);
    }
}

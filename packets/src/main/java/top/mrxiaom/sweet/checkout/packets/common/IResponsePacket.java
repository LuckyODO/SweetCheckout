package top.mrxiaom.sweet.checkout.packets.common;

public interface IResponsePacket extends IPacket<NoResponse> {
    @Override
    default Class<NoResponse> getResponsePacket() {
        return null;
    }
}

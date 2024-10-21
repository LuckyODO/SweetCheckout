package top.mrxiaom.sweet.checkout.packets.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.common.IResponsePacket;

/**
 * 插件主动取消订单
 */
@Getter
@AllArgsConstructor
public class PacketPluginCancelOrder implements IPacket<PacketPluginCancelOrder.Response> {
    /**
     * 订单ID
     */
    private String orderId;

    @Override
    public Class<Response> getResponsePacket() {
        return Response.class;
    }
    @Getter
    @AllArgsConstructor
    public static class Response implements IResponsePacket {
        /**
         * 错误信息，无错误时为空字符串
         */
        private String error;
    }
}

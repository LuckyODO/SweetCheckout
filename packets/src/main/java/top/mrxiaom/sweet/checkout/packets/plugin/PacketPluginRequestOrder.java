package top.mrxiaom.sweet.checkout.packets.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.common.IResponsePacket;

/**
 * 插件向后端申请新支付订单
 */
@Getter
@AllArgsConstructor
public class PacketPluginRequestOrder implements IPacket<PacketPluginRequestOrder.Response> {
    /**
     * 玩家名
     */
    private String playerName;
    /**
     * 支付类型，可用 wechat 和 alipay
     */
    private String type;
    /**
     * 金额字符串，后端会自动处理数字
     */
    private String price;

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
        /**
         * 订单ID
         */
        private String orderId;
        /**
         * 付款二维码地址
         */
        private String paymentUrl;
    }
}

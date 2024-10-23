package top.mrxiaom.sweet.checkout.packets.plugin;

import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.common.IResponsePacket;

/**
 * 插件向后端申请新支付订单
 */
@SuppressWarnings("FieldMayBeFinal")
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
     * 商品名
     */
    private String productName;
    /**
     * 金额字符串，后端会自动处理数字
     */
    private String price;

    public PacketPluginRequestOrder(String playerName, String type, String productName, String price) {
        this.playerName = playerName;
        this.type = type;
        this.productName = productName;
        this.price = price;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getType() {
        return type;
    }

    public String getProductName() {
        return productName;
    }

    public String getPrice() {
        return price;
    }

    @Override
    public Class<Response> getResponsePacket() {
        return Response.class;
    }
    @SuppressWarnings("FieldMayBeFinal")
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

        public Response(String error) {
            this.error = error;
            this.orderId = "";
            this.paymentUrl = "";
        }

        public Response(String orderId, String paymentUrl) {
            this.error = "";
            this.orderId = orderId;
            this.paymentUrl = paymentUrl;
        }

        public String getError() {
            return error;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getPaymentUrl() {
            return paymentUrl;
        }
    }
}

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

    /**
     * 是否允许自增金额，即使用 Hook 支付时，如果金额已被锁定，则使用 +0.01 金额
     */
    private boolean allowIncreasing;

    public PacketPluginRequestOrder(String playerName, String type, String productName, String price) {
        this(playerName, type, productName, price, false);
    }
    public PacketPluginRequestOrder(String playerName, String type, String productName, String price, boolean allowIncreasing) {
        this.playerName = playerName;
        this.type = type;
        this.productName = productName;
        this.price = price;
        this.allowIncreasing = allowIncreasing;
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

    public void setPrice(String price) {
        this.price = price;
    }

    public boolean isAllowIncreasing() {
        return allowIncreasing;
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
         * 支付子类型，可用 face2face, native 和 hook
         */
        private String subType;
        /**
         * 订单ID
         */
        private String orderId;
        /**
         * 最终下单金额
         */
        private String money;
        /**
         * 付款二维码地址
         */
        private String paymentUrl;

        public Response(String error) {
            this.error = error;
            this.orderId = "";
            this.paymentUrl = "";
        }

        public Response(String subType, String orderId, String money, String paymentUrl) {
            this.error = "";
            this.subType = subType;
            this.orderId = orderId;
            this.money = money;
            this.paymentUrl = paymentUrl;
        }

        public String getError() {
            return error;
        }

        public String getSubType() {
            return subType;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getMoney() {
            return money;
        }

        public String getPaymentUrl() {
            return paymentUrl;
        }
    }
}

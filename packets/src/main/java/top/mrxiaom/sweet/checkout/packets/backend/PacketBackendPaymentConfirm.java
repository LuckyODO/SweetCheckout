package top.mrxiaom.sweet.checkout.packets.backend;

import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.common.NoResponse;

/**
 * 向插件端反馈订单已完成
 */
@SuppressWarnings("FieldMayBeFinal")
public class PacketBackendPaymentConfirm implements IPacket<NoResponse> {
    /**
     * 订单ID
     */
    private String orderId;
    /**
     * 付款人名字
     */
    private String name;
    /**
     * 支付的金额
     */
    private String money;

    public PacketBackendPaymentConfirm(String orderId, String name, String money) {
        this.orderId = orderId;
        this.name = name;
        this.money = money;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getName() {
        return name;
    }

    public String getMoney() {
        return money;
    }

    @Override
    public Class<NoResponse> getResponsePacket() {
        return NoResponse.class;
    }
}

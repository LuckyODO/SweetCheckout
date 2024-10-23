package top.mrxiaom.sweet.checkout.packets.backend;

import top.mrxiaom.sweet.checkout.packets.common.IPacket;
import top.mrxiaom.sweet.checkout.packets.common.NoResponse;

/**
 * 向插件端反馈订单已完成
 */
@SuppressWarnings("FieldMayBeFinal")
public class PacketBackendPaymentCancel implements IPacket<NoResponse> {
    /**
     * 订单ID
     */
    private String orderId;
    /**
     * 取消原因
     */
    private String reason;

    public PacketBackendPaymentCancel(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public Class<NoResponse> getResponsePacket() {
        return NoResponse.class;
    }
}

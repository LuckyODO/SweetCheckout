package top.mrxiaom.sweet.checkout.backend.data;

public class HookReceive {
    /**
     * 支付来源，可用 wechat 和 alipay
     */
    private String type;
    /**
     * 付款人名字
     */
    private String name;
    /**
     * 付款金额
     */
    private String money;

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getMoney() {
        return money;
    }
}

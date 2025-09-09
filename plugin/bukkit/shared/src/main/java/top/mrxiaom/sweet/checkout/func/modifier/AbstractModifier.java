package top.mrxiaom.sweet.checkout.func.modifier;

import com.ezylang.evalex.Expression;

public abstract class AbstractModifier {
    protected final String moneyExpression, pointExpression;
    protected final int priority;
    protected final boolean end;
    protected AbstractModifier(String moneyExpression, String pointExpression, int priority, boolean end) {
        this.moneyExpression = moneyExpression;
        this.pointExpression = pointExpression;
        this.priority = priority;
        this.end = end;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 修改订单信息
     * @param orderInfo 订单信息
     * @return 返回 <code>true</code> 代表修改完成，终止剩下的 Modifier 修改进程
     */
    public abstract boolean modify(OrderInfo orderInfo) throws Exception;

    public void doModify(OrderInfo orderInfo) throws Exception {
        if (!moneyExpression.isEmpty()) {
            double money = new Expression(moneyExpression)
                    .with("money", orderInfo.getMoney())
                    .evaluate()
                    .getNumberValue()
                    .doubleValue();
            orderInfo.setMoney(money);
        }
        if (!pointExpression.isEmpty()) {
            int point = new Expression(pointExpression)
                    .with("point", orderInfo.getPoint())
                    .evaluate()
                    .getNumberValue()
                    .intValue();
            orderInfo.setPoint(point);
        }
    }
}

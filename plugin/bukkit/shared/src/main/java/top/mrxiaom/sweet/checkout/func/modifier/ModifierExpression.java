package top.mrxiaom.sweet.checkout.func.modifier;

import com.ezylang.evalex.Expression;
import top.mrxiaom.pluginbase.utils.PAPI;

public class ModifierExpression extends AbstractModifier {
    private final String expression;
    protected ModifierExpression(String moneyExpression, String pointExpression, int priority, boolean end, String expression) {
        super(moneyExpression, pointExpression, priority, end);
        this.expression = expression;
    }

    @Override
    public boolean modify(OrderInfo orderInfo) throws Exception {
        String expression = PAPI.setPlaceholders(orderInfo.getPlayer(), this.expression);
        Boolean condition = new Expression(expression)
                .evaluate()
                .getBooleanValue();
        if (condition == Boolean.TRUE) {
            doModify(orderInfo);
            return end;
        }
        return false;
    }
}

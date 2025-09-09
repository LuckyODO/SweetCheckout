package top.mrxiaom.sweet.checkout.func.modifier;

public class ModifierPermission extends AbstractModifier {
    private final String permission;
    public ModifierPermission(String moneyExpression, String pointExpression, int priority, boolean end, String permission) {
        super(moneyExpression, pointExpression, priority, end);
        this.permission = permission;
    }

    @Override
    public boolean modify(OrderInfo orderInfo) throws Exception {
        if (orderInfo.getPlayer().hasPermission(permission)) {
            doModify(orderInfo);
            return end;
        }
        return false;
    }
}

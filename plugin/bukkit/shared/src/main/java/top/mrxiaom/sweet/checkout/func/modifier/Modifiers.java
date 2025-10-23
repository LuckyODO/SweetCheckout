package top.mrxiaom.sweet.checkout.func.modifier;

import org.bukkit.configuration.ConfigurationSection;
import top.mrxiaom.pluginbase.utils.ConfigUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Modifiers {
    private final List<AbstractModifier> modifiers;
    public Modifiers(List<AbstractModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public List<AbstractModifier> getModifiers() {
        return modifiers;
    }

    public void modify(OrderInfo orderInfo) throws Exception {
        for (AbstractModifier modifier : modifiers) {
            if (modifier.modify(orderInfo)) {
                break;
            }
        }
    }

    public static Modifiers load(ConfigurationSection config, String key) {
        List<AbstractModifier> modifiers = new ArrayList<>();
        for (ConfigurationSection section : ConfigUtils.getSectionList(config, key)) {
            String money = section.getString("operator.money", "");
            String point = section.getString("operator.point", "");
            int priority = section.getInt("priority", 1000);
            boolean end = section.getBoolean("end", false);
            if (section.contains("permission")) {
                String permission = section.getString("permission");
                modifiers.add(new ModifierPermission(money, point, priority, end, permission));
                continue;
            }
            if (section.contains("eval")) {
                String expression = section.getString("eval");
                modifiers.add(new ModifierExpression(money, point, priority, end, expression));
                continue;
            }
        }
        modifiers.sort(Comparator.comparingInt(AbstractModifier::getPriority));
        return new Modifiers(modifiers);
    }
}

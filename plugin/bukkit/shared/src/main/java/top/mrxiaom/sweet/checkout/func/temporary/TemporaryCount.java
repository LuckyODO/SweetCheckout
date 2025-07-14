package top.mrxiaom.sweet.checkout.func.temporary;

import top.mrxiaom.pluginbase.temporary.TemporaryInteger;
import top.mrxiaom.pluginbase.temporary.period.Period;

import java.util.function.Supplier;

public class TemporaryCount extends TemporaryInteger {
    private Runnable reset;
    public TemporaryCount(Period period, Runnable reset, Supplier<Integer> defaultValue) {
        super(period, defaultValue);
        this.reset = reset;
    }

    public void setResetActions(Runnable reset) {
        this.reset = reset;
    }

    @Override
    public void applyDefaultValue() {
        super.applyDefaultValue();
        reset.run();
    }
}

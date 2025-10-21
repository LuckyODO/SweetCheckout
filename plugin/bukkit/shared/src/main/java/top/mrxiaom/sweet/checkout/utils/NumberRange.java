package top.mrxiaom.sweet.checkout.utils;

import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;

public class NumberRange {
    private final double min, max;

    public NumberRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public boolean isInRange(double v) {
        return v >= min && v <= max;
    }

    @Nullable
    public static NumberRange from(@Nullable String str) {
        if (str == null) return null;
        if (!str.contains("-")) {
            Double v = Util.parseDouble(str).orElse(null);
            return v == null ? null : new NumberRange(v, v);
        }
        String[] split = str.split("-", 2);
        Double min = Util.parseDouble(split[0]).orElse(null);
        Double max = Util.parseDouble(split[1]).orElse(null);
        return min == null || max == null ? null : new NumberRange(min, max);
    }
}

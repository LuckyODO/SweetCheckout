package top.mrxiaom.sweet.checkout.func.temporary;

import top.mrxiaom.pluginbase.temporary.period.Period;

import java.time.LocalDateTime;

public class NeverReset implements Period {
    public static final NeverReset INSTANCE = new NeverReset();
    private static final LocalDateTime FOREVER = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    private NeverReset() {}
    @Override
    public LocalDateTime getNextOutdateTime(LocalDateTime localDateTime) {
        return FOREVER;
    }
}

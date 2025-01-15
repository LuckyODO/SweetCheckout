package top.mrxiaom.sweet.checkout.utils;

import java.util.List;
import java.util.Random;

public class Utils {
    public static <T> T random(List<T> list, T def) {
        if (list.isEmpty()) return def;
        if (list.size() == 1) return list.get(0);
        return list.get(new Random().nextInt(list.size()));
    }
}

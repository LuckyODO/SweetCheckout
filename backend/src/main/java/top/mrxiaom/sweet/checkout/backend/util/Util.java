package top.mrxiaom.sweet.checkout.backend.util;

import java.util.Optional;

public class Util {
    public static Optional<Double> parseDouble(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

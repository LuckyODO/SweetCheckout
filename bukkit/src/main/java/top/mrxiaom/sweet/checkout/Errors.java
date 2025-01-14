package top.mrxiaom.sweet.checkout;

import top.mrxiaom.pluginbase.func.language.IHolderAccessor;
import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder;

import java.util.List;

import static top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder.wrap;

@Language(prefix = "errors.")
public enum Errors implements IHolderAccessor {
    // TODO: 来自后端的错误提示
    unknown("&e未知错误 %type%")

    ;
    Errors(String defaultValue) {
        holder = wrap(this, defaultValue);
    }
    Errors(String... defaultValue) {
        holder = wrap(this, defaultValue);
    }
    Errors(List<String> defaultValue) {
        holder = wrap(this, defaultValue);
    }
    private final LanguageEnumAutoHolder<Errors> holder;
    public LanguageEnumAutoHolder<Errors> holder() {
        return holder;
    }

    public static Errors fromString(String type) {
        String key = type.toLowerCase().replace(".", "__").replace("-", "_");
        for (Errors error : values()) {
            if (error.name().equals(key)) {
                return error;
            }
        }
        return unknown;
    }
}

package top.mrxiaom.sweet.checkout;

import top.mrxiaom.pluginbase.func.language.IHolderAccessor;
import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder;

import java.util.List;

import static top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder.wrap;

@Language(prefix = "errors.")
public enum Errors implements IHolderAccessor {
    unknown("&e未知错误 %type%"),
    payment__not_a_number("&e请输入正确的金额"),
    payment__already_requested("&e请先完成正在进行的订单"),
    payment__type_unknown("&e未知支付类型"),
    payment__cancel__not_found("&e你要取消的订单不存在"),
    payment__hook_price_locked("&e支付系统繁忙，请稍后再试"),
    payment__can_not_create_id("&e支付系统生成订单号繁忙，请稍后再试"),
    payment__internal_error("&e支付接口出现内部错误，请联系服务器管理员"),

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

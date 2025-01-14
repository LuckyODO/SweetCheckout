package top.mrxiaom.sweet.checkout;

import top.mrxiaom.pluginbase.func.language.IHolderAccessor;
import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder;

import java.util.List;

import static top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder.wrap;

@Language(prefix = "messages.")
public enum Messages implements IHolderAccessor {
    not_connect("&e插件未连接到后端，请联系服务器管理员"),
    commands__reload("&a配置文件已重载"),
    commands__points__disabled__wechat("&e管理员已禁用微信支付"),
    commands__points__disabled__alipay("&e管理员已禁用支付宝支付"),
    commands__points__unknown_type("&e未知支付类型"),
    commands__points__processing("&e请先完成你正在进行的订单"),
    commands__points__send("&f正在请求…"),
    commands__map__not_found("&e请手持一张有效的地图"),
    commands__map__success("&a地图数据已导出到 output.map 文件"),
    commands__map__invalid("&e该文件不是有效的地图文件"),
    commands__map__given("&e已开启二维码扫描模拟"),

    ;
    Messages(String defaultValue) {
        holder = wrap(this, defaultValue);
    }
    Messages(String... defaultValue) {
        holder = wrap(this, defaultValue);
    }
    Messages(List<String> defaultValue) {
        holder = wrap(this, defaultValue);
    }
    private final LanguageEnumAutoHolder<Messages> holder;
    public LanguageEnumAutoHolder<Messages> holder() {
        return holder;
    }
}

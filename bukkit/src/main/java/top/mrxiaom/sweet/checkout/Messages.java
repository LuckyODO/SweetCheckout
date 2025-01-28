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
    commands__reload_database("&a已重新连接到数据库"),
    commands__points__disabled__wechat("&e管理员已禁用微信支付"),
    commands__points__disabled__alipay("&e管理员已禁用支付宝支付"),
    commands__points__unknown_type("&e未知支付类型"),
    commands__points__processing("&e请先完成你正在进行的订单"),
    commands__points__send("&f正在请求…"),
    commands__points__sent("",
            "&a&l下单成功&r",
            "&f 订单号: &e&l%order_id%&r",
            "&f 金额: &c&l￥%money%&r",
            "&7&m-----------------------------&r",
            "&e 请在&b %timeout% &e秒内扫码支付。",
            "&e 支付期间请不要离开服务器，完成后，",
            "&e 奖励将发放到你的账户，",
            "&7&m-----------------------------&r",
            "&f 按下&c&lＱ&r&f键取消订单",
            "&7&m-----------------------------&r",
            "&b 支付此订单，即代表你已同意服务器的相关条款",
            "&7&m-----------------------------&r",
            "", ""),
    commands__buy__not_found("&e无效的商品ID"),
    commands__buy__disabled__wechat("&e管理员已禁用微信支付"),
    commands__buy__disabled__alipay("&e管理员已禁用支付宝支付"),
    commands__buy__unknown_type("&e未知支付类型"),
    commands__buy__processing("&e请先完成你正在进行的订单"),
    commands__buy__send("&f正在请求…"),
    commands__buy__sent("",
            "&a&l下单成功&r",
            "&f 订单号: &e&l%order_id%&r",
            "&f 商品: &e&l%display%&r",
            "&f 金额: &c&l￥%money%&r",
            "&7&m-----------------------------&r",
            "&e 请在&b %timeout% &e秒内扫码支付。",
            "&e 支付期间请不要离开服务器，完成后，",
            "&e 奖励将发放到你的账户，",
            "&7&m-----------------------------&r",
            "&f 按下&c&lＱ&r&f键取消订单",
            "&7&m-----------------------------&r",
            "&b 支付此订单，即代表你已同意服务器的相关条款",
            "&7&m-----------------------------&r",
            "", ""),
    commands__map__not_found("&e请手持一张有效的地图"),
    commands__map__success("&a地图数据已导出到 output.map 文件"),
    commands__map__invalid("&e该文件不是有效的地图文件"),
    commands__map__given("&e已开启二维码扫描模拟"),
    cancelled("已取消付款，原因: %reason%"),

    commands__rank__success("",
            "&e&l充值排行榜&r",
            " &c1. %player_1%    %money_1%&r",
            " &c2. %player_2%    %money_2%&r",
            " &c3. %player_3%    %money_3%&r",
            " &c4. %player_4%    %money_4%&r",
            " &c5. %player_5%    %money_5%&r",
            " &c6. %player_6%    %money_6%&r",
            " &c7. %player_7%    %money_7%&r",
            " &c8. %player_8%    %money_8%&r",
            " &c9. %player_9%    %money_9%&r",
            "&c10. %player_10%    %money_10%&r",
            "&b排行榜每隔&e1&b分钟刷新一次", ""),

    rank__exist__player("&e%name%"),
    rank__exist__money("&f&l￥%money%"),
    rank__not_found__player("&7虚位以待"),
    rank__not_found__money(""),

    commands__log__no_player("&e找不到该玩家"),
    commands__log__no_number("&e你输入的不是一个正确的金额"),
    commands__log__success("&a你已为玩家&e %name% &a添加一条交易记录，使用&e %type% &a支付 &e￥%money%&a，理由为&e %reason%"),

    commands__help__normal(
            "&e&lSweetCheckout&r &b支付系统",
            "&f/checkout points <类型> <金额> &7通过微信(wechat)或支付宝(alipay)下单指定金额的点券",
            "&f/checkout buy <商品ID> <类型> &7通过微信(wechat)或支付宝(alipay)下单指定商品",
            ""),
    commands__help__admin(
            "&e&lSweetCheckout&r &b支付系统",
            "&f/checkout points <类型> <金额> &7通过微信(wechat)或支付宝(alipay)下单指定金额的点券",
            "&f/checkout buy <商品ID> <类型> &7通过微信(wechat)或支付宝(alipay)下单指定商品",
            "&f/checkout map [文件名] &7不输入文件名时，将手中的地图保存到&f output.map &7文件；输入文件名时，通过地图预览文件以测试文件是否正常",
            "&f/checkout log <玩家> <类型> <金额> <原因...> &7手动添加充值记录",
            "&f/checkout reload database &7重新连接数据库",
            "&f/checkout reload &7重载配置文件",
            ""),


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

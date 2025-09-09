package top.mrxiaom.sweet.checkout.func.modifier;

import org.bukkit.entity.Player;

public class OrderInfo {
    private final Player player;
    private double money;
    private double point;

    public OrderInfo(Player player, double money, double point) {
        this.player = player;
        this.money = money;
        this.point = point;
    }

    public Player getPlayer() {
        return player;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public double getPoint() {
        return point;
    }

    public void setPoint(double point) {
        this.point = point;
    }
}

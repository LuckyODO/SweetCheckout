package top.mrxiaom.sweet.checkout.func.modifier;

import org.bukkit.entity.Player;

public class OrderInfo {
    private final Player player;
    private double money;
    private int point;

    public OrderInfo(Player player, double money, int point) {
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

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }
}

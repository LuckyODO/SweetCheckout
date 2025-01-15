package top.mrxiaom.sweet.checkout.func.entry;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.sweet.checkout.func.PaymentsAndQRCodeManager;
import top.mrxiaom.sweet.checkout.nms.NMS;

import java.util.function.Consumer;

public class PaymentInfo {
    public final int mapId;
    public final Player player;
    public final byte[] colors;
    public final ItemStack original;
    public final ItemStack newItem;
    public final String orderId;
    public final long outdateTime;
    public final Consumer<Double> done;

    public PaymentInfo(int mapId, Player player, byte[] colors, ItemStack original, ItemStack newItem, String orderId, long outdateTime, Consumer<Double> done) {
        this.mapId = mapId;
        this.player = player;
        this.colors = colors;
        this.original = original;
        this.newItem = newItem;
        this.orderId = orderId;
        this.outdateTime = outdateTime;
        this.done = done;
        updateMapColors();
    }

    public void updateMapColors() {
        Object packet = NMS.createMapPacket(mapId, colors);
        NMS.sendPacket(player, packet);
    }

    public void giveItemBack() {
        player.getInventory().setItemInMainHand(original);
    }
}

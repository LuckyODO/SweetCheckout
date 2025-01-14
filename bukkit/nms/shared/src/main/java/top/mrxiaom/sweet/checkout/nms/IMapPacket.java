package top.mrxiaom.sweet.checkout.nms;

import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;

public interface IMapPacket {
    Object createMapPacket(int mapId, byte[] colors);
    void sendPacket(Player player, Object packet);
    byte[] getColors(MapRenderer renderer);
}

package top.mrxiaom.sweet.checkout.nms;

import net.minecraft.server.v1_16_R3.MapIcon;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketPlayOutMap;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MapPacket_v1_16_R3 implements IMapPacket {
    @Override
    public Object createMapPacket(int mapId, byte[] colors) {
        List<MapIcon> mapIcons = new ArrayList<>();
        return new PacketPlayOutMap(mapId, (byte) 0, false, false, mapIcons, colors, 0, 0, 128, 128);
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        CraftPlayer p = (CraftPlayer) player;
        p.getHandle().playerConnection.sendPacket((Packet<?>) packet);
    }
}

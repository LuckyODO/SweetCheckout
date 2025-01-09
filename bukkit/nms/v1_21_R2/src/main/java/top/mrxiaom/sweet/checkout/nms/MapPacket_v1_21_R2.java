package top.mrxiaom.sweet.checkout.nms;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.world.level.saveddata.maps.MapIcon;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import org.bukkit.craftbukkit.v1_21_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MapPacket_v1_21_R2 implements IMapPacket {
    @Override
    public Object createMapPacket(int mapId, byte[] colors) {
        List<MapIcon> mapIcons = new ArrayList<>();
        WorldMap.c c = of(colors);
        return new PacketPlayOutMap(new MapId(mapId), (byte) 0, false, mapIcons, c);
    }

    private WorldMap.c of(byte[] colors) {
        if (colors == null) return null;
        return new WorldMap.c(0, 0, 128, 128, colors);
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        CraftPlayer p = (CraftPlayer) player;
        p.getHandle().f.sendPacket((Packet<?>) packet);
    }
}

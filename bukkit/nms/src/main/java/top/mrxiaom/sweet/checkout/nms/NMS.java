package top.mrxiaom.sweet.checkout.nms;

import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.entity.Player;

import static de.tr7zw.changeme.nbtapi.utils.MinecraftVersion.*;

public class NMS {
    private static IMapPacket mapPacket;
    private static boolean loaded;

    public static boolean init() {
        if (loaded) return true;
        MinecraftVersion version = MinecraftVersion.getVersion();
        if (version.equals(MC1_20_R3)) {
            mapPacket = new MapPacket_v1_20_R3();
            return loaded = true;
        }

        return false;
    }

    public static Object createMapPacket(int mapId, byte[] colors) {
        return mapPacket.createMapPacket(mapId, colors);
    }

    public static void sendPacket(Player player, Object packet) {
        mapPacket.sendPacket(player, packet);
    }
}

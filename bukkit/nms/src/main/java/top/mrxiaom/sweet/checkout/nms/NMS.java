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
        if (version.equals(MC1_21_R3)) {
            mapPacket = new MapPacket_v1_21_R3();
            return loaded = true;
        }
        if (version.equals(MC1_21_R2)) {
            mapPacket = new MapPacket_v1_21_R2();
            return loaded = true;
        }
        if (version.equals(MC1_21_R1)) {
            mapPacket = new MapPacket_v1_21_R1();
            return loaded = true;
        }
        if (version.equals(MC1_20_R4)) {
            mapPacket = new MapPacket_v1_20_R4();
            return loaded = true;
        }
        if (version.equals(MC1_20_R3)) {
            mapPacket = new MapPacket_v1_20_R3();
            return loaded = true;
        }
        if (version.equals(MC1_20_R2)) {
            mapPacket = new MapPacket_v1_20_R2();
            return loaded = true;
        }
        if (version.equals(MC1_20_R1)) {
            mapPacket = new MapPacket_v1_20_R1();
            return loaded = true;
        }
        if (version.equals(MC1_19_R3)) {
            mapPacket = new MapPacket_v1_19_R3();
            return loaded = true;
        }
        if (version.equals(MC1_18_R2)) {
            mapPacket = new MapPacket_v1_18_R2();
            return loaded = true;
        }
        if (version.equals(MC1_17_R1)) {
            mapPacket = new MapPacket_v1_17_R1();
            return loaded = true;
        }
        if (version.equals(MC1_16_R3)) {
            mapPacket = new MapPacket_v1_16_R3();
            return loaded = true;
        }
        if (version.equals(MC1_15_R1)) {
            mapPacket = new MapPacket_v1_15_R1();
            return loaded = true;
        }
        if (version.equals(MC1_14_R1)) {
            mapPacket = new MapPacket_v1_14_R1();
            return loaded = true;
        }
        if (version.equals(MC1_13_R2)) {
            mapPacket = new MapPacket_v1_13_R2();
            return loaded = true;
        }
        if (version.equals(MC1_12_R1)) {
            mapPacket = new MapPacket_v1_12_R1();
            return loaded = true;
        }
        if (version.equals(MC1_11_R1)) {
            mapPacket = new MapPacket_v1_11_R1();
            return loaded = true;
        }
        if (version.equals(MC1_10_R1)) {
            mapPacket = new MapPacket_v1_10_R1();
            return loaded = true;
        }
        if (version.equals(MC1_9_R2)) {
            mapPacket = new MapPacket_v1_9_R2();
            return loaded = true;
        }
        if (version.equals(MC1_8_R3)) {
            mapPacket = new MapPacket_v1_8_R3();
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

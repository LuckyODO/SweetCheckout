package top.mrxiaom.sweet.checkout.func;

import com.google.common.collect.Lists;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.qrcode.QRCode;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.nms.NMS;
import top.mrxiaom.sweet.checkout.packets.plugin.PacketPluginCancelOrder;

import java.util.*;
import java.util.function.Consumer;

@AutoRegister
public class PaymentsAndQRCodeManager extends AbstractModule implements Listener {

    public static final String FLAG_SWEET_CHECKOUT_MAP = "SWEET_CHECKOUT_MAP";

    public static class PaymentInfo {
        public final Player player;
        public final byte[] colors;
        public final ItemStack original;
        public final ItemStack newItem;
        public final String orderId;
        public final long outdateTime;
        public final Consumer<Double> done;

        public PaymentInfo(Player player, byte[] colors, ItemStack original, ItemStack newItem, String orderId, long outdateTime, Consumer<Double> done) {
            this.player = player;
            this.colors = colors;
            this.original = original;
            this.newItem = newItem;
            this.orderId = orderId;
            this.outdateTime = outdateTime;
            this.done = done;
            update();
        }

        public void update() {
            Object packet = NMS.createMapPacket(mapId, colors);
            NMS.sendPacket(player, packet);
        }

        public void giveItemBack() {
            player.getInventory().setItemInMainHand(original);
        }
    }
    private final Map<UUID, PaymentInfo> players = new HashMap<>();
    private final Set<UUID> processPlayers = new HashSet<>();
    private static Material filledMap;
    protected static int mapId = 20070831;
    private String mapName;
    private List<String> mapLore;
    private Integer mapCustomModelData;
    private String paymentActionBarProcess;
    private String paymentActionBarDone;
    private String paymentActionBarTimeout;
    private String paymentActionBarCancel;
    public PaymentsAndQRCodeManager(SweetCheckout plugin) {
        super(plugin);
        filledMap = Util.valueOr(Material.class, "FILLED_MAP", Material.MAP);
        registerEvents();
        Bukkit.getScheduler().runTaskTimer(plugin, this::everySecond, 20L, 20L);
    }

    private void everySecond() {
        long now = System.currentTimeMillis();
        List<PaymentInfo> list = Lists.newArrayList(players.values());
        for (PaymentInfo info : list) {
            Player player = info.player;
            if (now >= info.outdateTime) {
                PaymentAPI.inst().send(new PacketPluginCancelOrder(info.orderId));
                info.giveItemBack();
                t(player, "超时未支付，已自动取消");
                if (paymentActionBarTimeout != null) {
                    AdventureUtil.sendActionBar(player, PAPI.setPlaceholders(player, paymentActionBarTimeout));
                }
                remove(info.orderId);
            } else if (paymentActionBarProcess != null) {
                int timeout = (int) Math.floor((info.outdateTime - now) / 1000.0);
                AdventureUtil.sendActionBar(player, PAPI.setPlaceholders(player, paymentActionBarProcess.replace("%timeout%", String.valueOf(timeout))));
            }
        }
    }

    public void putProcess(Player player) {
        processPlayers.add(player.getUniqueId());
    }

    public boolean isProcess(Player player) {
        UUID uuid = player.getUniqueId();
        return processPlayers.contains(uuid) || players.containsKey(uuid);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        mapId = config.getInt("map-item.id", 20070831);
        if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1)) {
            mapId = Math.min(32767, mapId);
        }
        mapName = config.getString("map-item.name");
        mapLore = config.getStringList("map-item.lore");
        mapCustomModelData = Util.parseInt(config.getString("map-item.custom-model-data")).orElse(null);
        paymentActionBarProcess = config.getString("payment.action-bar.process", null);
        paymentActionBarDone = config.getString("payment.action-bar.done", null);
        paymentActionBarTimeout = config.getString("payment.action-bar.timeout", null);
        paymentActionBarCancel = config.getString("payment.action-bar.cancel", null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerInventory inv = e.getPlayer().getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isMap(item)) {
                inv.setItem(i, null);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        onLeave(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        onLeave(e.getPlayer());
    }

    private void onLeave(Player player) {
        remove(player);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        if (e.isCancelled()) return;
        Player player = e.getPlayer();

        PaymentInfo paymentInfo = players.get(player.getUniqueId());
        if (paymentInfo != null) {
            if (isMap(player.getInventory().getItem(e.getNewSlot()))) {
                paymentInfo.update();
            } else {
                e.setCancelled(true);
                if (!isMap(player.getInventory().getItem(e.getPreviousSlot()))) {
                    for (int i = 0; i < 9; i++) {
                        if (isMap(player.getInventory().getItem(i))) {
                            player.getInventory().setHeldItemSlot(i);
                            paymentInfo.update();
                            break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (e.isCancelled() || !isMap(e.getItemDrop().getItemStack())) return;
        Player player = e.getPlayer();

        if (remove(player) != null) {
            Item item = e.getItemDrop();
            item.getItemStack().setAmount(0);
            item.setItemStack(new ItemStack(Material.AIR));
            item.remove();
            if (paymentActionBarCancel != null) {
                AdventureUtil.sendActionBar(player, PAPI.setPlaceholders(player, paymentActionBarCancel));
            }
        }
    }

    public void requireScan(Player player, QRCode code, String orderId, long outdateTime, Consumer<Double> done) {
        ItemStack item = AdventureItemStack.buildItem(filledMap, mapName, mapLore);
        boolean component = MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4);
        NBT.modify(item, nbt -> {
            nbt.setBoolean(FLAG_SWEET_CHECKOUT_MAP, true);
            if (!component) { // 1.8-1.20.4
                if (mapCustomModelData != null) {
                    nbt.setInteger("CustomModelData", mapCustomModelData);
                }
                nbt.setInteger("map", mapId);
            }
        });
        if (component) { // 1.20.5+
            NBT.modifyComponents(item, nbt -> {
                if (mapCustomModelData != null) {
                    nbt.setInteger("minecraft:custom_model_data", mapCustomModelData);
                }
                nbt.setInteger("minecraft:map_id", mapId);
            });
        }
        UUID uuid = player.getUniqueId();
        ItemStack old = player.getInventory().getItemInMainHand();
        putProcess(player);
        players.put(uuid, new PaymentInfo(player, generateMapColors(code), old, item, orderId, outdateTime, done));
        player.getInventory().setItemInMainHand(item);
    }

    public PaymentInfo remove(String orderId) {
        PaymentInfo payment = null;
        for (PaymentInfo pi : players.values()) {
            if (orderId.equals(pi.orderId)) {
                payment = pi;
                break;
            }
        }
        if (payment == null) return null;
        UUID uuid = payment.player.getUniqueId();
        processPlayers.remove(uuid);
        players.remove(uuid);
        return payment;
    }

    public PaymentInfo remove(Player player) {
        UUID uuid = player.getUniqueId();
        processPlayers.remove(uuid);
        PaymentInfo info = players.remove(uuid);
        if (info != null) {
            PaymentAPI.inst().send(new PacketPluginCancelOrder(info.orderId));
            info.giveItemBack();
        }
        return info;
    }

    public void markDone(String orderId, String money) {
        PaymentInfo payment = remove(orderId);
        if (payment == null) return;
        Double moneyValue = Util.parseDouble(money).orElse(null);
        if (moneyValue == null) {
            t(payment.player, "内部错误"); // TODO: 移到语言文件
            return;
        }
        if (paymentActionBarDone != null) {
            AdventureUtil.sendActionBar(payment.player, PAPI.setPlaceholders(payment.player, paymentActionBarDone));
        }
        payment.done.accept(moneyValue);
    }

    public static boolean isMap(ItemStack item) {
        return item != null && item.getType().equals(filledMap) && NBT.get(item, nbt -> {
            return nbt.hasTag(FLAG_SWEET_CHECKOUT_MAP);
        });
    }

    public static byte[] generateMapColors(QRCode code) {
        byte dark = getMapColor(29, 3);
        byte light = getMapColor(8, 2);
        int width = code.getModuleCount();
        boolean scaling = width * 2 < 128;
        if (scaling) width *= 2;
        int start = (128 - width) / 2;
        byte[] colors = new byte[16384];
        Arrays.fill(colors, light);
        for (int z = 0; z < width; z++) {
            for (int x = 0; x < width; x++) {
                int index = (start + x) + 128 * (start + z);
                if (scaling ? code.isDark(z / 2,x / 2) : code.isDark(z, x)) {
                    colors[index] = dark;
                }
            }
        }
        return colors;
    }

    /**
     * 来自 Minecraft Wiki: <a href="https://zh.minecraft.wiki/w/%E5%9C%B0%E5%9B%BE%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F#%E5%9C%B0%E5%9B%BE%E6%95%B0%E6%8D%AE%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F">地图存储格式</a>
     */
    public static byte getMapColor(int baseColor, int modifier) {
        return (byte) (baseColor << 2 | modifier & 3);
    }

    public static PaymentsAndQRCodeManager inst() {
        return instanceOf(PaymentsAndQRCodeManager.class);
    }
}

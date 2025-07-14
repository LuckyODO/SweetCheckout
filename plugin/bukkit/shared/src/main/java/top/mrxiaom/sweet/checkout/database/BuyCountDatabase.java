package top.mrxiaom.sweet.checkout.database;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.checkout.PluginCommon;
import top.mrxiaom.sweet.checkout.func.AbstractPluginHolder;
import top.mrxiaom.sweet.checkout.func.entry.ShopItem;
import top.mrxiaom.sweet.checkout.func.temporary.TemporaryCount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class BuyCountDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private String TABLE_GLOBAL, TABLE_PLAYER;
    private final Map<String, TemporaryCount> globalCaches = new HashMap<>();
    private final Map<String, Map<String, TemporaryCount>> playerCaches = new HashMap<>();
    public BuyCountDatabase(PluginCommon plugin) {
        super(plugin);
    }

    @Override
    public void reload(Connection conn, String prefix) throws SQLException {
        TABLE_GLOBAL = prefix + "count_global";
        TABLE_PLAYER = prefix + "count_player";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_GLOBAL + "`(" +
                        "`shop` VARCHAR(64) PRIMARY KEY," +
                        "`count` VARCHAR(128)" +
                ");"
        )) { ps.execute(); }
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_PLAYER + "`(" +
                        "`uuid` VARCHAR(48)," +
                        "`name` VARCHAR(48)," +
                        "`shop` VARCHAR(64)," +
                        "`count` VARCHAR(128)," +
                        "PRIMARY KEY (`uuid`, `shop`)" +
                ");"
        )) { ps.execute(); }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String uuid = e.getPlayer().getUniqueId().toString();
        playerCaches.remove(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        String uuid = e.getPlayer().getUniqueId().toString();
        playerCaches.remove(uuid);
    }

    public void setPeriod(ShopItem shop) {
        switch (shop.limitationMode) {
            case GLOBAL: {
                TemporaryCount cache = globalCaches.get(shop.id);
                if (cache != null) {
                    cache.setPeriod(shop.limitationReset);
                    cache.setResetActions(shop::whenReset);
                }
                break;
            }
            case PER_PLAYER: {
                for (Map<String, TemporaryCount> map : playerCaches.values()) {
                    TemporaryCount cache = map.get(shop.id);
                    if (cache != null) {
                        cache.setPeriod(shop.limitationReset);
                        cache.setResetActions(shop::whenReset);
                    }
                }
                break;
            }
        }
    }

    public TemporaryCount getGlobalCount(ShopItem shop, boolean update) {
        TemporaryCount cache = globalCaches.get(shop.id);
        if (cache != null) {
            if (!update) {
                return cache;
            }
        } else {
            cache = new TemporaryCount(shop.limitationReset, shop::whenReset, () -> 0);
        }
        try (Connection conn = plugin.getConnection()) {
            globalCaches.put(shop.id, getGlobalCount(conn, shop, cache));
            return cache;
        } catch (SQLException e) {
            warn(e);
        }
        return cache;
    }

    public TemporaryCount getGlobalCount(Connection conn, ShopItem shop, TemporaryCount cache) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM `" + TABLE_GLOBAL + "` WHERE `shop`=?;"
        )) {
            ps.setString(1, shop.id);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    cache.deserialize(resultSet.getString("count"));
                } else {
                    cache.setValue(cache.getDefaultValue());
                }
            }
        }
        return cache;
    }

    public void addGlobalCount(ShopItem shop, int count) {
        TemporaryCount cache = Util.getOrPut(globalCaches, shop.id, () -> new TemporaryCount(shop.limitationReset, shop::whenReset, () -> 0));
        cache.setValue(cache.getValue() + count);
        try (Connection conn = plugin.getConnection()) {
            setGlobalCount(conn, shop, cache);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void setGlobalCount(Connection conn, ShopItem shop, TemporaryCount cache) throws SQLException {
        boolean mySQL = plugin.options.database().isMySQL();
        String data = cache.serialize();
        String statement = mySQL
                ? ("INSERT INTO `" + TABLE_GLOBAL + "`(`shop`,`count`) VALUES(?, ?) on duplicate key update `count`=?;")
                : ("INSERT OR REPLACE INTO `" + TABLE_GLOBAL + "`(`shop`,`count`) VALUES(?, ?);");
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
            ps.setString(1, shop.id);
            ps.setString(2, data);
            if (mySQL) {
                ps.setString(3, data);
            }
            ps.execute();
        }
    }

    public TemporaryCount getPlayerCount(OfflinePlayer player, ShopItem shop, boolean update) {
        String uuid = player.getUniqueId().toString();
        Map<String, TemporaryCount> map = Util.getOrPut(playerCaches, uuid, () -> new HashMap<>());
        TemporaryCount cache = map.get(shop.id);
        if (cache != null) {
            if (!update) {
                return cache;
            }
        } else {
            cache = new TemporaryCount(shop.limitationReset, shop::whenReset, () -> 0);
        }
        try (Connection conn = plugin.getConnection()) {
            map.put(shop.id, getPlayerCount(conn, player, shop, cache));
            return cache;
        } catch (SQLException e) {
            warn(e);
        }
        return cache;
    }

    public TemporaryCount getPlayerCount(Connection conn, OfflinePlayer player, ShopItem shop, TemporaryCount cache) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM `" + TABLE_PLAYER + "` WHERE `uuid`=? AND `shop`=?;"
        )) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, shop.id);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    cache.deserialize(resultSet.getString("count"));
                } else {
                    cache.setValue(cache.getDefaultValue());
                }
            }
        }
        return cache;
    }

    public void addPlayerCount(OfflinePlayer player, ShopItem shop, int count) {
        String uuid = player.getUniqueId().toString();
        Map<String, TemporaryCount> map = Util.getOrPut(playerCaches, uuid, () -> new HashMap<>());
        TemporaryCount cache = Util.getOrPut(map, shop.id, () -> new TemporaryCount(shop.limitationReset, shop::whenReset, () -> 0));
        cache.setValue(cache.getValue() + count);
        try (Connection conn = plugin.getConnection()) {
            setPlayerCount(conn, player, shop, cache);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void setPlayerCount(Connection conn, OfflinePlayer player, ShopItem shop, TemporaryCount cache) throws SQLException {
        boolean mySQL = plugin.options.database().isMySQL();
        String uuid = player.getUniqueId().toString();
        String name = player.getName() == null ? "[UNKNOWN]" : player.getName();
        String data = cache.serialize();
        String statement = mySQL
                ? ("INSERT INTO `" + TABLE_PLAYER + "`(`uuid`,`name`,`shop`,`count`) VALUES(?, ?) on duplicate key update `name`=?, `count`=?;")
                : ("INSERT OR REPLACE INTO `" + TABLE_PLAYER + "`(`uuid`,`name`,`shop`,`count`) VALUES(?, ?);");
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setString(3, shop.id);
            ps.setString(4, data);
            if (mySQL) {
                ps.setString(5, name);
                ps.setString(6, data);
            }
            ps.execute();
        }
    }
}

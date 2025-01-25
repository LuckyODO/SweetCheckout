package top.mrxiaom.sweet.checkout.database;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.func.AbstractPluginHolder;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class TradeDatabase extends AbstractPluginHolder implements IDatabase {
    public static class Log implements Comparable<Log> {
        public final UUID uuid;
        public final String name;
        public final LocalDateTime time;
        public final String type;
        public final String money;
        public final String reason;

        Log(UUID uuid, String name, LocalDateTime time, String type, String money, String reason) {
            this.uuid = uuid;
            this.name = name;
            this.time = time;
            this.type = type;
            this.money = money;
            this.reason = reason;
        }

        @Nullable
        public OfflinePlayer getPlayer() {
            return Util.getOfflinePlayer(uuid).orElse(null);
        }

        @Override
        public int compareTo(@NotNull TradeDatabase.Log o) {
            // 按时间逆序排序
            return o.time.compareTo(time);
        }
    }
    private String TABLE_TRADE_LOG;
    public TradeDatabase(SweetCheckout plugin) {
        super(plugin);
    }

    @Override
    public void reload(Connection conn, String prefix) throws SQLException {
        TABLE_TRADE_LOG = prefix + "trade_log";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_TRADE_LOG + "`(" +
                        "`uuid` VARCHAR(48)," +
                        "`name` VARCHAR(48)," +
                        "`time` TIMESTAMP," +
                        "`type` VARCHAR(24)," +
                        "`money` VARCHAR(32)," +
                        "`reason` LONGTEXT" +
                ");"
        )) {
            ps.execute();
        }
    }

    public void log(Player player, LocalDateTime time, String type, String money, String reason) {
        try (Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `" + TABLE_TRADE_LOG + "`(`uuid`,`name`,`time`,`type`,`money`,`reason`) VALUES(?,?,?,?,?,?);"
            )) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName());
            ps.setTimestamp(3, Timestamp.valueOf(time));
            ps.setString(4, type);
            ps.setString(5, money);
            ps.setString(6, reason);
            ps.execute();
        } catch (SQLException e) {
            warn(e);
        }
    }

    public List<Log> get(OfflinePlayer player) {
        return get(player.getUniqueId());
    }

    public List<Log> get(UUID uuid) {
        try (Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `" + TABLE_TRADE_LOG + "` WHERE `uuid`=?;"
            )) {
            ps.setString(1, uuid.toString());
            try (ResultSet result = ps.executeQuery()) {
                List<Log> list = new ArrayList<>();
                while (result.next()) {
                    String name = result.getString("name");
                    Timestamp time = result.getTimestamp("time");
                    String type = result.getString("type");
                    String money = result.getString("money");
                    String reason = result.getString("reason");
                    list.add(new Log(uuid, name, time.toLocalDateTime(), type, money, reason));
                }
                Collections.sort(list);
                return list;
            }
        } catch (SQLException e) {
            warn(e);
            return null;
        }
    }
}

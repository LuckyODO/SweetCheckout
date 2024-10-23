package top.mrxiaom.sweet.checkout.backend.data;

import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClientInfo {
    public class Order {
        String id;
        String type;
        String playerName;
        String money;
        TimerTask task = null;
        Runnable cancelAction = null;

        private Order(String id, String type, String playerName, String money) {
            this.id = id;
            this.type = type;
            this.playerName = playerName;
            this.money = money;
        }

        public TimerTask getTask() {
            return task;
        }

        public void setTask(TimerTask task) {
            this.task = task;
        }

        public Runnable getCancelAction() {
            return cancelAction;
        }

        public void setCancelAction(Runnable cancelAction) {
            this.cancelAction = cancelAction;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getMoney() {
            return money;
        }

        public ClientInfo getClient() {
            return ClientInfo.this;
        }

        public void remove() {
            getClient().removeOrder(this);
        }
    }
    private final Map<String, Order> orders = new HashMap<>();
    private static final Set<String> locked = new HashSet<>();
    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private WebSocket webSocket;

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public Order createOrder(String id, String type, String playerName, String money) {
        Order order = new Order(id, type, playerName, money);
        addOrder(order);
        return order;
    }

    public String nextOrderId() {
        String id;
        LocalDateTime time = LocalDateTime.now();
        String base = time.format(format);
        int i = 1;
        do {
            id = String.format("%s%03d", base, i++);
        } while ((orders.containsKey(id) || locked.contains(id)) && i <= 999);
        if (i >= 1000) return null;
        locked.add(id);
        return id;
    }

    public boolean addOrder(Order order) {
        if (orders.containsKey(order.id)) return false;
        orders.put(order.id, order);
        return true;
    }

    @Nullable
    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }

    @Nullable
    public Order getOrderByPlayer(String playerName) {
        for (Order order : orders.values()) {
            if (order.getPlayerName().equals(playerName)) {
                return order;
            }
        }
        return null;
    }

    @Nullable
    public Order removeOrder(Order order) {
        TimerTask task = order.getTask();
        if (task != null) {
            order.setTask(null);
            task.cancel();
        }
        return removeOrder(order.id);
    }

    @Nullable
    public Order removeOrder(String orderId) {
        Order order = orders.remove(orderId);
        if (order != null) {
            TimerTask task = order.getTask();
            if (task != null) {
                task.cancel();
            }
            locked.remove(orderId);
        }
        return order;
    }
}

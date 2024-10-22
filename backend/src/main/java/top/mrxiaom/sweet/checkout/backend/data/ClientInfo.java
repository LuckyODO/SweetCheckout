package top.mrxiaom.sweet.checkout.backend.data;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClientInfo {
    public static class Order {
        String id;
        String type;
        String playerName;
        String money;
    }
    private final Map<String, Order> orders = new HashMap<>();
    private final Set<String> locked = new HashSet<>();

    public String nextOrderId() {
        String id;
        do {
            id = UUID.randomUUID().toString().replace("-", "");
        } while (orders.containsKey(id) || locked.contains(id));
        locked.add(id);
        return id;
    }

    public boolean addOrder(Order order) {
        if (orders.containsKey(order.id)) return false;
        orders.put(order.id, order);
        locked.remove(order.id);
        return true;
    }

    @Nullable
    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }

    @Nullable
    public Order removeOrder(Order order) {
        locked.remove(order.id);
        return orders.remove(order.id);
    }
}

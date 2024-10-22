package top.mrxiaom.sweet.checkout.backend.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientInfo {
    public static class Order {
        String id;
        String type;
        String playerName;
        String money;
    }
    private final Map<String, Order> orders = new HashMap<>();

    public String nextOrderId() {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (orders.containsKey(id));
        return id;
    }
}

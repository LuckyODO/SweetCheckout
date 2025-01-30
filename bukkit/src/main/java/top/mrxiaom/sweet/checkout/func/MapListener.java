package top.mrxiaom.sweet.checkout.func;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.checkout.SweetCheckout;

import static top.mrxiaom.sweet.checkout.func.PaymentsAndQRCodeManager.isMap;

@AutoRegister
public class MapListener extends AbstractModule implements Listener {
    public MapListener(SweetCheckout plugin) {
        super(plugin);
        registerEvents();
    }

    @EventHandler
    @SuppressWarnings({"deprecation"})
    public void onClick(InventoryClickEvent e) {
        HumanEntity player = e.getWhoClicked();
        if (player instanceof Player) {
            PaymentsAndQRCodeManager manager = PaymentsAndQRCodeManager.inst();
            if (manager.isProcess((Player) player)) {
                return;
            }
        }
        if (isMap(e.getCurrentItem())) {
            e.setCurrentItem(null);
            e.setCancelled(true);
            return;
        }
        if (isMap(e.getCursor())) {
            e.setCursor(null);
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (isMap(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (isMap(e.getItem().getItemStack())) {
            e.setCancelled(true);
            e.getItem().remove();
        }
    }
}

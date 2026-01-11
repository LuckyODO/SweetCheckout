package top.mrxiaom.sweet.checkout.func;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.checkout.Messages;
import top.mrxiaom.sweet.checkout.PluginCommon;
import top.mrxiaom.sweet.playermarket.api.event.MarketItemBeforeCreateEvent;

@AutoRegister(requirePlugins = "SweetPlayerMarket")
public class SweetPlayerMarketCompatibility extends AbstractModule implements Listener {
    public SweetPlayerMarketCompatibility(PluginCommon plugin) {
        super(plugin);
        registerEvents();
    }

    @EventHandler
    public void beforeCreate(MarketItemBeforeCreateEvent e) {
        if (PaymentsAndQRCodeManager.isMap(e.getMarketItem().item())) {
            Messages.market__not_allow.tm(e.getPlayer());
            e.setCancelled(true);
        }
    }
}

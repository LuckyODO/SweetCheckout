package top.mrxiaom.sweet.checkout.func;
		
import top.mrxiaom.sweet.checkout.SweetCheckout;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetCheckout> {
    public AbstractPluginHolder(SweetCheckout plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetCheckout plugin, boolean register) {
        super(plugin, register);
    }
}

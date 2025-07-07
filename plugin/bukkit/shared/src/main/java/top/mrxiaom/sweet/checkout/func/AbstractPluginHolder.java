package top.mrxiaom.sweet.checkout.func;

import top.mrxiaom.sweet.checkout.PluginCommon;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<PluginCommon> {
    public AbstractPluginHolder(PluginCommon plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(PluginCommon plugin, boolean register) {
        super(plugin, register);
    }
}

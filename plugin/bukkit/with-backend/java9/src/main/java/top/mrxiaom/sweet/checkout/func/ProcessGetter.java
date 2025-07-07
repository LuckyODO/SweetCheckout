package top.mrxiaom.sweet.checkout.func;

import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.checkout.PluginCommon;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.backend.BukkitMain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AutoRegister
public class ProcessGetter extends AbstractModule {
    public ProcessGetter(PluginCommon plugin) {
        super(plugin);
        BukkitMain backend = ((SweetCheckout) plugin).getBackend();
        backend.getServer().setJava9ProcessGetter(() -> {
            List<String> list = new ArrayList<>();
            ProcessHandle.allProcesses().forEach(it -> {
                Optional<String> command = it.info().command();
                command.ifPresent(list::add);
            });
            return list;
        });
    }
}

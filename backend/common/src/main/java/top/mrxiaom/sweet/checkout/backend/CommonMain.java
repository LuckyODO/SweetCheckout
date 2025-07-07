package top.mrxiaom.sweet.checkout.backend;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mrxiaom.sweet.checkout.backend.api.ISender;
import top.mrxiaom.sweet.checkout.backend.data.ClientInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings({"FieldMayBeFinal"})
public abstract class CommonMain<C extends ClientInfo<C>, S extends AbstractPaymentServer<C>> {
    protected Configuration config;
    protected File dataFolder;
    protected boolean running = true;
    protected final Logger logger;
    protected static final Gson gson = new GsonBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    return false;
                }
                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    String name = aClass.getName();
                    return name.startsWith("com.alipay") || name.startsWith("com.wechat.pay");
                }
            })
            .setPrettyPrinting()
            .create();
    public CommonMain(Logger logger, File dataFolder) {
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    public Configuration getConfig() {
        return config;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public Logger getLogger() {
        return logger;
    }

    public abstract S getServer();

    public void reloadConfig() {
        try {
            File file = new File(getDataFolder(), "config.json");
            if (file.exists()) {
                String configRaw = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                config = gson.fromJson(configRaw, Configuration.class);
            } else {
                config = new Configuration();
            }
            config.postLoad(getDataFolder());
            String configRaw = gson.toJson(config);
            FileUtils.writeStringToFile(file, configRaw, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("加载配置文件时出现异常", e);
        }
    }

    protected void runCommand(ISender sender, String s) {
        if ("reload".equals(s)) {
            reloadConfig();
            sender.send("配置文件已重载.");
            return;
        }
        if ("process".equals(s)) {
            for (String name : getServer().getAllProcess()) {
                sender.send(name);
            }
            return;
        }
        if ("stop".equals(s)) {
            running = false;
            sender.send("再见.");
            System.exit(0);
            return;
        }
        sender.send("未知命令.");
    }
}

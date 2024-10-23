package top.mrxiaom.sweet.checkout.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings({"FieldMayBeFinal"})
public class ConsoleMain extends SimpleTerminalConsole {
    private static PaymentServer server;
    private static Configuration config;
    private boolean running = true;
    private static Logger logger = LoggerFactory.getLogger("Server");
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    public static void main(String[] args) {
        logger.info("正在运行 SweetCheckout 后端.");
        reloadConfig();
        server = new PaymentServer(logger, config.getPort());
        server.start();

        new ConsoleMain().start();
    }

    public static PaymentServer getServer() {
        return server;
    }

    public static Configuration getConfig() {
        return config;
    }

    public static Logger getLogger() {
        return logger;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public static void reloadConfig() {
        try {
            File file = new File("config.json");
            if (file.exists()) {
                String configRaw = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                config = gson.fromJson(configRaw, Configuration.class);
            } else {
                config = new Configuration();
            }
            config.postLoad();
            String configRaw = gson.toJson(config);
            FileUtils.writeStringToFile(file, configRaw, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("加载配置文件时出现异常", e);
        }
    }

    @Override
    protected void runCommand(String s) {
        if ("reload".equals(s)) {
            logger.info("配置文件已重载.");
            return;
        }
        if ("stop".equals(s)) {
            running = false;
            logger.info("再见.");
            System.exit(0);
            return;
        }
        logger.info("未知命令.");
    }

    @Override
    protected void shutdown() {
        try {
            server.stop();
        } catch (InterruptedException e) {
            logger.warn("停止服务端时出现异常", e);
        }
    }
}

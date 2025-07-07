package top.mrxiaom.sweet.checkout.backend;

import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mrxiaom.sweet.checkout.backend.data.WebSocketClientInfo;

import java.io.File;

public class ConsoleMain extends CommonMain<WebSocketClientInfo, PaymentServer> {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger("Server");
        logger.info("正在运行 SweetCheckout 后端.");
        new ConsoleMain(logger).start();
    }

    private final PaymentServer server;
    private final Console console;

    private ConsoleMain(Logger logger) {
        super(logger, new File("."));
        reloadConfig();
        this.server = new PaymentServer(this, logger, config.getPort());
        this.console = new Console();
    }

    private void start() {
        this.server.getWebSocketServer().start();
        this.console.start();
    }

    @Override
    public PaymentServer getServer() {
        return server;
    }

    public class Console extends SimpleTerminalConsole {
        private Console() {
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        protected void runCommand(String s) {
            if ("reload".equals(s)) {
                reloadConfig();
                logger.info("配置文件已重载.");
                return;
            }
            if ("process".equals(s)) {
                for (String name : getServer().getAllProcess()) {
                    logger.info(name);
                }
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
                server.getWebSocketServer().stop();
            } catch (InterruptedException e) {
                logger.warn("停止服务端时出现异常", e);
            }
        }
    }
}

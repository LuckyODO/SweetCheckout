package top.mrxiaom.sweet.checkout.backend;

import com.alipay.api.AlipayConfig;
import com.google.gson.annotations.Expose;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class Configuration {
    @Expose(serialize = false, deserialize = false)
    private Logger logger = LoggerFactory.getLogger(Configuration.class);
    private int port = 62233;
    private WeChatNative weChatNative = new WeChatNative();
    private AlipayFaceToFace alipayFaceToFace = new AlipayFaceToFace();
    private Hook hook = new Hook();

    private static String parseString(Logger logger, String name, String str) {
        if (!str.startsWith("file:")) return str;
        String path = str.substring(5);
        File file = new File(path);
        if (!file.exists()) {
            logger.warn("无法从配置 {} 找到对应文件 {}", name, path);
            return null;
        }
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            String log = "读取文件 " + path + " 时出现一个异常";
            logger.warn(log, e);
            return null;
        }
    }

    protected void postLoad() {
        getAlipayFaceToFace().postLoad(this);
    }

    public int getPort() {
        return port;
    }

    public WeChatNative getWeChatNative() {
        return weChatNative;
    }

    public AlipayFaceToFace getAlipayFaceToFace() {
        return alipayFaceToFace;
    }

    public Hook getHook() {
        return hook;
    }

    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    public static class WeChatNative {
        private boolean enable = false;

        public boolean isEnable() {
            return enable;
        }
    }
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    public static class AlipayFaceToFace {
        private boolean enable = false;
        private String appId;
        private String privateKey = "file:secrets/alipay/private.txt";
        private String alipayPublicKey = "file:secrets/alipay/public.txt";
        @Expose(serialize = false, deserialize = false)
        private AlipayConfig config;
        private void postLoad(Configuration config) {
            if (isEnable()) {
                String privateKeyStr = getPrivateKey();
                String publicKeyStr = getAlipayPublicKey();
                String privateKey = parseString(config.logger, "alipayFaceToFace.privateKey", privateKeyStr);
                if (privateKey == null) return;
                String publicKey = parseString(config.logger, "alipayFaceToFace.alipayPublicKey", publicKeyStr);
                if (publicKey == null) return;
                AlipayConfig alipayConfig = new AlipayConfig();
                alipayConfig.setServerUrl("https://openapi.alipay.com/gateway.do");
                alipayConfig.setAppId(getAppId());
                alipayConfig.setPrivateKey(privateKey);
                alipayConfig.setFormat("json");
                alipayConfig.setAlipayPublicKey(publicKey);
                alipayConfig.setCharset("UTF-8");
                alipayConfig.setSignType("RSA2");
                this.config = alipayConfig;
            }
        }
        public boolean isEnable() {
            return enable;
        }

        public String getAppId() {
            return appId;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public String getAlipayPublicKey() {
            return alipayPublicKey;
        }

        private AlipayConfig getConfig() {
            return config;
        }
    }
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    public static class Hook {
        private boolean enable = true;
        private String endPoint = "/api/hook/receive";
        private WeChatHook weChat = new WeChatHook();
        public boolean isEnable() {
            return enable;
        }

        public String getEndPoint() {
            return endPoint;
        }

        public WeChatHook getWeChat() {
            return weChat;
        }
    }
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    public static class WeChatHook {
        private boolean enable = false;
        private String requireProcess = "SweetCheckout.Hook.WeChat.exe";
        private String paymentUrl = "";
        private Map<String, String> paymentUrls = new HashMap<String, String>() {{
            put("1.00", "");
        }};

        public boolean isEnable() {
            return enable;
        }

        public String getRequireProcess() {
            return requireProcess;
        }

        public String getPaymentUrl() {
            return paymentUrl;
        }

        public Map<String, String> getPaymentUrls() {
            return paymentUrls;
        }
    }
}

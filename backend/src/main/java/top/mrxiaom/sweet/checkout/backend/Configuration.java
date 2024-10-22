package top.mrxiaom.sweet.checkout.backend;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class Configuration {
    private int port = 62233;
    private WeChatNative weChatNative = new WeChatNative();
    private AlipayFaceToFace alipayFaceToFace = new AlipayFaceToFace();
    private Hook hook = new Hook();

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

        public boolean isEnable() {
            return enable;
        }
    }
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    public static class Hook {
        private boolean enable = true;
        private String endPoint = "/api/hook/receive";

        public boolean isEnable() {
            return enable;
        }

        public String getEndPoint() {
            return endPoint;
        }
    }

}

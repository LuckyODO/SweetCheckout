package top.mrxiaom.sweet.checkout.backend;

import lombok.Getter;

@Getter
public class Configuration {
    private int port = 62233;
    private WeChatNative weChatNative = new WeChatNative();
    private AlipayFaceToFace alipayFaceToFace = new AlipayFaceToFace();
    private Hook hook = new Hook();

    @Getter
    public static class WeChatNative {
        private boolean enable = false;
    }
    @Getter
    public static class AlipayFaceToFace {
        private boolean enable = false;
    }
    @Getter
    public static class Hook {
        private boolean enable = true;
        private String endPoint = "/api/hook/receive";
    }
}

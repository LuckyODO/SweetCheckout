package top.mrxiaom.sweet.checkout.map;

import top.mrxiaom.qrcode.QRCode;
import top.mrxiaom.qrcode.enums.ErrorCorrectionLevel;
import top.mrxiaom.sweet.checkout.SweetCheckout;
import top.mrxiaom.sweet.checkout.func.PaymentsAndQRCodeManager;

public interface IMapSource {
    byte[] generate(PaymentsAndQRCodeManager manager);

    static IMapSource fromUrl(SweetCheckout plugin, String url) {
        if (url.startsWith("file:")) {
            return new MapFile(plugin.resolve(url.substring(5)));
        } else {
            QRCode code = QRCode.create(url, ErrorCorrectionLevel.H);
            return new MapQRCode(code);
        }
    }
}

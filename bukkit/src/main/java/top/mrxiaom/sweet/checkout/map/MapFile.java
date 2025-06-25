package top.mrxiaom.sweet.checkout.map;

import top.mrxiaom.sweet.checkout.func.PaymentsAndQRCodeManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class MapFile implements IMapSource {
    private final File file;
    public MapFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public byte[] generate(PaymentsAndQRCodeManager manager) {
        try {
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.drawImage(ImageIO.read(file), 0, 0, 128, 128, null);
            graphics.dispose();
            return MapConverter.imageToBytes(image);
        } catch (Throwable t) {
            manager.warn("读取 " + file.getAbsolutePath() + " 时出现异常", t);
            byte[] colors = new byte[16384];
            Arrays.fill(colors, manager.getMapLightColor());
            return colors;
        }
    }
}

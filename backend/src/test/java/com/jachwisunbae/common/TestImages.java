package com.jachwisunbae.common;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class TestImages {

    private TestImages() {
    }

    public static byte[] png() {
        return image("png");
    }

    public static byte[] jpeg() {
        return image("jpg");
    }

    private static byte[] image(final String format) {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        image.setRGB(1, 0, Color.GREEN.getRGB());
        image.setRGB(0, 1, Color.BLUE.getRGB());
        image.setRGB(1, 1, Color.WHITE.getRGB());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, output)) {
                throw new IllegalStateException("테스트 이미지를 만들 수 없습니다.");
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("테스트 이미지를 만들 수 없습니다.", exception);
        }
    }
}

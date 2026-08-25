package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PropertyComparisonPhotoOptimizerTest {

    @Test
    void PDF용_사진은_한계_해상도의_JPEG로_축소한다() throws Exception {
        BufferedImage source = new BufferedImage(2_400, 1_600, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(new Color(85, 112, 48));
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(source, "png", png);

        byte[] optimized = new PropertyComparisonPhotoOptimizer().optimize(png.toByteArray());
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(optimized));

        assertThat(optimized).startsWith((byte) 0xFF, (byte) 0xD8);
        assertThat(Math.max(result.getWidth(), result.getHeight())).isLessThanOrEqualTo(1_200);
    }
}

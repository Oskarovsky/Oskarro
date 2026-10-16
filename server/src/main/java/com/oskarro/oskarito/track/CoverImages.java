package com.oskarro.oskarito.track;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class CoverImages {

    static final int MAX_BYTES_IN = 2 * 1024 * 1024;
    static final int MAX_EDGE = 480;
    static final int TARGET_BYTES = 80 * 1024;
    private static final long MAX_PIXELS = 8_000_000L;

    private CoverImages() {
    }

    static byte[] miniaturize(byte[] raw) {
        if (raw == null || raw.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wybierz obrazek");
        }
        if (raw.length > MAX_BYTES_IN) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Obrazek może mieć najwyżej 2 MB");
        }
        BufferedImage image = read(raw);
        if ((long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Obrazek ma za dużo pikseli");
        }
        int edge = MAX_EDGE;
        float quality = 0.75f;
        byte[] jpeg = encode(scale(image, edge), quality);
        while (jpeg.length > TARGET_BYTES && (quality > 0.45f || edge > 160)) {
            if (quality > 0.45f) {
                quality -= 0.1f;
            } else {
                edge = Math.max(160, edge - 80);
                quality = 0.7f;
            }
            jpeg = encode(scale(image, edge), quality);
        }
        return jpeg;
    }

    private static BufferedImage read(byte[] raw) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(raw));
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To nie jest obsługiwany obrazek");
            }
            return image;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nie udało się odczytać obrazka");
        }
    }

    private static BufferedImage scale(BufferedImage image, int edge) {
        double ratio = Math.min(1d, edge / (double) Math.max(image.getWidth(), image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(image.getHeight() * ratio));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setColor(new Color(0x14130f));
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private static byte[] encode(BufferedImage image, float quality) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writer.setOutput(new MemoryCacheImageOutputStream(out));
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nie udało się zmniejszyć obrazka");
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}

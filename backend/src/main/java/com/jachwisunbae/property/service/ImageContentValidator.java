package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.property.service.dto.command.UploadPhotoCommand;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class ImageContentValidator {

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    public ValidatedImage validate(final UploadPhotoCommand command) {
        final byte[] content = command.content();
        if (content == null || content.length == 0) {
            throw unsupportedFormat();
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new InvalidCommandException(ErrorCode.PHOTO_SIZE_EXCEEDED);
        }

        final String detectedContentType = detectContentType(content);
        if (!detectedContentType.equals(command.contentType())) {
            throw unsupportedFormat();
        }
        verifyDecodable(content);
        return new ValidatedImage(content, detectedContentType, sha256(content));
    }

    private String detectContentType(final byte[] content) {
        if (isJpeg(content)) {
            return "image/jpeg";
        }
        if (isPng(content)) {
            return "image/png";
        }
        if (isWebp(content)) {
            return "image/webp";
        }
        throw unsupportedFormat();
    }

    private boolean isJpeg(final byte[] content) {
        return content.length >= 3
                && unsigned(content[0]) == 0xFF
                && unsigned(content[1]) == 0xD8
                && unsigned(content[2]) == 0xFF;
    }

    private boolean isPng(final byte[] content) {
        final int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (unsigned(content[index]) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(final byte[] content) {
        return content.length >= 12
                && content[0] == 'R'
                && content[1] == 'I'
                && content[2] == 'F'
                && content[3] == 'F'
                && content[8] == 'W'
                && content[9] == 'E'
                && content[10] == 'B'
                && content[11] == 'P';
    }

    private int unsigned(final byte value) {
        return Byte.toUnsignedInt(value);
    }

    private void verifyDecodable(final byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw unsupportedFormat();
            }
            final var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw unsupportedFormat();
            }
            final ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                final BufferedImage image = reader.read(0);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw unsupportedFormat();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof InvalidCommandException invalidCommandException) {
                throw invalidCommandException;
            }
            throw new InvalidCommandException(ErrorCode.PHOTO_FORMAT_UNSUPPORTED, exception);
        }
    }

    private String sha256(final byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private InvalidCommandException unsupportedFormat() {
        return new InvalidCommandException(ErrorCode.PHOTO_FORMAT_UNSUPPORTED);
    }

    public record ValidatedImage(byte[] content, String contentType, String checksumSha256) {

        public ValidatedImage {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }

        public long sizeBytes() {
            return content.length;
        }
    }
}

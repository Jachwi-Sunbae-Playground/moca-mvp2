package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.TestImages;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.property.service.dto.command.UploadPhotoCommand;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageContentValidatorTest {

    private final ImageContentValidator validator = new ImageContentValidator();

    @DisplayName("JPEG와 PNG와 WebP의 MIME·시그니처·디코딩을 검증한다")
    @Test
    void validateSupportedImages() {
        final byte[] webp = Base64.getDecoder().decode(
                "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA"
        );

        assertThat(validator.validate(new UploadPhotoCommand("image/jpeg", TestImages.jpeg())).contentType())
                .isEqualTo("image/jpeg");
        assertThat(validator.validate(new UploadPhotoCommand("image/png", TestImages.png())).contentType())
                .isEqualTo("image/png");
        assertThat(validator.validate(new UploadPhotoCommand("image/webp", webp)).contentType())
                .isEqualTo("image/webp");
    }

    @DisplayName("빈 파일과 선언 MIME 불일치와 손상된 이미지를 거부한다")
    @Test
    void rejectInvalidImages() {
        assertError(new UploadPhotoCommand("image/png", new byte[0]), ErrorCode.PHOTO_FORMAT_UNSUPPORTED);
        assertError(new UploadPhotoCommand("image/jpeg", TestImages.png()), ErrorCode.PHOTO_FORMAT_UNSUPPORTED);
        assertError(
                new UploadPhotoCommand("image/png", new byte[] {
                        (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
                }),
                ErrorCode.PHOTO_FORMAT_UNSUPPORTED
        );
    }

    @DisplayName("10 MiB를 넘는 파일을 크기 오류로 거부한다")
    @Test
    void rejectOversizedImage() {
        assertError(
                new UploadPhotoCommand("image/png", new byte[(int) ImageContentValidator.MAX_SIZE_BYTES + 1]),
                ErrorCode.PHOTO_SIZE_EXCEEDED
        );
    }

    private void assertError(final UploadPhotoCommand command, final ErrorCode errorCode) {
        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(InvalidCommandException.class)
                .extracting(exception -> ((InvalidCommandException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}

package com.jachwisunbae.property.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "단건 사진 업로드 multipart 요청")
public record UploadPhotoRequest(
        @Schema(
                type = "string",
                format = "binary",
                description = "JPEG·PNG·WebP 사진 파일",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        byte[] file
) {
}

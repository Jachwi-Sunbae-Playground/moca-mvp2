package com.jachwisunbae.property.service.dto.result;

import java.io.InputStream;

public record PhotoContentResult(String contentType, long sizeBytes, InputStream content) {
}

package com.jachwisunbae.property.service.dto.command;

public final class UploadPhotoCommand {

    private final String contentType;
    private final byte[] content;

    public UploadPhotoCommand(final String contentType, final byte[] content) {
        this.contentType = contentType;
        this.content = content == null ? null : content.clone();
    }

    public String contentType() {
        return contentType;
    }

    public byte[] content() {
        return content == null ? null : content.clone();
    }
}

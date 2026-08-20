package com.jachwisunbae.common.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public abstract class BaseTimeEntity {

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected BaseTimeEntity(final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    protected final void updateUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

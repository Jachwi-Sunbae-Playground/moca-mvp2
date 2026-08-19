package com.jachwisunbae.member.entity;

import lombok.Getter;
import com.jachwisunbae.common.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
public class Member extends BaseTimeEntity {

    private final Long id;
    private final String email;
    private final String name;
    private LocalDateTime lastLoginAt;

    private Member(final Long id, final String email, final String name,
                   final LocalDateTime lastLoginAt, final LocalDateTime createdAt,
                   final LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
        this.id = id;
        this.email = email;
        this.name = name;
        this.lastLoginAt = lastLoginAt;
    }

    public static Member create(final String email, final String name, final LocalDateTime now) {
        return new Member(null, email, name, now, now, now);
    }

    public static Member reconstruct(final Long id, final String email, final String name,
                                     final LocalDateTime lastLoginAt, final LocalDateTime createdAt,
                                     final LocalDateTime updatedAt) {
        return new Member(id, email, name, lastLoginAt, createdAt, updatedAt);
    }

    public void recordLogin(final LocalDateTime loginAt) {
        this.lastLoginAt = loginAt;
        updateUpdatedAt(loginAt);
    }
}

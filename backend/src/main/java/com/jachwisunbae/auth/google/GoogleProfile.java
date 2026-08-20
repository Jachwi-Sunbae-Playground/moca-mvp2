package com.jachwisunbae.auth.google;

public record GoogleProfile(
        String subject,
        String email,
        String name) {
}

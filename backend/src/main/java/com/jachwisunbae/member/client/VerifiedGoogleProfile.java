package com.jachwisunbae.member.client;

public record VerifiedGoogleProfile(String subject, String email, String displayName) {

    private static final int MAXIMUM_SUBJECT_LENGTH = 255;
    private static final int MAXIMUM_EMAIL_LENGTH = 320;
    private static final int MAXIMUM_DISPLAY_NAME_LENGTH = 100;

    public VerifiedGoogleProfile {
        if (subject == null || subject.isBlank() || subject.length() > MAXIMUM_SUBJECT_LENGTH) {
            throw new IllegalArgumentException("Google subject는 비어 있을 수 없습니다.");
        }
        if (email == null || email.isBlank() || email.length() > MAXIMUM_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Google email은 비어 있을 수 없습니다.");
        }
        if (displayName == null
                || displayName.isBlank()
                || displayName.codePointCount(0, displayName.length()) > MAXIMUM_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("표시 이름은 비어 있을 수 없습니다.");
        }
    }
}

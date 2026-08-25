package com.jachwisunbae.auth.nickname;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import java.text.Normalizer;
import java.util.Locale;

public record NicknameIdentity(String displayName, String key) {

    private static final int MAX_LENGTH = 30;

    public static NicknameIdentity from(String rawNickname) {
        if (rawNickname == null) {
            throw invalidNickname();
        }
        String displayName = Normalizer.normalize(rawNickname, Normalizer.Form.NFKC).trim();
        if (displayName.isEmpty()
                || displayName.codePointCount(0, displayName.length()) > MAX_LENGTH
                || displayName.codePoints().anyMatch(Character::isISOControl)) {
            throw invalidNickname();
        }
        return new NicknameIdentity(displayName, displayName.toLowerCase(Locale.ROOT));
    }

    private static BusinessException invalidNickname() {
        return new BusinessException(DomainErrorCode.NICKNAME_INVALID,
                "닉네임은 trim 후 1자 이상 30자 이하이고 제어 문자를 포함하지 않아야 합니다.");
    }
}

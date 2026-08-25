package com.jachwisunbae.auth.nickname;

import java.util.Optional;

public interface NicknameCredentialRepository {

    Optional<NicknameCredential> findByNicknameKey(String nicknameKey);

    Optional<NicknameCredential> findByMemberId(Long memberId);

    void save(NicknameCredential credential);
}

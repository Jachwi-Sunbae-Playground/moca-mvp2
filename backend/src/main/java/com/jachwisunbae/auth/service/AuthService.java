package com.jachwisunbae.auth.service;

import com.jachwisunbae.auth.controller.dto.LoginMemberResponse;
import com.jachwisunbae.auth.controller.dto.LoginResponse;
import com.jachwisunbae.auth.controller.dto.NicknameLoginRequest;
import com.jachwisunbae.auth.nickname.NicknameCredential;
import com.jachwisunbae.auth.nickname.NicknameCredentialRepository;
import com.jachwisunbae.auth.nickname.NicknameIdentity;
import com.jachwisunbae.auth.nickname.NicknameLoginAttemptLimiter;
import com.jachwisunbae.auth.token.JwtTokenProvider;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.member.entity.Member;
import com.jachwisunbae.member.repository.MemberRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 64;
    private static final int BCRYPT_MAX_BYTES = 72;

    private final MemberRepository memberRepository;
    private final NicknameCredentialRepository credentialRepository;
    private final NicknameLoginAttemptLimiter attemptLimiter;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final Clock clock;
    private final long accessTokenSeconds;

    public AuthService(
            MemberRepository memberRepository,
            NicknameCredentialRepository credentialRepository,
            NicknameLoginAttemptLimiter attemptLimiter,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtProvider,
            Clock clock,
            @Value("${auth.jwt.access-token-seconds}") long accessTokenSeconds) {
        this.memberRepository = memberRepository;
        this.credentialRepository = credentialRepository;
        this.attemptLimiter = attemptLimiter;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.clock = clock;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    @Transactional
    public synchronized LoginResponse loginNickname(NicknameLoginRequest request) {
        NicknameIdentity identity = NicknameIdentity.from(request.nickname());
        String password = normalizePassword(request.password());
        attemptLimiter.checkAllowed(identity.key());

        NicknameCredential existing = credentialRepository.findByNicknameKey(identity.key()).orElse(null);
        if (existing == null) {
            return createNicknameMember(identity, password);
        }
        return loginExisting(existing, password);
    }

    private LoginResponse createNicknameMember(NicknameIdentity identity, String password) {
        LocalDateTime now = LocalDateTime.now(clock);
        String internalEmail = "nickname-" + UUID.randomUUID() + "@moca.local";
        Member member = memberRepository.save(Member.create(internalEmail, identity.displayName(), now));
        String passwordHash = password == null ? null : passwordEncoder.encode(password);
        NicknameCredential credential = new NicknameCredential(
                member.getId(), identity.displayName(), identity.key(), passwordHash, now, now);
        credentialRepository.save(credential);
        attemptLimiter.reset(identity.key());
        return createLoginResponse(member, credential.passwordProtected());
    }

    private LoginResponse loginExisting(NicknameCredential credential, String password) {
        if (!credential.passwordProtected() && password != null) {
            throw new BusinessException(DomainErrorCode.NICKNAME_PASSWORD_UNEXPECTED,
                    "비밀번호 없이 사용하는 기존 닉네임에는 비밀번호를 입력할 수 없습니다.");
        }
        if (credential.passwordProtected() && !matches(password, credential.passwordHash())) {
            attemptLimiter.recordFailure(credential.nicknameKey());
            throw new BusinessException(DomainErrorCode.NICKNAME_AUTHENTICATION_FAILED,
                    "닉네임 또는 비밀번호가 일치하지 않습니다.");
        }

        Member member = memberRepository.findById(credential.memberId())
                .orElseThrow(() -> new BusinessException(DomainErrorCode.MEMBER_NOT_FOUND,
                        "닉네임과 연결된 회원을 찾을 수 없습니다."));
        LocalDateTime now = LocalDateTime.now(clock);
        member.recordNicknameLogin(credential.nickname(), now);
        memberRepository.update(member);
        attemptLimiter.reset(credential.nicknameKey());
        return createLoginResponse(member, credential.passwordProtected());
    }

    private boolean matches(String password, String passwordHash) {
        if (password == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(password, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalizePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return null;
        }
        int length = rawPassword.codePointCount(0, rawPassword.length());
        int bytes = rawPassword.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH || bytes > BCRYPT_MAX_BYTES) {
            throw new BusinessException(DomainErrorCode.NICKNAME_PASSWORD_INVALID,
                    "비밀번호는 4자 이상 64자 이하이고 UTF-8 72바이트 이하여야 합니다.");
        }
        return rawPassword;
    }

    private LoginResponse createLoginResponse(Member member, boolean passwordProtected) {
        return new LoginResponse(
                jwtProvider.createAccessToken(member.getId()),
                "Bearer",
                accessTokenSeconds,
                new LoginMemberResponse(member.getId(), member.getName(), passwordProtected));
    }
}

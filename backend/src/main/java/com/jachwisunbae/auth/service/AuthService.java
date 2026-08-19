package com.jachwisunbae.auth.service;

import com.jachwisunbae.auth.controller.dto.LoginMemberResponse;
import com.jachwisunbae.auth.controller.dto.LoginResponse;
import com.jachwisunbae.auth.controller.dto.OAuthLoginRequest;
import com.jachwisunbae.auth.provider.OAuthProfile;
import com.jachwisunbae.auth.provider.OAuthProviderRegistry;
import com.jachwisunbae.auth.provider.OAuthProviderType;
import com.jachwisunbae.auth.token.JwtTokenProvider;
import com.jachwisunbae.member.entity.Member;
import com.jachwisunbae.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final OAuthProviderRegistry providerRegistry;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtProvider;
    private final Clock clock;
    private final long accessTokenSeconds;

    public AuthService(
            OAuthProviderRegistry providerRegistry,
            MemberRepository memberRepository,
            JwtTokenProvider jwtProvider,
            Clock clock,
            @Value("${auth.jwt.access-token-seconds}") long accessTokenSeconds) {
        this.providerRegistry = providerRegistry;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
        this.clock = clock;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    @Transactional
    public LoginResponse login(OAuthProviderType providerType, OAuthLoginRequest request) {
        OAuthProfile profile = authenticate(providerType, request);
        Member member = findOrCreateMember(profile);
        return createLoginResponse(member);
    }

    private OAuthProfile authenticate(OAuthProviderType providerType, OAuthLoginRequest request) {
        return providerRegistry.get(providerType).authenticate(request.toCommand());
    }

    private Member findOrCreateMember(OAuthProfile profile) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Member> existingMember = memberRepository.findByEmail(profile.email());
        if (existingMember.isPresent()) {
            return updateMember(existingMember.get(), profile, now);
        }
        return createMember(profile, now);
    }

    private Member updateMember(Member member, OAuthProfile profile, LocalDateTime loginAt) {
        member.updateLoginProfile(profile.email(), profile.name());
        memberRepository.update(member);
        return member;
    }

    private Member createMember(OAuthProfile profile, LocalDateTime loginAt) {
        return memberRepository.save(Member.create(profile.email(), profile.name(), loginAt));
    }

    private LoginResponse createLoginResponse(Member member) {
        return new LoginResponse(
                jwtProvider.createAccessToken(member.getId()),
                "Bearer",
                accessTokenSeconds,
                new LoginMemberResponse(
                        member.getId(),
                        member.getName(),
                        member.getEmail()));
    }

}

package com.jachwisunbae.member.service;

import com.jachwisunbae.common.time.DatabaseTime;
import com.jachwisunbae.member.client.VerifiedGoogleProfile;
import com.jachwisunbae.member.domain.Member;
import com.jachwisunbae.member.domain.OAuthProvider;
import com.jachwisunbae.member.repository.MemberRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberAuthenticationService {

    private final MemberRepository memberRepository;
    private final Clock clock;

    public MemberAuthenticationService(final MemberRepository memberRepository, final Clock clock) {
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    @Transactional(timeout = 30)
    public Member authenticate(final VerifiedGoogleProfile profile) {
        return memberRepository.upsert(
                OAuthProvider.GOOGLE,
                profile.subject(),
                profile.email(),
                profile.displayName(),
                DatabaseTime.normalize(clock.instant())
        );
    }
}

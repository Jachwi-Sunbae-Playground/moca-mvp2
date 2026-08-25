package com.jachwisunbae.member.service;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;
import com.jachwisunbae.member.entity.Member;
import com.jachwisunbae.member.repository.MemberRepository;
import com.jachwisunbae.auth.nickname.NicknameCredential;
import com.jachwisunbae.auth.nickname.NicknameCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final NicknameCredentialRepository credentialRepository;

    public MemberService(final MemberRepository memberRepository,
                         final NicknameCredentialRepository credentialRepository) {
        this.memberRepository = memberRepository;
        this.credentialRepository = credentialRepository;
    }

    public Member findById(final Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        DomainErrorCode.MEMBER_NOT_FOUND,
                        "회원을 찾을 수 없습니다."
                ));
    }

    public MemberProfile findProfileById(final Long memberId) {
        Member member = findById(memberId);
        boolean passwordProtected = credentialRepository.findByMemberId(memberId)
                .map(NicknameCredential::passwordProtected)
                .orElse(false);
        return new MemberProfile(member, passwordProtected);
    }
}

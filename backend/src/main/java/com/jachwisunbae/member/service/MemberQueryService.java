package com.jachwisunbae.member.service;

import com.jachwisunbae.common.exception.client.AuthenticationFailedException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.member.repository.MemberRepository;
import com.jachwisunbae.member.service.dto.result.MemberResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public MemberQueryService(final MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public MemberResult getMe(final long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResult::from)
                .orElseThrow(() -> new AuthenticationFailedException(ErrorCode.ACCESS_TOKEN_INVALID));
    }
}

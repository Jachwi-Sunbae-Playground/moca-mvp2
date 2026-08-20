package com.jachwisunbae.member.repository;

import com.jachwisunbae.member.entity.Member;

import java.util.Optional;

public interface MemberRepository {

    Optional<Member> findById(Long memberId);

    Optional<Member> findByIdForUpdate(Long memberId);

    Optional<Member> findByEmail(String email);

    Member save(Member member);

    void update(Member member);
}

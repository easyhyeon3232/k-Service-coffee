package com.example.coffee.domin.member.repository;

import com.example.coffee.domin.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}

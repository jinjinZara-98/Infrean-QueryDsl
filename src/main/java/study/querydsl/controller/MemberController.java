package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;

import java.util.List;

/**
 * 조회하는거만 저장X, 간단하게 샘플 데이터 넣음 스프링 올라올때
 * API호출해서 데이터만 보는
 * 프로파일을 나눠서 테스트에 영향이 없도록, 테스트에서 실행할때랑, 로컬에서 스프링부트로 톰캣을 띄울때랑 다른 상황으로
 * 톰캣으로 돌리면 샘플 데이터 넣는 로직이 동작, 테스트 케이스 돌릴때는 동작하기 않게
 *
 * 조회 API 컨트롤러 개발
 *
 * 조회 컨트롤러
 * */
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    //url에 들어오는 db조건을 동적쿼리 파라미터에 넣음
    //teamName=team@ageGie=31&ageLoe=35
    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition)
    {
        return memberJpaRepository.search(condition);
    }

    //스프링 데이터가 Pageable인터페이스 넘기면 컨트롤러가 바인딩 될 때 데이터를 넣어서 줌
    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }
}

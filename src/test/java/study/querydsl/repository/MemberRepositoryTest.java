package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;
import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/** 스프링 데이터 JPA 테스트 */
@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);
        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

//    사용자 정의 리포지토리, 커스텀 리포지토리 동작 테스트 추가
    @Test
    public void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        //MemberRepositoryImpl에서 구현한 search메서드 실행함
        List<MemberTeamDto> result = memberRepository.search(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }

    //스프링 데이터 페이징 활용1 - Querydsl 페이징 연동
    //스프링 데이터의 Page, Pageable을 활용해보자.
    //전체 카운트를 한번에 조회하는 단순한 방법
    //데이터 내용과 전체 카운트를 별도로 조회하는 방법
    @Test
    public void searchPageSimple() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        //0페이지의 3개 사이즈
        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member1");
    }

    //스프링 데이터 JPA가 제공하는 Querydsl 기능
    //테이블이 한개면 추상화해서 쓸 수 있지만 테이블이 두 세개이고 조인이 들어가면 쓰기 번거롭
    @Test
    public void querydslPredicateExecutorTest() {
        QMember member = QMember.member;

        Iterable result = memberRepository.findAll(
                member.age.between(10, 40)
                        .and(member.username.eq("member1"))
        );
    }
    //한계점
    //조인X (묵시적 조인은 가능하지만 left join이 불가능하다.)
    //클라이언트가 Querydsl에 의존해야 한다. 서비스 클래스가 Querydsl이라는 구현 기술에 의존해야 한다.
    //컨트롤러나 서비스에서 리포지토리를 호출할텐데 findAll에서 넘겨야하는게 querydslPredicate
    //서비스나 컨트롤러가 이걸 만들언 넘겨야하는데 결과적으로 다른 계층의 기술들이
    //querydsl을 다른거로 바꾸거나 할 때 난감
    //복잡한 실무환경에서 사용하기에는 한계가 명확하다.
}


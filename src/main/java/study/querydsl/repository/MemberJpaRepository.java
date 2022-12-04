package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
//import study.querydsl.dto.MemberSearchCondition;
//import study.querydsl.dto.MemberTeamDto;
//import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * transactional 선언 안되어 있어 컨트롤러에서 바로 부르거나 할때 저장같은건 transactional 적절하게 붙여줘야함
 * 순수 JPA 리포지토리, 리포지토리는 dao와 비슷, 엔티티를 조회하기 위한 데이터를 접근하기 위한 계층
 * */
@Repository
public class MemberJpaRepository {

    //JPA에 접근하기 위해 필요
    private final EntityManager em;

    //querydsl 사용하기 휘 필요
    private final JPAQueryFactory queryFactory;


    public MemberJpaRepository(EntityManager em, JPAQueryFactory jpaQueryFactory) {
        this.em = em;
        //이렇게 해도 스프링 빈으로 등록해서 사용해도 상관없음
        //this.queryFactory = new JPAQueryFactory(em);
        this.queryFactory = jpaQueryFactory;
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }


    //Querydsl 추가, 100프로 자바코드로로, 컴파일 시점에 오류나서 빌드자체가 안됨, 코드 간단
   public List<Member> findAll_Querydsl() {
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }


    // Querydsl 추가
    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    //MemberSearchCondition
    //Builder 사용
    //회원명, 팀명, 나이(ageGoe, ageLoe)
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();

        //hasText는 StringUtils.hasText
        //파라미터로 들어온거로 동일한거 있는지 검색하는 조건을 추가
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null) {
            //보다 크고
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (condition.getAgeLoe() != null) {
            //보다 작고
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                //원하는 컬럼만 조회
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                //팀의 데이터 다 가져오므로 조인
                .leftJoin(member.team, team)
                //완성된 조건 넣음음
                .where(builder)
                .fetch();
    }

//    동적 쿼리와 성능 최적화 조회
//    Where절에 파라미터를 사용한 예제, 더 깔끔, 굉장히 선호
//    조건 재사용 가능, list의 제네릭을 바꿔도

    //회원명, 팀명, 나이(ageGoe, ageLoe)
    public List<MemberTeamDto> search(MemberSearchCondition condition) {

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    //조합할수 있게 BooleanExpression로
    private BooleanExpression usernameEq(String username) {
        //비어있으면 null 비어있지 않으면 조건을 반환
        return isEmpty(username) ? null : member.username.eq(username);
    }

    private BooleanExpression teamNameEq(String teamName) {
        return isEmpty(teamName) ? null : team.name.eq(teamName);
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }
}

package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import javax.persistence.EntityManager;
import java.util.List;
import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/** 굉장히 특정 기능에 맞춰진 조회 기능이면 별로도로 MemberQueryRepositoty를 따로 클래스로 만들어줘도 됨
//인터페이스가 아닌 구현체
//커스텀에 억압될 필요는 없다, 핵심 비즈니스 로직을 재사용 가능성이 있으면 MemberRepository에
//수정 라이프 사이클 자체가 API나 화면에 맞춰서 기능이 변경됨, 찾기도 편하고

//사용자 정의 리포지토리
//사용자 정의 리포지토리 사용법
//1. 사용자 정의 인터페이스 작성 MemberRepositoryCustom
//2. 사용자 정의 인터페이스 구현 MemberRepositoryImpl
//3. 스프링 데이터 리포지토리에 사용자 정의 인터페이스 상속 MemberRepository

//현재 코드는 2번

//사용자 정의 리포지토리 MemberRepositoryCustom의 구현체,
//MemberRepositoryCustom을 상속받는 인터페이스 MemberRepository에 Impl을 꼭 맞춰서 이름 작성 */
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    //Querydsl쓰기 위해
    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    /** 이전에 만든 메서드 갖고옴 */
    @Override
    /** 회원명, 팀명, 나이(ageGoe, ageLoe) */
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

    /**
     * 단순한 페이징, fetchResults() 사용
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {

        QueryResults<MemberTeamDto> results = queryFactory
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
                //페이징만 추가, 몇번쨰부터 시작해 한 번 조회할때 몇개 조회할건지
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                /**
                 * fetch()는 반환타입이 데이터 컨텐츠를 List로 바로 가져오게됨
                 * fetchResults()를 쓰면 컨텐츠용쿼리와 카운트용쿼리 알아서 2번 날림
                 * 조인이런게 다 붙어 최적화를 못함
                 * orderby 들어가면 다 지움
                 * */
                .fetchResults();

        /** getResults()로 실제 데이터 */
        List<MemberTeamDto> content = results.getResults();

        /** getTotal()는 토탈 카운트쿼리 결과, 총 몇 개인지 */
        long total = results.getTotal();

        /** fetchResults() 위 쿼리는 총 2개의 쿼리문을 날림,
         * 그래서 실제 데이터와 총 개수를 갖고 올 수 있는 */

        /**
         * PageImpl이 page의 구현체
         * new PageImpl<>로 반환타입 page
         * */
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 복잡한 페이징
     * 데이터 조회 쿼리와, 전체 카운트 쿼리를 분리, 위 코드는 한 꺼번에 하는
     */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition,
                                                 Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();/** 바로 결과 가져오는 */

        /**
         * 쿼리를 분리, 내가 직접 쿼리를 나눠 날리는, 최적화 가능,
         * 토탈쿼리가 더 간단할때는 따로 생성해
         * 데이터가 많으면 분리?
         * */
        long total = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetchCount();

        /**
         * 스프링 데이터 라이브러리가 제공
         * count 쿼리가 생략 가능한 경우 생략해서 처리
         * 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
         * 마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함
         * */
        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()));

        /**
         * JPAQuery<Member> countQuery면 countQuery.fetchCount()로 해야 실제 카운트쿼리 날아감
         * 함수기 때문에 구문이 실행이 안되고, content랑 pageable의 토탈사이즈를 보고
         * 페이지의 시작이면서 컨텐츠의 사이즈가 페이지의 사이즈보다 작거나 마지막 페이지면 카운트쿼리 호출 안함
         * 여기서 판단해 카운트쿼리 날릴지 안날릴지
         * 만약에 db에 100개 있는데 110개를 갖고오라하면 초과하니까 토탈쿼리 날릴 필요없음
         * 그냥 처음에 데이터 다 불러오므로, 두번쨰 페이지로 넘어갈 데이터가 없으므로
         * */
        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchCount());
        //return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression usernameEq(String username) {
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
    /**
     * 전체 카운트를 조회 하는 방법을 최적화 할 수 있으면 이렇게 분리하면 된다.
     *  (예를 들어서 전체 카운트를 조회할 때 조인 쿼리를 줄일 수 있다면 상당한 효과가 있다.)
     * 코드를 리펙토링해서 내용 쿼리과 전체 카운트 쿼리를 읽기 좋게 분리하면 좋다
     * */
}

package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

/** Querydsl 테스트*/

@SpringBootTest
//테스트 코드에서만 롤백함
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    //JPAQueryFactory생성할때 엔티티매니저 객체를 생성자에 넣어줘야함
    //필드로 뺄 수 있음
    JPAQueryFactory queryFactory;

    //테스트 증명, fetchJoinNo()에 사용
    @PersistenceUnit
    EntityManagerFactory emf;

    //데이터 넣고 빼고보단 쿼리하는거 자체가 중요하므로
    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**Querydsl은 jpql의 빌더역할, 결국엔 jpql이 됨 */
    @Test
    public void startQuerydsl() {

        /**
         * compileQuerydsl로 만들어줘야함, 이름을 m이라고 주어 어떤 QMember인지 구분하기 위한
         * 별칭같은, 크게 중요하진 않음, jpql에서 member m의 그 m, 그 별칭임같은 테이블을
         * 조인해서 쓰는 경우에만 헷갈리니 사용함
         * */
//        QMember m = new QMember("m");
        QMember m = member; //기본 인스턴스 사용

        //q타입을 생성하므로 런타임오류가 아닌 컴파일오류로 오류를 잡아냄
        Member findMember = queryFactory
                .select(m)
                .from(m)
                /**
                 *파라미터 바인딩 처리,
                 *  회원의 이름이 파라미터로 들어온 이름과 같은파라미터 바인딩을 안해줌
                 */
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /** jpql이 지원하는거 다 지원함 */
    @Test
    public void search() {
        //Q객체 생성 안하고 QMember.member를 바로 써줘 static import
        //jpql 콘솔에 찍힐때 QMember.member의 값 member1이 찍힘
        Member findMember = queryFactory
                //select from을 합침, 동일하면
                .selectFrom(member)
                .where(member.username.eq("member1")
                        //조건추가, or도 가능, 그냥 ,쉼표도 가능
                        .and(member.age.eq(10)))
                .fetchOne();

        /** .where(member.username.eq("member1"),
         * member.age.eq(10)) and빼고 이렇게도 가능 */
        assertThat(findMember.getUsername()).isEqualTo("member1");

        /**JPQL이 제공하는 모든 검색 조건 제공
         *
         * member.username.eq("member1") // username = 'member1'
         * member.username.ne("member1") //username != 'member1'
         * member.username.eq("member1").not() // username != 'member1'
         * member.username.isNotNull() //이름이 is not null
         * member.age.in(10, 20) // age in (10,20)
         * member.age.notIn(10, 20) // age not in (10, 20)
         * member.age.between(10,30) //between 10, 30
         * member.age.goe(30) // age >= 30
         * member.age.gt(30) // age > 30
         * member.age.loe(30) // age <= 30
         * member.age.lt(30) // age < 30
         * member.username.like("member%") //like 검색
         * member.username.contains("member") // like ‘%member%’ 검색
         * member.username.startsWith("member") //like ‘member%’ 검색
         *
         * 결과 조회
         *
         * fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
         * fetchOne() : 단 건 조회
         * 결과가 없으면 : null
         * 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
         * fetchFirst() : limit(1).fetchOne()
         * fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행, 쿼리 두번 실행
         * select할때 회원 컬럼 다 들고옴, total은 id로 count쿼리로 하나 컬럼만 조회
         * 페이징쿼리가 복잡해지면 컨텐츠를 가지고오는쿼리와 total쿼리가 다를때가 있음. 성능때문에
         * 그래서 total쿼리를 더 심플하게 만들때도 있음
         * fetchCount() : count 쿼리로 변경해서 count 수 조회
         * jpql에서 count()에 엔티티 직접 지정하면 아이디로 바뀜
         *
         * List
         * List<Member> fetch = queryFactory
         *  .selectFrom(member)
         *  .fetch();
         *
         * 단 건
         * Member findMember1 = queryFactory
         *  .selectFrom(member)
         *  .fetchOne();
         *
         * 처음 한 건 조회
         * Member findMember2 = queryFactory
         *  .selectFrom(member)
         *  .fetchFirst();
         *
         * 페이징에서 사용
         * QueryResults<Member> results = queryFactory
         *  .selectFrom(member)
         *  .fetchResults();
         *
         * count 쿼리로 변경
         * long count = queryFactory
         *  .selectFrom(member)
         *  .fetchCount();
         * */
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                /** member.username.asc().nullsLast() 이름이 없으면 null이 마지막 */
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        //get(0) 첫번째행
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /** 페이징, 조회 건수 제한 */
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                //몇번째부터 끊어서 몇개를 스킵할꺼냐, 0부터 시작(zero index),
                //하나를 스킵
                .offset(1)
                //최대 2건 조회
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    /** 전체 조회 수가 필요하면?, 쿼리 2번 나감
    //페이징쿼리가 단순하면 써도 되지만 컨텐츠쿼리는 복잡하고 카운트쿼리는 단순하게 짜야하면 둘이 따로 작성
    //where로 조건을 넣으면 두 쿼리애 조건이 다 붙기 때문에 */
    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {

        /** 단일 타입이 아니라 데이터 타입이 여러 개 들어와서
         * 데이터를 조회하면 tuple이란걸로 조회함, 여러 개의 타입이 있을 때 꺼내오는
         * 단일타입이 아닐때 사용, 실무에서 많이 사용 안함 DTO로 사용 */
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        //첫행
        Tuple tuple = result.get(0);

        //위에 사용했던거 그대로
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {

        List<Tuple> result = queryFactory
                //Qteam.team을 static import
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        /** .groupBy(item.price)
         * .having(item.price.gt(1000)) having예시, 가격 그룹핑하고 1000원 넘는거만*/

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 조인 - 기본 조인
     *
     * 기본 조인
     * 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고,
     * 두 번째 파라미터에 별칭(alias)으로 사용할 Q 타입을 지정하면 된다.
     * join(조인 대상, 별칭으로 사용할 Q타입)
     * 기본 조인
     *
     * join() , innerJoin() : 내부 조인(inner join)
     * leftJoin() : left 외부 조인(left outer join)
     * rightJoin() : rigth 외부 조인(rigth outer join)
     * JPQL의 on 과 성능 최적화를 위한 fetch 조인 제공
     */

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {

        QMember member = QMember.member;
        QTeam team = QTeam.team;

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }


    /**
     * 세타 조인(연관관계가 없는 필드로 조인), 연관관계가 없어도 조인하는
     * 회원의 이름이 팀 이름과 같은 회원 조회, 아무 연관관계 없음
     * 연관관계 조인은 회원의 팀 아이디인 외래키와 팀에서 팀 아이디인 기본키 조인
     *
     * 왜 자꾸 까먹는가
     * 세타 조인: 조인에 참여하는 두 테이블의 컬럼 값 비교해 조건 만족하는 튜플만 반환
     * 동등 조인(내부 조인, 이너 조인): 세타조인에서 =연산자 사용한
     * 위에 join처럼 쓰면 자동으로 이너 조인인데 여기서는 세타조인 써서 직접 =조건을 주는
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                /** 연관관계가 없는, 모든 회원과 모든 팀을 다 조인 */
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

        /**
         * from 절에 여러 엔티티를 선택해서 세타 조인외부 조인 불가능(left right).
         * 다음에 설명할 조인 on을 사용하면 외부 조인 가능
         */

    }

    /**
     * 조인 - on절
     * ON절을 활용한 조인(JPA 2.1부터 지원)
     * 1. 조인 대상 필터링
     * 2. 연관관계 없는 엔티티 외부 조인
     *
     * 1. 조인 대상 필터링, 조인하는 대상을 줄이는
     *
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * jpql 에서 조인 사용하면 기본적으로 기본키 외래키 동등 조건 줌
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        //여러가지 타입이기 때문에 튜플로로
       List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                /**
                 * leftJoin이므로 멤버는 다 갖고오지만 팀은 팀 a인거만 선택,
                 * 그냥 join하면 join(member.team, team) where(team.name.eq("teamA")랑 똑같음
                 * 그냥 join쓰면 TeamB인건 출력이 안되는
                 * 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때,
                 * 내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용
                 * left조인은 on절이 의미가 있는데
                 * 이너조인은 on이나 where조건이나 똑같음
                 */
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인, 아무상관이 없는 두 컬럼
     * 연관관계 조인은 회원에서 팀 아이디인 외래키와 팀에서 팀 아이디인 기본키 조인
     *
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                /** 세타조인이면 left조인 안되므로 member만 */
                .from(member)
                /**
                 * 기존에는 member.team함,
                 * 회원과 팀 이름이 같은 경우엔 오른쪽에 있는 조인 대상을 그만큼 갖고와 조인 같지않으면 null
                 * team 대신 member.team 하면 기본키 외래키 동등 조건 on절에 들어감.
                 * 조인하는 대상이 매칭하게 되어있음
                 * team을 넣으면 아이디를 매칭 안하기 때문에 이름으로만 매칭하는
                 * 조건을 만족하지 않는 Team의 컬럼은 null 만족하면 갖고오는
                 * 그냥 조인하면 null이 나오는 행은 아예 출력이 안되는
                 */
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }

        /**
         * 하이버네이트 5.1부터 on 을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다.
         * 여기서 연관관계 없는 필드는 member.username team.name
         * 물론 내부 조인도 가능하다.
         * 주의! 문법을 잘 봐야 한다. leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
         * 일반조인: leftJoin(member.team, team) 외래키 기본키인 아이디값이 같은걸로 조인
         * on조인: from(member).leftJoin(team).on(xxx) on절에 조건을 주는, leftJoin에 엔티티 하나만
         * 즉 연관관계 없는 필드로 외부조인하려면 leftJoin() 안에 엔티티 하나만
         * */
    }

    /**
     * 조인 - 페치 조인
     * 페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를 SQL 한번에
     * 조회하는 기능이다. 주로 성능 최적화에 사용하는 방법이다.
     * 페치 조인 미적용
     * 지연로딩으로 Member, Team SQL 쿼리 각각 실행
     *
     * DTO 쓰면 못쓴다
     */
    @Test
    public void fetchJoinNo() throws Exception {
        /** 결과를 제대로 보기 어려우므로 영컨에 있는거 db에 날린 후 실행 */
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                /** 패치조인 적용, 그냥 조인 문법과 같음, 대신 fetchJoin() 넣음
                 * 연관된 team도 한 쿼리로 한 번에 끌고옴 */
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded =
                /**
                 * findMember.getTeam()는 이미 로딩된 엔티티인지 아직 초기화가 안된 엔티티인지 알려줌
                 * Member 갖고오면서 Team도 한꺼번에 갖고왔으니
                 * */
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        /** 패치조인 적용안했을때는 false가 나옴 */
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }


    /**
     * 서브 쿼리
     * com.querydsl.jpa.JPAExpressions 사용, static import가능
     * 서브 쿼리 eq 사용
     *
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {
        /** 서브쿼리이므로 바깥에 있는 멤버와 겹치면 안됨, 별칭 지정
         *
         * 메인 쿼리에서 사용하는 Member는 static import해서 따른거
         * */
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                /**
                 * 회원의 가장 많은 나이 조회해
                 *그 나이와 같은 회원 안에 바깥에 회원 객체 다름름
                 */
               .where(member.age.eq(
                        select(memberSub.age.max())
                       .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 서브 쿼리 goe 사용
     *
     * 나이가 평균 나이 이상인 회원
     */
    @Test
    public void subQueryGoe() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                /** 크거나 같은 */
                .where(member.age.goe(
                        select(memberSub.age.avg())
                       .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {

        /** 메인쿼리에서 사용할 Member 와 다른 Member 사용하게 별칭 지정*/
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                       .from(memberSub)
                        /**
                         * 10살보다 큰
                         * gt 는 더 큰 geo 는 크거나 같은
                         * */
                       .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     *  where절이 아닌 select에 서브쿼리
     *  from 에 서브쿼리 못함
     * */
    @Test
    public void selectSubQuery() throws Exception {
        /** 메인쿼리에서 사용할 Member 와 다른 Member 사용하게 별칭 지정*/
        QMember memberSub = new QMember("memberSub");

        /** 유저이름을 다 뽑고 유저이름의 평균나이를 뽑는 */
        List<Tuple> fetch = queryFactory
                .select(member.username,

                 select(memberSub.age.avg())
                .from(memberSub)

                ).from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " +
                    tuple.get(select(memberSub.age.avg())
                            .from(memberSub)));
        }
    }
    /**
     * from 절의 서브쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
     * 당연히 Querydsl 도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다.
     * Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.
     *
     * from 절의 서브쿼리 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     * 3. nativeSQL을 사용한다.
     * */


    /**
     * Case 문
     * select, 조건절(where), order by에서 사용 가능
     * 나이 10이면 10 출력이 아니라 열살로 출력
     *
     * select 안에 when 으로 출력할 값 대신 출력할 메시지 조건으로 줘서
     */
    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                     .when(10).then("열살")
                     .when(20).then("스무살")
                     .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /** 가급적이면 이런 문제는 db에서 해결 안한다? 최소한으로 데이터를 줄이는 일만 */
    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0~20살")
                    .when(member.age.between(21, 30)).then("21~30살")
                    .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * orderBy에서 Case 문 함께 사용하기 예제
     * 참고: 강의 이후 추가된 내용입니다.
     *
     * 예를 들어서 다음과 같은 임의의 순서로 회원을 출력하고 싶다면?
     * 1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     * 2. 0 ~ 20살 회원 출력
     * 3. 21 ~ 30살 회원 출력
     */
    @Test
    public void orderbywithCase() {

        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                /** rankPath 내림차순, 즉 높은 순대로 30살이 아닌 회원 먼저 출력*/
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }

        /**
         * Querydsl은 자바 코드로 작성하기 때문에 rankPath 처럼 복잡한 조건을 변수로 선언해서
         * select 절, orderBy 절에서 함께 사용할 수 있다.
         */
    }

    /**
     * 상수, 문자 더하기
     * 상수가 필요하면 Expressions.constant(xxx) 사용, jpql결과에서는 안보임 상수가
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        /**
         * 참고: 위와 같이 최적화가 가능하면 SQL에 constant 값을 넘기지 않는다.
         * 상수를 더하는 것 처럼 최적화가어려우면 SQL에 constant 값을 넘긴다
         */

    }

    /** 문자 더하기 concat */
    @Test
    public void concat() {
        List<String> result = queryFactory

                /**
                 * 이름과 나이를 더해주는 문자, 타입이 다르므로 .stringValue() 해줘야함, 쓸 일 많음
                 * enum 처리할 때 많이 사용
                 */
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s=" + s);
        }
        /**
         * 참고: member.age.stringValue() 부분이 중요한데,
         * 문자가 아닌 다른 타입들은 stringValue() 로 문자로 변환할 수 있다.
         * 이 방법은 ENUM을 처리할 때도 자주 사용한다.
         */
    }

    /**
     * 프로젝션: select절에 가져올 대상을 정하는
     * 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음
     * 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
     * DTO는 비즈니스에 맞게 임의로 만들면 되고
     * 튜플은 여러 개를 조회 할 때 대비하여 만든 타입
     * */
    @Test
    public void simpleProjection() {

        /** username타입과 맞는거만 패치결과에 나옴 */
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 투플은 리포지토리 계층에서만 사용하는걸 권고,
     * 서비스나 컨트롤러까지 넘어가면 좋은 설계가 아님
     * 하부 구현 기술이 jpa querydsl 쓴다는걸 앞단에서 알면 좋지 않다?
     * 의존이 없게 설계하는게 좋다
     * 바깥 계층으로 던질땐 dto로 바꿔서
     * 튜플은 querydsl에 종속적인 타입
     */
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                /** 이 두 컬럼만 필요하는 */
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            //이름만 꺼내는
            String username = tuple.get(member.username);
            //나이만 꺼내는
            Integer age = tuple.get(member.age);

            System.out.println("username=" + username);
            System.out.println("age=" + age);
        }
    }

    /** 순수 JPA에서 DTO 조회
     *  DTO의 package이름을 다 적어줘야해서 지저분함
     *  생성자 방식만 지원함*/
    @Test
    public void findDtoByJPQL() {

        /**
         * 쿼리는 member엔티티를 조회하기 때문에 타입오류남,
         * 그래서 뉴 오퍼레이션으로마치 생성자처럼 문법이 이렇게 생김 */
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                        "from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl 빈 생성(Bean population)
     * 결과를 DTO 반환할 때 사용
     *
     * 다음 3가지 방법 지원
     * 프로퍼티 접근
     * 필드 직접 접근
     * 생성자 사용
     *
     * 프로퍼티 접근 - Setter */
    @Test
    public void findDtoBySetter() {
        /** bean은 겟터 셋터말하는 빈, 셋터로 데이터 주입, 타입 지정하고 꺼내올 값 지정 */
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /** 필드 직접 접근 */
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                /** 게터 세터 없어도됨, DTO 필드에 바로, 프로퍼티는 세터를 통해해 */
               .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /** 생성자 사용 */
    @Test
    public void findDtoByConstructer() {
        List<MemberDto> result = queryFactory
                //이름이 아니라 필드 타입이 맞아야함
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }

    /** member에서 UserDto란 이름으로 조회하고 싶을때
     * 조회하는 컬럼 이름이 DTO랑 필드랑 다르면 .as로 매칭 시켜야함
     * */
    @Test
    public void findUserDto() {
        /** 파라미터 memberSub는 별칭 */
        QMember memberSub = new QMember("memberSub");

        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        /**
                         * 똑같이 이름이 없는 경우에 사용하는, 서브쿼리
                         * 이름이 없으므로 ExpressionUtils 두번째 파라미터 age로 감싼다
                         * 그래서 이름이 같아 age로 들어간다
                         */
                        ExpressionUtils.as(
                                select(memberSub.age.max())
                                .from(memberSub), "age")
                        )
                /** 메인 쿼리 Member 는 static import*/
                ).from(member)
                .fetch();
        /**
         * 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
         * ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 적용
         * username.as("memberName") : 필드에 별칭 적용
         *  */
    }

    /**
     * @QueryProjection 활용, 위 생성자처럼 런타임오류를 내는게 아니라 컴파일오류로
     * 이미 MemberDto의 생성자 기반으로 Q 타입도 생성자를 만들었으므로 지정해준 필드말고 다른걸 더 넣으면 컴파일오류
     * 단점 q파일 생성해야하는거, dto가 Querydsl에 의존해야하는거
     */
    @Test
    public void findDtoByQueryProjection() {

        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 동적 쿼리 - BooleanBuilder 사용
     *
     * 동적 쿼리를 해결하는 두가지 방식
     * BooleanBuilder
     * Where 다중 파라미터 사용
     */
    @Test
    public void 동적쿼리_BooleanBuilder() throws Exception {
        //검색조건
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    /** 파라미터값이 null인지 아닌지에 따라 쿼리가 동적으로 바뀌는 */
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();

        /**
         * 값이 있으면 builder에 들어가는, 그리고 builder에 있는 결과인 조건을 where에 넣어주는
         *  builder는 and or로 조립가능, 값이 있따면 값비교 조건을 넣어주는
         */
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        /** 쿼리는 그냥 회원을 찾음 검색조건을 따로 생성 */
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적 쿼리 - Where 다중 파라미터 사용
     * 실무에서 좋아하는 방법, 더 깔끔
     * BooleanBuilder보다 훨씬 깔끔, 기능은 같은데 코드는 다른
     *
     * 메서드를 다른 쿼리에서도 재활용 할 수 있다. 쿼리 자체의 가독성이 높아진다.
     */
    @Test
    public void 동적쿼리_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                //이 where문안에서 바로 해결
                .where(usernameEq(usernameCond), ageEq(ageCond))
//                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        //값이 null이 아니면 조건 리턴, 응답값이 null이면 where문에선 무시함
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    /** 조합 가능, 조립해서, null 체크는 주의해서 처리해야함 */
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 수정, 삭제 벌크 연산
     * 쿼리 한번으로 대량 데이터 수정
     *
     * jpa는 엔티티 가져와서 엔티티 값 바꾸면 트랜잭션 커밋할때 변경감지 일어나면서
     * 엔티티 바뀌었네 하면서 업데이트쿼리 만들어지면서 db에 날아감, 영컨에 있는건 수정안됨
     * db상태와 영컨상태와 다름, 값을 가져올때는 영컨에 값 있으면 db에서 가지고 온 값 버리고
     * 영컨에서 가져와서 수정한 값을 안가지고옴
     * */
    @Test
    /** 롤백되므로 db에 안보여서 */
    @Commit
    public void bulkUpdate() {

        //count는 영향을 받은 행의 수
        //28살 미만이면 비회원
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                /** 조건, lt 미만 lowTehn, gt gae lt lae*/
                .where(member.age.lt(28))
                .execute();

        /**
         *  db와 영컨 안맞기 때문에 무조건
         * 영컨이 비어있으니까 db에서 가져온게 그대로 올라옴
         */
        em.flush(); em.clear();

        List<Member> result = queryFactory.selectFrom(member).fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

        /**
         * 주의: JPQL 배치와 마찬가지로,
         * 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에 배치 쿼리를
         */
    }

    /** 기존 숫자에 1 더하기, where 절 없음 */
    @Test
    public void bulkAdd() {

        long count = queryFactory
                .update(member)
                //minus()는 없으므로 -1해주면됨, 곱하기: multiply(x)
                .set(member.age, member.age.add(1))
                .execute();
    }

    /** 쿼리 한번으로 대량 데이터 삭제 */
    @Test
    public void bulkDelete() {

        /** 쿼리 한번으로 대량 데이터 삭제*/
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * SQL function 호출하기, jpql뿐만아니라 querydsl도 호출 가능
     * SQL function은 JPA와 같이 Dialect라는 방언에 등록된 내용만 호출할 수 있다.
     * 하이버네이트 구현체를 사용하면
     *
     * 직접 임의로 db에서 funciton만들어서 사용하고 싶다면 h2dialect상속받아 하나 만들고
     * 그걸로 설정에다가 등록해서 사용해야함
     */
   @Test
    public void sqlFunction() {
        String result = queryFactory
                //member라는 단어를 M으로 바꿔서 조회
                .select(Expressions.stringTemplate
                        ("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                .from(member)
                .fetchFirst();
    }

    /** 소문자로 변경해서 비교해라, 이런 간단한거는 표준에 등록이 되어있다? querydsl이 내장하고 있다 */
    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();
    }
}


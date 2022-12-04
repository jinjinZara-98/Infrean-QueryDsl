package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;
import java.util.List;

/** 스프링 데이터 JPA니까 인터페이스로 만들고 상속받는

//사용자 정의 리포지토리
//사용자 정의 리포지토리 사용법
//1. 사용자 정의 인터페이스 작성 MemberRepositoryCustom
//2. 사용자 정의 인터페이스 구현 MemberRepositoryImpl
//3. 스프링 데이터 리포지토리에 사용자 정의 인터페이스 상속

//현재 코드는 3번, 인터페이스를 인터페이스 상속할때는 extends

//MemberRepositoryCustom의 기능 상속받아 search메서드 사용 가능, 외부에서 호출 가능
//MemberRepositoryCustom의 search메서드 호출하지만 MemberRepositoryImpl의 search메서드 사용하는

//QuerydslPredicateExecutor의 기능을 다 사용하여 파라미터에 Querydsl조건을 넣을 수 있음 */
public interface MemberRepository extends JpaRepository<Member, Long> ,MemberRepositoryCustom
                , QuerydslPredicateExecutor<Member> {

    //기본적인 crud쿼리는 제공해주고
    //메서드만 만들면 메서드이름을 이용해 쿼리 만드는
    //select m from Member m where
    List<Member> findByUsername(String username);
}

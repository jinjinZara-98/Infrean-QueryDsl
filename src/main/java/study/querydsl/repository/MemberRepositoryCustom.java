package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

/**사용자 정의 리포지토리
//사용자 정의 리포지토리 사용법
//1. 사용자 정의 인터페이스 작성 MemberRepositoryCustom
//2. 사용자 정의 인터페이스 구현 MemberRepositoryImpl
//3. 스프링 데이터 리포지토리에 사용자 정의 인터페이스 상속 MemberRepository

//현재 코드는 1번

//스프링 데이터 JPA쓰면서 복잡한구현 하거나 커스텀한 기능 필요할때 쓰는 사용자 정의 리포지토리
//인터페이스로 동작하기 때문에 원하는 구현 코드를 넣으려면 사용자 정의 리포지토리라는복잡한 방법을 써야함
//스프링 데이터 jpa 리포지토리를 사용하면서 직접 구현한걸 쓰고 싶으면

//인터페이스는 이렇게 만들고 이름 어떻게 만들어도 상관없음
//틀만 만들어주기, 메서드 반환타입, 메서드 이름, 파라미터 타입 */
public interface MemberRepositoryCustom {

    /** 스프링 데이터 JPA를 쓰면서 직접 구현한한 것도 쓰는 */
    List<MemberTeamDto> search(MemberSearchCondition condition);

    /**스프링 데이터 페이징 활용1 - Querydsl 페이징 연동 */
    /** 스프링 데이터의 Page, Pageable을 활용해보자. */

    /** 사용자 정의 인터페이스에 페이징 2가지 추가 */

    /** 전체 카운트를 한번에 조회하는 단순한 방법, 쉰게 단순한 쿼리 하는 */
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);

    /** 데이터 내용과 전체 카운트를 별도로 조회하는 방법 */
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}

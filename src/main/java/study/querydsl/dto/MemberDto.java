package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

/**
 * 프로젝션과 결과 반환 - DTO 조회
 * 순수 JPA에서 DTO 조회
 * 엔티티를 조회하면 member에 있는 값 다 불러와야해서 이름과 나이만 가져오고 싶을때
 * 두 데이터를 담는 통
 */
@Data
public class MemberDto {

    private String username;

    private int age;

    /** @Data는 기본 생성자 안 만들어주므로 만들어줘야함, 안 그럼 에러남 */
    public MemberDto() {
    }

    /**
     * complieQueryDsl해줘서 이 dto도 q파일 생성
     * 기존에는 @QueryProjection이 없었기 때문에 Querydsl에 대한 라이브러리 의존성이 없었는데
     * 이렇게 어노테이션 붙어주면 Querydsl의존성을 갖게됨, 그래서 순수하지 않다?
     */
    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}

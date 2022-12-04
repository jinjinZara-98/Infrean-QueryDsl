package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

/**
 * 동적 쿼리와 성능 최적화 조회 - Builder 사용
 * 멤버 팀 정보 섞어서 원하는 정보 갖고오는
 */
@Data
public class MemberTeamDto {

    private Long memberId;

    private String username;

    private int age;

    private Long teamId;

    private String teamName;

    /**
     * complieQueryDsl해줘서 이 dto도 q파일 생성, 원하는 컬럼만 갖고올때 사용?
     * 순수하지 않다, QueryDsl라이브러리에 의존하게 됨
     */
    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}

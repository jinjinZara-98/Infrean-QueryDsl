package study.querydsl.dto;

import lombok.Data;

/**
 * 회원 검색 조건
 * 밑에 필드중 하나로 검색하면 리스트에 나오는 필터링기능
 *
 * MemberJpaRepository에 만듬
 */
@Data
public class MemberSearchCondition {

    //회원명, 팀명, 나이(ageGoe, ageLoe)
    private String username;

    private String teamName;

    //값이 null일 수 있으므로 integer로
    private Integer ageGoe;

    private Integer ageLoe;

}

package study.querydsl.entity;

import lombok.*;
import javax.persistence.*;

//롬복 설명
//@Setter: 실무에서 가급적 Setter는 사용하지 않기

//JPA는 기본생성자가 있어야 하므로
//@NoArgsConstructor AccessLevel.PROTECTED: 기본 생성자 막고 싶은데, JPA 스팩상 PROTECTED로 열어두어야 함

//@ToString은 가급적 내부 필드만(연관관계 없는 필드만)
//changeTeam() 으로 양방향 연관관계 한번에 처리(연관관계 편의 메소드)
@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id
    @GeneratedValue
    //db에는 이 이름으로 컬럼명을 줌
    @Column(name = "member_id")
    private Long id;

    private String username;
    private int age;

    //연관관계주인
    @ManyToOne(fetch = FetchType.LAZY)
    //외래키 이름
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username) {
        this(username, 0);
    }
    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;

        //null이 아니면 hangeTeam 실행
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        //팀에 연관되어있는 나도 값을 세팅
        team.getMembers().add(this);
    }
}

package study.querydsl.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

//기본적인 자바 컴파일하는 프로세스 안에 컴파일 쿼리디에스엘 프로세스가 들어간다
//단순히 식별자가 있는 엔티티
@Entity
@Getter @Setter
public class Hello {

    @Id @GeneratedValue
    private Long id;
}
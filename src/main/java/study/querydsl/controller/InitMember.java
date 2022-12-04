package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/** 조회하는거만 저장X, 간단하게 샘플 데이터 넣음 스프링 올라올때
//API호출해서 데이터만 보는
//프로파일을 나눠서 테스트에 영향이 없도록, 테스트에서 실행할때랑, 로컬에서 스프링부트로 톰캣을 띄울때랑 다른 상황으로
//톰캣으로 돌리면 샘플 데이터 넣는 로직이 동작, 테스트 케이스 돌릴때는 동작하기 않게

//조회 API 컨트롤러 개발
//편리한 데이터 확인을 위해 샘플 데이터를 추가하자.

//샘플 데이터 추가

//local먹일때만 동작하게, text yml파일에 local해놓음, 스프링부트로 메인 실행하면 local이란 profile로 실행됨
//그래서 이게 실행, 실행하고 콘솔보면 ther following profiles are active: local이 보임

//test패키지의 yml피일에서는 active: test이므로 @PostConstruct의 init메서드가 실행 안됨
//즉 테스트 할 때 쿼리는 밑에 데이터를 넣고 실행 안하려교 */
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    /**
     * 데이터 넣는 코드 스프링 실행할때 항상 행되게
     * @PostConstruct와 같이 @Transactional 못써서 따로 실행코드 들어있는 클래스 만들어서 분리
     */
    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {

        @PersistenceContext
        EntityManager em;

        //데이터 초기화하게
        @Transactional
        public void init() {
            //스프링 띄울때 데이터 자동으로 넣는
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            //절반은 TeamA 절반은 TeamB에 소속되게
            for (int i = 0; i < 100; i++) {
                //i % 2 == 0이 조건에 부합하면 TeamA가 들어가는
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                //이름, 나이, 선택된 팀
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }
    }
}

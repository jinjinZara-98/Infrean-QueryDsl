package study.querydsl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
/** 테스트 끝나고 @Transactiona이 롤백하기 때문에 commit달아줘야함 */
//@Commit
class QuerydslApplicationTests {

//	@PersistenceContext 이거 써도됨
	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();

		em.persist(hello);

		//Querydsl을 쓰려면 JPAQueryFactory생성
		JPAQueryFactory query = new JPAQueryFactory(em);

		//Querydsl Q타입 동작 확인, new로 생성하지 않고 만들어진 메서드로 객체 생성
		QHello qHello = QHello.hello;

		//Querydsl, Hello엔티티가 아닌 쿼리와 관련된것은 Q타입인 QHello를 씀
		Hello result = query
				.selectFrom(qHello)
				.fetchOne();

		assertThat(result).isEqualTo(hello);//lombok 동작 확인 (hello.getId())
		assertThat(result.getId()).isEqualTo(hello.getId());
	}
}

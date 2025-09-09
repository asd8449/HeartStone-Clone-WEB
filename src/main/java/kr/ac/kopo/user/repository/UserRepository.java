package kr.ac.kopo.user.repository;

import java.util.Optional; // Optional 임포트 추가
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import kr.ac.kopo.user.vo.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    /**
     * 사용자 ID를 기준으로 사용자를 찾는 메소드
     * Spring Data JPA가 메소드 이름을 분석하여 자동으로 쿼리를 생성해줍니다.
     * @param id 찾을 사용자의 ID
     * @return Optional<UserEntity>
     */
    Optional<UserEntity> findById(String id);
}
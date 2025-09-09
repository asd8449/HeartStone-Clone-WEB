package kr.ac.kopo.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.ac.kopo.user.repository.UserRepository;
import kr.ac.kopo.user.vo.UserEntity;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * 로그인 처리를 위한 메소드
	 * @param inputUser 로그인 폼에서 입력받은 사용자 정보 (id, password)
	 * @return 로그인 성공 시 사용자 정보 전체를 담은 UserEntity, 실패 시 null
	 */
	public UserEntity login(UserEntity inputUser) {
		// 1. ID를 기준으로 DB에서 사용자 정보를 조회합니다.
		UserEntity dbUser = userRepository.findById(inputUser.getId()).orElse(null);

		// 2. DB에 해당 ID의 사용자가 존재하고, 입력된 비밀번호와 DB의 암호화된 비밀번호가 일치하는지 확인합니다.
		if (dbUser != null && passwordEncoder.matches(inputUser.getPassword(), dbUser.getPassword())) {
			return dbUser; // 로그인 성공
		}

		return null; // 로그인 실패
	}

	/**
	 * 회원가입 처리를 위한 메소드
	 * @param user 가입할 사용자 정보
	 */
	@Transactional
	public void signup(UserEntity user) {
		// 비밀번호를 BCrypt로 암호화하여 저장합니다.
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		userRepository.save(user);
	}
}
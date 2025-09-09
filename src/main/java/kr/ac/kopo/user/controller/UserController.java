package kr.ac.kopo.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import kr.ac.kopo.user.service.UserService;
import kr.ac.kopo.user.vo.UserEntity;

@Controller
public class UserController {

	@Autowired
	private UserService userService;
	
	/**
	 * 로그인 폼을 보여주는 GET 요청 처리
	 */
	@GetMapping("/login")
	public String loginForm() {
		return "user/loginForm";
	}

	/**
	 * 실제 로그인 데이터를 처리하는 POST 요청 처리
	 */
	@PostMapping("/login")
	public String login(UserEntity user, HttpSession session, RedirectAttributes redirectAttrs) {
		UserEntity loginUser = userService.login(user);
		
		if (loginUser != null) {
			// 로그인 성공 시 세션에 사용자 정보 저장
			session.setAttribute("user", loginUser);
			return "redirect:/"; // 메인 페이지로 리다이렉트
		} else {
			// 로그인 실패 시 에러 메시지와 함께 로그인 폼으로 리다이렉트
			redirectAttrs.addFlashAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
			return "redirect:/login";
		}
	}
	
	/**
	 * 로그아웃 처리
	 */
	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate(); // 세션 무효화
		return "redirect:/";
	}

	/**
	 * 회원가입 폼을 보여주는 GET 요청 처리
	 */
	@GetMapping("/signup")
	public String signupForm() {
		return "user/signupForm";
	}

	/**
	 * 실제 회원가입 데이터를 처리하는 POST 요청 처리
	 */
	@PostMapping("/signup")
	public String signup(UserEntity user, RedirectAttributes redirectAttrs) {
		userService.signup(user);
		redirectAttrs.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
		return "redirect:/login";
	}
}
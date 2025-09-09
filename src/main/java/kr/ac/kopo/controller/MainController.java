package kr.ac.kopo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;
import kr.ac.kopo.user.vo.UserEntity; // UserEntity 임포트 추가

@Controller
public class MainController {
	
	@GetMapping("/")
	public ModelAndView main(HttpSession session) {
		ModelAndView mav = new ModelAndView("main");
		
		// 세션에서 'user' 정보를 가져옵니다.
		UserEntity user = (UserEntity) session.getAttribute("user");
		
		// 'user' 객체가 존재하면 모델에 추가합니다.
		// 이렇게 하면 템플릿에서 {{#user}}...{{/user}} 구문을 사용할 수 있습니다.
		if (user != null) {
			mav.addObject("user", user);
		}
		
		return mav;
	}
}
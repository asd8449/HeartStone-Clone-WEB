# HeartStone-Clone-WEB

## 🎮 프로젝트 소개
이 프로젝트는 **블리자드 엔터테인먼트**의 인기 디지털 카드 게임 *하스스톤(Hearthstone)* 을 웹 기반으로 구현한 클론입니다.  
백엔드에 **Spring Boot / Java**를 사용하며, **WebSocket**을 통해 실시간 멀티플레이어 대전을 지원합니다.  
사용자는 직접 덱을 구성하고, 다른 플레이어와 실시간 카드 대전을 즐길 수 있습니다.

---

## ✨ 주요 기능

### 회원 관리
- 사용자 회원가입 및 로그인 기능 제공

### 덱 관리
- 다양한 영웅 중 하나를 선택해 새로운 덱 생성
- 카드 필터링 및 검색 기능으로 덱에 카드 추가/제거
- 덱 목록 확인 및 편집, 삭제 기능

### 실시간 게임 플레이
- **WebSocket (STOMP / SockJS)** 기반 실시간 게임 진행
- 카드 플레이, 하수인 공격, 영웅 능력 사용 등 하스스톤 핵심 플레이 구현
- **GSAP** 라이브러리를 활용한 카드 공격, 하수인 파괴 등 부드러운 애니메이션 효과 제공

### 카드 및 영웅 데이터 관리
- 관리자 페이지를 통해 카드 능력치와 특수 능력을 JSON으로 관리
- 실행 시 **HearthstoneJSON API** 호출로 최신 카드 및 영웅 이미지 자동 업데이트

---

## 🛠 기술 스택

- **백엔드**: Java, Spring Boot, Spring Security, JPA(Hibernate)  
- **프론트엔드**: HTML, CSS, JavaScript, Mustache, GSAP  
- **데이터베이스**: Oracle DB  
- **실시간 통신**: WebSocket, STOMP, SockJS  
- **라이브러리**: Lombok, Jackson  

---

## 🚀 시작하기

1. **사전 준비**
   - JDK 17 이상  
   - Oracle Database  
   - Maven 설치

2. **프로젝트 설정**
   - `src/main/resources/application.properties` 파일에서 본인 환경에 맞춰 `spring.datasource` 설정 수정:
     ```properties
     spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
     spring.datasource.username=hr
     spring.datasource.password=hr
     ```
   - 프로젝트 루트 디렉토리에서 다음 명령어로 애플리케이션 실행:
     ```bash
     mvn spring-boot:run
     ```

3. **게임 플레이 방법**
   - 웹 브라우저에서 `http://localhost:8080` 접속  
   - 회원가입 및 로그인 진행  
   - `덱 보기` 메뉴에서 새 덱 생성  
   - `게임 시작하기` 버튼 클릭 → 자동 매칭 → 게임방 진입 → 플레이 시작

---

## 📁 프로젝트 구조
```
.
├── gradle
│ └── wrapper
│ ├── gradle-wrapper.jar
│ └── gradle-wrapper.properties
├── src
│ ├── main
│ │ ├── java/kr/ac/kopo
│ │ └── resources
│ └── test
└── neutral_cards.csv
```

---

### 1. `gradle/wrapper`
이 디렉토리는 **Gradle Wrapper** 설정 파일들을 포함합니다. 이를 통해 다른 개발 환경에서도 동일한 버전의 Gradle을 사용하여 프로젝트를 일관성 있게 빌드할 수 있습니다.

- `gradle-wrapper.jar`: Gradle Wrapper 실행을 위한 JAR 파일  
- `gradle-wrapper.properties`: 사용할 Gradle 버전 및 다운로드 URL 등의 설정 정의  

---

### 2. `src/main/java/kr/ac/kopo`
애플리케이션의 핵심 **백엔드 로직**이 담긴 Java 소스 코드 디렉토리입니다.

- **HearthStoneApplication.java**: Spring Boot 애플리케이션을 시작하는 메인 클래스  
- **ServletInitializer.java**: 프로젝트를 WAR 파일로 배포할 때 필요한 설정 클래스  

#### 📂 `/admin`
- **AdminController.java**: 카드 정보를 수정하고 관리하는 관리자 페이지 요청 처리  

#### 📂 `/card`
- **CardController.java**: 전체 카드 목록, 카드 상세 정보 페이지 요청 처리  
- **CardService.java, HeroService.java**: 카드와 영웅 데이터에 대한 비즈니스 로직 담당  
- **repository/\*.java**: Spring Data JPA를 사용해 카드, 영웅, 카드 능력 DB 작업 처리  
- **vo/\*.java**: CardVO, HeroVO 등 JPA Entity 및 HeroDTO 같은 DTO 포함  

#### 📂 `/config`
- **SecurityConfig.java**: Spring Security 설정 (현재 모든 요청 허용)  
- **WebSocketConfig.java**: WebSocket 엔드포인트 및 메시지 브로커 설정  
- **CardDataInitializer.java**: 애플리케이션 시작 시 외부 API 호출 → 카드 데이터 DB 업데이트  

#### 📂 `/controller`
- **MainController.java**: 메인 랜딩 페이지(`/`) 요청 처리  

#### 📂 `/deck`
- **DeckController.java**: 덱 목록 조회, 생성, 편집, 삭제 요청 처리  
- **DeckService.java**: 덱 저장, 수정, 조회 로직 담당  
- **repository/\*.java**: DeckEntity, CardOfDeckEntity DB 작업 처리  

#### 📂 `/game`
- **GameController.java**: 게임 덱 선택, 게임방 입장 등 HTTP 요청 처리  
- **GameMessageController.java**: WebSocket을 통한 실시간 게임 액션 메시지 처리  
- **GameService.java**: 게임 시작, 턴 관리, 카드 플레이, 공격, 영웅 능력 로직 담당  
- **GameRoomService.java**: 게임 매칭 및 게임방 생성/참여 로직 담당  

#### 📂 `/user`
- **UserController.java**: 회원가입, 로그인, 로그아웃 요청 처리  
- **UserService.java**: 로그인 인증 및 회원가입 비즈니스 로직 담당  
- **UserRepository.java**: 사용자 DB 작업 처리  

---

### 3. `src/main/resources`
애플리케이션 실행에 필요한 **설정 및 정적 파일**이 포함된 디렉토리입니다.

- **application.properties**: DB 연결 정보, 로깅 레벨 등 주요 설정  

#### 📂 `/static`
- **css/game-room.css**: 게임방 스타일 정의  
- **js/game-room.js**: SockJS + Stomp.js를 통한 서버와 실시간 통신, 게임 상태 업데이트 및 사용자 입력 처리  

#### 📂 `/templates`
Mustache 템플릿 파일 모음 (**서버 사이드 렌더링**)  

- **main.mustache**: 메인 페이지  
- **/user/**: 로그인(`loginForm.mustache`), 회원가입(`signupForm.mustache`) 페이지  
- **/deck/**: 덱 목록, 덱 생성(영웅 선택), 덱 편집 페이지들  
- **/classic/**: 게임 덱 선택, 매칭 대기, 게임방 페이지들  
- **/admin/**: 관리자용 카드 목록 및 수정 페이지  

---

### 4. `src/test/java`
- **HearthStoneApplicationTests.java**: Spring Boot 애플리케이션 컨텍스트 로드 여부 확인 기본 테스트 클래스  

---

### 5. `neutral_cards.csv`
프로젝트 루트에 위치한 **CSV 파일**  
- 중립 카드들의 기본 데이터를 포함  
- `CardDataInitializer`가 API를 통해 데이터를 받아오므로 초기 데이터 로딩 및 참고용으로 사용됨  

---

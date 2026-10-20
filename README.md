# 인프라 검증용 CRUD 게시판 풀스택 시스템

독립 **Nginx 게이트웨이(Reverse Proxy)**를 앞단에 둔 **Pattern 2 아키텍처** 기반 풀스택 시스템입니다.  
스프링 부트(Java 21) + MySQL 8.0 + React SPA로 구성되어 있으며, 실무 인프라와 동일하게 단일 진입점(포트 80)을 통해 모든 트래픽을 내부 마이크로서비스로 라우팅합니다.

---

## 🏗️ 시스템 아키텍처 (Pattern 2: Standalone Gateway)

```
[ 브라우저 (사용자) ]
        │
        ▼ (Port 80)
┌──────────────────────────────────────────────┐
│       [ Gateway (독립 Nginx 컨테이너) ]         │
│  - 단일 진입점, SSL/HTTPS 종단, 로드밸런싱     │
└──────┬──────────────────────┬────────────────┘
       │ (내부 라우팅)          │ (내부 라우팅)
       ▼ /api/*, /actuator/*  ▼ /*
┌───────────────────────┐   ┌──────────────────────┐
│  [ Backend 컨테이너 ]   │   │ [ Frontend 컨테이너 ] │
│  Spring Boot 4.x      │   │ React SPA (정적 서빙) │
│  (Port 8080 - 내부격리)│   │ (Port 80 - 내부격리)  │
└──────────┬────────────┘   └──────────────────────┘
           │ (내부 통신)
           ▼ Port 3306
┌───────────────────────┐
│   [ MySQL 8.0 컨테이너 ]│
│   (Volume 영속성 보장) │
└───────────────────────┘
```

---

## 🚀 빠른 시작 (Docker Compose)

### 1) 환경 변수 파일 (.env)
프로젝트 루트의 `.env` 파일에서 게이트웨이 포트, DB 비밀번호, 계정명을 관리합니다:
```env
GATEWAY_PORT=80

MYSQL_DATABASE=board_db
MYSQL_ROOT_PASSWORD=root_secret_1234
MYSQL_USER=board_user
MYSQL_PASSWORD=board_secret_1234
MYSQL_PORT=3306
```

### 2) 실행 명령어
```bash
# 전체 컨테이너 빌드 및 백그라운드 실행
docker compose up -d --build

# 컨테이너 상태 및 헬스체크 확인
docker compose ps

# 로그 실시간 확인
docker compose logs -f
```

### 접속 주소
* **웹 대시보드 (Frontend)**: [http://localhost](http://localhost) (기본 80 포트)
* **백엔드 API**: [http://localhost/api/posts](http://localhost/api/posts)
* **스프링 Actuator 헬스체크**: [http://localhost/actuator/health](http://localhost/actuator/health)
* **MySQL 직접 접속**: `localhost:3306` (계정: `board_user`, 비밀번호: `board_secret_1234`, DB: `board_db`)

---

## 💻 로컬 개발 환경에서 개별 실행

### 1) MySQL만 도커로 실행
```bash
docker compose up -d mysql
```

### 2) 백엔드 실행 (Spring Boot)
```bash
cd backend
./gradlew bootRun
```
* 백엔드가 정상 실행되면 `http://localhost:8080`으로 서비스됩니다.

### 3) 프론트엔드 실행 (Vite Dev Server)
```bash
cd frontend
npm install
npm run dev
```
* `http://localhost:3000`에서 핫 리로딩과 함께 실행되며, Vite 프록시를 통해 `localhost:8080` 백엔드로 자동 연동됩니다.

---

## 📡 API 엔드포인트 명세

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/posts` | 게시글 목록 조회 (검색 파라미터: `?keyword=...`) |
| `GET` | `/api/posts/{id}` | 게시글 상세 조회 (조회수 1 증가) |
| `POST` | `/api/posts` | 게시글 등록 (`title`, `content`, `author`) |
| `PUT` | `/api/posts/{id}` | 게시글 수정 (`title`, `content`, `author`) |
| `DELETE` | `/api/posts/{id}` | 게시글 삭제 |
| `GET` | `/actuator/health` | 스프링 부트 및 MySQL 통합 헬스체크 (상태/디스크/DB) |
| `GET` | `/actuator/health/liveness` | K8s 프로세스 활성 상태 검사 |
| `GET` | `/actuator/health/readiness` | K8s 트래픽 유입 준비 완료 검사 |

---

## 🧪 인프라 테스트 시나리오 가이드

### 1. Actuator 헬스체크 검증 (curl)
```bash
curl -i http://localhost:8080/actuator/health
```

### 2. CRUD 동작 검증 (curl)
```bash
# 게시글 등록
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"인프라 테스트 1","content":"도커 네트워크 및 DB 통신 테스트","author":"테스터"}'

# 게시글 목록 조회
curl -s http://localhost:8080/api/posts

# 게시글 단건 조회 (ID: 1)
curl -s http://localhost:8080/api/posts/1
```

### 3. MySQL 데이터 영속성 (Volume) 검증
1. 웹 브라우저(`http://localhost:3000`)에서 새 글을 작성합니다.
2. 백엔드와 MySQL 컨테이너를 종료합니다:
   ```bash
   docker compose down
   ```
3. 다시 컨테이너를 올립니다:
   ```bash
   docker compose up -d
   ```
4. 새로고침 후 이전에 작성한 글이 영구 저장되어 있는지 확인합니다. (`mysql_data` 볼륨으로 데이터가 보존됩니다.)

---

## 🛠️ 주요 기술 스택
* **Backend**: Java 21, Spring Boot 4.1.x, Spring Data JPA, Hibernate, Spring Boot Actuator, Lombok
* **Database**: MySQL 8.0 (utf8mb4)
* **Frontend**: React 19, Vite, Lucide Icons, Vanilla CSS (Modern Dark/Glassmorphism UI)
* **Infra/Container**: Docker, Docker Compose, Nginx (Alpine), Multi-stage build

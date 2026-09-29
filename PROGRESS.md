# 백엔드 작업 진척도 (StreamerCalendar)

> 점검일: 2026-09-28 · 기준: `1df3617` (Render 배포 완료) · 스펙 기준: 루트 `CLAUDE.md` (API 명세 MVP / ERD v0.1)

## 요약

| 항목 | 상태 |
| --- | --- |
| MVP API (Streamer 3종 + Stream Record 5종 + Auth 3종) | ✅ 구현 완료 |
| 관리자 인증 / 인가 | ✅ 세션 로그인 + 로그인 상태 유지(remember-me). 조회는 공개, 등록·수정·삭제는 ADMIN만 |
| 공통 에러 응답 포맷 | ✅ 잘못된 요청은 400/404/405/415, 예상 못 한 예외만 500 (로그 기록) |
| 엔티티 / ERD 반영 | 🟡 `STREAMER`, `STREAM_RECORD`만 반영 (`PLATFORM_CHANNEL`은 Phase 2) |
| 테스트 코드 | 🟡 `./gradlew test` 51건 통과 (컨트롤러 슬라이스 42건 + 쿼리 수 검증 4건 + 설정값 검증 4건 + `contextLoads` 1건). 서비스 단위 테스트 없음 |
| Phase 2 (후보 큐, 크롤링 연동) | ⬜ 미착수 |

**진척도(체감): MVP 기준 약 85%.** 기능, 권한 분리, 에러 처리, N+1 해결까지 끝났습니다. 정렬·인덱스, 입력 검증, 테스트 범위 등이 남아 있습니다.

> 운영 API: https://streamercalendar.onrender.com (Render, Singapore) · DB: Neon (Singapore)

## 기술 스택 (실제)

- Spring Boot **4.1.0** / Java 21 (toolchain) / Gradle
- Spring Data JPA, Validation, Web MVC, Security, Lombok, DevTools
- PostgreSQL (`localhost:5432/stream_cal`), `ddl-auto=update`, `open-in-view=false`

## 현재 구현 현황

### 패키지 구조
```
com.example.streamercalendar
├── config/        SecurityConfig(권한 규칙, remember-me, 로그아웃, 401/403 JSON, CORS+credentials), AdminAccountConfig, AdminProperties
├── controller/    AuthController, StreamerController, StreamRecordController
├── domain/        Streamer, StreamRecord, Platform(enum), Source(enum)
├── dto/           LoginRequest, AuthStatusResponse, StreamerCreateRequest/Response, StreamRecordRequest/Response, ErrorResponse
├── exception/     GlobalExceptionHandler, ResourceNotFoundException
├── repository/    StreamerRepository, StreamRecordRepository(+JpaSpecificationExecutor)
└── service/       StreamerService, StreamRecordService
```

### API

| API | 권한 | 비고 |
| --- | --- | --- |
| `GET /api/streamers` | 공개 | 정렬 없음 |
| `POST /api/streamers` | 관리자 | 201. name 누락 시 400. **중복 409는 미구현** |
| `DELETE /api/streamers/{id}` | 관리자 | 204 / 404. 연관 방송 기록까지 **함께 삭제됨** (방송 기록 → 스트리머 순서로 DELETE 2번) |
| `GET /api/streams` | 공개 | `streamerId`/`platform`/`year`(1~9999)/`month`(1~12) 필터 (Specification). streamer를 함께 조회해 쿼리 1번. 정렬·페이지네이션 없음 |
| `GET /api/streams/{id}` | 공개 | 404 `STREAM_NOT_FOUND`. 쿼리 1번 |
| `POST /api/streams` | 관리자 | `source=MANUAL` 서버에서 설정. 스트리머 없으면 404 |
| `PUT /api/streams/{id}` | 관리자 | 스트리머 변경도 지원 |
| `DELETE /api/streams/{id}` | 관리자 | 204 / 404 |
| `POST /api/auth/login` | 공개 | 200 / 400 / 401 `INVALID_CREDENTIALS`. 세션 ID 재발급. `rememberMe: true`면 remember-me 쿠키(14일) 발급 |
| `POST /api/auth/logout` | 공개 | 204. 세션 무효화 + remember-me 쿠키 삭제 |
| `GET /api/auth/me` | 공개 | 비로그인도 200 `{ authenticated: false }` |

- 권한 없는 요청: 비로그인 401 `UNAUTHORIZED`, 관리자가 아니면 403 `FORBIDDEN` (공통 에러 포맷)
- 잘못된 요청: 파라미터 타입·범위 오류, 깨진 JSON, 형식이 틀린 필드(enum·날짜) 400 `INVALID_REQUEST` (필드 이름 포함) · 없는 경로 404 `NOT_FOUND` · 지원하지 않는 메서드 405 `METHOD_NOT_ALLOWED`(`Allow` 헤더) · JSON이 아닌 Content-Type 415 `UNSUPPORTED_MEDIA_TYPE`. `GlobalExceptionHandler`가 `ResponseEntityExceptionHandler`를 상속해 처리합니다.
- 공개할 조회 경로만 `SecurityConfig`에서 명시적으로 열고, 나머지(앞으로 추가될 API 포함)는 기본적으로 관리자 전용입니다.

### 테스트 (51건)
- `AuthControllerTest` (12건): 로그인 성공/실패/400, 세션 ID 재발급, 로그아웃, remember-me 발급·미발급·쿠키만으로 로그인·위조 쿠키·로그아웃 시 삭제
- `StreamerControllerTest` (8건), `StreamRecordControllerTest` (22건): API 동작, 비로그인 401 / 관리자 아님 403, 잘못된 요청 400/404/405/415, 예상 못 한 예외 500
- `QueryCountTest` (4건, `@DataJpaTest`): Hibernate Statistics로 SQL 수 검증. 목록 조회 1번, 상세 조회 1번, 스트리머 삭제 2번(기록 수와 무관). 로컬 PostgreSQL을 쓰고 테스트마다 롤백
- `AdminPropertiesTest` (4건): 관리자 비밀번호에 `{bcrypt}` 같은 인코딩 접두사가 없으면 기동 단계에서 실패
- `StreamercalendarApplicationTests`: `contextLoads` (로컬 PostgreSQL이 떠 있어야 통과)
- 보안 설정은 `support/ImportSecurityConfig` 애너테이션으로 슬라이스 테스트에 올립니다.

## 관리자 계정 설정

서버를 띄우려면 `application-secret.properties`(또는 환경변수 `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` / `APP_ADMIN_REMEMBER_ME_KEY`)에 관리자 계정과 로그인 유지용 키가 있어야 합니다. 값이 비어 있으면 서버가 뜨지 않습니다. (로컬 파일에는 설정 완료)

```
./gradlew hashPassword -Ppassword=원하는비밀번호   # → {bcrypt}$2a$10$... 출력
openssl rand -base64 32                           # → remember-me-key로 쓸 임의 문자열
```

```properties
app.admin.username=admin
app.admin.password={bcrypt}$2a$10$...
app.admin.remember-me-key=(openssl로 만든 값)
```

## 남은 문제 (우선순위순)

### 🟠 P1 — 스펙/품질
1. **정렬 없음.** 목록 순서가 보장되지 않습니다. `broadcastDate ASC, id ASC`처럼 명시적인 정렬이 필요합니다.
2. **인덱스 없음.** 캘린더 조회 패턴에 맞춰 `stream_record(broadcast_date)`, `stream_record(streamer_id, broadcast_date)` 인덱스를 검토해야 합니다.
3. **`month`만 넘어오면 무시됨.** `year` 없이 `month`만 오면 필터가 적용되지 않습니다. 400으로 막을지, 올해 기준으로 처리할지 정해야 합니다.
4. **cascade 정책이 스펙에 없음.** 코드는 스트리머를 삭제하면 방송 기록도 삭제하고, 프론트 확인창도 이에 맞춰 경고합니다. 정책을 확정해 `CLAUDE.md`에 반영해야 합니다.
5. **409 중복 스트리머 미구현.** 정책(이름 unique 여부)을 정한 뒤 unique 제약과 `DUPLICATE_STREAMER` 코드를 추가해야 합니다.
6. **URL 형식·길이 검증 없음.** `vodUrl`, `youtubeUrl`, `profileImageUrl`에 `@URL`이나 길이 제한(`varchar(255)`)이 없어 긴 URL이 들어오면 DB 에러(500)가 납니다.
7. **로그인 시도 횟수 제한 없음.** 브루트포스 방어가 없습니다 (계정 잠금 또는 IP별 요청 제한).

### 🟡 P2 — 개선
8. 엔티티에 `@Setter`가 열려 있음 → 의도한 메서드(`update`, `changeStreamer`)만 남기는 편이 좋습니다.
9. 시간 필드가 `OffsetDateTime.now()`라 서버 타임존을 따릅니다. 스펙은 UTC이므로 `OffsetDateTime.now(ZoneOffset.UTC)`나 `Instant`로 통일하고, JPA Auditing(`@CreatedDate`) 도입을 검토합니다.
10. 서비스 단위 테스트와 Specification 필터·월 경계를 검증하는 리포지토리 테스트가 없습니다. Testcontainers(PostgreSQL) 도입을 권장합니다.
11. `contextLoads`와 `QueryCountTest`가 로컬 DB에 의존해서 CI에서 실패합니다. 테스트 프로파일이나 Testcontainers로 분리해야 합니다.
12. `ddl-auto=update` → Flyway/Liquibase 마이그레이션으로 전환하면 스키마 변경 이력을 관리할 수 있습니다 (포트폴리오 어필 포인트).
13. API 문서화 (springdoc-openapi / Swagger UI)가 없습니다.
14. "로그인 상태 유지" 쿠키는 서버에 저장하지 않는 서명 토큰이라 하나씩 폐기할 수 없습니다. 관리자 비밀번호나 `remember-me-key`를 바꾸면 전부 무효가 됩니다. 개별 폐기가 필요하면 `PersistentTokenBasedRememberMeServices`(DB 저장)로 전환합니다.

## 배포 (Vercel + Render + Neon)

`Dockerfile`, `.dockerignore`, `render.yaml`로 Render(Docker)에 배포했습니다. 포트는 `PORT` 환경변수, CORS origin은 `APP_CORS_ALLOWED_ORIGINS`로 받고, 프록시 헤더(`X-Forwarded-Proto`)를 신뢰해 운영에서 쿠키에 `Secure`가 붙습니다. 컨테이너 로케일은 한국어로 고정해 검증 메시지가 한국어로 나옵니다.

- [x] Neon 프로젝트 (Singapore, PostgreSQL 18). `streamer`, `stream_record` 테이블 생성 확인
- [x] Render Web Service (Singapore, Free) 배포. 운영 주소에서 조회 200, 비로그인 쓰기 401 확인
- [ ] Render `SPRING_JPA_HIBERNATE_DDL_AUTO`를 `validate`로 변경 (테이블 생성 완료)
- [x] Vercel 프론트 배포 (https://streamer-calendar-web.vercel.app), /api 프록시 경유 조회·권한 체크 확인
- [ ] 운영 관리자 계정으로 로그인·등록 흐름 수동 점검

환경변수 (Render Environment 탭, 값은 저장소에 두지 않음)
- `SPRING_DATASOURCE_URL` (`jdbc:postgresql://...?sslmode=require&channelBinding=require`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`(`{bcrypt}` 해시), `APP_ADMIN_REMEMBER_ME_KEY`
- `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`, `SPRING_JPA_HIBERNATE_DDL_AUTO`

배포하며 겪은 문제
- `gradlew` 실행 권한 누락(exit 126): Windows 커밋에서 권한이 빠짐 → git에 `100755` 반영 + Dockerfile에서 `chmod +x`
- 관리자 환경변수 누락 시 기동 실패: 의도한 동작(`@NotBlank`). Key 이름을 정확히(`APP_ADMIN_USERNAME` 등) 입력해야 함

참고
- Render 무료 플랜은 15분 동안 요청이 없으면 잠들어 첫 요청이 30~60초 걸립니다. 재시작되면 세션은 사라지지만 "로그인 상태 유지" 쿠키로 다시 로그인됩니다.
- 이미지 빌드에서는 테스트를 건너뜁니다 (로컬 DB가 필요한 테스트가 있어서). CI를 붙이려면 Testcontainers 전환(P2)이 먼저 필요합니다.

## 결정 필요
- [ ] 스트리머 삭제 cascade 정책 (현재 코드: 함께 삭제)
- [ ] 스트리머 이름 중복 허용 여부
- [ ] `PUT` vs `PATCH`
- [ ] `GET /api/streams` 페이지네이션 필요 여부 (월 단위 조회라 우선 불필요해 보임)

## Phase 2
- [ ] `YOUTUBE_UPLOAD_CANDIDATE` 컬럼 설계 → 엔티티 추가 (상태: `PENDING | MATCHED | IGNORED`)
- [ ] 후보 큐 API: `GET/POST /api/youtube-candidates`, `POST /{id}/match`, `POST /{id}/ignore` (관리자 전용)
- [ ] `PLATFORM_CHANNEL` 엔티티 + `POST /api/platform-channels`
- [ ] `POST /api/crawl-jobs` (크롤러 연동)

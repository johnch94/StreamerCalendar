# 백엔드 작업 진척도 (StreamerCalendar)

> 점검일: 2026-09-28 · 기준 커밋: `9d4d793` (2026-09-28) · 스펙 기준: 루트 `CLAUDE.md` (API 명세 MVP / ERD v0.1)

## 요약

| 항목 | 상태 |
| --- | --- |
| MVP API (Streamer 3종 + Stream Record 5종) | ✅ 구현 완료 |
| 공통 에러 응답 포맷 | 🟡 부분 완료 (일부 예외가 500으로 떨어짐) |
| 엔티티 / ERD 반영 | 🟡 `STREAMER`, `STREAM_RECORD`만 반영 (`PLATFORM_CHANNEL`은 Phase 2) |
| 테스트 코드 | 🟡 `./gradlew test` 14건 통과 (컨트롤러 슬라이스 13건 + `contextLoads` 1건, 서비스/리포지토리 테스트 없음) |
| 설정 / 보안 | 🟡 Security permitAll + CORS만 설정, DB 접속 정보는 gitignore 파일로 분리 완료 |
| Phase 2 (후보 큐, 크롤링 연동) | ⬜ 미착수 |

**진척도(체감): MVP 기준 약 75%.** 기능 코드는 스펙대로 다 있고 테스트 컴파일 문제와 DB 접속 정보 노출(P0)은 해결했습니다. 에러 처리, 쿼리 성능, 테스트 범위 등 실무 완성도가 아직 부족합니다.

## 기술 스택 (실제)

- Spring Boot **4.1.0** / Java 21 (toolchain) / Gradle
- Spring Data JPA, Validation, Web MVC, Security, Lombok, DevTools
- PostgreSQL (`localhost:5432/stream_cal`), `ddl-auto=update`, `open-in-view=false`

## 현재 구현 현황

### 패키지 구조
```
com.example.streamercalendar
├── config/        SecurityConfig (permitAll, CSRF off, CORS: localhost:5173 → /api/**)
├── controller/    StreamerController, StreamRecordController
├── domain/        Streamer, StreamRecord, Platform(enum), Source(enum)
├── dto/           StreamerCreateRequest/Response, StreamRecordRequest/Response, ErrorResponse
├── exception/     GlobalExceptionHandler, ResourceNotFoundException
├── repository/    StreamerRepository, StreamRecordRepository(+JpaSpecificationExecutor)
└── service/       StreamerService, StreamRecordService
```

### API 구현 상태

| API | 상태 | 비고 |
| --- | --- | --- |
| `GET /api/streamers` | ✅ | 정렬 없음 |
| `POST /api/streamers` | ✅ | 201. name 누락 시 400. **중복 409는 미구현** |
| `DELETE /api/streamers/{id}` | ✅ | 204 / 404. `CascadeType.ALL + orphanRemoval` → 연관 방송 기록까지 **함께 삭제됨** |
| `GET /api/streams` | ✅ | `streamerId`/`platform`/`year`/`month` 필터 (Specification). 정렬·페이지네이션 없음 |
| `GET /api/streams/{id}` | ✅ | 404 `STREAM_NOT_FOUND` |
| `POST /api/streams` | ✅ | `source=MANUAL` 서버에서 설정. 스트리머 없으면 404 |
| `PUT /api/streams/{id}` | ✅ | 스트리머 변경도 지원 |
| `DELETE /api/streams/{id}` | ✅ | 204 / 404 |

### 에러 처리
- `ResourceNotFoundException` → 404 + 도메인 코드 (`STREAMER_NOT_FOUND`, `STREAM_NOT_FOUND`)
- `MethodArgumentNotValidException`, `IllegalArgumentException` → 400 `INVALID_REQUEST`
- 그 외 모든 예외 → 500 `INTERNAL_SERVER_ERROR`

### 테스트
- `StreamerControllerTest` (5건), `StreamRecordControllerTest` (8건): `@WebMvcTest` + `@MockitoBean` 슬라이스 테스트
- `StreamercalendarApplicationTests`: `contextLoads` (로컬 PostgreSQL이 떠 있어야 통과)
- 서비스 / 리포지토리 테스트는 없음

## 확인된 문제 (우선순위순)

### 🔴 P0 — 바로 고쳐야 함

1. ~~**테스트 컴파일 실패**~~ → ✅ 2026-09-23 수정
   의존성은 이미 정상이었습니다(`spring-boot-webmvc-test`, Jackson 3.1.4). 테스트 코드의 import만 Boot 4 기준으로 바꿨습니다.
   - `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
   - `com.fasterxml.jackson.databind.ObjectMapper` → `tools.jackson.databind.ObjectMapper`
   - 결과: `./gradlew test` 14건 전부 통과
2. ~~**DB 접속 정보가 `application.properties`에 커밋됨**~~ → ✅ 2026-09-23 수정
   - `application.properties`에서 `spring.datasource.*`를 삭제했고, `spring.config.import=optional:classpath:application-secret.properties`로 읽습니다.
   - `application-secret.properties`는 `.gitignore`에 등록돼 있고, 템플릿은 `application-secret.properties.example`입니다.
   - 환경변수 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`가 있으면 그 값이 우선합니다 (배포 시 사용).
   - ⚠️ **남은 조치:** 이전 비밀번호(`1234`, `postgres123`)는 git 히스토리에 그대로 남아 있고, 해당 커밋(`f72b425`)은 이미 원격(`origin/main`)에 푸시돼 있습니다. 같은 비밀번호를 다른 곳에서 쓰고 있다면 바꿔야 합니다.

### 🟠 P1 — 스펙/품질 이슈

3. **잘못된 입력이 500으로 떨어짐.** 스펙상 400이어야 하는 경우입니다.
   - `?platform=ABC`, `?year=abc` → `MethodArgumentTypeMismatchException`
   - 깨진 JSON, 잘못된 enum 값이나 날짜 형식의 Body → `HttpMessageNotReadableException`
   - `?month=13` → `DateTimeException`
   - 없는 경로 → `NoResourceFoundException`도 500 처리됨 (404여야 함)
   - 500 핸들러에서 로그를 남기지 않아 원인 추적이 어려움
4. **`GET /api/streams` N+1 쿼리.** `StreamRecordResponse.from()`이 LAZY인 `streamer`의 name을 조회해서 레코드 수만큼 추가 쿼리가 나갑니다. `@EntityGraph(attributePaths = "streamer")`나 fetch join으로 해결해야 합니다.
5. **정렬 없음.** 목록 순서가 보장되지 않습니다. `broadcastDate ASC, id ASC`처럼 명시적인 정렬이 필요합니다.
6. **인덱스 없음.** 캘린더 조회 패턴에 맞춰 `stream_record(broadcast_date)`, `stream_record(streamer_id, broadcast_date)` 인덱스를 검토해야 합니다.
7. **`month`만 넘어오면 무시됨.** `year` 없이 `month`만 오면 필터가 적용되지 않습니다. 400으로 막을지, 올해 기준으로 처리할지 정해야 합니다.
8. **cascade 정책이 코드에서 이미 결정돼 있음.** 스펙상 미정이지만 현재는 스트리머를 삭제하면 방송 기록도 삭제됩니다. 프론트 삭제 확인창은 이 동작에 맞춰 "방송 기록 N건도 함께 삭제돼요"라고 경고하도록 바뀌었습니다. 남은 일은 정책을 확정하고 `CLAUDE.md` 스펙에 반영하는 것입니다.
9. **409 중복 스트리머 미구현.** 정책(이름 unique 여부)을 정한 뒤 unique 제약과 `DUPLICATE_STREAMER` 코드를 추가해야 합니다.
10. **URL 형식 검증 없음.** `vodUrl`, `youtubeUrl`, `profileImageUrl`에 `@URL`이나 길이 제한(`varchar(255)`)이 없어 긴 URL이 들어오면 DB 에러(500)가 납니다.

### 🟡 P2 — 개선

11. 엔티티에 `@Setter`가 열려 있음 → 의도한 메서드(`update`, `changeStreamer`)만 남기는 편이 좋습니다.
12. 시간 필드가 `OffsetDateTime.now()`라 서버 타임존을 따릅니다. 스펙은 UTC이므로 `OffsetDateTime.now(ZoneOffset.UTC)`나 `Instant`로 통일하고, JPA Auditing(`@CreatedDate`) 도입을 검토합니다.
13. 서비스 단위 테스트와 `@DataJpaTest`(Specification 필터·월 경계 검증)가 없습니다. Testcontainers(PostgreSQL) 도입을 권장합니다.
14. `contextLoads`가 로컬 DB에 의존해서 CI에서 실패합니다. 테스트 프로파일이나 Testcontainers로 분리해야 합니다.
15. `ddl-auto=update` → Flyway/Liquibase 마이그레이션으로 전환하면 스키마 변경 이력을 관리할 수 있습니다 (포트폴리오 어필 포인트).
16. README가 비어 있음 → 실행 방법, API 요약, ERD 링크를 추가해야 합니다.
17. API 문서화 (springdoc-openapi / Swagger UI)가 없습니다.

## 남은 과제 (체크리스트)

### MVP 마무리
- [x] 테스트 코드 Boot 4 대응 (P0-1) 후 `./gradlew test` 통과 확인
- [x] DB 접속 정보 외부화 (P0-2)
- [ ] 이전 DB 비밀번호 변경 (히스토리에 남아 있음)
- [ ] 400/404 예외 핸들러 보강 + 500 로깅 (P1-3)
- [ ] N+1 해결, 정렬 추가, 인덱스 추가 (P1-4~6)
- [ ] 스트리머 삭제 cascade 정책 확정 → 스펙 반영 (코드와 프론트 문구는 cascade 기준으로 이미 일치)
- [ ] 스트리머 중복(409) 정책 확정 및 구현
- [ ] URL/길이 validation
- [ ] 서비스·리포지토리 테스트 추가
- [ ] README / Swagger

### 스펙상 미결정 사항 (결정 필요)
- [ ] 인증/인가 방식 (현재 permitAll)
- [ ] `PUT` vs `PATCH`
- [ ] `GET /api/streams` 페이지네이션 필요 여부 (월 단위 조회라 우선 불필요해 보임)
- [ ] 배포 방식 (Docker / EC2 등). CORS origin을 프로퍼티로 분리하는 작업 포함

### Phase 2
- [ ] `YOUTUBE_UPLOAD_CANDIDATE` 컬럼 설계 → 엔티티 추가 (상태: `PENDING | MATCHED | IGNORED`)
- [ ] 후보 큐 API: `GET/POST /api/youtube-candidates`, `POST /{id}/match`, `POST /{id}/ignore`
- [ ] `PLATFORM_CHANNEL` 엔티티 + `POST /api/platform-channels`
- [ ] `POST /api/crawl-jobs` (크롤러 연동)

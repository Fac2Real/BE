# BE

팩토리얼 Backend Repository

## 🛠️ 프로젝트 환경 세팅 가이드 (for Backend)

### 💻 개발 환경

- Java 17 (Amazon Corretto)
- Spring Boot 3.4.4
- Gradle
- MySQL
- Kafka, MQTT, AWS SQS, FCM, WebSocket, JPA, Flyway, Swagger, Docker, Jenkins, ArgoCD
- CI/CD: Jenkins + Docker + AWS ECR + ArgoCD
- 모놀리식 구조

---

### 📦 프로젝트 초기 셋업 (처음 클론할 경우)

```bash
./gradlew clean build -x test
./gradlew bootRun
```

---

## 🧩 주요 기능/구현 목록

- **Kafka Consumer**: 센서/웨어러블/설비 토픽 수신, DTO 파싱, 이벤트 위임
- **MQTT Client**: AWS IoT Shadow 연동, 인증서 기반 연결, 재시도/예외처리
- **FCM Push**: Firebase Cloud Messaging, 비동기 푸시, 예외/토큰 검증
- **AWS SQS Listener**: S3 이벤트 등 비동기 메시지 처리
- **메일 발송**: JavaMailSender, 일/월간 리포트 첨부, CSV 변환
- **WebSocket**: STOMP 기반 실시간 알림
- **Flyway**: DB 마이그레이션 자동화
- **Swagger**: API 문서 자동화 (`/swagger-ui.html`)
- **테스트**: JUnit5, Mockito, 통합/단위 테스트

---

# 🛠️ Backend 프로젝트 개발 가이드

## 📦 폴더/패키지 구조 및 역할

| 경로/폴더명                            | 설명                                                             |
| -------------------------------------- | ---------------------------------------------------------------- |
| `src/main/java/com/factoreal/backend/` | 백엔드 전체 소스 루트                                            |
| ├─ `controller/`                       | REST API 엔드포인트, @RestController, @RequestMapping 등         |
| ├─ `domain/`                           | JPA 엔티티, 도메인 서비스, DTO, 레포지토리 등 핵심 비즈니스 로직 |
| ├─ `messaging/`                        | Kafka, MQTT, FCM, SQS, WebSocket 등 메시징 연동 계층             |
| │ ├─ `kafka/consumer/`                 | Kafka Consumer, 메시지 파싱 및 위임                              |
| │ ├─ `mqtt/`                           | MQTT 연결, Shadow Subscription 등                                |
| │ ├─ `fcm/application/`                | FCM 푸시 발송 서비스                                             |
| │ ├─ `sqs/listener/`                   | AWS SQS 이벤트 리스너                                            |
| │ ├─ `common/util/`                    | 메시징 관련 공통 유틸                                            |
| ├─ `global/config/`                    | 전역 설정 (Mail, Swagger, Kafka, MQTT, Firebase 등)              |
| ├─ `global/fileUtil/`                  | CSV 등 파일 유틸리티                                             |
| ├─ `util/`                             | 공통 유틸, 상수, 헬퍼 함수 등                                    |
| `src/main/resources/`                  | 환경설정, 마이그레이션, 인증서, FCM 키 등                        |
| ├─ `application.yml`                   | 메인 환경설정 (yml 사용, properties 미사용)                      |
| ├─ `application-local.yml`             | 로컬 개발용 설정                                                 |
| ├─ `application-cloud.yml`             | 클라우드/운영 환경 설정                                          |
| ├─ `db/migration/`                     | Flyway 마이그레이션 SQL                                          |
| ├─ `fcm_root_key/`                     | FCM 서비스 계정 키 (json)                                        |
| `src/test/java/`                       | 단위/통합 테스트 코드                                            |

---

## ⚙️ 환경 변수 및 설정

- 모든 민감 정보는 환경 변수 또는 `.env` 파일로 관리
- 주요 환경 변수 예시:
  - `MAIL_SERVER_USERNAME`, `MAIL_SERVER_PASSWORD`
  - `FIREBASE_JSON_BASE64`
  - `AWS_IAM_ACCESS_KEY`, `AWS_IAM_SECRET_KEY`
  - `GRAFANA_URL_OUTER`
  - `spring.datasource.*`, `spring.kafka.*`, `spring.mail.*` 등

---

## 🔁 공통 작업 시 주의사항

### 1. `build.gradle`, `settings.gradle` 변경 시

```bash
git pull
./gradlew clean build --refresh-dependencies -x test
```

## 🔧 개발자 환경 동기화 컨벤션

| 파일                     | 수정 시 규칙                                                                                           |
| ------------------------ | ------------------------------------------------------------------------------------------------------ |
| `application.properties` | ⚠️ 반드시 변경 주석 작성<br>예: `# MQTT 브로커 주소 수정 by 승희`                                      |
| `build.gradle`           | ⚠️ 변경 시 팀원들에게 공지 or PR 설명에 명확히 기재                                                    |
| `application.yml`        | ❌ 사용하지 않음. 모든 설정은 `.properties`로 통일                                                     |
| `config/` 클래스         | 새로운 설정 파일 추가 시 파일명은 `XXConfig.java` 로 명명<br>예: `MqttConfig.java`, `KafkaConfig.java` |

---

## ✅ Git 커밋 메시지 컨벤션 (구분자 `|')

```
[type] | sprint | JIRA-KEY | 기능 요약 | 담당자
```

- type: feat, fix, docs, config, refactor, test, chore, style 등
- sprint: sprint0, sprint1, ...
- JIRA-KEY: JIRA이슈번호 또는 없음
- 기능 요약: 핵심 변경 내용
- 담당자: 실명

---

### 📌 예시

```
feat    | sprint0 | 없음 | 센서 등록 API 구현         | 유승희
feat    | sprint0 | IOT-123 | 센서 등록 API 구현         | 유승희
fix     | sprint1 | IOT-210 | MQTT 수신 실패 예외 처리   | 윤다인
config  | sprint0 | IOT-001 | H2 DB 설정 추가            | 김우영
docs    | sprint1 | IOT-999 | README 초안 작성           | 정민석
```

---

### ✅ 항목별 설명

| 항목       | 내용                  | 예시                                                                  |
| ---------- | --------------------- | --------------------------------------------------------------------- |
| `type`     | 작업 유형             | `feat`, `fix`, `docs`, `config`, `refactor`, `test`, `chore`, `style` |
| `sprint`   | 스프린트 구분         | `sprint0`, `sprint1`, `sprint2` 등                                    |
| `JIRA-KEY` | 연동 이슈 번호        | `IOT-123` , `없음`                                                    |
| 기능 요약  | 핵심 변경 내용        | 예: `센서 등록 API 구현`                                              |
| 담당자     | 개발자 실명 or 닉네임 | 예: `유승희`                                                          |

---

### ✅ 추천 커밋 예시 (복붙용)

```bash
git commit -m "feat    | sprint1 | IOT-112 | 작업자 센서 조회 API 추가 | 유승희"
git commit -m "fix     | sprint0 | IOT-009 | H2 연결 오류 수정         | 윤다인"
git commit -m "config  | sprint0 | IOT-000 | Spring Boot 3.4.4 적용    | 김우영"
git commit -m "chore   | sprint1 | IOT-999 | 커밋 컨벤션 README 정리   | 정민석"
```

---

## 🌐 주요 URL

| 유형    | URL                                   |
| ------- | ------------------------------------- |
| Swagger | http://localhost:8080/swagger-ui.html |
| Grafana | (운영 환경) 환경 변수 참조            |

---

## 🔒 보안/주의사항

- `.env`, `src/main/resources/certs/`, FCM 키 등 민감 파일은 git에 커밋 금지
- `.gitignore`에 이미 포함되어 있음
- 환경 변수/비밀키는 운영 서버 또는 CI/CD에서 안전하게 주입

---

## 🧑‍💻 개발/운영 참고

- **새로운 설정 파일**: 반드시 `XXConfig.java` 네이밍
- **테스트 코드**: `src/test/java/`에 JUnit5 기반 작성
- **DB 마이그레이션**: Flyway SQL은 `src/main/resources/db/migration/`에 추가
- **Slack/ArgoCD 연동**: Jenkinsfile 참고

---

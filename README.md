# Fream Backend

Fream은 확장 가능한 웹 서비스를 위한 견고한 인프라를 제공하는 Spring Boot 기반의 백엔드 애플리케이션입니다. 이 프로젝트는 [이전 버전](https://github.com/junhyeong9812/Improve_Fream_Back)에서 더 나아가 배포 자동화와 운영 효율성을 크게 개선한 버전입니다.



**윈도우 배포 완료** | **[📝 1차 프로젝트 회고록](/docs/fream-project-phase1-review.md)**


## 프로젝트 재구성 배경

### 이번 개선의 초점
1. **자동화된 배포 파이프라인**
   - GitHub Actions를 활용한 CI/CD 구축
   - GitHub Container Registry를 통한 체계적인 이미지 관리
   - 무중단 배포 구조 도입

2. **효율적인 개발 프로세스**
   - 기능별 브랜치 전략을 통한 병렬 개발
   - 환경별 설정 분리로 안정적인 운영
   - 모니터링과 로깅 체계 개선

3. **최적화된 인프라 구조**
   - Docker 컨테이너 기반 마이크로서비스 아키텍처
   - AWS 클라우드 인프라 활용
   - 서비스 확장성 고려한 구조 설계


## 🏛 시스템 아키텍처

### 시스템 구성도
![image](https://github.com/user-attachments/assets/3b64669d-0452-456c-8f34-8382c17c63fe)



### 주요 컴포넌트 설명

#### 1. 프론트엔드
- **React 애플리케이션**: 사용자 인터페이스 제공
- **Nginx**: 정적 파일 서빙, 리버스 프록시, 캐싱 레이어

#### 2. 백엔드 (Spring Boot)
- REST API 제공
- 비즈니스 로직 처리
- JWT 기반 인증/인가 (OAuth2 소셜 로그인 지원)
- 외부 서비스 연동 (포트원, 이메일 등)
- WebSocket을 통한 실시간 통신

#### 3. 데이터 저장소
- **MySQL**: 영구 데이터 저장
   - 사용자/상품/주문 정보
   - 트랜잭션 처리
- **Redis**: 캐시 및 세션 관리
   - 접근 빈도가 높은 데이터 캐싱
   - 실시간 데이터 처리
- **Elasticsearch**: 검색 엔진
   - Nori 형태소 분석기를 활용한 한글 검색
   - 상품 검색 기능
   - 로그 데이터 분석

#### 4. 메시지 브로커
- **Kafka & Zookeeper**
   - 이벤트 기반 비동기 처리
   - 시스템 간 느슨한 결합
   - 확장성 있는 데이터 파이프라인

#### 5. DevOps
- **GitHub Actions**: CI/CD 파이프라인
   - 자동 빌드/테스트
   - 도커 이미지 생성 및 배포
- **GitHub Container Registry**: 도커 이미지 저장소
- **Certbot**: SSL 인증서 자동 갱신

#### 6. 모니터링
- **Kibana**: 로그 및 검색 데이터 시각화
- **Metricbeat**: 시스템 및 애플리케이션 메트릭 수집

#### 7. 인프라 (AWS)
- **EC2**: 애플리케이션 서버
- **Route 53**: DNS 관리
- **Docker**: 컨테이너 기반 배포

### 주요 기능 흐름

1. **사용자 인증**
   - JWT 기반 인증
   - OAuth2 소셜 로그인 (Google, Naver, Kakao)
   - Redis를 통한 토큰 관리

2. **상품 관리**
   - MySQL: 상품 기본 정보 저장
   - Elasticsearch: 상품 검색 기능
   - Redis: 인기 상품 캐싱

3. **주문/결제 처리**
   - 포트원 결제 시스템 연동
   - Kafka를 통한 비동기 처리
   - 트랜잭션 관리

4. **실시간 처리**
   - Kafka를 통한 이벤트 처리
   - WebSocket을 통한 실시간 알림

### 배포 환경
- **개발 환경**: 로컬 Docker 컨테이너
- **운영 환경**: AWS EC2 + Docker
- **도메인**: www.pinjun.xyz

## 🛠 기술 스택

### 백엔드
- **프레임워크:** Spring Boot 3.4.1
- **개발 언어:** Java 17
- **빌드 도구:** Gradle 8.x
- **ORM:** Spring Data JPA + QueryDSL
- **보안:** Spring Security, JWT

### 데이터베이스 및 캐싱
- **주 데이터베이스:** MySQL 8.0
- **캐시:** Redis 7.0
- **검색 엔진:** Elasticsearch 8.13.4 (Nori 형태소 분석기 포함)

### 메시징
- **이벤트 브로커:** Apache Kafka 7.4.0
- **클러스터 관리:** Zookeeper 7.4.0

### 웹서버 및 프록시
- **웹서버:** Nginx 1.25.2 (캐시 퍼지 모듈 포함)
- **SSL:** Let's Encrypt (Certbot)

### 모니터링 및 로깅
- **시각화:** Kibana 8.10.2
- **메트릭 수집:** Metricbeat 8.13.4
- **로그 수집:** Spring Boot Actuator

### 기타 도구
- **컨테이너화:** Docker
- **CI/CD:** GitHub Actions
- **이미지 저장소:** GitHub Container Registry
- **이메일:** Spring Mail + Gmail SMTP

## 프로젝트 구조

프로젝트는 다음과 같은 구조로 구성되어 있습니다:

```
fream-back/
├── docker/                # 도커 설정 파일들
│   ├── dev/              # 개발 환경 도커 설정
│   │   └── nginx/
│   ├── prod/             # 운영 환경 도커 설정
│   │   └── nginx/
│   │       ├── nginx.conf          # Nginx 설정
│   │       └── certbot/            # SSL 인증서 관리
│   ├── elasticsearch/    # Elasticsearch 도커 설정
│   │   ├── userdict_ko.txt         # 한글 사용자 사전
│   │   └── synonyms.txt            # 동의어 사전
│   ├── kafka/           # Kafka & Zookeeper 도커 설정
│   ├── mysql/           # MySQL 도커 설정
│   └── redis/           # Redis 도커 설정
├── src/
│   ├── main/
│   │   ├── java/com/fream/
│   │   │   ├── config/              # 애플리케이션 설정
│   │   │   ├── controller/          # API 엔드포인트
│   │   │   ├── domain/              # 도메인 모델
│   │   │   ├── repository/          # 데이터 접근 레이어
│   │   │   ├── service/             # 비즈니스 로직
│   │   │   ├── security/            # 인증 및 보안
│   │   │   ├── exception/           # 예외 처리
│   │   │   ├── elasticsearch/       # 검색 엔진 관련
│   │   │   ├── kafka/               # 메시지 큐 관련
│   │   │   ├── util/                # 유틸리티 클래스
│   │   │   └── FreamApplication.java # 진입점
│   │   └── resources/
│   │       ├── application.yml           # 공통 설정
│   │       ├── application-local.yml     # 로컬 환경 설정
│   │       └── application-prod.yml      # 운영 환경 설정
│   └── test/                       # 테스트 코드
├── .github/workflows/             # GitHub Actions CI/CD 파이프라인
├── build.gradle                   # Gradle 빌드 설정
└── Dockerfile                     # 애플리케이션 도커 이미지 빌드
```

## 🚀 시작하기

### 로컬 개발 환경 설정

1. 레포지토리 클론:
   ```bash
   git clone https://github.com/your-username/fream-back.git
   cd fream-back
   ```

2. 환경 설정 파일 준비:
   - `.env.example`을 `.env`로 복사하고 필요한 환경 변수 설정
   - `application-local.yml.example`을 `application-local.yml`로 복사하고 필요한 설정값 업데이트

3. Docker를 사용하여 로컬 종속성 시작:
   ```bash
   cd docker/dev
   docker-compose -f docker-compose.dev.yml up -d
   ```

4. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

5. 애플리케이션 접속:
   - 웹 브라우저에서 http://localhost:8080 접속
   - Swagger API 문서: http://localhost:8080/swagger-ui.html

### 운영 환경 배포

프로젝트는 GitHub Actions를 통한 CI/CD 파이프라인을 사용합니다:

1. main 브랜치에 push하면 자동으로 다음 과정이 실행됩니다:
   - 빌드 및 테스트
   - Docker 이미지 생성
   - GitHub Container Registry에 푸시
   - 운영 서버에 배포

2. 운영 환경 수동 배포 (필요시):
   ```bash
   # 환경 변수 설정
   cp .env.example .env
   # .env 파일 편집하여 실제 값으로 설정

   # 운영 환경 컨테이너 시작
   cd docker/prod
   docker-compose -f docker-compose.prod.yml up -d
   ```

3. 배포 후 확인:
   - 서비스 상태 확인: `docker-compose -f docker-compose.prod.yml ps`
   - 로그 확인: `docker-compose -f docker-compose.prod.yml logs -f app`
   - 헬스 체크: `curl -k https://www.pinjun.xyz/api/actuator/health`

## 📋 도메인 설계

프로젝트는 다음과 같은 주요 도메인으로 구성되어 있습니다:

### 핵심 도메인

1. **User 도메인**
   - User: 사용자 기본 정보 관리
   - Profile: 사용자 프로필 정보
   - Follow: 사용자 간 팔로우 관계
   - Point: 포인트 시스템
   - BankAccount: 판매 정산 계좌 정보

2. **Product 도메인**
   - Product: 상품 기본 정보
   - ProductColor: 상품 색상별 정보
   - ProductSize: 사이즈별 정보
   - Brand/Category/Collection: 상품 분류 체계
   - Interest: 관심 상품 정보

3. **Order/Sale 도메인**
   - Order: 구매 주문 정보
   - OrderBid: 구매 입찰 정보
   - Sale: 판매 정보
   - SaleBid: 판매 입찰 정보

4. **Style 도메인**
   - Style: 스타일 게시글
   - StyleComment: 댓글 시스템
   - StyleLike: 좋아요 기능
   - MediaUrl: 미디어 파일 관리

### 지원 도메인

1. **Payment 도메인**
   - Payment: 결제 기본 정보
   - CardPayment/AccountPayment: 결제 수단별 처리

2. **Shipment 도메인**
   - OrderShipment: 구매자 배송 정보
   - SellerShipment: 판매자 배송 정보

3. **기타 도메인**
   - Notice: 공지사항 관리
   - FAQ: 자주 묻는 질문
   - Event: 이벤트 관리
   - UserAccessLog: 사용자 접근 기록

### ERD 다이어그램

#### 1. User 도메인
![User 도메인 ERD](https://github.com/user-attachments/assets/45faa0b9-14ed-4ad5-8f77-5d0f8365bed7)

#### 2. Product 도메인
![Product 도메인 ERD](https://github.com/user-attachments/assets/ef07520b-c06d-4aef-8e05-fe8c2708d14b)

#### 3. Order/Sale/Style 도메인
![Order/Sale/Style 도메인 ERD](https://github.com/user-attachments/assets/4ac3f22c-c882-435c-9642-2b098a555f13)

## 🔌 주요 의존성 및 라이브러리

```gradle
dependencies {
    // Spring Boot 핵심 의존성
    implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-data-redis'
	implementation 'org.springframework.boot:spring-boot-starter-websocket'
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.boot:spring-boot-starter-batch'
	implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
	implementation 'org.springframework.boot:spring-boot-starter-mail'
	implementation 'org.springframework.kafka:spring-kafka'
	implementation 'org.apache.httpcomponents.client5:httpclient5'
    // 테스트 의존성
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
    testImplementation 'org.springframework.batch:spring-batch-test'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // 데이터베이스
    runtimeOnly 'com.h2database:h2'  // 개발/테스트용
    runtimeOnly 'mysql:mysql-connector-java:8.0.33'  // 운영용
    
    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
    
    // 유틸리티 및 보안
    implementation 'com.auth0:java-jwt:3.18.2'
    implementation 'org.jsoup:jsoup:1.16.1'
    implementation 'co.elastic.clients:elasticsearch-java:8.9.1'
}
```

## 🛡 보안 및 인증

애플리케이션에 구현된 보안 조치:

- **인증 방식**: JWT 기반 인증 시스템
- **소셜 로그인**: OAuth2를 통한 Google, Naver, Kakao 통합
- **암호화**: 사용자 비밀번호 안전한 해싱
- **HTTPS**: Let's Encrypt 인증서를 통한 전송 계층 보안
- **CORS**: 안전한 크로스 오리진 요청 정책
- **CSRF**: 교차 사이트 요청 위조 보호
- **토큰 관리**: Redis를 활용한 효율적인 JWT 관리

## 📦 인프라 및 배포

### 컨테이너화

모든 서비스는 Docker를 통해 컨테이너화되어 있어 일관된 환경을 보장합니다:

- **애플리케이션**: Java 17 + Spring Boot
- **웹서버**: Nginx (캐시 퍼지 모듈 포함)
- **데이터베이스**: MySQL 8.0
- **캐시**: Redis 7.0
- **검색**: Elasticsearch 8.13.4 + Kibana 8.10.2
- **메시징**: Kafka 7.4.0 + Zookeeper 7.4.0
- **SSL**: Certbot (Let's Encrypt)

### AWS 인프라 구성

- **EC2**: t3.medium 인스턴스 (운영 환경)
- **EBS**: 애플리케이션 및 데이터 저장용 볼륨
- **Route 53**: 도메인 관리 (www.pinjun.xyz)

### 최적화 및 성능

- **Nginx 캐싱**: 자주 사용되는 API 응답 캐싱 (상품 목록, 검색 결과 등)
- **Redis 캐싱**: 세션 및 자주 접근하는 데이터 인메모리 캐싱
- **DB 인덱싱**: 최적화된 MySQL 쿼리 성능을 위한 인덱스 설계
- **Elasticsearch**: 빠른 검색 성능을 위한 인덱스 최적화

## 🔄 CI/CD

GitHub Actions를 통해 자동화된 CI/CD 파이프라인을 구축했습니다:

### 워크플로우

1. **빌드 및 테스트**:
   - Java 17 환경에서 Gradle 빌드
   - 단위 테스트 및 통합 테스트 실행

2. **Docker 이미지 생성**:
   - 애플리케이션 및 의존성 서비스 이미지 빌드
   - GitHub Container Registry에 이미지 푸시

3. **배포**:
   - AWS EC2 인스턴스에 SSH 접속
   - 최신 이미지 풀 및 서비스 재시작
   - 롤백 메커니즘 구현 (배포 실패 시)

### 무중단 배포

- Blue-Green 배포 전략을 통한 서비스 연속성 보장
- 자동화된 롤백 매커니즘으로 안정성 확보

## 📊 모니터링 및 로깅

### Elasticsearch + Kibana

- 중앙 집중식 로그 수집 및 분석
- 사용자 행동 패턴 및 시스템 성능 시각화
- 알림 설정을 통한 이상 징후 감지

### Metricbeat

- 시스템 및 애플리케이션 메트릭 수집
- 자원 사용량 모니터링 (CPU, 메모리, 디스크, 네트워크)
- 성능 병목 현상 식별 및 분석

### Spring Actuator

- 애플리케이션 건강 상태 모니터링
- 런타임 메트릭 수집
- API 응답 시간 및 오류율 추적


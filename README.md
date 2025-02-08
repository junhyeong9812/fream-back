# Fream Backend

Fream은 확장 가능한 웹 서비스를 위한 견고한 인프라를 제공하는 Spring Boot 기반의 백엔드 애플리케이션입니다. 
## 🏛 시스템 아키텍처

### 시스템 구성도
![image](https://github.com/user-attachments/assets/7393d224-9af2-48c0-9ce0-f65d96432659)


### 주요 컴포넌트 설명

#### 1. 프론트엔드
- **React 애플리케이션**: 사용자 인터페이스 제공
- **Nginx**: 정적 파일 서빙 및 리버스 프록시

#### 2. 백엔드 (Spring Boot)
- REST API 제공
- 비즈니스 로직 처리
- JWT 기반 인증/인가
- 외부 서비스 연동 (포트원, 이메일 등)

#### 3. 데이터 저장소
- **MySQL**: 영구 데이터 저장
  - 사용자/상품/주문 정보
  - 트랜잭션 처리
- **Redis**: 캐시 및 세션 관리
  - 접근 빈도가 높은 데이터 캐싱
  - 실시간 데이터 처리
- **Elasticsearch**: 검색 엔진
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

#### 6. 인프라 (AWS)
- **EC2**: 애플리케이션 서버
- **Route 53**: DNS 관리
- **Docker**: 컨테이너 기반 배포

### 주요 기능 흐름

1. **사용자 인증**
   - JWT 기반 인증
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

### 데이터 흐름
1. 클라이언트의 요청이 Nginx를 통해 백엔드로 전달
2. Spring Boot 애플리케이션에서 요청 처리
3. 필요한 데이터를 각 저장소에서 조회/저장
4. 비동기 처리가 필요한 경우 Kafka로 이벤트 발행
5. 처리 결과를 클라이언트에 반환

### 배포 환경
- **개발 환경**: 로컬 Docker 컨테이너
- **운영 환경**: AWS EC2 + Docker
- **도메인**: www.pinjun.xyz

  
## 🛠 기술 스택

- **프레임워크:** Spring Boot 3.4.1
- **개발 언어:** Java 17
- **빌드 도구:** Gradle
- **데이터베이스:** 
  - 주 데이터베이스: MySQL
  - 캐시: Redis
  - 검색 엔진: Elasticsearch
- **메시지 브로커:** Apache Kafka
- **인프라:** Docker, GitHub Actions (CI/CD)
- **보안:** Spring Security, JWT

## 🏗 프로젝트 구조

프로젝트는 다음과 같은 주요 컴포넌트로 구성된 모듈식 아키텍처를 따릅니다:

```
fream-back/
├── docker/                # 도커 설정 파일들
│   ├── dev/              # 개발 환경 도커 설정
│   │   └── nginx/
│   ├── prod/             # 운영 환경 도커 설정
│   │   └── nginx/
│   ├── elasticsearch/    # Elasticsearch 도커 설정
│   ├── kafka/           # Kafka 도커 설정
│   ├── mysql/           # MySQL 도커 설정
│   └── redis/           # Redis 도커 설정
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── application.yml           # 공통 설정
│   │       ├── application-local.yml     # 로컬 환경 설정
│   │       └── application-prod.yml      # 운영 환경 설정
│   └── test/
└── build.gradle          # 프로젝트 빌드 설정
```

## 🚀 시작하기

### 로컬 개발 환경 설정

1. 레포지토리 클론:
   ```bash
   git clone https://github.com/your-username/fream-back.git
   cd fream-back
   ```

2. 로컬 환경 설정:
   - `application-local.yml.example`을 `application-local.yml`로 복사
   - 필요한 설정값 업데이트

3. Docker를 사용하여 로컬 종속성 시작:
   ```bash
   cd docker/dev
   docker-compose -f docker-compose.dev.yml up -d
   ```

4. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

### 운영 환경 배포

프로젝트는 GitHub Actions를 통한 CI/CD 파이프라인을 사용합니다:

1. main 브랜치에 push하면 자동으로 다음 과정이 실행됩니다:
   - 빌드
   - 테스트
   - Docker 이미지 생성
   - GitHub Container Registry에 푸시
   - 운영 서버에 배포

2. 운영 환경 배포는 Docker Compose를 사용합니다:
   ```bash
   cd docker/prod
   docker-compose -f docker-compose.prod.yml up -d
   ```

## 🌍 환경 설정

애플리케이션은 Spring profiles를 통해 다양한 환경을 지원합니다:

- **로컬:** H2 데이터베이스와 로컬 서비스 사용
- **운영:** MySQL과 컨테이너화된 서비스 사용

운영 환경에 필요한 주요 환경 변수:
- `DB_USERNAME`: 데이터베이스 사용자 이름
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 시크릿 키
- `MAIL_USERNAME`: 메일 서버 사용자 이름
- `MAIL_PASSWORD`: 메일 서버 비밀번호
- `IMP_KEY`: 아임포트 키
- `IMP_SECRET`: 아임포트 시크릿
- `IMP_PG`: PG사 정보
- `IMP_STOREID`: 상점 ID

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
![image](https://github.com/user-attachments/assets/45faa0b9-14ed-4ad5-8f77-5d0f8365bed7)


#### 2. Product 도메인
![image](https://github.com/user-attachments/assets/ef07520b-c06d-4aef-8e05-fe8c2708d14b)


#### 3. Order/Sale/Style 도메인
![image](https://github.com/user-attachments/assets/4ac3f22c-c882-435c-9642-2b098a555f13)


### 주요 도메인 관계

1. **상품 거래 관계**
   - Product -> ProductColor -> ProductSize: 상품의 계층 구조
   - Order <-> Sale: 구매와 판매의 매칭
   - OrderBid <-> SaleBid: 구매 입찰과 판매 입찰의 매칭

2. **사용자 관계**
   - User -> Profile: 1:1 관계
   - Profile <-> Follow -> Profile: 팔로우 관계
   - User -> Point: 포인트 이력 관리

3. **스타일 관련 관계**
   - Profile -> Style: 스타일 게시글 작성
   - Style -> StyleComment: 댓글 시스템
   - Style -> StyleOrderItem -> OrderItem: 스타일에 사용된 상품 정보

## 🔗 주요 의존성

프로젝트에서 사용하는 주요 라이브러리:
- Spring Boot Starters (Web, JPA, Security, Batch)
- QueryDSL (동적 쿼리 생성)
- JWT Authentication (인증)
- Elasticsearch Client (검색 엔진)
- Kafka (메시지 브로커)
- Redis (캐시)
- MySQL Connector
- Lombok (보일러플레이트 코드 감소)

## 🛡 보안

애플리케이션에 구현된 보안 조치:
- JWT 기반 인증
- Spring Security 설정
- 안전한 비밀번호 처리
- 운영 환경의 HTTPS (Certbot 사용)

## 📦 인프라

애플리케이션은 다음과 같은 서비스들을 Docker로 컨테이너화하여 사용합니다:
- Nginx (웹 서버)
- MySQL (데이터베이스)
- Redis (캐시)
- Kafka (메시지 브로커)
- Elasticsearch (검색 엔진)
- Zookeeper (Kafka 의존성)
- Certbot (SSL 인증서 관리)

각 서비스는 `docker-compose.yml` 파일을 통해 구성되며, 개발 환경과 운영 환경에서 각각 다른 설정을 사용합니다.

## 🔄 CI/CD

GitHub Actions를 통해 자동화된 CI/CD 파이프라인을 구축했습니다:
- main 브랜치 푸시 시 자동 빌드 및 테스트
- GitHub Container Registry를 통한 Docker 이미지 관리
- EC2 환경에 자동 배포


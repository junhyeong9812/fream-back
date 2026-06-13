# 베이스 이미지
# openjdk:17-jdk-slim 은 Docker Hub에서 제거됨(openjdk 공식 이미지 deprecated) → Eclipse Temurin으로 교체
FROM eclipse-temurin:17-jdk-jammy

# Playwright/Chromium 의존 라이브러리 등 (필요시)
RUN apt-get update && apt-get install -y \
    libgtk-3-0 libgbm-dev libnotify-dev libnss3 libxss1 libasound2 \
    fonts-liberation libappindicator3-1 xdg-utils \
    && rm -rf /var/lib/apt/lists/*

# 빌드 산출물(app.jar)을 복사
# 예: Gradle 빌드 산출물이 build/libs/fream-back-0.0.1-SNAPSHOT.jar
ARG JAR_FILE=build/libs/fream-back-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]


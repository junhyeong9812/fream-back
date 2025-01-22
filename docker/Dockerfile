# 베이스 이미지
FROM openjdk:17-jdk-slim

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

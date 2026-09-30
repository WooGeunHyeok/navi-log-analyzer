# ---------- 1단계: 빌드 ----------
# JDK 이미지에서 Gradle Wrapper로 실행 가능한 jar(bootJar)를 만든다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 관련 파일만 먼저 복사 → 소스가 바뀌어도 의존성 다운로드 레이어는 캐시 재사용
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# ---------- 2단계: 실행 ----------
# 빌드 도구가 필요 없으므로 가벼운 JRE 이미지에 jar만 가져온다.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

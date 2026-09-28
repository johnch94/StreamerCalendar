# ---- 빌드 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 먼저 받아서 레이어 캐시 (소스만 바뀌면 이 단계는 재사용)
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# Windows에서 커밋하면 실행 권한이 빠질 수 있어 명시적으로 부여
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src src
# 테스트는 로컬 PostgreSQL이 필요한 것이 있어 이미지 빌드에서는 제외 (로컬에서 ./gradlew test로 확인)
RUN ./gradlew bootJar -x test --no-daemon -q

# ---- 실행 ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home spring
USER spring

COPY --from=build /app/build/libs/*.jar app.jar

# Render 무료 플랜(512MB)에 맞춰 힙을 컨테이너 메모리 비율로 제한
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY src src
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

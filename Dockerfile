FROM maven:3.10-eclipse-temurin-25 AS build

WORKDIR /build
COPY pom.xml .
RUN mvn --batch-mode dependency:go-offline

COPY src ./src
RUN mvn --batch-mode clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app
RUN useradd --system --no-create-home appuser \
    && mkdir -p /app/logs \
    && chown appuser:appuser /app/logs
USER appuser

COPY --from=build /build/target/app.jar app.jar

EXPOSE 7070
ENTRYPOINT ["java", "-jar", "app.jar"]

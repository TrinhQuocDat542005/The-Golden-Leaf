FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl libssl3 libfreetype6 \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app
COPY --from=build /workspace/target/datban-*.jar /app/app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

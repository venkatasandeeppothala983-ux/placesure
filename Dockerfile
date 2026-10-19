# Java 17 build; no local JAR, credentials, or database files enter the image.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
COPY frontend ./frontend
COPY docs ./docs
COPY database/database.sql ./database/database.sql
COPY README.md ./
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --uid 10001 --create-home --shell /usr/sbin/nologin placesure
COPY --from=build /workspace/target/placesure.jar /app/placesure.jar
COPY deploy/start.sh /app/start.sh
RUN chmod 0555 /app/start.sh && chmod 0444 /app/placesure.jar
ENV BIND_HOST=0.0.0.0 \
    APP_JAR=/app/placesure.jar \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=20.0"
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/start.sh"]

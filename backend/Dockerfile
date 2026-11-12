FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S cssms && adduser -S cssms -G cssms && mkdir -p /app/uploads && chown -R cssms:cssms /app
WORKDIR /app
COPY --from=build /build/target/cssms-backend-*.jar app.jar
USER cssms
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75" \
    UPLOAD_DIR=/app/uploads
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

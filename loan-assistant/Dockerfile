# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/loan-assistant-1.0.0.jar app.jar
RUN mkdir -p /data/uploads && chown -R app:app /data
USER app
ENV UPLOAD_DIR=/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

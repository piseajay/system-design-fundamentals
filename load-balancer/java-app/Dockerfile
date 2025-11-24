# -------- Build stage --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -e -DskipTests package

# -------- Run stage --------
FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=java-app-0.0.1-SNAPSHOT.jar
COPY --from=build /app/target/${JAR_FILE} app.jar
# Environment-driven port selection
ENV SERVER_PORT=8081
ENV INSTANCE_ID="unset-instance"
EXPOSE 8081 8082 8083
# Pass dynamic port via JVM system property
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${SERVER_PORT}"]

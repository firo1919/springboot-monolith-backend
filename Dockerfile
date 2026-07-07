FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

# Copy the POM first so dependency resolution is cached when only sources change
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests

COPY src ./src

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine AS runner

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --chown=spring:spring --from=builder /app/target/monolith-0.0.1-SNAPSHOT.jar ./app.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]

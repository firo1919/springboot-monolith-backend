FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY src ./src

COPY pom.xml .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

COPY --from=builder ./app/target/monolith-0.0.1-SNAPSHOT.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
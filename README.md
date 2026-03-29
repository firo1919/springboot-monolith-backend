# Monolith Spring Boot Backend

A production-ready Spring Boot monolith template focused on authentication, role-based authorization, OTP flows, file upload pre-signing, and infrastructure-friendly local development.

## Table of Contents

- [Overview](#overview)
- [Core Features](#core-features)
- [Production Readiness Checklist](#production-readiness-checklist)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Development with Dev Container](#development-with-dev-container)
- [Environment Configuration](#environment-configuration)
- [Run the Project](#run-the-project)
    - [Option A: Local Java Run (Recommended for Development)](#option-a-local-java-run-recommended-for-development)
    - [Option B: Dockerized App Run](#option-b-dockerized-app-run)
- [API Documentation](#api-documentation)
- [Authentication and Authorization](#authentication-and-authorization)
- [Testing](#testing)
- [Build and Packaging](#build-and-packaging)
- [Troubleshooting](#troubleshooting)
- [Security Notes](#security-notes)
- [Contributing](#contributing)

## Overview

This backend exposes versioned REST APIs under `/api/v1/**` and includes:

- JWT-based stateless authentication
- OTP verification and resend flows
- Refresh token and logout flow
- Role-based access control (Admin, Employee)
- Pre-signed S3-compatible upload ticket generation
- OpenAPI/Swagger UI documentation
- Test setup using JUnit + Testcontainers + H2

## Core Features

- Authentication endpoints for admin registration, login, OTP confirmation/resend, refresh, and logout
- Upload endpoint to generate pre-signed upload tickets
- Spring Security resource server configuration with JWT (HS256)
- CORS configuration using environment-provided allowed origins
- Postgres-based persistence for runtime
- Mail support (Mailhog locally, SMTP for production)
- S3-compatible object storage support (configured for RustFS/MinIO-style endpoints)

## Feature Checklist

- [x] JWT-based stateless authentication
- [x] Role-based authorization
- [x] OTP confirmation and resend flow
- [x] Refresh token and logout endpoints
- [x] Environment-variable based configuration (`.env` driven)
- [x] CORS allowlist configuration
- [x] OpenAPI/Swagger documentation (`/docs`, `/v3/api-docs`)
- [x] Database integration with PostgreSQL (runtime)
- [x] Integration, Unit and End-to-End testing with Testcontainers, RestTestClient, and H2
- [x] Local developer stack via Docker Compose (Postgres, Mailhog, S3-compatible storage)
- [x] Global exception handling layer
- [x] Containerized app build and run flow
- [ ] Liveness and readiness health groups configured for orchestration probes
- [ ] Actuator endpoints for monitoring and metrics
- [ ] Database migration tooling (Flyway or Liquibase) replacing ad-hoc schema updates
- [ ] Secrets management integration (Vault, cloud secret manager, or Kubernetes secrets)
- [ ] Rate limiting / abuse protection for auth endpoints
- [ ] CI pipeline with linting, tests, and build verification on pull requests

## Tech Stack

- Java 25 (project target)
- Spring Boot 4.0.4
- Spring Web MVC
- Spring Security OAuth2 Resource Server
- Spring Data JPA
- PostgreSQL
- SpringDoc OpenAPI + Swagger UI
- Spring Cloud AWS S3 Starter
- MapStruct
- Lombok
- Testcontainers + JUnit 5
- Docker + Docker Compose

## Project Structure

```text
.
├── src/main/java/com/firomsa/monolith
│   ├── config
│   ├── exception
│   ├── model
│   ├── repository
│   ├── security
│   └── v1
│       ├── controller
│       ├── dto
│       ├── mapper
│       └── service
├── src/main/resources
│   └── application.properties
├── src/test
│   ├── java
│   └── resources/application.properties
├── docker-compose-dev.yaml
├── docker-compose.yaml
├── Dockerfile
└── pom.xml
```

## Prerequisites

Install the following tools:

- Java 25 (for local Maven builds)
- Docker + Docker Compose plugin
- Optional: Maven (or use the included Maven wrapper `./mvnw`)

## For local dev

### Development with Dev Container

You can run this project fully inside a VS Code Dev Container.

Steps:

1. Install the VS Code Dev Containers extension.
2. Open the repository in VS Code.
3. Run: `Dev Containers: Reopen in Container`.
4. Wait for the post-create command to finish (`mvn clean install -DskipTests`).
5. Inside the container terminal, continue with the normal setup:

```bash
cp example.env .env
docker compose --env-file .env -f docker-compose-dev.yaml up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

This gives you a consistent development environment without needing to install Java/Maven locally.

### On Local Machine

1. Copy env template:

```bash
cp example.env .env
```

then

```bash
docker network create monolith_network || true
docker compose --env-file .env -f docker-compose-dev.yaml up -d
```

This starts:

- PostgreSQL on `5432`
- Mailhog SMTP/UI on `1025` / `8025`
- RustFS S3 API/Console on `9000` / `9001`
- SQL Studio on `3030`

next

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

App runs on:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/docs`

Stop all:

```bash
docker compose -f docker-compose-dev.yaml down
```

## API Documentation

OpenAPI/Swagger is exposed at:

- UI: `GET /docs`
- Spec: `GET /v3/api-docs`

## Authentication and Authorization

### Public Routes

- `/api/v1/auth/**`
- `/docs`
- `/v3/api-docs/**`
- Swagger resource paths

### Protected Routes

- `/api/v1/admin/**` requires `SCOPE_ADMIN`
- `/api/v1/employee/**` requires `SCOPE_EMPLOYEE`
- Any other route requires authentication

### Main Auth Endpoints

Base path: `/api/v1/auth`

- `POST /admins`
- `POST /confirm-otp`
- `POST /resend-otp`
- `POST /login`
- `POST /refresh`
- `POST /logout`

### Upload Endpoint

Base path: `/api/v1/uploads`

- `POST /presign`

## Testing

Run all tests:

```bash
./mvnw test
```

Notes:

- Repository integration tests use Testcontainers PostgreSQL (`postgres:18-alpine`).
- Test profile also includes H2 config in `src/test/resources/application.properties`.

## Build and Packaging

Build jar:

```bash
./mvnw clean package -DskipTests
```

Built artifact:

- `target/monolith-0.0.1-SNAPSHOT.jar`

Run packaged jar:

```bash
java -jar target/monolith-0.0.1-SNAPSHOT.jar
```

## Contributing

1. Create a feature branch.
2. Make your changes and add tests.
3. Run `./mvnw test` and ensure green build.
4. Open a pull request with a concise change summary.

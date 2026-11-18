# Calculadora RTC (regime regular) as a private service.
#
# The SQLite database is built here, from the Flyway scripts of the same commit, so the image
# always carries the tax tables of the code it runs; the application opens it read-only. Tests
# run in CI (they need the same database), not in this build.

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode --quiet dependency:go-offline

COPY flyway flyway
RUN mkdir -p calculadora/db \
    && ./mvnw --batch-mode --quiet flyway:migrate -Dflyway.configFiles=./flyway/flyway-pro.conf

COPY src src
RUN ./mvnw --batch-mode --quiet package -DskipTests

FROM eclipse-temurin:21-jre

RUN groupadd --system calculadora && useradd --system --gid calculadora --home /app calculadora

WORKDIR /app

COPY --from=builder --chown=root:calculadora /build/target/api-regime-geral.jar app.jar
COPY --from=builder --chown=root:calculadora /build/calculadora/db/calculadora-pro.db calculadora/db/calculadora-pro.db
RUN chmod 0444 calculadora/db/calculadora-pro.db

USER calculadora

ENV TZ=America/Sao_Paulo
EXPOSE 8080

# The working directory must be /app: the offline profile opens ./calculadora/db.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Dspring.profiles.active=offline", "-jar", "app.jar"]

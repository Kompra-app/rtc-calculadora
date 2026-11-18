# Calculadora RTC (regime regular) as a private service.
#
# The SQLite database is the one the Receita Federal ships in its offline package
# (official-db/, see VERSION there): the Flyway scripts on GitHub stop at V0059, while the
# package carries newer tax tables. The build refuses a database whose checksum differs from
# the one recorded in VERSION. The application opens it read-only. Tests run in CI against the
# database Flyway generates, not in this build.

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode --quiet dependency:go-offline

COPY official-db official-db
RUN . official-db/VERSION \
    && echo "${DB_SHA256}  official-db/calculadora-pro.db" | sha256sum --check --strict

COPY src src
RUN ./mvnw --batch-mode --quiet package -DskipTests

FROM eclipse-temurin:21-jre

RUN groupadd --system calculadora && useradd --system --gid calculadora --home /app calculadora

WORKDIR /app

COPY --from=builder --chown=root:calculadora /build/target/api-regime-geral.jar app.jar
COPY --from=builder --chown=root:calculadora /build/official-db/calculadora-pro.db calculadora/db/calculadora-pro.db
RUN chmod 0444 calculadora/db/calculadora-pro.db

USER calculadora

ENV TZ=America/Sao_Paulo
EXPOSE 8080

# The working directory must be /app: the offline profile opens ./calculadora/db.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Dspring.profiles.active=offline", "-jar", "app.jar"]

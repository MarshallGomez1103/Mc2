FROM maven:3.9.16-eclipse-temurin-17-noble AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
COPY test ./test
RUN mvn -B -ntp package

FROM eclipse-temurin:17.0.20.1_1-jre-noble
WORKDIR /app

RUN useradd --system --create-home app \
    && mkdir -p /app/worlds \
    && chown app:app /app/worlds

COPY --from=build /build/target/minecraft2-0.1.0-SNAPSHOT.jar app.jar

USER app
ENTRYPOINT ["java", "-Dmc2.console=true", "-Dmc2.gpu=AUTO", "-Dmc2.worlds.dir=/app/worlds", "-jar", "/app/app.jar"]

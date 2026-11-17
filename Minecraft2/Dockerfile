# Imagen de mc2-server: el JAR en modo servidor headless (protocolo v1 en 7777/TCP, /health y /metrics en 8080).
# Construcción desde la raíz del repositorio: docker build -f Minecraft2/Dockerfile Minecraft2
FROM maven:3.9.16-eclipse-temurin-17-noble AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
COPY test ./test
# Las pruebas corren en ci.yml; aquí solo se empaqueta el JAR.
RUN mvn -B -ntp -DskipTests -DskipUnitTests=true -Djacoco.skip=true package \
    && cp target/minecraft2-*.jar /build/app.jar

FROM eclipse-temurin:17.0.20.1_1-jre-noble
WORKDIR /app

ARG MC2_VERSION=dev
ARG MC2_REVISION=desconocida
LABEL org.opencontainers.image.title="mc2-server" \
      org.opencontainers.image.description="Servidor headless de Minecraft2 (protocolo v1, /health y /metrics)" \
      org.opencontainers.image.source="https://github.com/MarshallGomez1103/Mc2" \
      org.opencontainers.image.version="${MC2_VERSION}" \
      org.opencontainers.image.revision="${MC2_REVISION}"

RUN useradd --system --uid 10001 --create-home app \
    && mkdir -p /data/worlds \
    && chown -R app:app /data

COPY --from=build --chown=root:root /build/app.jar /app/app.jar

ENV MC2_SERVER_PORT=7777 \
    MC2_HEALTH_PORT=8080 \
    MC2_TICK_HZ=20 \
    MC2_LOAD_ZOMBIES=0 \
    MC2_WORLD_DIR=/data/worlds

USER app
EXPOSE 7777 8080
VOLUME ["/data/worlds"]

# /health por bash y /dev/tcp: la imagen JRE no trae curl y no se instala solo para esto.
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${MC2_HEALTH_PORT} && printf 'GET /health HTTP/1.0\\r\\nHost: localhost\\r\\n\\r\\n' >&3 && head -1 <&3 | grep -q ' 200'"]

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.awt.headless=true", "-Dmc2.server=true", "-Dmc2.worlds.dir=/data/worlds", "-jar", "/app/app.jar"]

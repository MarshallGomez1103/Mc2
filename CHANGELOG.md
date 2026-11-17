# Registro de cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones con [versionado semántico](https://semver.org/lang/es/). Cada versión publicada tiene un tag `vX.Y.Z`, imágenes `mc2-server` y `mc2-front` en GHCR con esa etiqueta y una Release con el JAR del cliente. El workflow `cd.yml` toma las notas de la Release de la sección de su versión; sin esa sección, la Release no se publica.

## [Sin publicar]

## [1.0.0]

Primera versión del Corte 3: el juego se separa en un servidor desplegable y un cliente accesible desde el navegador.

### Añadido

- Servidor headless `mc2-server` con el protocolo v1 en el puerto 7777 y `/health` y `/metrics` en el 8080.
- Front `mc2-front`: el cliente LibGDX en el navegador con noVNC (puerto 6080) y OpenGL por software.
- Multijugador en la misma red con tres apariencias de jugador (`skinId` 0–2).
- `docker-compose.yml` y `.env.example` para levantar los dos servicios con un solo comando.
- Pipeline de integración (`ci.yml`) con pruebas, cobertura del dominio, reglas de arquitectura y controles de seguridad.
- Pipeline de entrega (`cd.yml`): imágenes en GHCR etiquetadas con SHA y versión, despliegue con Compose, readiness, humo, carga corta con el SLO y reversión.
- Carga contra el servidor desplegado (`perf/run-slo.sh`) con el gate `LoadSloGate` de SLO-CARGA-01 y SLO-RED-01, y carga larga bajo demanda (`load.yml`).

### Cambiado

- `Minecraft2/Dockerfile` empaqueta el servidor en lugar de la consola del Corte 2.

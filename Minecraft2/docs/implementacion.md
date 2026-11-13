# Guía de implementación de Minecraft2

Esta guía es para el equipo técnico que va a poner en marcha y operar Minecraft2 en su propia infraestructura. Supone que conoce Docker y Git, pero no el proyecto, y que no puede consultar al grupo que lo construyó. Los pasos están en esta guía; para ampliar un tema se enlazan [devops.md](devops.md) y [seguridad.md](seguridad.md).

> **Estado del documento: esqueleto.** Contiene la estructura de las nueve partes y los comandos previstos del contrato (`cp .env.example .env`, `docker compose up`), que se comprueban cuando existan `.env.example` y `docker-compose.yml`. Cada paso marcado «Previsto — responsable: Persona N» se escribe con el comando exacto y el resultado observado al seguirlo desde una máquina limpia, y el marcador se retira antes de la entrega. Los nombres de servicios, variables y puertos son los del contrato del proyecto y deben coincidir con `docker-compose.yml`, `.env.example` y los workflows.

Responsable: Persona 1, con Persona 3 (despliegue, variables y operación).

Convención de los comandos: los valores que cambian se escriben entre `<` y `>` (por ejemplo, `<cuenta-github>`). Los comandos se ejecutan desde la raíz del repositorio.

## 1. Qué se va a implementar

Minecraft2 es un juego voxel en Java 17. En su forma desplegada se compone de dos servicios que se levantan con Docker Compose:

| Servicio | Qué es | Puertos |
| --- | --- | --- |
| `mc2-server` | Servidor headless del juego: recibe la entrada de los jugadores por TCP (protocolo de líneas JSON) y publica `/health` y `/metrics` por HTTP. | 7777 (TCP) y 8080 (HTTP) |
| `mc2-front` | Cliente gráfico dentro de un contenedor, jugable desde el navegador mediante noVNC. | 6080 (HTTP) |

Además, el cliente de escritorio se publica como un JAR en GitHub Releases para conectarse al servidor desde un equipo con Java. Varios jugadores comparten el mismo mundo dentro de la misma red.

La vista de despliegue que muestra dónde corre cada contenedor y cómo se comunican está en [arquitectura §8](arquitectura.md#8-vista-de-despliegue). Los servicios, los puertos y el comportamiento descritos aquí dependen de la implementación del Corte 3: Previsto — responsable: Persona 3.

## 2. Requisitos previos

| Requisito | Detalle |
| --- | --- |
| Cuentas y servicios | Una cuenta de GitHub con acceso al repositorio; GitHub Container Registry para descargar las imágenes. Cuenta y permisos exactos: Previsto — responsable: Persona 3 |
| Software | Docker Engine y Docker Compose v2, y Git. Versiones mínimas probadas: Previsto — responsable: Persona 3. Para el cliente de escritorio: Java 17 (JRE) y un equipo con OpenGL. |
| Recursos mínimos | CPU, memoria y disco para los dos servicios: Previsto — responsable: Persona 3, a partir de la ejecución real |
| Puertos | 7777 (TCP), 8080 (HTTP) y 6080 (HTTP) libres en la máquina anfitriona. |
| Dominio y certificado HTTPS | Previsto — responsable: Persona 3: se indica si aplican. El proyecto se ejecuta en la red local. |
| Red | Los jugadores deben estar en la misma red que la máquina anfitriona. |

## 3. Costos estimados

Previsto — responsable: Persona 3 (con Persona 1). Se escriben cifras solo con el plan y el proveedor realmente usados; no se estiman sin respaldo.

| Componente | Costo mensual con el plan usado en el proyecto | Costo mensual recomendado para el volumen real del negocio |
| --- | --- | --- |
| Máquina anfitriona y runner autohospedado | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |
| Registro de imágenes (GitHub Container Registry) | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |
| Ejecución de los workflows (GitHub Actions) | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |

## 4. Configuración

Previsto: toda la configuración será por variables de entorno; ninguna contraseña, token o cadena de conexión está en el código ni en la imagen. `.env.example` incluye todas las variables con valores de ejemplo; el archivo `.env` real no se versiona.

| Nombre | Para qué sirve | Valor de ejemplo | Obligatoria | Dónde se configura | Cómo generar |
| --- | --- | --- | --- | --- | --- |
| `MC2_SERVER_HOST` | Nombre o dirección con la que `mc2-front` y los clientes encuentran el servidor. | `mc2-server` | Previsto — responsable: Persona 3 | `.env` | Es el nombre del servicio en Compose; para un cliente fuera de Docker, la dirección de la máquina anfitriona. |
| `MC2_SERVER_PORT` | Puerto TCP del protocolo del juego. | `7777` | Previsto — responsable: Persona 3 | `.env` | Cualquier puerto libre; debe coincidir con el publicado en Compose. |
| `MC2_HEALTH_PORT` | Puerto HTTP de `/health` y `/metrics` del servidor. | `8080` | Previsto — responsable: Persona 3 | `.env` | Cualquier puerto libre; lo usan el healthcheck y la prueba de humo. |
| `MC2_TICK_HZ` | Frecuencia del tick del servidor, en ticks por segundo. | `20` | Previsto — responsable: Persona 3 | `.env` | Se deja el valor de ejemplo; cambiarlo altera el comportamiento del juego. |
| `MC2_LOAD_ZOMBIES` | Población de zombis del perfil de carga. `0` desactiva el perfil. | `0` (`80` para la prueba de carga) | Previsto — responsable: Persona 3 | `.env` o variable del workflow de carga | `0` en operación normal; `80` solo para la prueba de carga. |
| `MC2_PLAYER_NAME` | Nombre del jugador que presenta el cliente. | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 | `.env` | Texto libre. |
| `MC2_SKIN_ID` | Apariencia del jugador. | `0` | Previsto — responsable: Persona 3 | `.env` | Número entero de 0 a 2. |
| `MC2_WORLD_DIR` | Directorio, dentro del contenedor del servidor, donde se guardan los mundos. | `/data/worlds` | Previsto — responsable: Persona 3 | `.env` y volumen de Compose | Debe coincidir con el volumen persistente. |
| `MC2_IMAGE_TAG` | Etiqueta de las imágenes que se despliegan (SHA del commit o versión). Nunca `latest`. | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 | `.env` o variable del Environment de despliegue | Se toma de una versión publicada en el registro (ver la sección 7). |

Secretos de despliegue (GitHub Secrets o Environments): Previsto — responsable: Persona 3. Se listan con nombre, uso y forma de generarlos cuando el despliegue los defina; hoy no hay ninguno definido.

## 5. Instalación paso a paso

Cada paso termina con su resultado esperado. Los comandos del contrato son previstos hasta que existan `.env.example` y `docker-compose.yml`; todos se escriben con el comando exacto al verificarlos.

1. **Preparar el entorno.** Instalar Docker, Docker Compose v2 y Git. Resultado esperado: versiones disponibles con `docker --version` y `docker compose version`. Previsto — responsable: Persona 3: versiones mínimas.
2. **Obtener el repositorio.** `git clone <url-del-repositorio>` y entrar en el directorio clonado. Resultado esperado: el directorio contiene `docker-compose.yml` y `.env.example`. Previsto — responsable: Persona 3.
3. **Crear el archivo de configuración.**

   ```bash
   cp .env.example .env
   ```

   Resultado esperado: existe `.env` con todas las variables de la sección 4. Revisar sus valores; los de ejemplo sirven para una instalación local.
4. **Configurar el repositorio y sus secretos** (solo si se va a desplegar con GitHub Actions): registrar el runner autohospedado, crear el Environment y cargar sus secretos. Previsto — responsable: Persona 3 (con capturas de la consola web, si ayudan).
5. **Levantar el sistema.**

   ```bash
   docker compose up
   ```

   Resultado esperado: los servicios `mc2-server` y `mc2-front` llegan al estado saludable y `mc2-front` se inicia después de `mc2-server`. Salida concreta: Previsto — responsable: Persona 3.
6. **Primer despliegue por el pipeline** (solo si se usa el despliegue automático): fusionar a `main` y comprobar la ejecución en Actions. Previsto — responsable: Persona 3.
7. **Cargar los datos iniciales.** Previsto — responsable: Persona 3: confirmar si hay datos iniciales. Los mundos se guardan en el directorio `MC2_WORLD_DIR`.
8. **Crear la cuenta de administración.** No aplica: el sistema no tiene cuentas ni roles ([seguridad §2.1](seguridad.md#21-autenticación-y-autorización)).
9. **Instalar y conectar el cliente de escritorio** (opcional, para jugar sin el navegador):
   1. Descargar el JAR del cliente desde la página de Releases del repositorio, en `https://github.com/<cuenta-github>/<repositorio>/releases`. Previsto — responsable: Persona 3: nombre del archivo.
   2. Instalar Java 17 si no está presente.
   3. Ejecutar el cliente apuntando al servidor, con `MC2_SERVER_HOST` y `MC2_SERVER_PORT`. Comando exacto: Previsto — responsable: Persona 3.
   4. Resultado esperado: el cliente se conecta y aparece el jugador en el mundo compartido. Previsto — responsable: Persona 3.

## 6. Verificación

Cómo comprobar que la instalación quedó bien. Previsto — responsable: Persona 3: comandos exactos y resultado observado.

1. **Healthcheck.** El servidor responde en `http://<host>:8080/health`. Resultado esperado: Previsto — responsable: Persona 3.
2. **Estado de los servicios.** `docker compose ps` muestra `mc2-server` y `mc2-front` saludables. Resultado esperado: Previsto — responsable: Persona 3.
3. **Prueba de humo.** Comando del script de humo y resultado esperado: Previsto — responsable: Persona 3.
4. **Recorrido corto por el flujo principal.** Abrir `http://<host>:6080` en el navegador, crear o cargar un mundo, moverse, y comprobar que un segundo jugador aparece. Previsto — responsable: Persona 3.

## 7. Operación

Previsto — responsable: Persona 3 para cada procedimiento: comando exacto y resultado esperado.

| Tarea | Cómo se hace |
| --- | --- |
| Publicar una nueva versión | Previsto — responsable: Persona 3: crear el tag `vMAJOR.MINOR.PATCH`, esperar el pipeline y comprobar la imagen en el registro y la versión en Releases. |
| Volver a la versión anterior | Previsto — responsable: Persona 3: procedimiento de [devops §7](devops.md#7-reversión), con la etiqueta previa en `MC2_IMAGE_TAG`. |
| Copia de seguridad de los datos | Previsto — responsable: Persona 3: copia del volumen de `MC2_WORLD_DIR` con el servicio detenido. |
| Restauración de los datos | Previsto — responsable: Persona 3: restaurar la copia en el volumen y verificar que el servidor lista los mundos. |
| Cambio de un secreto | Previsto — responsable: Persona 3: actualizar el secreto en GitHub Secrets o en el Environment y volver a desplegar. |
| Consultar los logs | `docker compose logs mc2-server` y `docker compose logs mc2-front`. Resultado esperado: Previsto — responsable: Persona 3. |
| Reiniciar un servicio | `docker compose restart mc2-server`. Resultado esperado: Previsto — responsable: Persona 3. |

## 8. Solución de problemas

Se documentan al menos cinco problemas con su síntoma, su causa y su solución, a partir de los que realmente aparezcan al instalar y operar. Los títulos son los problemas probables.

| Problema | Síntoma | Causa | Solución |
| --- | --- | --- | --- |
| El puerto 7777, 8080 o 6080 ya está en uso | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |
| `mc2-server` no llega al estado saludable | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |
| El navegador muestra pantalla negra o el juego va lento | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |
| El cliente de escritorio no se conecta al servidor | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |
| No se puede descargar la imagen del registro (permiso denegado o etiqueta inexistente) | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |
| Los mundos no se conservan al reiniciar | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 | Previsto — responsable: Persona 3 |

## 9. Recomendaciones para producción

Qué debe cambiar el negocio respecto al entorno del proyecto. Previsto — responsable: Persona 1 (con Persona 3 y Persona 2):

- **Credenciales y valores de ejemplo.** Reemplazar los valores de `.env.example` y los secretos del despliegue.
- **HTTPS.** noVNC y el protocolo TCP no están cifrados en el entorno del proyecto: definir cómo protegerlos.
- **Copias de seguridad programadas** del volumen de mundos.
- **Tamaño de los recursos** según el número de jugadores real.
- **Riesgos aceptados abiertos.** Los de [seguridad.md](seguridad.md#6-riesgos-aceptados) que sigan abiertos, con lo que haría falta para corregirlos.

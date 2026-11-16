# Servidor headless y protocolo v1 — incremento de fase B

Implementación inicial de Jasub, 16 de noviembre de 2026. Capacidad actual: **una conexión de jugador activa**, sobre una sesión autoritativa. El cliente gráfico remoto y el multijugador de varias conexiones pertenecen al siguiente incremento; este documento no los presenta como terminados.

## Arranque

Desde `Minecraft2/`, con Java 17:

```bash
mvn -B -ntp clean verify
java -Djava.awt.headless=true -Dmc2.server=true -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

En PowerShell, usar argumentos Maven entre comillas cuando contienen puntos, por ejemplo `'-Djacoco.skip=true'`. Maven debe usar Java 17 (`mvn -version`); no basta comprobar `java -version`.

La bandera de servidor se evalúa antes de preparar la GPU o crear ventanas. La salida informa puertos efectivos y `capacity=1`. El modo gráfico y el modo consola existentes conservan su selección anterior cuando no se activa esta bandera.

El Dockerfile de Ethian puede arrancar el mismo JAR con `-Dmc2.server=true`, `-Djava.awt.headless=true` y `-Dmc2.worlds.dir=/data/worlds`. No hace falta otra dependencia Maven.

## Configuración

| Variable | Por defecto | Comportamiento |
| --- | --- | --- |
| `MC2_SERVER_PORT` | 7777 | Puerto TCP de escucha del juego; 0 permite puerto efímero en pruebas. |
| `MC2_HEALTH_PORT` | 8080 | Puerto HTTP de salud/métricas; distinto del de juego, salvo ambos 0 en pruebas. |
| `MC2_TICK_HZ` | 20 | Frecuencia fija, rango 20–120. Valores inferiores excederían el delta máximo del núcleo de 0,05 s y se rechazan. |
| `MC2_LOAD_ZOMBIES` | 0 | 0: partida normal persistente. 1–320: escenario transitorio de población objetivo para carga. |
| `MC2_WORLD_DIR` | Resolución existente de WorldDirectory | Directorio del mundo normal; prevalece sobre `-Dmc2.worlds.dir`. |

La dirección de escucha acepta conexiones en las interfaces del host/contenedor. `MC2_SERVER_HOST` es el destino del cliente, no una dirección de bind. Compose controla qué puertos publica al host. Variables de nombre y skin del cliente se traducen al mensaje `hello`; la skin de este incremento es metadato de la conexión y no modifica el formato de persistencia del dominio.

## Mensajes y secuencias

TCP, UTF-8 estricto, una línea JSON por mensaje. Máximo 4096 bytes de entrada por línea, sin contar el salto de línea; CRLF también se acepta. Una línea incompleta al desconectarse se rechaza. JSON inválido, campos desconocidos, tipos erróneos, números no finitos y mensajes fuera de orden cierran esa conexión.

Primero el cliente envía:

```json
{"type":"hello","name":"Jasub","skinId":2}
```

Nombre no vacío, máximo 32 unidades UTF-16, sin controles; skin entero 0–2. La primera respuesta es:

```json
{"type":"welcome","playerId":0,"tickHz":20,"protocolVersion":1}
```

Después puede enviar una entrada como la que usa el generador de Ethian:

```json
{"type":"input","seq":1,"forward":true,"backward":false,"left":false,"right":false,"sprint":false,"jump":false,"deltaYaw":0.5,"deltaPitch":0.0,"primary":false,"secondary":false,"togglePistol":false}
```

- `seq`: entero positivo hasta 9007199254740991, creciente por conexión. Duplicados/antiguos se ignoran; los saltos se permiten. El servidor no acepta un `playerId` elegido por el cliente.
- Acciones omitidas son neutrales. Mirada limitada por mensaje a ±360 grados de yaw y ±180 de pitch. `selectedType` opcional es un nombre de `BlockType` distinto de AIR; null conserva el material actual.
- Se procesa como máximo una entrada por tick; máximo 32 pendientes por conexión. No se repiten saltos, clics o deltas de mirada por reutilizar la última entrada.
- El servidor emite `state` con `tick`, `ack` numérico en la raíz, `players` y `zombies`. `ack` empieza en 0 y solo avanza tras aplicar la entrada. Una acción ignorada por una partida muerta no se confirma como aplicada.
- Cada jugador del estado incluye `playerId`, `name`, `skinId`, posición `x/y/z`, `yaw/pitch`, `health` y `dead`. Los zombis incluyen posición, salud y estado de IA. La sincronización gráfica de terreno/bloques se completa en la fase C.
- `{"type":"bye"}` o fin de conexión libera el puesto; un cliente nuevo puede reclamarlo. Otra conexión que intente ocupar simultáneamente la partida se cierra, sin sustituir al dueño.

La sesión normal conserva su cuerpo/estado entre conexiones; en esta fase no hay orden de respawn por red. El nombre no constituye autenticación. El protocolo TCP todavía no implementa cifrado ni autenticación: ese límite se debe tratar en el modelo de amenazas, sin atribuirle garantías inexistentes.

## Responsabilidades y límites de recursos

`bootstrap.ServerBootstrap` selecciona el modo servidor y gestiona su cierre. `network.ProtocolCodec` valida los mensajes. `network.GameServer` separa lectores, hilo único de simulación y escritores. `network.ServerSimulation` reutiliza las reglas existentes de aplicación/dominio. `network.ServerMetrics` publica mediciones coherentes.

Los lectores encolan intenciones; solo el hilo del tick modifica y lee el mundo para formar snapshots. Los escritores no bloquean ese hilo. Se permiten hasta ocho conexiones de transporte, pero solo un jugador admitido. Hay como máximo ocho lectores y ocho escritores, colas de salida de cuatro mensajes y una cola de 32 entradas por conexión. Un cliente que acumula salida se desconecta. El saludo tiene timeout de lectura de 5 s y la lectura posterior 30 s de inactividad. No son una garantía contra todo abuso de red; se documentan como límites concretos.

## Salud, métricas y carga

- `GET /health`: 200 con `{"status":"ready"}` después de un tick correcto y mientras el tick siga reciente (menos de 5 s); 503 si no está listo o ocurre un fallo fatal. Otras rutas bajo estos contextos devuelven 404 y métodos distintos de GET, 405.
- `GET /metrics`: texto Prometheus con histograma acumulativo `mc2_tick_duration_seconds` (`_bucket`, `_count`, `_sum`), `mc2_tick_failures_total`, `mc2_zombies_active`, `mc2_players_connected`, `mc2_population_deficit_ticks_total`. Las series existen también en cero.
- El histograma incluye límites finos hasta 16,67 ms, límites superiores y +Inf. Mide trabajo de simulación y preparación/encolado de snapshots; excluye la espera hasta el siguiente tick y las escrituras de socket. Un tick fallido incrementa fallos, sin convertirse en muestra de latencia exitosa. La falta de población se cuenta.
- En carga se genera mundo 10×10, semilla 20260925 y dificultad VERY_HARD, como la recuperación del Corte 2. Se usan IA, colisiones y controles reales. Se repone población viva con intentos acotados (máximo cuatro incorporaciones por tick y 32 intentos por llamada); el calentamiento llena progresivamente la población. No se falsifican gauges. Las oleadas crecientes se sustituyen por población objetivo y se reaparece automáticamente tras morir para no congelar la IA del benchmark. No se guarda ese escenario sobre una partida normal.
- El mundo normal `server.json` se crea/carga mediante la aplicación existente y se guarda al cerrar de forma ordenada. La persistencia JSON v1 permanece sin cambios. La terminación forzada del proceso no garantiza guardar el último estado.

El consumidor de Ethian calcula el RTT al recibir un estado que reconoce el seq. La comprobación oficial de carga de tres clientes se hará después de implementar varias conexiones en C. Cualquier prueba de carga de B con un cliente demuestra compatibilidad de este incremento y no acredita el SLO multijugador final.

## Verificación y evidencia

`ProtocolCodecTest`, `ServerConfigTest` y `ServerMetricsTest` ejecutan nueve pruebas unitarias nuevas. `GameServerIT` añade cinco pruebas Failsafe: protocolo y ack, aislamiento del puesto/rechazo de mensajes, población real de 80 zombis, cierre/reinicio con persistencia y JAR sin gráficos en otro proceso.

Comando completo: `mvn -B -ntp clean verify`. Resultado del incremento de código `57be0e1`: 227 unitarias, 153 integración, cero fallos, errores u omitidas; cobertura de líneas del dominio 92,68 % y gate de 70 % aprobado. El fallo inicial de limpieza de salida temporal en Windows se corrigió capturando stdout por pipe y cerrando los streams del proceso.

Historia local: `1fe08c3` (pruebas de protocolo rojas), `40aa59c` (pruebas de servidor rojas), `57be0e1` (implementación verde). No se ha publicado ni fusionado esta rama durante esta comprobación. Las pruebas locales no sustituyen los checks de Actions que deben verificarse al publicar el PR.

### Comprobación de integración y rendimiento de un cliente

El incremento posterior `7de8745` acota el relleno de población y distribuye las búsquedas iniciales de rutas. `mvn -B -ntp clean verify` volvió a pasar: 227 unitarias y 153 integración, sin fallos, errores u omitidas, con cobertura y gate conservados.

Se construyeron los Dockerfiles originales de `c3/ethian` `fb28706` con los archivos de Jasub superpuestos en un worktree aislado. `mc2-server` y `mc2-front` quedaron saludables como usuario `app`, y pasó el script original `scripts/deploy.sh --humo` (salud, métricas, bienvenida TCP y noVNC). Son imágenes locales de prueba, no un despliegue definitivo publicado en GHCR ni una ejecución de CD en Actions.

La carga real de Ethian contra estos contenedores, con **un cliente**, 80 zombis y calentamiento explícito de 30 s con tráfico antes de medir, obtuvo:

| Repetición de 30 s | Tick p95 (ms) | Entrada → ack p95 (ms) | Población mínima | Fallos / déficit / errores |
| --- | ---: | ---: | ---: | --- |
| 1 | 11,104 | 47,170 | 80 | 0 / 0 / 0 |
| 2 | 11,352 | 57,982 | 80 | 0 / 0 / 0 |
| 3 | 11,518 | 59,266 | 80 | 0 / 0 / 0 |

El gate pasó para esa configuración. Se conservan también resultados fallidos previos: hubo incumplimientos de tick y, en algunas repeticiones, RTT en frío; repartir el relleno no bastó para que pasaran todas las repeticiones iniciales. El perfil JFR mostró búsquedas de rutas y pausas GC, sin demostrar una causa única para todos los picos.

El consumidor actual espera población hasta `MC2_LOAD_WARMUP_SECONDS`, pero retorna antes si ya existe; después solo calienta dos segundos con el cliente conectado. La propuesta revisable en [integración con Ethian](../../integracion/LEEME.md) separa el calentamiento real de esa espera. **La carga final de tres clientes y la comprobación en frío desde CD siguen pendientes de C/G y de incorporar/verificar el ajuste con Ethian.** Los resultados de un cliente ya calentado no los sustituyen.

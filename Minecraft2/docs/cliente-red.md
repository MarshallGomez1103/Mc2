# Cliente remoto y mundo compartido

El cliente LibGDX representa la partida del servidor autoritativo. El modo local anterior se conserva cuando no se selecciona un destino de red.

## Conectar

Con el servidor ya iniciado, desde Minecraft2 y con JDK 17:

```sh
java -Dmc2.client=true -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

El destino por defecto de esa bandera es 127.0.0.1:7777. `MC2_SERVER_HOST` no vacío también activa el modo remoto. Se leen `MC2_SERVER_PORT`, `MC2_PLAYER_NAME` y `MC2_SKIN_ID`; la skin se resuelve mediante `SkinSelection.fromEnvironment` de Thomas (0–2, fallback 0). El contenedor front de Ethian ya proporciona estas variables y abre este mismo modo.

WASD mueve, ratón mira, espacio salta, shift corre, clic izquierdo/derecho actúa, 1–7 selecciona material y Q alterna arma. R solicita reaparición cuando el servidor informa muerte. ESC libera/captura cursor: pausa únicamente las intenciones de este cliente, la partida compartida continúa. Cerrar la ventana cierra el socket y el servidor retira al jugador por EOF. Los fallos de conexión se muestran en pantalla; para reconectar se reinicia el cliente.

## Responsabilidades

`NetworkClient` realiza conexión, DNS y E/S fuera del hilo gráfico. Envía intenciones mediante una cola de 128 mensajes, mantiene secuencia por instancia y valida ticks/ack recibidos. La cola llena desconecta explícitamente. `ClientProtocol` produce snapshots inmutables; admite estados de hasta 1 MiB, mientras las entradas siguen limitadas a 4 KiB. Un timeout de conexión de 3 s no incluye el tiempo de DNS; lectura de estados tiene timeout de 5 s. El cierre del socket no garantiza enviar bye.

`VoxelGame` delega su modo remoto a `RemoteVoxelGame`; este recoge controles con `GameInput`, los acumula con `NetworkInputBuffer` hasta el siguiente envío al ritmo de tickHz y dibuja estados recibidos. Conserva acciones puntuales y deltas entre frames. No crea GameSession ni ejecuta física o IA compartida. `RemoteWorldReplica` solo materializa los chunks y aplica los cambios de bloques del servidor, en el hilo gráfico. Los cambios de chunks aún no recibidos quedan incluidos en su transmisión inicial posterior.

`RemotePlayerRenderer` representa otros jugadores por identidad, con tres variantes humanoides diferenciadas por color; excluye al jugador local y retira a quien deja de aparecer. Los zombis remotos se representan con cuerpos verdes desde sus posiciones/salud recibidas. El modo remoto tiene HUD de conexión, salud, jugadores y tick; sus visuales de zombis y arma son más simples que los del modo local (no reproduce sus animaciones ni el modelo de pistola). Las acciones y el combate se resuelven en el servidor. No se presenta equivalencia visual completa con el modo local.

La inicialización envía un chunk por tick, hasta completar el mundo real. La pantalla informa progreso y espera ese mundo antes de habilitar controles; no genera otro terreno a partir de una semilla. Una cola independiente de 128 actualizaciones conserva chunks y cambios aunque el último snapshot de actores sea reemplazado. Recursos gráficos y la copia visual se usan y destruyen solamente en el hilo gráfico.

## Verificación

```sh
mvn -B -ntp clean verify
```

`ClientProtocolTest`, `TerrainCodecTest`, `NetworkInputBufferTest` y `RemotePlayerRendererTest` comprueban lógica sin GPU. `NetworkClientIT` prueba transporte TCP y el servidor real. `MultiplayerServerIT` comprueba tres clientes, identidades y ack independientes, bajas/reingreso y terreno/minería compartidos. `GameServerIT` comprueba además proceso JAR headless, salud, métricas, persistencia y población real. La comprobación OpenGL/noVNC se registra separadamente de las pruebas sin GPU y del rendimiento.

La red TCP no tiene autenticación fuerte ni TLS. El nombre es un dato de presentación. El servidor ata las acciones a la conexión; no acepta un id de jugador elegido por el cliente. Ver el [contrato del servidor](servidor.md).

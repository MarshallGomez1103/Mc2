# Cliente de red — preparación de fase C

Estado al 16 de noviembre de 2026: adaptador y renderer preparados; todavía no están conectados al arranque gráfico. No constituye multijugador terminado.

## Responsabilidades

`network.NetworkClient` abre una conexión por instancia. El constructor valida la configuración y lanza un lector; resolver el nombre, conectar y esperar mensajes ocurre fuera del hilo gráfico. Tras `welcome`, un escritor independiente consume una cola de hasta 128 intenciones. `send(PlayerFrameInput)` devuelve inmediatamente si pudo aceptar la entrada; una cola llena produce un error y desconexión explícitos. No se descartan silenciosamente acciones discretas. Cada instancia empieza su secuencia en uno.

`network.ClientProtocol` codifica `hello` e `input` de acuerdo con el servidor actual y convierte `welcome` y `state` en records inmutables. Valida tipos, números finitos, identidad única, skins y tamaños. El límite de mensajes del servidor es 1 MiB; el de las intenciones sigue siendo 4 KiB. Los snapshots incluyen tick, ack, jugadores y zombis; todavía no transportan terreno ni cambios de bloques.

El cliente exige ticks crecientes, ack que no retrocede ni supera las entradas enviadas, y presencia del jugador admitido. `latestState()` publica únicamente el último snapshot completo mediante una referencia volatile. No se comparte un `GameSession`, `World` o `Player` mutable con el hilo de red. `status()` y `error()` permiten mostrar conexión, fallo y cierre en pantalla. Un servidor que deja de enviar datos provoca timeout de lectura a los cinco segundos; conectar tiene timeout de tres segundos, aparte de la resolución DNS. `close()` cierra el socket sin esperar al lector y el servidor procesa la desconexión por EOF; no se garantiza enviar `bye` en ese cierre.

`presentation.game.RemotePlayerRenderer` crea tres modelos humanoides de seis partes, diferenciados por color según skinId 0, 1 o 2. Recibe vistas de red, excluye al jugador local y retira las instancias ausentes del siguiente snapshot. Construcción, render y dispose deben ocurrir en el hilo gráfico. Las pruebas sin GPU comprueban selección y colores; no acreditan la apariencia OpenGL.

## Integración que falta con Thomas

1. Incorporar el PR #3 después de su fusión, respetando el orden del equipo. Usar `join`, `leave`, `advanceAll` y snapshots solamente desde el hilo de simulación. Cada conexión conserva identidad y ack propios.
2. Definir y probar el transporte del mundo inicial y las modificaciones de bloques; no generar un mundo local independiente que pueda divergir.
3. Entregar a Thomas el cambio concreto de su cableado `VoxelGame` una vez fijada esa representación. En modo remoto, recoger intenciones y enviarlas al ritmo del servidor, conservando acciones puntuales y deltas entre envíos; no invocar `session.advance` para otra partida compartida. El adaptador no regula por sí mismo la frecuencia de `send`.
4. Dibujar cámara local, zombis, terreno y jugadores remotos desde los estados recibidos. Conectar `MC2_SERVER_HOST`, `MC2_SERVER_PORT`, `MC2_PLAYER_NAME` y `MC2_SKIN_ID` en el arranque autorizado.
5. Probar aislamiento, reconexiones, dos o tres clientes y noVNC con el código integrado. Ninguna de esas comprobaciones se sustituye por las pruebas actuales del adaptador.

## Pruebas

`ClientProtocolTest` comprueba codificación de intenciones, snapshots inmutables y rechazo de mensajes inválidos y sobredimensionados. `NetworkClientIT` prueba TCP con un peer controlado (saludo, secuencia, ack, cierre y cancelación), un saludo inválido y un intercambio real con `GameServer`. `RemotePlayerRendererTest` prueba la lógica independiente de GPU.

Desde `Minecraft2`, con Maven usando JDK 17:

```sh
mvn -B -ntp clean verify
```

Los reportes se generan en Surefire y Failsafe; los resultados de esta preparación y sus límites se registran fuera del repositorio en `FASE_C_ESTADO.md` y `evidencias/fase-c/`. No atribuir estas pruebas a tres jugadores ni al despliegue final.

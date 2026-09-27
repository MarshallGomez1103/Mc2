# Minecraft 2

Videojuego voxel universitario sencillo inspirado en Minecraft Classic, desarrollado en Java por tres estudiantes. El MVP del Corte 1 está completado; los tres frentes del Corte 2 están integrados en `main`.

> Este proyecto no es todavía un videojuego completo. Incluye estructuras, contratos, conexiones entre capas, generación de terreno, movimiento, física, interacción por raycast, renderizado voxel, controles básicos y persistencia JSON.

## Estado del proyecto

### Corte 1 — Completado

Menú de consola, crear/listar/cargar/guardar/eliminar mundos, generación inicial de
cuatro chunks, bloques, jugador, cámara, movimiento, salto, gravedad, colisiones,
raycast e interacción, renderizado 3D y persistencia JSON. Incluye Factory,
Singleton, Observer y texturas de bloques ON/OFF. Baseline: 41 pruebas JUnit.

### Corte 2 — Integrado en main, ampliación de jugabilidad

Biomas PLAINS/DESERT/MOUNTAINS, aldea, mundos finitos, zombies FSM + A* y oleadas
están integrados. El inicio normal abre el menú gráfico para crear/listar/cargar/
eliminar mundos; ESC pausa la partida y permite guardar, cambiar opciones y salir
con confirmación. F alterna pantalla completa conservando la partida.

Los zombies tienen cabeza, torso, brazos y piernas, UV por cara/parte y marcha.
Sus cuerpos se separan; spawn ocupado o a menos de 2.5 bloques del jugador se
rechaza. NORMAL: velocidad 4.3, detección 24, oleada inicial 4 a los 8 s.
VERY_HARD: velocidad 4.7, detección 28, oleada inicial 4 a los 4 s.
Ambas dificultades duplican cantidades: 4, 8, 16, 32, 64, 128… Las apariciones
fallidas se reintentan, sin descontarlas de la ronda.
Caminar sigue en 4.3; sprint 7.095 con estamina para aproximadamente 10 s.

Estamina: 100, consumo 10/s al moverse, regeneración 20/s, desbloqueo al 20% tras
agotarse. Pausa y muerte congelan la simulación; reaparecer restaura estamina.
La cámara añade un movimiento moderado y hasta 4 grados de FOV al correr.

Enemigos y texturas de zombies se aplican inmediatamente desde Opciones.
Dificultad y texturas de bloques se aplican en la próxima partida, con aviso.
Gráficos muestra el renderer OpenGL efectivo y permite Integrada/Dedicada/Automática en Linux
con dispositivos compatibles; requiere próximo inicio. Ver límites de GPU abajo.

Todos los menús son transparentes: un paisaje aleatorio temporal de 16 chunks sirve
de fondo sin partida; la pausa conserva el mundo de la partida. El menú muestra solo
Integrada o Dedicada en Intel/NVIDIA; los modelos quedan en la terminal.
El jugador tiene 100 de vida y una barra roja; cada golpe enemigo resta 25. El primero
es inmediato; siguientes golpes cada 0.6 s NORMAL / 0.4 s VERY_HARD. Al reaparecer
recupera toda la vida. Los zombis detectados conservan el objetivo mientras viva.
A* permite ocho direcciones con diagonales de coste sqrt(2) y protección de esquinas.
Los zombis ya no suben bloques instantáneamente: saltan con la gravedad y velocidad del
jugador, con mínimo 1 segundo entre saltos. Hay destellos rojos cortos al recibir daño.
La vida regenera 5/s después de 5 s sin golpes. Desde la oleada 3 aparece una pistola
sobre terreno aleatorio: acercarse la recoge; Q alterna, clic dispara y 1–7 vuelven a
bloques. Daño 3, alcance 36, intervalo 0.28 s, munición ilimitada.
Los zombis pueden entrar en pozos, de uno en uno, y recuerdan el borde cuando el jugador
se aleja por la cueva. Caída de 8 bloques o más: 70% se desarman y 30% sobreviven sin
piernas, con altura 0.8 y velocidad reducida al 28% (1.204 bloques/s en NORMAL);
persiguen por túneles bajos. Preparan cada salto al pozo durante 5 s en el borde;
el mismo hueco admite una entrada por segundo, siempre que esté libre.
Los cuerpos se desarman en 18 piezas texturizadas: chocan con suelo y paredes,
se asientan y permanecen durante toda la sesión, sin colisión ni bloquear oleadas.
Los restos no se guardan en JSON; se limpian al cerrar la partida.
La muerte por kamikaze dice «Te mató el zombi kamikaze»; por vacío «Aquí no hay piso».

El menú de pausa es transparente. La barra de estamina aparece arriba sin etiquetas.
El radio inicial de chunks es 2 (limitado por el tamaño del mundo). Los mensajes de
muerte recorren únicamente las tres frases restantes, una por muerte.
Detalle actual: [oleadas y restos](docs/corte2/testing/final-horde-20260926.md).
La validación anterior de combate se conserva como evidencia histórica.

Gate real 2026-09-26: **313 pruebas, cero fallos/errores/ignoradas**.
El smoke gráfico automatizado pasó menú, sprint, pausa, opciones, fullscreen,
resize y guardado/carga. No equivale a un playtest humano de diversión.
Resultados nuevos de PIT/carga y fronteras de evidencia:
[reporte de integración](docs/corte2/testing/integration-20260926.md).

### Corte 3

Pendiente de definición.

## Problema

El equipo necesita iniciar un videojuego voxel sin mezclar la interfaz, las reglas del mundo y el acceso a archivos. Una base sin separación de responsabilidades dificultaría que tres personas trabajen en paralelo y que el proyecto evolucione sin dependencias circulares.

## Objetivo original del Corte 1

Ofrecer un esqueleto pequeño, claro y compilable que establezca:

- una arquitectura por capas;
- los modelos principales del dominio;
- los únicos tres patrones permitidos: Factory, Singleton y Observer;
- operaciones CRUD conceptuales para mundos guardados como JSON;
- un menú principal de consola desde el cual conectar los casos de uso.

## Alcance del MVP — Corte 1 completado

- Menú principal.
- Crear, listar, cargar, guardar y eliminar mundos.
- Uno o pocos chunks por mundo.
- Generación pseudoaleatoria sencilla.
- Bloques de aire, césped, tierra, piedra, arena, grava, madera y hojas.
- Movimiento hacia adelante, atrás, izquierda y derecha.
- Salto y gravedad.
- Colocación y eliminación de bloques mediante clic.
- Ventana 3D con renderizado de chunks y controles básicos de teclado y ratón.
- Persistencia local en archivos JSON.

## Fuera del alcance del Corte 2

- Un videojuego terminado o una réplica completa de Minecraft.
- Multijugador, agente LLM, survival completo, inventario complejo o crafting.
- Mundo infinito/streaming y carga dinámica de chunks; motor gráfico completo.
- ECS, behavior trees, machine learning, redes neuronales y arquitectura distribuida.
- Base de datos.
- Refactors masivos y reescritura de la arquitectura. La restricción original de
  patrones del Corte 1 se conserva como contexto histórico, no excluye la FSM acordada del Corte 2.
- Frameworks o librerías que no sean necesarios para esta base.

## Tecnologías

- Java 17 o superior.
- IntelliJ IDEA.
- Maven para compilación reproducible.
- LibGDX 1.12.1 con backend LWJGL3 para la ventana y el renderizado 3D.
- Archivos JSON locales, sin base de datos ni librería externa.

## Arquitectura

El código mantiene las tres capas solicitadas. Dentro de Domain/Game Logic se separa `application` (coordinación de casos de uso) de `domain` (modelo y reglas), sin convertirlas en capas independientes. Las dependencias avanzan hacia el dominio y no existe una dependencia desde este hacia la presentación o la persistencia.

```text
bootstrap ──> presentation ──> application ──> domain
   │              │                  │
   │              └──> presentation.game ──> domain
   ├──> persistence ──> domain       ├──> persistence
   └──> application                  └──> patterns.singleton ──> domain

domain.world ──> patterns.observer
patterns.factory ──> domain
```

- **Presentation:** `presentation` muestra el menú y `presentation.game` abre la ventana, traduce los controles y dibuja el mundo; las reglas se delegan hacia `application` y `domain`.
- **Domain/Game Logic:** `application` coordina casos de uso y `domain` representa mundo, chunks, bloques, jugador y posiciones.
- **Persistence:** el paquete `persistence` define y realiza las operaciones locales CREATE, READ, UPDATE y DELETE.
- **Apoyo arquitectónico:** `patterns` aloja únicamente Factory, Singleton y Observer; `bootstrap` solo conecta objetos al iniciar.

La explicación ampliada se encuentra en [docs/arquitectura.md](docs/arquitectura.md). Las decisiones compartidas de dimensiones, coordenadas, JSON, tecnología gráfica y pruebas están congeladas en [docs/decisiones-compartidas.md](docs/decisiones-compartidas.md).

## Patrones utilizados

### Factory

`BlockFactory` centraliza la creación de `Block` a partir de un `BlockType` y una `Position`. Reconoce AIR, GRASS, DIRT, STONE, SAND, GRAVEL, WOOD y LEAVES. No contiene todavía propiedades especiales por material. Véase [docs/factory.md](docs/factory.md).

### Singleton

`WorldManager` conserva la referencia al mundo actualmente cargado y ofrece un único punto de acceso con `getInstance()`. Es el único Singleton del proyecto. Véase [docs/singleton.md](docs/singleton.md).

### Observer

`World` actúa como sujeto de notificaciones `BlockChange`. Al colocar o eliminar un bloque, comunica un cambio `PLACED` o `REMOVED` a los observadores registrados. No hay bus global ni sistema complejo de eventos. Véase [docs/observer.md](docs/observer.md).

## Estructura del proyecto

```text
Minecraft2/
├── docs/
│   ├── evidencias/
│   │   ├── ct-10-render.md
│   │   ├── ct-10-render.png
│   │   └── ct-11-flujo-mvp.md
│   ├── arquitectura.md
│   ├── decisiones-compartidas.md
│   ├── factory.md
│   ├── observer.md
│   ├── singleton.md
│   └── uml.md
├── src/
│   ├── bootstrap/
│   │   └── Minecraft2Application.java
│   ├── application/
│   │   ├── ChunkGenerationService.java
│   │   ├── PlayerInteractionService.java
│   │   ├── TargetedBlock.java
│   │   └── WorldApplicationService.java
│   ├── presentation/
│   │   ├── ConsoleIO.java
│   │   ├── MainMenu.java
│   │   └── game/
│   │       ├── BlockAppearance.java
│   │       ├── ChunkMeshBuilder.java
│   │       ├── GameInput.java
│   │       ├── GameWindow.java
│   │       └── VoxelGame.java
│   ├── domain/
│   │   ├── Position.java
│   │   ├── block/
│   │   │   ├── Block.java
│   │   │   └── BlockType.java
│   │   ├── player/
│   │   │   ├── CollisionResolver.java
│   │   │   ├── MovementInput.java
│   │   │   ├── Player.java
│   │   │   ├── PlayerMovementService.java
│   │   │   └── PlayerPhysics.java
│   │   └── world/
│   │       ├── BlockChange.java
│   │       ├── Chunk.java
│   │       ├── SimpleTerrainGenerator.java
│   │       └── World.java
│   ├── manualtest/
│   │   ├── BlockInteractionAndPatternsManualTest.java
│   │   ├── CollisionManualTest.java
│   │   ├── PlayerMovementManualTest.java
│   │   ├── PlayerPhysicsManualTest.java
│   │   └── WorldBlockOperationsTest.java
│   ├── patterns/
│   │   ├── factory/BlockFactory.java
│   │   ├── observer/
│   │   │   ├── Observer.java
│   │   │   └── Subject.java
│   │   └── singleton/WorldManager.java
│   └── persistence/
│       ├── InvalidWorldFileException.java
│       ├── JsonParser.java
│       ├── JsonWorldStorage.java
│       ├── WorldJsonCodec.java
│       └── WorldStorage.java
├── test/
│   ├── application/
│   │   └── WorldLifecycleTest.java
│   ├── presentation/game/
│   │   └── VoxelGameObserverTest.java
│   └── persistence/
│       ├── InvalidWorldFileTest.java
│       ├── JsonWorldStorageTest.java
│       ├── WorldJsonCodecTest.java
│       └── WorldSamples.java
├── .gitignore
├── Minecraft2.iml
├── pom.xml
├── README.md
└── TODO.md
```

## Instrucciones de ejecución

### IntelliJ IDEA

1. Abrir la carpeta raíz `Minecraft2`.
2. Configurar un JDK 17 o superior si IntelliJ lo solicita.
3. Esperar a que IntelliJ importe `pom.xml`.
4. Ejecutar el método `main` de `bootstrap.Minecraft2Application`.

### Terminal

```bash
mvn clean package
java -Dfile.encoding=UTF-8 -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

Se abre el menú gráfico; la consola no es necesaria para jugar.
Para mundos grandes puede usarse `java -Xmx2g -jar target/minecraft2-0.1.0-SNAPSHOT.jar`.
El mundo completo sigue en memoria; no hay streaming.

La consola histórica se conserva como compatibilidad y diagnóstico:

```bash
java -Dmc2.console=true -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

### Controles

| Acción | Control |
| --- | --- |
| Caminar / correr | WASD / Shift con estamina |
| Saltar / mirar | Espacio / ratón |
| Golpear zombie o eliminar bloque | Clic izquierdo |
| Colocar / elegir bloque | Clic derecho / 1–7 |
| Distancia visible | J/K |
| Reaparecer | R |
| Pausa y menú | ESC |
| Pantalla completa / ventana | F |

ESC libera el cursor y congela movimiento, física, enemigos, oleadas y estamina.
Los clics de menús no editan el mundo. Salir ofrece Guardar y salir / Salir sin
guardar / Cancelar. No hay guardado automático silencioso.

### Guardados locales

Crear un mundo ya guarda su estado inicial. Guardar desde pausa conserva los
cambios posteriores. Mundos muestra la carpeta absoluta y los errores de lectura.
Un archivo inválido permanece visible, y cargarlo informa el error sin sustituir
el mundo actual. JSON versión 1 se conserva; enemigos y estamina son de sesión.

La ruta se resuelve desde IDE/JAR hasta la raíz del proyecto, no desde el cwd.
Puede fijarse `-Dmc2.worlds.dir=/ruta/absoluta/worlds`. Copiar el JAR fuera del
proyecto puede cambiar la carpeta por defecto. El archivo local Jaimito.json
incompleto se preservó: no se elimina ni se presenta como lista vacía.

### Gráficos y GPU

LibGDX usa OpenGL. Opciones → Gráficos/GPU muestra la tarjeta en uso y ofrece
**Integrada · menor consumo**, **Dedicada · mayor rendimiento** y **Automática**.
Elige una y vuelve a abrir el juego: la selección se realiza antes del contexto.
La integrada selecciona por PCI el adaptador de arranque Mesa compatible; la dedicada
solicita PRIME NVIDIA o DRI_PRIME Mesa cuando Linux expone varios dispositivos.
Un fallo de arranque permite continuar con la selección del sistema.
La preferencia se guarda en graphics.properties, fuera de los mundos.
La terminal conserva GL_VENDOR, GL_RENDERER, GL_VERSION y backend.
También puede usarse `-Dmc2.gpu=INTEGRATED` o `-Dmc2.gpu=DEDICATED` sin guardar.

Pruebas físicas de la retroalimentación: Integrada → Intel/Mesa; Dedicada → NVIDIA RTX 4060.
AMD tiene ruta Mesa implementada y tests de selección; no hubo hardware AMD
para comprobarla. Windows/macOS delegan selección al sistema y no ofrecen aquí
el relanzamiento dedicado. Java 17 es el objetivo; el JDK 26 local mostró avisos
LWJGL/Unsafe aunque las pruebas gráficas pasaron.

Las comprobaciones que todavía no están en JUnit se ejecutan una a una:

```bash
java -cp target/classes manualtest.WorldBlockOperationsTest
java -cp target/classes manualtest.PlayerMovementManualTest
java -cp target/classes manualtest.PlayerPhysicsManualTest
java -cp target/classes manualtest.CollisionManualTest
java -cp target/classes manualtest.BlockInteractionAndPatternsManualTest
```

Los archivos creados desde el menú se guardan en la carpeta local `worlds/`. Esa carpeta está ignorada por Git.

## Limitaciones y revisión pendiente

- Mundos finitos de 4/100/256 chunks en memoria; no hay streaming, crafting ni multiplayer.
- Vida mínima: ataque enemigo letal, sin survival completo ni salud gradual.
- Percepción del zombie por distancia; detecta a través de paredes, pero navegación
  consulta terreno y el golpe cuerpo a cuerpo exige línea despejada.
- Separación local; no es pathfinding multiagente. Corredores pueden formar filas.
- Biomas/aldea/interacción están cubiertos por tests existentes; la revisión humana
  prolongada de equilibrio, cámara, melee, salto, R/J/K y estructuras queda pendiente.
- ENDURANCE nuevo es un minuto real headless, no una prueba térmica/GPU prolongada.

## Integrantes

- Estudiante 1: **Elioth Thomas Gomez Morales**.
- Estudiante 2: **Jasub Sastre**.
- Estudiante 3: **Ethian Daniel White Ortiz**.

## Modelado UML

El modelado UML de la asignatura está en [docs/uml.md](docs/uml.md). Incluye el diagrama de clases obligatorio y vistas complementarias de casos de uso, patrones, componentes y secuencias, actualizadas con la ventana 3D, los controles básicos y el observador visual concreto.

## Backlog del Corte 2

Consultar [TODO.md](TODO.md) para tareas por estudiante y dependencias, y
[ROADMAP_CORTE_2.md](ROADMAP_CORTE_2.md) para fases, ownership y gates.
La documentación de arquitectura está en `docs/corte2/`; los resultados vigentes
están en `docs/corte2/testing/integration-20260926.md`. Los resultados históricos
se conservan identificados por fecha. `docs/uml.md` incluye la arquitectura integrada.

## Actualizar desde main y jugar

Desde la raíz del repositorio Mc2, con JDK 17+ y Maven instalados:

```bash
git pull origin main
./Minecraft2/jugar.sh
```

El lanzador compila el código actualizado antes de abrir el juego. En IntelliJ:
actualizar main, recargar Maven y ejecutar `bootstrap.Minecraft2Application`.
`worlds/` y `graphics.properties` son locales y no se publican; cada computador
conserva sus mundos y elige su propia tarjeta gráfica. Las ramas de estudiantes
se mantienen sin cambios.

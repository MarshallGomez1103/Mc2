# Minecraft 2

Base arquitectónica de un videojuego voxel sencillo inspirado en Minecraft Classic. El repositorio contiene un punto de partida compilable en Java para que tres estudiantes desarrollen el MVP manualmente en IntelliJ IDEA.

> Este proyecto no es todavía un videojuego completo. Incluye estructuras, contratos, conexiones entre capas, generación de terreno, movimiento, física, interacción por raycast, renderizado voxel, controles básicos y persistencia JSON.

## Problema

El equipo necesita iniciar un videojuego voxel sin mezclar la interfaz, las reglas del mundo y el acceso a archivos. Una base sin separación de responsabilidades dificultaría que tres personas trabajen en paralelo y que el proyecto evolucione sin dependencias circulares.

## Objetivo

Ofrecer un esqueleto pequeño, claro y compilable que establezca:

- una arquitectura por capas;
- los modelos principales del dominio;
- los únicos tres patrones permitidos: Factory, Singleton y Observer;
- operaciones CRUD conceptuales para mundos guardados como JSON;
- un menú principal de consola desde el cual conectar los casos de uso.

## Alcance del MVP

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

## Fuera del alcance

- Un videojuego terminado o una réplica completa de Minecraft.
- Multijugador, red, enemigos, inventario avanzado o crafting.
- Texturas, carga dinámica de chunks y motor gráfico completo.
- Base de datos.
- Patrones distintos de Factory, Singleton y Observer.
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

Se ejecuta el jar y no `-cp target/classes`, porque ahora el proyecto depende de LibGDX: el jar generado incluye las librerías y los binarios nativos.

`mvn clean package` compila y ejecuta la batería de pruebas JUnit, así que sirve además como comprobación de CT-01.

### Controles del juego

Desde el menú, la opción **6. Jugar** abre la ventana 3D con el mundo cargado.

| Acción | Control |
| --- | --- |
| Moverse | W, A, S, D |
| Saltar | Espacio |
| Mirar | Ratón |
| Eliminar bloque | Clic izquierdo |
| Colocar bloque | Clic derecho |
| Elegir material | Teclas 1 a 7 |
| Volver al menú | ESC o cerrar la ventana |

Al volver al menú, "4. Guardar mundo actual" conserva los bloques colocados y eliminados.

`-Dfile.encoding=UTF-8` es necesario en Windows: sin él, Java 17 escribe en la codificación de la plataforma y los acentos del menú salen como `Opci�n`. En IntelliJ no hace falta.

Las comprobaciones que todavía no están en JUnit se ejecutan una a una:

```bash
java -cp target/classes manualtest.WorldBlockOperationsTest
java -cp target/classes manualtest.PlayerMovementManualTest
java -cp target/classes manualtest.PlayerPhysicsManualTest
java -cp target/classes manualtest.CollisionManualTest
java -cp target/classes manualtest.BlockInteractionAndPatternsManualTest
```

Los archivos creados desde el menú se guardan en la carpeta local `worlds/`. Esa carpeta está ignorada por Git.

## Estado actual e implementación intencionalmente pendiente

**El MVP está completo y es jugable.** Funcionan de extremo a extremo el menú y sus casos de uso, la generación pseudoaleatoria del terreno, el cálculo del chunk de cada posición con sus validaciones, el movimiento con salto, gravedad y colisiones, colocar y eliminar bloques apuntando con el ratón, el renderizado 3D del mundo y la persistencia JSON. El ciclo crear → jugar → guardar → cerrar → cargar conserva metadatos, jugador, chunks y bloques.

Quedan pendientes:

- texturas: los bloques se dibujan con color plano y sombreado por cara, sin imágenes;
- distancia de visión y carga dinámica de chunks: se genera y dibuja una cuadrícula fija de 2×2;
- portar a JUnit las comprobaciones de `manualtest`, que cubren CT-02 a CT-07 y hoy se ejecutan a mano;
- completar el nombre del tercer integrante en este README.

El detalle ejecutable de trabajo se encuentra en [TODO.md](TODO.md).

## Integrantes

- Estudiante 1: **Elioth Thomas Gomez Morales**.
- Estudiante 2: **Jasub Sastre**.
- Estudiante 3: **Ethian Daniel White Ortiz**.

## Modelado UML

El modelado UML de la asignatura está en [docs/uml.md](docs/uml.md). Incluye el diagrama de clases obligatorio y vistas complementarias de casos de uso, patrones, componentes y secuencias, actualizadas con la ventana 3D, los controles básicos y el observador visual concreto.

## Backlog de siguientes iteraciones

1. Incorporar texturas a los bloques, que por ahora usan color plano y sombreado por cara.
2. Implementar distancia de visión y carga dinámica de chunks; actualmente se genera una cuadrícula fija de 2×2.
3. Portar a JUnit las comprobaciones de `manualtest` que cubren CT-02 a CT-07.

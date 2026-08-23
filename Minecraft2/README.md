# Minecraft 2

Base arquitectónica de un videojuego voxel sencillo inspirado en Minecraft Classic. El repositorio contiene un punto de partida compilable en Java para que tres estudiantes desarrollen el MVP manualmente en IntelliJ IDEA.

> Este proyecto no es todavía un videojuego completo. Incluye estructuras, contratos, conexiones entre capas y una persistencia JSON inicial; no incluye motor gráfico, física ni generación de terreno terminados.

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
- Persistencia local en archivos JSON.

## Fuera del alcance

- Un videojuego terminado o una réplica completa de Minecraft.
- Multijugador, red, enemigos, inventario avanzado o crafting.
- Motor gráfico y físicas completos.
- Base de datos.
- Patrones distintos de Factory, Singleton y Observer.
- Frameworks o librerías que no sean necesarios para esta base.

## Tecnologías

- Java 17 o superior.
- IntelliJ IDEA.
- Maven únicamente para compilación reproducible.
- Archivos JSON locales, sin base de datos ni librería externa.

## Arquitectura

El código mantiene las tres capas solicitadas. Dentro de Domain/Game Logic se separa `application` (coordinación de casos de uso) de `domain` (modelo y reglas), sin convertirlas en capas independientes. Las dependencias avanzan hacia el dominio y no existe una dependencia desde este hacia la presentación o la persistencia.

```text
bootstrap ──> presentation ──> application ──> domain
   │                                 │
   ├──> persistence ──> domain       ├──> persistence
   └──> application                  └──> patterns.singleton ──> domain

domain.world ──> patterns.observer
patterns.factory ──> domain
```

- **Presentation:** el paquete `presentation` muestra el menú, captura datos y presenta resultados.
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
│   │   └── ct-11-flujo-mvp.md
│   ├── arquitectura.md
│   ├── decisiones-compartidas.md
│   ├── factory.md
│   ├── observer.md
│   └── singleton.md
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
│   │   └── MainMenu.java
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
java -Dfile.encoding=UTF-8 -cp target/classes bootstrap.Minecraft2Application
```

`mvn clean package` compila y ejecuta la batería de pruebas JUnit, así que sirve además como comprobación de CT-01.

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

Ya funcionan de extremo a extremo: la navegación del menú y los casos de uso, la generación pseudoaleatoria del terreno, el cálculo del chunk correspondiente a cada posición con sus validaciones de límites, el movimiento con salto, gravedad y colisiones, la selección de bloques por raycast, y la persistencia JSON completa. El ciclo crear → guardar → cerrar → cargar → eliminar conserva metadatos, jugador, chunks y bloques.

Quedan pendientes:

- renderizado voxel y ventana gráfica (LibGDX/LWJGL3);
- conectar el teclado y el clic real del ratón con los casos de uso;
- interfaz visual del menú principal, que sigue siendo de consola;
- confirmación antes de eliminar un mundo;
- automatizar las pruebas con JUnit 5. Por ahora las comprobaciones viven en el paquete `manualtest` y se ejecutan a mano: `WorldPersistenceTest` cubre CT-08 y CT-09, `BlockInteractionAndPatternsManualTest` cubre CT-06 y CT-07, y las de movimiento, física y colisión acompañan al jugador.

El detalle ejecutable de trabajo se encuentra en [TODO.md](TODO.md).

## Integrantes

- Estudiante 1: **Elioth Thomas Gomez Morales**.
- Estudiante 2: **Jasub Sastre**.
- Estudiante 3: **pendiente de completar**.

## Backlog

Completado: serialización y reconstrucción JSON, generación pseudoaleatoria, movimiento con salto, gravedad y colisiones, selección de bloques por raycast, y pruebas del flujo completo.

Pendiente, casi todo en la fase de integración del equipo:

1. Integrar LibGDX con backend de escritorio LWJGL3 para renderizar el mundo.
2. Renderizar el mundo y los ocho tipos de bloques.
3. Conectar teclado y ratón con `PlayerMovementService`, `CollisionResolver` y `PlayerInteractionService`, que ya existen pero todavía no tienen quien los invoque.
4. Conectar observadores concretos para reaccionar a cambios visuales y de guardado.
5. Portar a JUnit las comprobaciones de `manualtest` que cubren CT-02 a CT-07.

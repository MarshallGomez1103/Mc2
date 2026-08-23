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
│   ├── arquitectura.md
│   ├── factory.md
│   ├── observer.md
│   └── singleton.md
├── src/
│   ├── bootstrap/
│   │   └── Minecraft2Application.java
│   ├── application/
│   │   └── WorldApplicationService.java
│   ├── presentation/
│   │   └── MainMenu.java
│   ├── domain/
│   │   ├── Position.java
│   │   ├── block/
│   │   │   ├── Block.java
│   │   │   └── BlockType.java
│   │   ├── player/
│   │   │   └── Player.java
│   │   └── world/
│   │       ├── BlockChange.java
│   │       ├── Chunk.java
│   │       └── World.java
│   ├── patterns/
│   │   ├── factory/BlockFactory.java
│   │   ├── observer/
│   │   │   ├── Observer.java
│   │   │   └── Subject.java
│   │   └── singleton/WorldManager.java
│   └── persistence/
│       ├── JsonWorldStorage.java
│       └── WorldStorage.java
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
java -cp target/classes bootstrap.Minecraft2Application
```

Los archivos creados desde el menú se guardan en la carpeta local `worlds/`. Esa carpeta está ignorada por Git.

## Estado actual e implementación intencionalmente pendiente

La navegación del menú, la coordinación de casos de uso, el CRUD básico de archivos y el JSON mínimo con nombre del mundo están conectados. El campo `chunks` se escribe vacío como marcador del esquema futuro.

Quedaron pendientes de forma intencional:

- representación y serialización completas de chunks y bloques;
- generación pseudoaleatoria del terreno;
- renderizado voxel y ventana gráfica;
- controles por teclado y ratón;
- movimiento, colisiones, salto y gravedad;
- cálculo del chunk correspondiente a cada posición;
- reglas para colocar y eliminar bloques;
- validaciones de límites y tamaño del mundo;
- pruebas automatizadas.

El detalle ejecutable de trabajo se encuentra en [TODO.md](TODO.md).

## Integrantes

- Estudiante 1: **Elioth Thomas Gomez Morales**.
- Estudiante 2: **Jasub Sastre**.
- Estudiante 3: **pendiente de completar**.

## Backlog inicial

1. Completar la serialización y reconstrucción de bloques y chunks según las decisiones acordadas.
2. Crear la generación pseudoaleatoria sencilla para uno o pocos chunks.
3. Integrar LibGDX con backend de escritorio LWJGL3 para renderizar el mundo.
4. Renderizar el mundo y los ocho tipos de bloques.
5. Implementar movimiento, colisiones, salto y gravedad.
6. Implementar selección, colocación y eliminación de bloques por clic.
7. Conectar observadores concretos para reaccionar a cambios visuales y de guardado.
8. Añadir pruebas unitarias y pruebas manuales del flujo completo.

# Proyecto de Diseño de Software – Corte Uno
## Minecraft 2 — Voxel World Java

---

## 1. Presentación del Problema

Nuestro proyecto consiste en desarrollar el esqueleto arquitectónico de un videojuego voxel de un solo jugador, inspirado en Minecraft Classic — no una réplica del juego completo, sino una base de software que demuestre cómo estructurar un sistema de este tipo con buenas prácticas de diseño.

**¿Cuál es el problema y a quién afecta?** Al construir un videojuego en equipo, es muy fácil terminar con clases que mezclan interfaz gráfica, reglas del mundo y acceso a archivos en un mismo lugar. Esto afecta directamente a cualquier equipo de desarrollo pequeño (como el nuestro, de tres personas): si el código no separa responsabilidades desde el inicio, resulta casi imposible que varias personas trabajen en paralelo sin pisarse el trabajo, y cualquier cambio pequeño en una parte arriesga romper otra que no tenía relación.
Por otro lado, aunque el objetivo de un videojuego, entretenimiento, no resuelve una problemática directamente relacionada con necesidades básicas, es un estímulo al que responden todos los seres humanos, por lo tanto es relevante. Además, se busca con el proyecto expandir habilidades de diseño y arquitectura de videojuegos.

**¿Por qué es relevante resolverlo con software (y no con otro medio)?** El problema es inherentemente de diseño de software: no es un problema de negocio ni de proceso, sino de cómo se organiza el código para que sea mantenible, extensible y divisible entre varios desarrolladores. La única forma de "resolverlo" es con una arquitectura por capas, principios de diseño y patrones aplicados correctamente — no hay un sustituto no técnico.
Viendo otro dilema en paralelo, resolver el "hambre de entretenimiento" de las personas con desarrollo de software es de lo más eficiente en términos de "personas entretenidas", pues, con una estimación de tres mil millones de usuarios activos globalmente, es la forma de entretenimiento digital más usada de la historia.
(refs: -https://store.steampowered.com/charts/?l=spanish - https://www.thecommonsense.co.za/Culture/why-gaming-now-bigger-than-movies-music-combined)

**Alcance de este módulo (Corte 1).** Este corte entrega el MVP funcional: abrir el programa, ver el menú principal, crear un mundo con nombre, generar terreno automáticamente, guardar el mundo, listar y eliminar mundos existentes, entrar a un mundo, mover al jugador, saltar, aplicar gravedad, colisionar contra el terreno, mirar alrededor, colocar y eliminar bloques con clic, tener varios tipos de bloque, y conservar los cambios al salir y volver a cargar.

**Explícitamente fuera de alcance en este corte:** enemigos, crafting, inventario complejo, multijugador, animales, clima, ciclo día/noche, servidores, mundos infinitos, base de datos, y cualquier patrón de diseño distinto de Factory, Singleton y Observer. Estas exclusiones fueron una decisión de equipo tomada antes de programar, precisamente para poder terminar un módulo funcional y evaluable en el tiempo de este corte, en vez de perseguir un alcance que no se alcanzaría a completar.

## 2. Creatividad en la Presentación

🎥 `[https://gamma.app/docs/Minecraft-2-Voxel-World-Java-76i61vejazld1dh]`

## 3. Fundamentos de Ingeniería de Software

| Atributo de calidad | ¿Cómo se sostiene en el diseño? (evidencia concreta)                                                                                                                                                                                                                                     | ¿Qué se sacrificó a cambio?                                                                                                                                                                                                                                                             |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Mantenibilidad**  | El proyecto se divide en módulos (`presentation`, `application`, `domain`, `persistence`, `patterns`, `manualtest`, `bootstrap`) con dependencia en una sola dirección hacia el dominio. Un cambio en cómo se guarda un mundo (`JsonWorldStorage`) no obliga a tocar `World` ni el menú. | En cuanto a documentación, se tienen los READMEs, comentarios a lo largo de las clases y varios archivos .md que describen componentes del desarrollo.                                                                                                                                  
| **Eficiencia**      |Chunk está acotado a 16×64×16 bloques (16,384 bloques máximo por chunk). Esto no es casualidad — es una decisión explícita del equipo (docs/decisiones-compartidas.md) que limita cuánta memoria puede ocupar un solo chunk, en vez de dejarlo con tamaño arbitrario.
La conversión de posición a coordenada de chunk es una operación matemática O(1) (Math.floorDiv(x, 16)), no una búsqueda ni iteración sobre una lista de chunks existentes. Esto importa porque cada vez que el jugador se mueve, coloca o elimina un bloque, esta operación se ejecuta — que sea aritmética directa en vez de una búsqueda lineal es una decisión de rendimiento, aunque no esté "documentada" como tal explícitamente.


| **Escalabilidad ** | no la vimos en clase, pero está respaldada por el ISO 25010. persistence/WorldStorage.java (interfaz) vs JsonWorldStorage.java (implementación) — es el único punto concreto de bajo acoplamiento que dejaría espacio para escalar el almacenamiento sin tocar la capa de aplicación.


## 4. Diseño de Software

### 4.1 Principios SOLID aplicados

#### Single Responsibility Principle (SRP) — con antes/después

```text
❌ ANTES (violación hipotética): una sola clase Player con un método
   update() que hace todo:

   public class Player {
       Position position;
       void update(Input input, World world, double deltaSeconds) {
           // leer teclado y calcular hacia dónde moverse
           // aplicar gravedad y salto
           // revisar si choca contra bloques del World
           // decidir si el clic apunta a un bloque y colocarlo/eliminarlo
       }
   }

   Problema: cualquier cambio — ajustar la velocidad de caminar, cambiar
   la fórmula de gravedad, o mejorar cómo se resuelven las esquinas en
   la colisión — obliga a modificar el mismo método gigante, con riesgo
   de romper una parte al tocar otra que no tenía relación. Además, es
   imposible probar la colisión sin también ejecutar el input y la física.

✅ DESPUÉS (aplicando SRP, código real del proyecto): la responsabilidad
   se dividió en cuatro clases, cada una con una sola razón para cambiar:

   - Player                — solo guarda estado (posición, orientación,
                              velocidad vertical, si está en el suelo).
   - PlayerMovementService — solo calcula el desplazamiento horizontal
                              deseado según la entrada y la orientación.
   - PlayerPhysics         — solo gravedad y salto.
   - CollisionResolver     — solo resuelve colisiones contra World.

   Por qué resuelve el problema: un cambio en la velocidad de caminar
   solo toca PlayerMovementService; un cambio en la gravedad solo toca
   PlayerPhysics. Ninguno de los dos arriesga romper la colisión o el
   estado del jugador, y cada clase se puede probar por separado
   (de hecho, cada una tiene su propia clase de verificación manual).
```

#### Open/Closed Principle (OCP)

**Dónde se aplica:** `World.placeBlock`/`removeBlock` notifican cambios (`BlockChange`) a través del método `notifyObservers`, que recorre la lista interna de observadores sin preguntar qué tipo concreto es cada uno.

**Por qué esta decisión y no otra:** se pudo agregar un observador de prueba (para verificar que la notificación ocurre exactamente una vez al colocar/eliminar un bloque) sin modificar ni una línea de `World.java`. La clase queda cerrada a modificación pero abierta a extensión: cualquier clase nueva que implemente `Observer<BlockChange>` puede sumarse en cualquier momento futuro (por ejemplo, un componente visual).

#### Interface Segregation Principle (ISP)

**Dónde se aplica:** la interfaz `Observer<T>` (paquete `patterns.observer`) exige un solo método, `update(T)`.

**Por qué esta decisión y no otra:** se descartó deliberadamente una interfaz más "completa" con métodos separados por tipo de evento (`onPlaced()`, `onRemoved()`, `onError()`), porque obligaría a cualquier observador simple (como el de conteo usado en pruebas) a implementar métodos que no necesita. Un contrato de un solo método evita ese costo.

#### Dependency Inversion Principle (DIP)

**Dónde se aplica:** `World` depende de la abstracción `Observer<BlockChange>`, nunca de una clase concreta de observador. De forma análoga, `WorldApplicationService` depende de la interfaz `WorldStorage`, no directamente de `JsonWorldStorage`.

**Por qué esta decisión y no otra:** permite sustituir la implementación concreta (el observador, o el mecanismo de guardado) sin tocar la clase de alto nivel que depende de la abstracción — y permite probar `World` con un observador de mentira sin depender de ningún componente gráfico real.

### 4.2 Patrones de diseño utilizados

| Patrón | Categoría | Problema que resuelve aquí | Por qué no se usó [alternativa] |
|---|---|---|---|
| **Factory** (`BlockFactory`) | Creacional | Centraliza la creación de objetos `Block` a partir de un `BlockType` y una `Position`, evitando que la construcción de bloques (con sus validaciones) quede dispersa por todo el código que genera terreno o coloca bloques. | Se descartó Abstract Factory porque no hay familias de objetos relacionados que crear (solo una variante de `Block` según el tipo), y se descartó Builder porque un `Block` no requiere construcción paso a paso — se arma completo con dos datos. |
| **Observer** (`World` como *Subject*, `BlockChange` como notificación) | Comportamiento | Permite que distintos componentes reaccionen a cambios de bloques (colocar/eliminar) sin que `World` conozca ni dependa de esos componentes concretos. | Se descartó un bus de eventos global porque el MVP solo necesita un tipo de evento (`BlockChange`) y un solo emisor (`World`); un bus completo añadiría complejidad innecesaria para este alcance. |

*(El proyecto también usa Singleton — `WorldManager`, creacional — pero se documenta aparte porque el criterio de la rúbrica pide como mínimo un creacional y un estructural/comportamiento, ya cubiertos arriba con Factory y Observer. Singleton se detalla en `docs/singleton.md`.)*

### 4.3 Modelado UML
Se encuentra en la carpeta docs/
## 5. Implementación

### Estructura de paquetes

```text
src/
├── bootstrap/       — construye y conecta los objetos concretos al iniciar (Minecraft2Application)
├── presentation/     — menú de consola, sin lógica de negocio (MainMenu)
├── application/      — casos de uso: crear/listar/cargar/guardar/eliminar mundo, generación de chunk,
│                       interacción del jugador con el mundo
├── domain/           — modelo y reglas del juego, Java puro, sin dependencias gráficas ni de persistencia
│   ├── block/        — Block, BlockType
│   ├── player/        — Player, MovementInput, PlayerMovementService, PlayerPhysics, CollisionResolver
│   └── world/         — Chunk, World, BlockChange
├── patterns/         — únicamente Factory, Observer y Singleton
└── persistence/       — contrato CRUD (WorldStorage) e implementación JSON (JsonWorldStorage)
```

### Enlaces a clases donde se aplican patrones/principios

- SRP: `src/domain/player/Player.java`, `PlayerMovementService.java`, `PlayerPhysics.java`, `CollisionResolver.java`.
- OCP / DIP / ISP: `src/domain/world/World.java`, `src/patterns/observer/Observer.java`, `Subject.java`.
- Factory: `src/patterns/factory/BlockFactory.java`.
- Singleton: `src/patterns/singleton/WorldManager.java`.

### Instrucciones de ejecución

**IntelliJ IDEA:**
1. Abrir la carpeta raíz `Minecraft2`.
2. Configurar un JDK 17 o superior si IntelliJ lo solicita.
3. Esperar a que IntelliJ importe `pom.xml`.
4. Ejecutar el método `main` de `bootstrap.Minecraft2Application`.

**Terminal:**
```bash
mvn clean package
java -cp target/classes bootstrap.Minecraft2Application
o también
java -Dfile.encoding=UTF-8 -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

Los mundos creados desde el menú se guardan en la carpeta local `worlds/` (ignorada por Git).

## 6. Análisis Técnico

### Cohesión y acoplamiento (con ejemplos concretos)

**Alta cohesión:** `Chunk` agrupa únicamente lo relacionado con administrar bloques dentro de sus límites de 16×64×16 y resolver a qué chunk pertenece una posición — no mezcla generación de terreno ni persistencia. La división de `Player` en cuatro clases (sección 4.1) también aumenta la cohesión frente a tener un único `Player` con toda la lógica: cada clase agrupa solo los métodos que cambian juntos por la misma razón.

**Bajo acoplamiento:** se logra principalmente por dependencia hacia abstracciones y no hacia implementaciones concretas. `World` se acopla a la interfaz `Observer<BlockChange>`, nunca a una clase de observador específica; `WorldApplicationService` se acopla a `WorldStorage`, no a `JsonWorldStorage`. Además, la regla de dependencia unidireccional entre capas (`domain` nunca importa de `presentation`, `application` ni `persistence`) evita dependencias circulares entre capas.

El único acoplamiento fuerte y deliberado del proyecto es la dependencia hacia el Singleton `WorldManager` — aceptable porque está explícitamente acotado a una sola responsabilidad (mantener el mundo actualmente cargado) y es, por decisión de equipo, el único punto de acceso global permitido.

### Qué extensiones futuras facilita el diseño, y cuáles no

**Facilita:** agregar nuevos tipos de reacción a cambios de bloque (por ejemplo, un componente visual que redibuje solo el bloque afectado, o un sistema de sonido) sin modificar `World`, gracias al patrón Observer. También facilita cambiar el mecanismo de persistencia (por ejemplo, a otro formato de archivo) sin tocar `WorldApplicationService`, gracias a depender de `WorldStorage` como interfaz.

**Límites honestos del diseño:** el diseño actual **no** facilita por sí solo agregar mundos verdaderamente grandes o infinitos — `World` mantiene una lista simple de chunks en memoria, sin estrategia de carga/descarga por distancia al jugador; eso requeriría rediseñar `World` y `WorldManager`, no es una extensión gratuita del diseño actual. Tampoco facilita multijugador: el modelo de dominio asume un único `Player` implícito y no separa "el mundo" de "la vista de un jugador sobre el mundo" — agregar un segundo jugador simultáneo exigiría cambios estructurales, no solo una clase nueva.

## 7. Créditos y Roles

| Integrante | Rol / contribución principal |
|---|---|
| Elioth Thomas Gómez Morales | Estudiante 1 — Mundo y generación: `Chunk`, generación pseudoaleatoria de terreno, `BlockFactory`, distribución de tipos de bloque, validaciones de `Chunk`. |
| Jasub Sastre | Estudiante 2 — Jugador e interacción: estado y orientación del jugador, movimiento horizontal, gravedad y salto, colisiones básicas, selección de bloque por clic, conexión con `World.placeBlock`/`removeBlock`, verificación de los patrones Observer y Singleton. |
| *(pendiente de completar)* | Estudiante 3 — Presentación y persistencia: interfaz del menú principal, conexión de pantallas de crear/listar/cargar/guardar/eliminar, escritura y reconstrucción JSON de chunks y bloques, manejo de errores y confirmación antes de eliminar un mundo. |

---

### Recordatorio de entregables (según enunciado oficial)
- [x] Repositorio en GitHub con código y documentación
- [x] Este Wiki completo
- [ ] Presentación creativa del problema
- [no] *(Opcional)* Video técnico explicando la solución (máx. 5 min)
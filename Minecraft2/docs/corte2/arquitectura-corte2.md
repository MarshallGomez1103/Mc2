# Arquitectura Corte 2 — acuerdos y estado actual

Estado: generación/estructuras y ampliación finita implementadas; enemigos y
calidad final pendientes. Este documento conserva los acuerdos iniciales.

## Objetivo y alcance

Extender el MVP sin reescritura, renombrado de paquetes ni nuevas capas.
Mantener Java 17 objetivo, Maven, LibGDX y presentation/application/domain/persistence/patterns.
KISS, YAGNI, cohesión y testabilidad; priorizar archivos nuevos y cambios pequeños.

## Base real que se preserva

World contiene Player y una lista/index de chunks. Chunk mide 16×64×16 y almacena
bloques por Position absoluta. World.findChunk usa índice; la creación genera
2×2, 10×10 o 16×16. VoxelGame crea/libera mallas cercanas, filtra frustum y muestra
FPS; J/K ajustan radio visible. GameInput coordina física del jugador y Shift corre.
El JSON versión 1 guarda snapshot del mundo y jugador, no regenera desde seed.
No existe sistema genérico Entity ni streaming. `PlayerLife` maneja muerte de sesión
por vacío y expone `die(ENEMY)` para la integración futura.

Hay dependencia de paquetes domain.world ↔ domain.player por World/CollisionResolver;
no se promete eliminarla en esta preparación. No hay dominio dependiente de LibGDX.

## Contratos mínimos disponibles

| Tipo | Ruta | Acuerdo |
| --- | --- | --- |
| ZombieState | src/domain/enemy/ZombieState.java | IDLE, CHASE, ATTACK, DEAD; sin transiciones |
| BiomeType | src/domain/world/biome/BiomeType.java | PLAINS, DESERT, MOUNTAINS; BiomeResolver ya lo resuelve regionalmente |
| Difficulty | src/domain/enemy/Difficulty.java | NORMAL, VERY_HARD; sin parámetros |

Difficulty pertenece al dominio para que ZombieConfig pueda consumirlo sin importar
application. GameSettings se deja para Estudiante 3; no fijamos todavía campos/defaults.
FOREST no se agrega por anticipación; VILLAGE se trata como estructura.

## Decisiones acordadas

- Zombie no hereda de Player; no crear jerarquía grande ni ECS.
- FSM decide qué hacer; A* decide por dónde ir. Sin Behavior Tree, ML o LLM.
- IA y navegación puras no importan LibGDX.
- Enemigos son estado de sesión: NO persistir zombies ni modificar WorldJsonCodec para ellos.
- Mantener JSON y Worlds existentes. Settings de sesión: propuesta pendiente de confirmar;
  no extender el formato innecesariamente.
- A* 2.5D X/Z + altura caminable del World ACTUAL, con apoyo, espacio y desnivel admisible.
- Repath por cambios relevantes, fin/invalidez de ruta o intervalo; no A* por zombie/frame.
- HordeManager coordina oleadas, no movimiento individual.
- Dificultad modifica parámetros centralizados, no duplica IA.
- Texturas de enemigos se controlarán separadamente de texturas de bloques.
- TDD formal exclusivamente sobre ZombieStateMachine, RED → GREEN → REFACTOR real.
- PIT: objetivo aproximado 80% en lógica pura seleccionada, no score global del juego.
- Mundo infinito/streaming NO pertenece al Corte 2; cuatro chunks siguen siendo baseline Pequeño.
- Estructuras iniciales pueden limitarse a un chunk; documentar decisión y comprobar bordes.

## Integraciones futuras, no código existente

EnemyUpdateService coordinará FSM, percepción, navegación y movimiento.
VoxelGame solo conectará actualización y representación; no contendrá reglas de IA.
HordeManager tendrá lifecycle propio y registrará enemigos por un contrato acordado.
No se decide aún una API Java de registro o caminabilidad: primero pactarla entre owners.
Ver [roadmap](../../ROADMAP_CORTE_2.md) para secuencia y ownership.

## TODO: acuerdos antes de desarrollo dependiente

- [ ] Definir cuerpo del zombie, apoyo, vecinos 4/8, subidas/caídas y groundY vs pies.
- [ ] Definir resultado sin ruta y política de invalidación de bloques.
- [ ] Acordar API de registro/consulta de enemigos entre HordeManager y EnemyUpdateService.
- [ ] Acordar percepción, cooldown, salud/daño y forma mínima de demostrar ataque/muerte
      sin implementar survival completo.
- [ ] Acordar valores/defaults de Difficulty y GameSettings y lifecycle al reabrir ventana.
- [x] Decidir escala de biomas: 18, con ejemplo reproducible seed 17.
- [x] Revisar caída fuera del mundo, guardado, mallas vecinas y colocación dentro
      del jugador; las correcciones se documentan en world-size-render-and-life.md.

## Evidencias posteriores

Pendientes: diagramas de IA, APIs finales entre owners, gate final y verificación
manual completa. El gate local del frente de generación es 63/63.

# Plan de trabajo — Jasub (Estudiante 2, Enemy AI)

Lee primero `TODO.md`, `ROADMAP_CORTE_2.md`, `docs/corte2/CONTRATOS_DE_TRABAJO.md`
y `docs/corte2/enemy-ai.md`. Trabaja solo en `feature/c2-jasub`; actualiza tu rama
desde `main` antes de empezar y comprueba `git status`. No modifiques generación,
PIT, menú ni JSON. No hagas commit/push automático: solicita aprobación de Jasub.

## Base disponible y contratos que no debes romper

- `World` tiene índice de chunks y puede contener 4, 100 o 256 chunks. Lee el estado
  **actual** con `findChunk/getBlock`; no uses solo `surfaceHeightAt` para navegar.
- `VoxelGame` crea mallas cercanas con presupuesto de dos por frame, las libera fuera
  del radio J/K y muestra FPS. Conserva ese loop al integrar enemigos.
- `GameInput` ya asigna clic izquierdo a romper bloques, derecho a colocar, Shift a
  correr. Reserva el clic izquierdo para golpear un zombi bajo la mira **solo si está
  en alcance**; si no hay zombi válido, debe seguir rompiendo bloques. Resuelve qué
  objetivo está delante y más cerca para que el golpe no atraviese una pared.
- `PlayerLife.die(PlayerLife.DeathCause.ENEMY)` es la entrada existente para muerte
  letal del jugador; pantalla «MORISTE» y R ya funcionan para caída. No cambies JSON.
- El mundo Grande puede exigir `-Xmx2g` al cargar; pruebas de IA headless deben
  empezar en Pequeño/Mediano. No generes ni busques rutas por todos los 256 chunks
  para cada zombi/frame.

## Checkpoints verificables

1. [ ] Acordar con Ethian API mínima de creación/registro/despawn, parámetros de
   dificultad y opciones visuales; dejar decisión escrita. No tocar su código.
2. [ ] TDD real de `ZombieStateMachine`: escribir primero tests de IDLE, CHASE,
   ATTACK, DEAD y límites; ejecutar RED y registrar salida; implementar GREEN;
   simplificar en REFACTOR; guardar evidencia en `testing/tdd-zombie-fsm.md`.
3. [ ] `Zombie` lógico puro, salud, posición y estado, sin heredar Player ni importar
   LibGDX. Separar percepción/transiciones de pathfinding.
4. [ ] `NavigationNode/Grid` 2.5D sobre X/Z + altura caminable del World actual:
   bloque de apoyo sólido, espacio para cuerpo, desnivel válido y chunk presente.
   Probar bloque colocado/destruido y frontera entre chunks.
5. [ ] A* real con open/closed, g/h/f, parent y reconstrucción. Probar recto,
   obstáculo/rodeo, imposible, origen=destino y desnivel inválido.
6. [ ] Seguimiento de waypoints con colisión/altura; repath por cambio de objetivo,
   ruta terminada/inválida o intervalo. Medir conteo de A*; nunca buscar por zombi
   en cada frame. Prueba de integración Enemy + World con edición de bloques.
7. [ ] `EnemyUpdateService` actualiza FSM, percepción, movimiento, ataque y muerte
   usando delta. Conectar el spawn por contrato acordado con Ethian.
8. [ ] En ventana acordada, integrar en `VoxelGame` sin deshacer J/K, HUD, muerte
   por vacío o liberación de recursos. Cubo/modelo sencillo y textura opcional.
9. [ ] Clic izquierdo: raycast de zombi **antes** de borrado de bloque, con prioridad
   por distancia/oclusión; golpe baja salud y puede llevar a DEAD. No impedir picar
   cuando no hay zombi. Probar ambos caminos.
10. [ ] Al morir un zombi, efecto visual breve tipo explosión/partículas que luego
    se libera. Es **solo visual**: no rompe bloques, no altera World ni causa daño
    de área. Despawn al terminar efecto. Probar que el número de recursos/entidades
    no crece indefinidamente tras muertes repetidas.
11. [ ] Ataque letal usa `PlayerLife.die(ENEMY)` y muestra pantalla existente;
    enemigos dejan de actualizarse cuando están desactivados o el jugador murió.
12. [ ] Ejecutar `mvn clean test`; smoke manual: spawn, chase, rodeo, golpe con
    clic izquierdo, borrado de bloques sin zombi, ataque, muerte visual, R y J/K.
    Documentar evidencia real y limitaciones.

Cada checkpoint relevante: revisar diff y tests antes del commit pequeño. Avisar
antes de tocar `World.java`, `VoxelGame.java`, `MainMenu.java`, `pom.xml` o README.
No declarar completado hasta que Jasub haya probado su rama y el equipo integre.

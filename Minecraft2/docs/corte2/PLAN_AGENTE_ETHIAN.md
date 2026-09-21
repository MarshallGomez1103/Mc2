# Plan de trabajo — Ethian (Estudiante 3, hordas/configuración/calidad)

Lee `TODO.md`, `ROADMAP_CORTE_2.md`, `docs/corte2/CONTRATOS_DE_TRABAJO.md` y
este plan. Trabaja solo en `feature/c2-Ethian`; actualiza desde `main` y comprueba
`git status` antes de modificar. No implementes FSM, A*, biomas ni generación.
No hagas commit/push automático: solicita aprobación de Ethian.

## Base nueva que debes preservar

- `MainMenu` pregunta tamaño al crear: Pequeño 2×2, Mediano 10×10, Grande 16×16.
  Agrega opciones de enemigos/dificultad sin eliminar esa pregunta ni el CRUD.
- `WorldDirectory` resuelve `worlds/` de forma estable aunque se ejecute el JAR
  desde otra carpeta. No regreses a `Path.of("worlds")` relativo al CWD.
- `VoxelGame` tiene J/K, FPS, Shift, muerte por vacío y recuperación R. Ethian no
  edita este hotspot sin coordinar con Jasub. La opción textura de enemigos no es
  la misma que la opción ya existente de textura de bloques.
- Grande mantiene 256 chunks y bloques en memoria; el JSON de ~93 MB puede fallar
  con `-Xmx1g` al cargar. Recomienda `-Xmx2g`, mide antes de prometer soporte en 8 GB.
- Los zombies son estado de sesión; no modificar `WorldJsonCodec` para guardarlos.

## Checkpoints verificables

1. [ ] Acordar con Jasub contrato mínimo de spawn/registro/despawn y parámetros
   consumidos por su servicio. Definir defaults claros y decisiones por escrito.
2. [ ] `GameSettings`: enemies ON/OFF, enemy textures ON/OFF y Difficulty;
   defaults probados, sin condicionales `if (VERY_HARD)` dispersos.
3. [ ] NORMAL y VERY_HARD: centralizar velocidad, detección, cooldown, repath,
   oleada e intervalo. Probar diferencias de comportamiento, no getters triviales.
4. [ ] `HordeManager` determinista con tiempo explícito: comienzo, cantidad,
   intervalo, término y siguiente oleada; no ejecuta FSM/A* ni renderiza.
5. [ ] `Enemies OFF` impide spawn y actualización. Probarlo en dominio y en la
   conexión de aplicación con Jasub.
6. [ ] En ventana coordinada, añadir ajustes mínimos al menú de consola.
   Preservar crear/listar/cargar/guardar/borrar y elección de tamaño.
7. [ ] Tests unitarios de settings/hordas y alguna integración de oleada + registro
   de zombies mediante contrato acordado, sin OpenGL ni JSON real innecesario.
8. [ ] PIT en `pom.xml` con scope pequeño de lógica pura (FSM, A*, resolver de biomas,
   hordas según madurez); registrar generated/killed/survived/no coverage/score.
   Objetivo ~80 % del scope, no global. Analizar mutantes de conducta.
9. [ ] Harness de carga separado del gate JUnit normal: BASELINE, PEAK, STRESS,
   ENDURANCE; medir A* calls/ms, AI update ms/p95, heap, enemigos, generación y
   errores. Registrar hardware, JVM, flags, seed, warm-up, duración, commit.
10. [ ] Incluir una comprobación de memoria de ciclos spawn/update/death/despawn;
    no declarar «sin memory leak» a partir de heap puntual. GPU/native y 8 GB
    físicos quedan sin verificar salvo medición real.
11. [ ] `mvn clean test` verde, `git diff --check`, smoke manual de menú y opciones
    en Pequeño/Mediano. Documentar resultados reales; README/UML solo en ventana
    de integración acordada.

Coordinar `MainMenu.java`, `pom.xml`, `World.java`, `VoxelGame.java` y README antes
de editar. En particular, Jasub integra ataque visual y clic izquierdo; Ethian
provee configuración/hordas, no reimplementa movimiento ni renderer. Cada commit
debe ser pequeño, descriptivo y revisado antes de push a **su** rama.

# Mutation testing — preparación

Estado: PIT NO está configurado ni ejecutado en esta fase. pom.xml no se modifica.
Owner de POM, configuración y análisis: Estudiante 3.

## Objetivo y scope futuro

Aproximadamente 80% mutation score sobre un alcance pequeño de lógica pura,
no 80% global de Minecraft 2. Seleccionar explícitamente clases ya implementadas
y con tests: ZombieStateMachine, AStarPathfinder, BiomeResolver o HordeManager.
No es obligatorio incluirlas todas en la primera demostración.

Excluir inicialmente VoxelGame, GameWindow, rendering LibGDX, bootstrap y atlas.
No mutar enums para fabricar un resultado de esta preparación.

## Procedimiento pendiente

- [ ] Elegir scope y justificar comportamiento cubierto.
- [ ] Configurar PIT/JUnit 5 en pom.xml, con compatibilidad de JVM verificada.
- [ ] Tener en cuenta sourceDirectory=src y testSourceDirectory=test.
- [ ] Ejecutar gate unitario previo, PIT y revisar cada superviviente relevante.
- [ ] Agregar solo pruebas de comportamiento; separar equivalentes y limitaciones.
- [ ] Registrar versiones, comando reproducible, duración y ruta del reporte.
- [ ] Si no llega al objetivo, informar resultado real sin cambiar scope para maquillarlo.

No registrar score sin reporte real. Score reportado:
killed / generated × 100, aclarando categorías y denominador del reporte;
no confundir con test strength o line coverage.

## Resultados futuros — no ejecutados

| Revisión/scope | Generated | Killed | Survived | No coverage | Otros estados | Score |
| --- | --- | --- | --- | --- | --- | --- |
| Pendiente | N/D | N/D | N/D | N/D | N/D | N/D |

Los main manuales no reemplazan tests JUnit para PIT. Compatibilidad exacta con
OpenJDK local 26, versión PIT y plugin JUnit queda pendiente; no hay resultados inventados.

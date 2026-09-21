# Generación procedural 2.0 — Estudiante 1

Estado: implementado y probado en dominio e integración headless. Se inspeccionaron
visualmente biomas, relieve y una casa; queda pendiente la interacción manual completa.

## Flujo real

`WorldApplicationService` crea ahora 2×2, 10×10 o 16×16 chunks según la opción,
siempre con la seed del mundo. Detalles y límites: [mundos y distancia visible](world-size-render-and-life.md).
`ChunkGenerationService` sigue materializando los `BlockType` no AIR mediante
`BlockFactory`. El formato JSON no cambió. Para sostener tamaños mayores, `World`
indexa chunks y `VoxelGame` crea/libera mallas próximas; esas extensiones se describen
en el documento de [tamaños y rendering](world-size-render-and-life.md).

`SimpleTerrainGenerator` conserva su API de Corte 1 y delega la selección regional en
`BiomeResolver`. El nombre de la clase se conserva para evitar un refactor transversal.
La función es determinista para seed y coordenadas enteras, incluidas negativas, y no
depende del orden de generación. Los mundos ya guardados cargan sus bloques JSON
existentes; no se regeneran ni se sobrescriben las ediciones del jugador.

## Biomas y alturas

`BiomeResolver(seed).biomeAt(worldX, worldZ)` usa ruido de valor 2D interpolado con
escala 18. La señal es continua; umbrales `< 0.35`, `0.35..0.65` y `> 0.65` seleccionan
DESERT, PLAINS y MOUNTAINS. Un umbral define el material visual, no una decisión
aleatoria independiente por bloque. No está garantizado que toda seed muestre los
tres biomas en solo 32×32 bloques; pueden comprobarse con la seed 17.

La altura natural combina relieve suavizado (escala 14), detalle (escala 7) y una
contribución de montaña que empieza antes del umbral de MOUNTAINS. Se limita a
`18..45`, por debajo de `Chunk.HEIGHT = 64`. La transición reduce saltos abruptos
entre biomas. Capas:

| Bioma | Superficie | Dos capas bajo ella | Inferior | Árboles |
| --- | --- | --- | --- | --- |
| PLAINS | GRASS | DIRT | STONE | Ocasionales, WOOD/LEAVES |
| DESERT | SAND | SAND | STONE | No |
| MOUNTAINS | GRAVEL o STONE según altura | DIRT | STONE | No |

No se implementó FOREST, física de arena, agua ni materiales nuevos. No se añadió
dependencia de noise: `SpatialNoise` es una función pequeña y pura.

## Estructura mínima

`VillageGenerator` decide ubicación por seed y coordenadas de chunk; `VillagePlan`
define la plantilla. Una ubicación candidata necesita PLAINS en toda la zona reservada,
variación natural de altura de como máximo cinco bloques y una decisión determinista
de probabilidad inicial 50 %. La zona reservada se nivela a la altura central.
La plantilla es una casa de madera 6×6, puerta abierta, ventanas como huecos AIR en
varias paredes y camino de grava. AIR explícito
mantiene vacío el interior. Los árboles se excluyen de la zona reservada.

Toda la estructura queda dentro de un solo chunk (margen local X 4..11, Z 1..11),
incluidos los bordes nivelados. No es una aldea extensa; puede no aparecer en un mundo
2×2 cuando ningún chunk cumple las condiciones. Ejemplo reproducible: seed `17`,
estructura en chunk `(0,1)`, altura nivelada `27`.

## Evidencia automatizada y límites

`BiomeResolverTest`: misma seed, negativos, cambio de seed, presencia de los tres
biomas y continuidad regional. `TerrainGenerationTest`: capas, altura/límites,
ausencia de árboles desérticos, árboles ocasionales en PLAINS, independencia del
orden de generación, igualdad entre instancias, fronteras de chunks, plantilla,
JSON existente y edición del jugador.

El 21 de septiembre de 2026, `mvn clean test` pasó con 63 pruebas (41 previas y 22
nuevas, incluidos tamaños, vida, sprint y colisiones), sin fallos, errores ni ignoradas. Esto valida lógica y serialización; no
sustituye la revisión visual del juego ni mediciones de rendimiento bajo carga.
Además, `mvn clean package` pasó y una ejecución del JAR desde una carpeta temporal
creó y guardó un mundo nuevo mediante el menú sin errores. Los archivos de esa prueba
temporal se eliminaron al finalizar; no se alteró el directorio `worlds/` del proyecto.

Se abrió Mediano y Grande y se observaron visualmente biomas y relieve. Una captura
temporal con seed 17 y cámara frente al chunk `(0,1)` mostró la casa y el camino.
Pendiente para la integración final: probar J/K y reaparición con teclado real, y
actualizar README/UML en ventana coordinada. No implementar mundo infinito en Corte 2.

# Generación procedural 2.0 — preparación

Estado: planificado. Solo se creó BiomeType; el generador del Corte 1 no cambió.

## Base preservada

SimpleTerrainGenerator utiliza seed + hash de coordenadas: altura 18..22,
STONE, dos capas DIRT, superficie GRASS/SAND/GRAVEL y árboles WOOD/LEAVES.
ChunkGenerationService genera bloques no AIR y cuatro chunks iniciales.
Dimensiones 16×64×16 y coordenadas absolutas con negativos mediante floorDiv.
No hay regiones de bioma, montañas coherentes ni aldeas implementadas.

## Decisiones

BiomeResolver debe ser puro y determinista por seed + coordenadas, con regiones
coherentes, no elecciones independientes por bloque. No añadir librería noise
sin necesidad demostrada. Regenerar coordenadas arbitrarias debe ser determinista.
No implementar streaming ni mundo infinito.

| Bioma obligatorio | Comportamiento futuro |
| --- | --- |
| PLAINS | Altura moderada y variación suave; GRASS/DIRT/STONE; árboles ocasionales |
| DESERT | SAND predominante, plano relativo, sin árboles o muy reducidos |
| MOUNTAINS | Altura espacialmente coherente, dentro de límites del chunk |

FOREST es opcional, no parte del vocabulario base obligatorio.
La escala regional debe permitir demostrar biomas en un mundo finito;
no aumentar chunks o dimensiones inadvertidamente.

## Estructura mínima

VILLAGE no es bioma. Separar elección determinista de ubicación de plantilla de
bloques: 1–3 casas simples y camino básico. Para primera versión se permite contener
estructura en un solo chunk si cruzar bordes complica demasiado.
Acordar apoyo/nivelación, reservas para árboles y límites antes de integrar.
No introducir física de caída de arena.

## TODO y pruebas

- [ ] Resolver regiones e incluir coordenadas negativas.
- [ ] Implementar altura suave y capas de cada bioma.
- [ ] Separar decisión/plantilla de estructura determinista.
- [ ] Tests misma seed, orden de generación independiente y límites de altura.
- [ ] Tests de chunks vecinos: continuidad de función regional y estructuras sin corte.
- [ ] Verificar que JSON preserva resultado y ediciones sin modificar esquema.
- [ ] Registrar seeds reproducibles, resultados y capturas reales posteriormente.

Owner: Estudiante 1; no modificar IA o VoxelGame.

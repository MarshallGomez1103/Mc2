# Factory de bloques

## Intención

`patterns.factory.BlockFactory` concentra la construcción de bloques. El resto del código solicita un bloque indicando `BlockType` y `Position`, sin repartir llamadas al constructor por la futura lógica de generación e interacción.

```java
BlockFactory factory = new BlockFactory();
Block grass = factory.create(BlockType.GRASS, new Position(1, 4, 2));
```

La fábrica acepta exactamente los tipos definidos para el MVP:

- AIR
- GRASS
- DIRT
- STONE
- SAND
- GRAVEL
- WOOD
- LEAVES

## Límite actual

Todos los tipos producen la misma clase sencilla `Block`. No se han inventado dureza, texturas, herramientas, herencia de materiales ni reglas especiales. Esos detalles solo deben añadirse si el equipo los necesita dentro del alcance.

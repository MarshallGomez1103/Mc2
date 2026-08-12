# Singleton del mundo actual

## Intención

`patterns.singleton.WorldManager` administra la referencia al mundo actualmente cargado. `getInstance()` devuelve siempre la misma instancia, mientras `load`, `getCurrentWorld` y `unload` controlan su estado mínimo.

```java
WorldManager manager = WorldManager.getInstance();
manager.load(world);
manager.getCurrentWorld();
manager.unload();
```

## Restricción

`WorldManager` es el único Singleton del proyecto. `BlockFactory`, los servicios, el menú y la persistencia son objetos normales. Al continuar el desarrollo no se deben convertir otras clases en Singleton.

El Singleton no guarda archivos ni genera chunks; esas responsabilidades pertenecen a persistencia y dominio, respectivamente.

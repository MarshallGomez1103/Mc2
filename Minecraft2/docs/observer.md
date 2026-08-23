# Observer de cambios de bloques

## Intención

Observer permite que componentes futuros reaccionen a la colocación o eliminación de un bloque sin introducir esas reacciones dentro de `World`.

- `Observer<T>` recibe una notificación.
- `Subject<T>` registra, retira y notifica observadores.
- `World` implementa `Subject<BlockChange>`.
- `BlockChange.Type` distingue `PLACED` y `REMOVED`.

Ejemplo conceptual:

```java
world.addObserver(change ->
        System.out.println(change.type() + " " + change.block().getPosition())
);
```

`World.placeBlock` emite `PLACED`. `World.removeBlock` emite `REMOVED` solo si había un bloque en la posición.

## Límite actual

No existe un bus de eventos, cola, prioridad, procesamiento asíncrono ni jerarquía compleja de mensajes. Los observadores concretos para actualizar el renderizado o marcar un mundo como modificado quedan pendientes.

# Observer de cambios de bloques

## Intención

Observer permite que componentes reaccionen a la colocación o eliminación de un bloque sin introducir esas reacciones dentro de `World`.

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

## Uso actual y límites

`presentation.game.VoxelGame` es el observador concreto actual. Al abrir la ventana se registra con `world.addObserver(this)`; al recibir un `BlockChange`, marca el chunk afectado y en el siguiente fotograma reconstruye únicamente su malla. Al cerrar la ventana se retira con `world.removeObserver(this)`.

No existe un bus de eventos, cola, prioridad, procesamiento asíncrono ni jerarquía compleja de mensajes. Tampoco existe todavía un observador separado para guardar automáticamente un mundo modificado.

# Decisiones compartidas — fase 1

**Fecha:** 20 de agosto de 2026  
**Estado:** acordadas antes de implementar generación, renderizado o persistencia completa.

Este documento congela las reglas mínimas que los tres integrantes deben usar. Si una decisión cambia, se modifica aquí primero y después se adapta el código y los casos de prueba.

## 1. Dimensiones y coordenadas del chunk

Cada chunk mide **16 bloques de ancho × 64 bloques de alto × 16 bloques de profundidad**.

| Eje | Significado | Valores locales válidos |
| --- | --- | --- |
| `x` | ancho | `0` a `15` |
| `y` | altura | `0` a `63` |
| `z` | profundidad | `0` a `15` |

La altura se simplifica frente a las versiones antiguas de Minecraft: aunque estas usaban `y = 0…255`, para este MVP se fija `y = 0…63`. Así un chunk contiene como máximo 16 384 posiciones y sigue siendo manejable para generar, renderizar, guardar y probar.

Los chunks solo se distribuyen horizontalmente. Un chunk se identifica por el par `(chunkX, chunkZ)`; no existe `chunkY` en el MVP.

### Conversión de coordenadas del mundo

Para una posición absoluta `(worldX, y, worldZ)`:

```text
chunkX = floorDiv(worldX, 16)
chunkZ = floorDiv(worldZ, 16)
localX = floorMod(worldX, 16)
localZ = floorMod(worldZ, 16)
localY = y
```

`floorDiv` y `floorMod` son obligatorios, no la división entera ordinaria: conservan el resultado correcto al cruzar hacia coordenadas negativas. Por ejemplo, `(-1, 10, -1)` pertenece al chunk `(-1, -1)` y su posición local es `(15, 10, 15)`.

Una posición con `y < 0` o `y > 63` es inválida y debe rechazarse. El mismo criterio se aplica a las coordenadas locales que salgan de sus límites.

`Chunk` aplica estas reglas al agregar, consultar o eliminar un bloque. Sus coordenadas `Position` son absolutas, por lo que valida el chunk horizontal con `floorDiv(x, 16)` y `floorDiv(z, 16)`.

`World.findChunk(Position)` usa la misma conversión y devuelve el chunk encontrado, o un `Optional` vacío si ese chunk aún no fue creado en el mundo.

## 2. Esquema JSON de un mundo

Cada archivo es un mundo y se guarda en `worlds/<id>.json`. El esquema inicial es deliberadamente directo para que Estudiante 3 pueda serializarlo sin una base de datos ni un patrón adicional.

```json
{
  "schemaVersion": 1,
  "id": "mundo_universidad",
  "name": "mundo_universidad",
  "seed": 48271,
  "createdAt": "2026-08-20T20:30:00Z",
  "player": { "x": 8, "y": 32, "z": 8 },
  "chunks": [
    {
      "x": 0,
      "z": 0,
      "blocks": [
        { "x": 0, "y": 0, "z": 0, "type": "STONE" },
        { "x": 0, "y": 3, "z": 0, "type": "GRASS" }
      ]
    }
  ]
}
```

Reglas del esquema:

- `schemaVersion` debe ser `1`; otro valor se informa como archivo no compatible.
- `id` es el identificador seguro del archivo: letras, números, `_` y `-`. En esta primera fase coincide con `name`; una interfaz futura podría separar nombre visible e identificador.
- `seed` es un entero largo (`long`) y permite repetir la misma generación pseudoaleatoria.
- `createdAt` usa fecha ISO-8601 UTC.
- `player` conserva enteros mientras `Player` usa `Position`. Cuando se implemente movimiento continuo, se evolucionará este campo de forma explícita, con una nueva versión de esquema si hace falta.
- Un chunk usa `x` y `z` como sus coordenadas de chunk. Las coordenadas de cada elemento de `blocks` son **locales** al chunk.
- Solo se guardan bloques distintos de `AIR`; una coordenada ausente representa aire. Esto evita escribir miles de entradas vacías y coincide con la semántica de eliminar un bloque.
- No se permiten bloques repetidos en la misma coordenada local, tipos fuera de `BlockType` ni coordenadas fuera de los límites establecidos arriba.

Antes de completar `JsonWorldStorage`, el modelo debe incorporar al mundo su `id`, `seed`, fecha de creación y jugador, o definir claramente dónde se mantienen. Esa modificación pertenece a persistencia/dominio; no corresponde a la interfaz gráfica.

## 3. Tecnología gráfica mínima

**Decisión: LibGDX con su backend de escritorio LWJGL3, Java 17 e IntelliJ IDEA.**

Se conserva Maven en esta fase para no cambiar el sistema de construcción que ya compila. Al integrar el renderizador se añaden únicamente las dependencias de LibGDX y JUnit 5 necesarias; no se agrega un motor, framework web ni física externa.

LibGDX es la alternativa más compatible con el estado del proyecto porque funciona con JDK 17 e IntelliJ IDEA, proporciona backend de escritorio y API 3D (`PerspectiveCamera`, entrada de teclado/ratón, mallas y renderizado). Permite mostrar el MVP sin obligarnos a trabajar directamente con OpenGL/LWJGL de bajo nivel.

No se usará JavaFX/Swing para el mundo 3D ni LWJGL directamente: el primero no está orientado a un prototipo voxel 3D y el segundo aumenta demasiado el trabajo técnico de una semana.

Referencias oficiales: [configuración de LibGDX](https://libgdx.com/wiki/start/setup), [uso con IntelliJ](https://libgdx.com/wiki/start/import-and-running) y [API 3D](https://libgdx.com/wiki/graphics/3d/3d-graphics).

## 4. Criterios de terminado y casos de prueba

Un ítem no se considera terminado por “verse bien”: debe cumplir su criterio y la prueba asociada.

| ID | Entregable | Criterio de terminado | Prueba |
| --- | --- | --- | --- |
| CT-01 | Base | `mvn clean package` termina correctamente con Java 17. | Ejecutar el comando desde la raíz. |
| CT-02 | Límites | Un chunk acepta solo `x,z=0…15` y `y=0…63`; los demás valores fallan con un mensaje claro. | Pruebas unitarias de borde: `(0,0,0)`, `(15,63,15)`, `y=-1`, `y=64`, `x=16`. |
| CT-03 | Coordenadas | La conversión mundo → chunk/local funciona también con negativos. | Verificar `(16,5,16) → (1,1)/(0,5,0)` y `(-1,5,-1) → (-1,-1)/(15,5,15)`. |
| CT-04 | Factory | `BlockFactory` crea los ocho `BlockType` en la posición solicitada. | Una prueba parametrizada por tipo. |
| CT-05 | Generación | Con la misma `seed`, mismas dimensiones y mismo algoritmo, dos chunks contienen los mismos bloques. | Generar dos veces y comparar posiciones/tipos. |
| CT-06 | Observer | Colocar o eliminar un bloque existente notifica una vez con `PLACED` o `REMOVED`; eliminar aire no notifica. | Observer de prueba que cuenta y guarda el último `BlockChange`. |
| CT-07 | Singleton | Dos llamadas a `WorldManager.getInstance()` devuelven la misma instancia y solo conserva un mundo activo. | Comparar referencias, cargar A y luego B. |
| CT-08 | JSON | Guardar y cargar conserva metadatos, jugador, chunks y bloques no-AIR. | Mundo de prueba con dos chunks y varios tipos; comparación campo a campo. |
| CT-09 | JSON inválido | Versión desconocida, tipo inválido o coordenada fuera de rango no se cargan silenciosamente. | Archivos JSON deliberadamente erróneos y aserción de error legible. |
| CT-10 | Render | La aplicación abre una ventana y muestra la superficie de un chunk sin excepción. | Prueba manual con captura de pantalla. |
| CT-11 | Flujo MVP | Crear → generar → guardar → cerrar → cargar conserva los cambios de colocar/eliminar. | Prueba manual guiada y evidencia en `docs/evidencias/`. |

Las pruebas CT-02 a CT-09 serán automatizadas con JUnit 5 cuando se implemente cada responsabilidad. CT-10 y CT-11 son manuales porque comprueban la integración gráfica y el flujo completo.

La primera suite automatizada, `WorldBlockOperationsTest`, cubre creación y consulta de un chunk, colocación de un bloque, eliminación de un bloque existente y eliminación de aire.

## 5. Responsabilidad por esta fase

- **Estudiante 1:** implementa CT-02, CT-03, CT-04 y CT-05 al construir el mundo y la generación.
- **Estudiante 2:** implementa CT-06 y CT-07 junto con interacción y estado del jugador.
- **Estudiante 3:** implementa CT-08 y CT-09 al completar la persistencia y verifica CT-01 al integrar dependencias.
- **Los tres:** validan CT-10 y CT-11 y conocen las decisiones de este documento.

## 6. Generación pseudoaleatoria sencilla

La primera generación ya está definida e implementada en `SimpleTerrainGenerator`:

- recibe una `seed` de tipo `long`;
- para cada columna `(worldX, worldZ)` calcula una altura estable entre `18` y `22`;
- desde `y = 0` hasta dos capas antes de la superficie produce `STONE`;
- las dos capas bajo la superficie son `DIRT`;
- la superficie es `GRASS` en la mayoría de columnas, `SAND` en una proporción pequeña y `GRAVEL` en otra pequeña;
- sobre algunas superficies de césped se crean árboles pequeños con `WOOD` y `LEAVES`;
- lo que quede por encima es `AIR`.

La función mezcla semilla y coordenadas, de manera que el resultado no depende del orden en el que se consulten las columnas. `ChunkGenerationService` usa ese resultado para crear uno o una cuadrícula de chunks y solicita cada bloque no vacío a `BlockFactory`. Las posiciones almacenadas en `Block` permanecen en coordenadas absolutas del mundo; la conversión a coordenadas locales se hará al persistir JSON.

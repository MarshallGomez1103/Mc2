# Mundos finitos, distancia visible y vida del jugador

Estado: implementación local del Corte 2, pendiente de revisión e integración del equipo.
No añade chunks dinámicos ni mundo infinito. Los bloques de todos los chunks elegidos
siguen en memoria y se guardan en el JSON existente; solo las mallas 3D son diferidas.

## Elegir tamaño

Al crear un mundo, el menú pide tamaño. Enter conserva el pequeño del Corte 1.

| Opción | Cuadrícula | Chunks totales | Ancho/profundidad en bloques |
| --- | ---: | ---: | ---: |
| Pequeño | 2×2 | 4 | 32×32 |
| Mediano | 10×10 | 100 | 160×160 |
| Grande | 16×16 | 256 | 256×256 |

No se implementó 128×128: serían 16 384 chunks y, con bloques y JSON completos,
excedería razonablemente la memoria objetivo. No hace falta que las medidas sean
potencias de dos. La seed y los biomas siguen funcionando en coordenadas arbitrarias.
Los mundos guardados del Corte 1 cargan como antes; el tamaño se deduce de los chunks,
sin cambiar versión o campos JSON. `World.findChunk` usa un índice de coordenadas para
evitar recorrer la lista en cada consulta de colisión o construcción de malla.

La búsqueda del punto inicial del jugador se limita a una zona de 16×16 bloques
alrededor del centro. No se escanean todas las columnas del mundo grande para nacer.
Los nombres repetidos se rechazan antes de generar terreno costoso.

## Distancia visible y FPS

En juego, **J disminuye** y **K aumenta** el radio cuadrado de chunks dibujables.
Radio 0 significa solo el chunk del jugador; radio 1 cubre hasta 3×3 chunks.
El máximo depende del mundo cargado (15 para uno de 16×16) y permite abarcarlo todo.
El HUD muestra FPS reportados por LibGDX, radio y número de mallas activas.

Las mallas se crean solo para chunks cercanos, con tope de dos por frame, y las lejanas
se liberan. Chunks fuera del frustum de cámara no se envían al `ModelBatch`.
Al subir mucho el radio pueden bajar los FPS y tardar varios frames en aparecer todas
las mallas; J vuelve a liberar las que quedan fuera del radio. Los bloques del World
no se descargan ni se regeneran: esto **no** es streaming de terreno.

## Morir y reaparecer

`PlayerLife` concentra el estado de vida de la sesión. Si el jugador cae por debajo
de Y = -8, registra `DeathCause.VOID`; la vista pausa movimiento/interacción y muestra
«MORISTE». **R** reaparece en la última posición en suelo recordada y reinicia la
velocidad vertical; **ESC** regresa al menú. La clase también acepta
`die(DeathCause.ENEMY)` para que la futura integración de zombis utilice el mismo
estado y la misma pantalla. No se implementó daño, salud ni ataque de zombis aquí.
La muerte no se persiste en JSON. Al cerrar la ventana estando muerto, la posición
vuelve al último punto seguro antes de regresar al menú para no guardar el vacío.

## Pruebas y medición puntual

- `mvn clean test`: 63 pruebas verdes (41 del Corte 1 y 22 nuevas), sin fallos.
- JUnit puro cubre tamaños, índice de chunks, radio, caída, primera causa de muerte
  y reaparición. No prueba OpenGL ni eventos reales de teclado.
- En JVM con `-Xmx1024m`, seed 17 y 16×16 chunks: 256 chunks, 1 867 170 bloques,
  generación ~1,8 s; generación + JSON ~267 MB de heap retenido después de GC,
  JSON ~93 MB. Son mediciones puntuales, no resultados de carga/endurance.
- Crear y guardar Grande desde el menú con `-Xmx1024m` funcionó. Cargar un JSON
  Grande falló con `OutOfMemoryError` a 1 GB por el parser actual; cargó con
  `-Xmx1280m`, `-Xmx1536m` y `-Xmx2g`. Por margen práctico se recomienda
  **`java -Xmx2g -jar target/minecraft2-0.1.0-SNAPSHOT.jar`** para Grande.
- Se abrió la ventana con Mediano y Grande; captura de Grande a 120 frames mostró
  FPS 60 y 9 mallas activas con radio 1. Eso no garantiza 60 FPS para todas las
  semillas, equipos ni distancias. Otra captura de una instancia temporal de World
  con jugador bajo Y=-8 mostró el overlay «MORISTE» y FPS 60. No se usó un archivo
  JSON para ese caso porque el lector rechaza correctamente jugadores guardados
  fuera de 0..64. J/K y R reales requieren comprobación manual de teclado: el envío
  automatizado de W no movió al jugador en la sesión Wayland utilizada.

La máquina de medición tiene 46 GiB de RAM y OpenJDK 26.0.2. El límite de heap se
forzó para aproximar la restricción de un equipo de 8 GB; **no** se ejecutó físicamente
en un equipo de 8 GB. Si en ese equipo Grande resulta incómodo al cargar, usar
Mediano y conservar Grande como opción experimental hasta medir allí.

## Correcciones de interacción y estabilidad

Shift acelera el desplazamiento horizontal por un factor 1,65 sin cambiar la
dirección normalizada. Colocar un bloque que intersectaría el volumen del jugador
se rechaza; las posiciones ya incrustadas se elevan hasta encontrar espacio libre.
Si una edición ocurre en la frontera de un chunk, se invalidan las mallas del
chunk propio y de los vecinos pertinentes para evitar caras faltantes (apariencia
de rayos X). El menú resuelve `worlds/` respecto al proyecto/JAR, no al directorio
actual de la terminal. Un JSON malformado puede aparecer en la lista, pero cargarlo
seguirá fallando con el manejo de error existente: no se borra automáticamente.

Tres ciclos gráficos de 120 frames con `-Xmx512m`, carga/cierre y GC explícito
terminaron con ~6 MB de heap y dos hilos de aplicación en cada ciclo. Es una
comprobación puntual, **no una prueba de ausencia de fugas de memoria nativa/GPU
ni un endurance test**. Al reducir distancia se liberan mallas, pero los bloques
del mundo permanecen en memoria.

## Coordinación

Se editaron temporalmente `World.java`, `MainMenu.java` y `VoxelGame.java` con permiso
del Estudiante 1 antes de que los Estudiantes 2 y 3 comenzaran. Antes de integrar
EnemyUpdateService, el Estudiante 2 debe leer el nuevo loop de mallas y `PlayerLife`;
el Estudiante 3 debe preservar la pregunta de tamaño al añadir opciones al menú.
La integración de ramas y el push se realizan solamente tras autorización explícita
del equipo; este documento no prescribe reescritura de historia ni force-push.

# TODO — trabajo pendiente

Este archivo enumera el trabajo que los tres estudiantes deben completar manualmente. La distribución es sugerida y debe ajustarse cuando se registren los nombres del equipo.

## Decisiones compartidas antes de programar

- [ ] Completar los nombres de los tres integrantes en `README.md`.
- [x] Acordar ancho, alto y profundidad de cada chunk: `16 × 64 × 16`.
- [x] Definir cómo una posición del mundo se convierte en coordenada de chunk.
- [x] Acordar el esquema JSON definitivo para mundo, chunks y bloques.
- [x] Elegir la tecnología gráfica mínima compatible con Java e IntelliJ: LibGDX + LWJGL3.
- [x] Establecer criterios de terminado y casos de prueba del MVP.

Las decisiones, límites, esquema y pruebas acordados están en [docs/decisiones-compartidas.md](docs/decisiones-compartidas.md).

## Estudiante 1 — mundo y generación

- [x] Implementar la generación pseudoaleatoria sencilla.
- [x] Crear uno o pocos chunks usando `BlockFactory`.
- [x] Distribuir césped, tierra, piedra, arena, grava, madera y hojas.
- [x] Definir límites y validaciones de `Chunk`.
- [x] Resolver el chunk correspondiente a una `Position`.
- [x] Probar creación, consulta, colocación y eliminación de bloques.

## Estudiante 2 — jugador e interacción

- [x] Definir orientación y estado mínimo adicional del jugador.
- [x] Implementar adelante, atrás, izquierda y derecha.
- [x] Implementar salto y gravedad.
- [x] Añadir colisiones básicas con bloques sólidos.
- [x] Implementar selección de bloque mediante clic (raycast en `PlayerInteractionService`; falta solo conectar el evento real de clic del mouse, que es integración gráfica/LibGDX y queda fuera de este plan).
- [x] Conectar colocar y eliminar con `World.placeBlock` y `World.removeBlock`.
- [ ] Suscribir un componente visual a `BlockChange` si resulta necesario (pendiente de la integración gráfica del equipo completo, fuera del alcance de los 4 participantes de este plan).

## Estudiante 3 — presentación y persistencia

- [ ] Diseñar la interfaz visual del menú principal sin lógica de negocio.
- [ ] Conectar las pantallas de crear, listar, cargar, guardar y eliminar.
- [x] Completar la escritura JSON de chunks y bloques en `JsonWorldStorage` (`WorldJsonCodec.write`).
- [x] Completar la reconstrucción del mundo desde JSON (`WorldJsonCodec.read`, con `JsonParser` propio).
- [x] Manejar archivos dañados, nombres duplicados y errores de lectura/escritura (`InvalidWorldFileException`, escritura atómica; CT-09 cubre 22 casos en `manualtest.WorldPersistenceTest`).
- [ ] Añadir confirmación antes de eliminar un mundo.
- [ ] Probar el ciclo crear → guardar → cerrar → cargar → eliminar.

## Integración del equipo

- [ ] Renderizar uno o pocos chunks.
- [ ] Asignar una representación visual a los ocho tipos de bloque.
- [ ] Conectar teclado y ratón con los casos de uso correspondientes.
- [ ] Verificar que no haya lógica del juego dentro de la interfaz.
- [ ] Verificar que `WorldManager` siga siendo el único Singleton.
- [ ] Verificar que no se hayan introducido patrones no permitidos.
- [ ] Revisar que no existan dependencias circulares.
- [ ] Añadir pruebas automatizadas para dominio, patrones y persistencia.
- [ ] Actualizar README y documentación después de cada decisión de diseño.

## No hacer en esta etapa

- [ ] No implementar multijugador, red, crafting, enemigos o inventario avanzado.
- [ ] No añadir base de datos.
- [ ] No introducir DAO, Strategy, Builder, Prototype, CQRS ni Event Sourcing.
- [ ] No convertir nuevas clases en Singleton.
- [ ] No agregar frameworks sin una necesidad acordada por el equipo.

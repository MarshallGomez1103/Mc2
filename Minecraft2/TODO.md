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

- [x] Diseñar la interfaz visual del menú principal sin lógica de negocio (`MainMenu` enruta y `ConsoleIO` concentra la entrada/salida; el menú gráfico con LibGDX corresponde a la integración del equipo).
- [x] Conectar las pantallas de crear, listar, cargar, guardar y eliminar.
- [x] Completar la escritura JSON de chunks y bloques en `JsonWorldStorage` (`WorldJsonCodec.write`).
- [x] Completar la reconstrucción del mundo desde JSON (`WorldJsonCodec.read`, con `JsonParser` propio).
- [x] Manejar archivos dañados, nombres duplicados y errores de lectura/escritura (`InvalidWorldFileException`, escritura atómica; CT-09 cubre 22 casos en `manualtest.WorldPersistenceTest`).
- [x] Añadir confirmación antes de eliminar un mundo (solo una respuesta afirmativa explícita borra).
- [x] Probar el ciclo crear → guardar → cerrar → cargar → eliminar (`application.WorldLifecycleTest` y [docs/evidencias/ct-11-flujo-mvp.md](docs/evidencias/ct-11-flujo-mvp.md)).

## Integración del equipo

- [ ] Renderizar uno o pocos chunks.
- [ ] Asignar una representación visual a los ocho tipos de bloque.
- [ ] Conectar teclado y ratón con los casos de uso correspondientes.
- [x] Verificar que no haya lógica del juego dentro de la interfaz: `presentation` no importa `domain` ni `persistence`; solo habla con `WorldApplicationService`.
- [x] Verificar que `WorldManager` siga siendo el único Singleton: es la única clase con instancia global; los demás constructores privados son de clases de utilidad estáticas.
- [x] Verificar que no se hayan introducido patrones no permitidos: las únicas menciones a DAO, Command o Strategy son comentarios que explican por qué **no** se usan.
- [x] Revisar que no existan dependencias circulares: `domain` no importa capas superiores, `application` no importa `presentation` ni `bootstrap`, y `persistence` no importa `application` ni `presentation`.
- [ ] Añadir pruebas automatizadas para dominio, patrones y persistencia. Persistencia y flujo completo ya están en JUnit (`test/`); CT-02 a CT-07 siguen como comprobaciones ejecutables en `manualtest` y sus autores deben portarlas.
- [ ] Actualizar README y documentación después de cada decisión de diseño.

## No hacer en esta etapa

- [ ] No implementar multijugador, red, crafting, enemigos o inventario avanzado.
- [ ] No añadir base de datos.
- [ ] No introducir DAO, Strategy, Builder, Prototype, CQRS ni Event Sourcing.
- [ ] No convertir nuevas clases en Singleton.
- [ ] No agregar frameworks sin una necesidad acordada por el equipo.

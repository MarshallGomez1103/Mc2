# TODO — trabajo pendiente

Este archivo enumera el trabajo que los tres estudiantes deben completar manualmente. La distribución es sugerida y debe ajustarse cuando se registren los nombres del equipo.

## Decisiones compartidas antes de programar

- [ ] Completar los nombres de los tres integrantes en `README.md`.
- [ ] Acordar ancho, alto y profundidad de cada chunk.
- [ ] Definir cómo una posición del mundo se convierte en coordenada de chunk.
- [ ] Acordar el esquema JSON definitivo para mundo, chunks y bloques.
- [ ] Elegir la tecnología gráfica mínima compatible con Java e IntelliJ.
- [ ] Establecer criterios de terminado y casos de prueba del MVP.

## Estudiante 1 — mundo y generación

- [ ] Implementar la generación pseudoaleatoria sencilla.
- [ ] Crear uno o pocos chunks usando `BlockFactory`.
- [ ] Distribuir césped, tierra, piedra, arena, grava, madera y hojas.
- [ ] Definir límites y validaciones de `Chunk`.
- [ ] Resolver el chunk correspondiente a una `Position`.
- [ ] Probar creación, consulta, colocación y eliminación de bloques.

## Estudiante 2 — jugador e interacción

- [ ] Definir orientación y estado mínimo adicional del jugador.
- [ ] Implementar adelante, atrás, izquierda y derecha.
- [ ] Implementar salto y gravedad.
- [ ] Añadir colisiones básicas con bloques sólidos.
- [ ] Implementar selección de bloque mediante clic.
- [ ] Conectar colocar y eliminar con `World.placeBlock` y `World.removeBlock`.
- [ ] Suscribir un componente visual a `BlockChange` si resulta necesario.

## Estudiante 3 — presentación y persistencia

- [ ] Diseñar la interfaz visual del menú principal sin lógica de negocio.
- [ ] Conectar las pantallas de crear, listar, cargar, guardar y eliminar.
- [ ] Completar la escritura JSON de chunks y bloques en `JsonWorldStorage`.
- [ ] Completar la reconstrucción del mundo desde JSON.
- [ ] Manejar archivos dañados, nombres duplicados y errores de lectura/escritura.
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

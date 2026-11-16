# ADR-004 — Frontera del adaptador de red

| Campo | Valor |
| --- | --- |
| Estado | Implementado mediante reglas de fuentes y pruebas; la evidencia de ejecución se registra en pruebas.md. |
| Fecha | 2026-11-16 |
| Responsable | Jasub, Persona 2 |
| Base | Núcleo y servidor/cliente integrados en main mediante PR #3 y #6; base de D `fa52cbb`. |
| Decisiones relacionadas | [ADR-001](ADR-001-estilo-recuperacion.md), §§5–6; [ADR-002](ADR-002-despliegue.md), §§3 y 6. |

## Contexto

ADR-001 eligió capas con frontera de aplicación explícita: dominio sin capas superiores, aplicación coordinando reglas y vista traduciendo/disponiendo datos. Su §6 pide revisar la separación cuando aparece red o multijugador. ADR-002 identifica `network` como una nueva frontera que debe proteger Persona 2. B/C implementaron esa entrada y un cliente remoto; las reglas anteriores todavía no prohibían dependencias del núcleo hacia el adaptador ni del servidor hacia gráficos.

La decisión de despliegue y su confirmación con el profesor siguen bajo ADR-002 y sus responsables. Este documento precisa dependencias de código; no aprueba su modalidad de despliegue ni sustituye ADR-003 de seguridad.

## Alternativas

1. **Conservar solo las reglas anteriores:** un import de network en dominio, o de LibGDX en el servidor, podría compilar sin ser detectado por esas fronteras. No protege el nuevo modo headless.
2. **Mantener las capas y ampliar sus reglas:** protege la entrada de red con el código y herramientas existentes, sin mover contratos compartidos ni cambiar persistencia. Es la alternativa elegida.
3. **Reorganizar todos los adaptadores y mover WorldStorage al núcleo:** mejora la inversión de dependencias, pero cambia contratos de aplicación/persistencia y coordinación de otros integrantes. No se implementa como una ampliación de pruebas; se conserva como posible evolución.

## Decisión y reglas comprobables

| Origen | Permitido en esta revisión | Prohibido |
| --- | --- | --- |
| domain | Dominio, contratos de patterns y bibliotecas Java sin gráficos/transporte. | application, persistence, presentation, bootstrap, network, gráficos y APIs de sockets/HTTP/NIO channels. |
| application | Dominio, patterns, bibliotecas Java y la interfaz persistence.WorldStorage. | presentation, bootstrap, network, gráficos/transporte y otras clases de persistence. |
| network | application/domain, patterns, APIs TCP/HTTP de Java y las clases JSON/almacenamiento ya usadas de persistence. | presentation, bootstrap, LibGDX/LWJGL y UI AWT/Swing/JavaFX. |
| persistence | Dominio, patterns y bibliotecas Java para archivos/JSON. | application, presentation, bootstrap, network y gráficos. |

Dirección esperada del juego remoto: presentación → network → application → domain. El núcleo no conoce conexiones ni ventanas. Bootstrap compone el arranque y puede depender de esos componentes. La vista puede leer la copia de estado para dibujar; las reglas del servidor se coordinan en GameSession, con join/leave/advanceAll y snapshots bajo el hilo de simulación.

La regla de presentación que ya existe se conserva: GameInput traduce dispositivos a PlayerFrameInput y no coordina física, estamina o combate. Estas pruebas de dependencias no demuestran por sí solas la propiedad del hilo ni el comportamiento del protocolo: lo cubren adicionalmente las pruebas de integración y el humo del JAR.

## Consecuencias y límites

- Se detectan inversiones reales de dependencias aunque Java permita compilarlas; una infracción hace fallar Surefire y `mvn verify`/build-pruebas.
- Se usan reglas de fuentes: reconocen imports, imports estáticos y nombres plenamente cualificados; ignoran comentarios y literales. No son análisis de bytecode ni un parser completo de Java. No detectan reflexión basada en strings, generación dinámica o todas las dependencias transitivas. No se promete detección absoluta.
- El gate no se reduce a comprobar existencia de clases. Los tests incluyen dependencias prohibidas y dependencias permitidas, y el ensayo negativo introduce clases compilables en las carpetas reales.
- `application` conserva su dependencia de WorldStorage en persistence. `network` conserva JsonParser/JsonWorldStorage y composición de sesión en ServerSimulation. Hay acoplamiento entre adaptadores; no se presenta como hexagonal completa ni se prohíbe una dependencia que la implementación todavía usa.
- El ciclo domain.world ↔ domain.player y el apoyo gráfico previo en bootstrap.GpuPreference siguen siendo deudas declaradas en ADR-001; este ADR no afirma resolverlas.
- Añadir una nueva API gráfica/transporte o mover paquetes requiere revisar la política y sus pruebas. Las APIs Java de archivos y excepciones I/O existentes no se prohíben en bloque.

## Evidencia

[ArchitectureBoundaryTest](../../test/architecture/ArchitectureBoundaryTest.java) implementa las fronteras y fixtures del escáner. [GameFlowSmokeIT](../../test/presentation/GameFlowSmokeIT.java) recorre el JAR real sin abrir ventana. [pruebas.md](../pruebas.md) y la tabla de trazabilidad del README registran la ejecución, el ensayo negativo/restauración y los límites de CI/CD.

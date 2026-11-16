# Evidencia de arquitectura e integración — fase D

16 de noviembre de 2026. D parte del servidor/cliente de #6 y conserva la documentación de Thomas fusionada en #14. La primera suite local se ejecutó sobre base funcional `fa52cbb`; después se rebasó el incremento sobre `e618961`, que solo añadió documentación/TODO. Se resolvió el conflicto de README sustituyendo exclusivamente su bloque de trazabilidad y conservando íntegro el resto de los cambios de Thomas.

## Incremento correcto

Código de reglas/humo y trazabilidad inicial: `d4ad1d1132b76396147e91ae431ec14d99da2983`. [PR #15](https://github.com/MarshallGomez1103/Mc2/pull/15). No cambia producción, POM ni workflows; el archivo nuevo de decisión es [ADR-004](../../adr/ADR-004-frontera-red.md).

[Actions positivo 43976250231](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976250231) terminó en success con los cinco checks. ArchitectureBoundaryTest protege cuatro fronteras de capas y comprueba su propio escáner mediante fixtures; 22 casos no significan 22 reglas distintas. GameFlowSmokeIT tiene tres casos de consola y un flujo nuevo de servidor desde el JAR.

El humo público crea un mundo mediante consola y arranca el JAR en otro proceso, con puertos TCP/HTTP efímeros. Espera readiness, conecta dos clientes, verifica una acción con ack propio, observa el mismo jugador desde el otro cliente, prueba bye/conectados 2→1→0 y reingreso sin fantasma. Imprime `D_JAR_SMOKE sha256=…` en el XML y logs para identificar el archivo ejecutado. No construye GameServer ni GameSession en el JVM de pruebas. No prueba GPU ni CD.

Verificación local: `mvn -B -ntp clean verify`, Java 17.0.8.7, 279 unitarias +161 integración, cero fallos/errores/omitidas; dominio 935/1006 líneas (92,94 %), gate ≥70 % activo. Surefire ejecuta arquitectura; Failsafe ejecuta los cuatro casos de GameFlowSmokeIT, sin duplicados entre ejecutores. En el ensayo local se guardaron XML y logs antes de retirar las infracciones, y clean verify confirmó que el JAR no contiene sus clases.

## Ensayo remoto de arquitectura: el check debe fallar

- Rama aislada: `evidencia/d-gates-20261116`; fuera del PR de entrega.
- Commit: [`9a402bfdb1a5240cbe4733e2466145a978df7be0`](https://github.com/MarshallGomez1103/Mc2/commit/9a402bfdb1a5240cbe4733e2466145a978df7be0).
- [Actions 43976266242](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976266242), ejecutado por workflow_dispatch usando ci.yml existente.
- Se añaden dos clases compilables: domain.world → network.ProtocolCodec y network → com.badlogic.gdx.Gdx. La compilación es válida; la arquitectura prohíbe esas dependencias.
- El check build-pruebas falla en Surefire. Sus XML confirman 279 unitarias y exactamente dos fallos en ArchitectureBoundaryTest (22 casos); los mensajes identifican ambas dependencias prohibidas.
- El Dockerfile actual de main también ejecuta unitarias con `mvn package`; por eso se conserva también el job imagen fallido del ensayo. No se modificó el Dockerfile para conseguir ese resultado.

## Ensayo remoto de integración: Failsafe también debe bloquear

- En la misma rama de evidencia se retiran las dos clases de arquitectura y se añade únicamente una prueba de integración que falla deliberadamente: `presentation.IntegrationGateProbeIT`.
- Commit: [`55c91d27de13425fa5ad475e6910ee2b5cc5a363`](https://github.com/MarshallGomez1103/Mc2/commit/55c91d27de13425fa5ad475e6910ee2b5cc5a363).
- [Actions 43976623068](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976623068), workflow_dispatch.
- Pasaron las 279 unitarias, los 22 casos de arquitectura y los cuatro del humo. Failsafe ejecutó 162 casos y falló únicamente IntegrationGateProbeIT; `failsafe:verify` bloqueó el job. Así se distingue selección de Failsafe de una prueba que simplemente existe sin ejecutarse.

Estos commits de evidencia no se fusionan ni forman parte del historial del PR de entrega. Su rama se conserva para reproducir el fallo. No son el PR negativo de seguridad exigido por el enunciado: ese entregable pertenece a E y permanece pendiente.

## Conservación y límites

Los artefactos `reportes-pruebas`, SARIF/SCA/imagen y logs de los runs se descargaron con sus metadatos. Se comprobaron los XML y la integridad CRC de todos los ZIP; los archivos voluminosos se conservan fuera del repo en `evidencias/fase-d/`, con respaldo en Ubuntu. Este documento enlaza las ejecuciones verificables del pipeline, no usa capturas como prueba del gate.

Las reglas son análisis de fuentes y no un parser/bytecode completo; sus límites y los acoplamientos conservados están en ADR-004. La cobertura del proceso hijo no se suma automáticamente al JaCoCo del JVM de Failsafe. El humo por proceso no garantiza guardado ante terminación forzada, ni reemplaza la carga/humo contra el despliegue de main. Los resultados de C se mantienen como históricos, con calentamiento y frentes detenidos durante la carga; su desconexión gráfica no explicada sigue registrada.

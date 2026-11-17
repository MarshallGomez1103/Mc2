# Evidencia de seguridad — fase E

16/11/2026, Jasub. Base522bd4c, sobre main e618961. Se amplía el PR15 existente, sin otro PR positivo ni fusión propia. [Seguridad](../../seguridad.md) contiene nueve amenazas específicas, autenticación/autoría limitada por conexión, decisiones residuales y59 filas de hallazgos de imagen. [ADR003](../../adr/ADR-003-seguridad.md) registra herramientas, umbrales reales, alternativas, costos medidos y revisión cruzada pendiente.

## Corrección y prueba roja/verde

Hallazgo E-LOG: JsonParser incluye el nombre de un campo duplicado en su excepción; GameServer imprimía getMessage, por lo que un nombre con salto de línea controlado por el cliente podía falsificar líneas del log. La prueba `malformedMessagesCannotForgeLogEntries` falla sobre el código anterior (un caso, un fallo, Maven1). Se conservan log y XML antes de corregir, no se limpia target primero.

[acbff1c](https://github.com/MarshallGomez1103/Mc2/commit/acbff1ccb72d35735d246e28b562df2bbfecf665) elimina el mensaje no confiable; conserva el evento fijo de cierre, ID interno y clase de excepción. La prueba pasa sin eliminar la observabilidad del rechazo.

`mvn -B -ntp clean verify` completo con Java17.0.8.7 en Windows, finalizado16/11/2026 21:02:05 UTC−05:00: **279 unitarias +164 integración**, cero fallos/errores/omitidas; dominio935/1006=92,94 %, gate>=70 % activo. GameServerIT incorpora tres casos: integridad del log; entrada sobredimensionada/identidad falsa/JSON profundo con recuperación del dueño y métricas sin nombres; ocho admitidos rechazan al noveno sin detener salud. ArchitectureBoundaryTest22 y GameFlowSmokeIT4 siguen seleccionados. No se cambió POM/workflow/scripts ni persistencia/dominio ajenos.

## Umbral SAST: reportar debe producir error

Se ejecutó el script real `scripts/umbral-sast.sh`, en una copia temporal normalizada LF sin modificar lógica, con cinco reportes SARIF sintéticos. En [umbral-sast](umbral-sast/) se conservan los reportes, logs y códigos:6.9=>0;7.0=>1;9.5=>1;sin-runs=>1;válido sin resultados=>0. No se agregó excepción. Esto prueba lectura del umbral; no equivale a inyectar una vulnerabilidad real en CodeQL ni a un PR SAST bloqueado.

## PR negativo existente revalidado, sin duplicación

[PR5](https://github.com/MarshallGomez1103/Mc2/pull/5), commit9130728, [Actions43736948906](https://github.com/MarshallGomez1103/Mc2/actions/runs/43736948906). API: cerrado, merged=false; rama eliminada (ref404). Job secretos falla, otros cuatro pasan. Artefacto redactado y log descargados, CRC correctos: un hallazgo generic-api-key; log«Secrets gate FAILED (gitleaks rc=1)» tras81 commits. La credencial fue inventada en esa demostración; E no reproduce su valor ni crea otra.

El primer intento de descarga de todos los artefactos se interrumpió y dejó una copia incompleta de reportes-pruebas. Se conserva fuera del repo con sufijo .zip.incompleto; no se presenta como ZIP verificado ni como evidencia del gate. La descarga enfocada de secretos/logs sí terminó y fue comprobada. No se atribuye a un fallo del pipeline.

Eliminar la rama evitó contaminación de --all en futuras ejecuciones. El PR/run retienen evidencia. No se agrega ignore de secretos. PR4 histórico probó cobertura, no CVE de imagen. No hay demostración vulnerable real de SCA/imagen; sus gates están configurados para fallar y sus ejecuciones positivas/reporte completo se revisan. La obligación de al menos un PR inseguro bloqueado se satisface con5.

## Escaneos y conservación

BaseD522bd4c, [Actions43978487608](https://github.com/MarshallGomez1103/Mc2/actions/runs/43978487608): cero resultados SAST, cero CVE SCA, imagen59 hallazgos (2HIGH,53MEDIUM,4LOW) y ceroCRITICAL, secretos sin hallazgos. Los59 se registran, no se omiten por estar bajo umbral de imagen. No se añaden excepciones ni se relajan controles. La imagen es la de Minecraft2/Dockerfile vigente, no las dos imágenes finales propuestas por Ethian.

La ejecución de CI de esta entrega queda identificada por SHA en los [checks del PR15](https://github.com/MarshallGomez1103/Mc2/pull/15/checks), con SARIF/Trivy/XML/logs publicados. La copia descargada con metadatos está en cte3/evidencias/fase-e/actions/positivo y respaldo Ubuntu; el informe personal FASE_E_ESTADO.md registra SHA/run final. Los archivos voluminosos quedan fuera del repo. Windows/Ubuntu conservan01 rojo,02 verify,03 fixtures,04 verificación PR5 y evidencias de publicación. El respaldo ZIP tiene manifiesto SHA256.

## Límites y coordinación

No hay cuentas/TLS de jugador; sí asignación y autorización por conexión. Goteo lento sigue pudiendo ocupar plazas; tamaño/symlink de JSON local dependen del operador; runner/noVNC/CD finales requieren verificación con Ethian. Los riesgos no son aprobaciones de producción ni excepciones de gates. Thomas/Ethian deben revisar ADR/PR; no se afirma revisión obtenida ni se envían mensajes automáticos. F/ZAP y G/integración final no iniciadas. No se guarda explicación didáctica E en recordatorio hasta indicación del usuario.

# Flujo de caja negra — consola del programa empaquetado

## Que prueba

`test/presentation/GameFlowSmokeIT.java` arranca `target/*.jar` como un proceso real
del sistema operativo (`ProcessBuilder`) y lo conduce unicamente a traves de su
consola publica (texto por stdin, lectura de stdout), sin invocar ninguna clase del
proyecto directamente. Es una prueba de caja negra: no conoce `WorldApplicationService`,
`World` ni ningun otro tipo interno.

Flujo cubierto: crear mundo -> listar mundos -> guardar mundo actual -> eliminar mundo ->
salir. Los mundos se escriben en un directorio temporal (`@TempDir` + `-Dmc2.worlds.dir`),
nunca en la carpeta real de guardado.

## Alcance declarado

- Cubre exclusivamente el menu de consola (`-Dmc2.console=true`).
- No ejercita la opcion "6. Jugar": esa ruta abre la ventana grafica de LibGDX y
  requiere display, fuera del alcance de este smoke.
- No sustituye al smoke grafico de 224 frames ya documentado en
  `feedback-20260926.md`; son evidencias complementarias de capas distintas.

## Como ejecutarla

```
mvn package
mvn verify
```

`mvn package` genera el jar sombreado que la prueba localiza en `target/`.
`mvn verify` ejecuta Failsafe, que recoge las clases `*IT.java` por separado de las
`*Test.java` (estas ultimas siguen corriendo con `mvn test`, sin cambios).

## Evidencia

Corrida real con `mvn verify` (incluye `package` -> `test` -> `integration-test` -> `verify`):

```
[INFO] --- failsafe:3.2.5:integration-test (default) @ minecraft2 ---
[INFO] Running presentation.GameFlowSmokeIT
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.781 s -- in presentation.GameFlowSmokeIT
[INFO] Results:
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- failsafe:3.2.5:verify (default) @ minecraft2 ---
[INFO] BUILD SUCCESS
```

- Resultado: BUILD SUCCESS
- GameFlowSmokeIT: 1/1 ejecutada, 0 fallos, 0 errores, 0 omitidas
- El jar sombreado (`minecraft2-0.1.0-SNAPSHOT-shaded.jar`) se genero correctamente antes de la fase de integracion y fue el artefacto real contra el que corrio la prueba
- Fecha de la corrida: 2026-10-08
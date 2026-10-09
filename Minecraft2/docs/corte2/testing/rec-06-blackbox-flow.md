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

Pendiente de ejecucion real en el entorno de desarrollo (JDK, Maven y dependencias
de LibGDX disponibles localmente). Completar aqui con la salida real de
`mvn package && mvn verify`: resultado (BUILD SUCCESS/FAILURE), conteo de pruebas de
integracion ejecutadas/fallidas, y fecha/hardware de la corrida.

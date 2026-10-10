# Integración de la recuperación del Corte 2

La versión conjunta conserva los controles de Thomas, el flujo público y renombrados de Jasub y los documentos/scripts de Ethian. La resolución del POM mantiene JaCoCo, las selecciones por contenido y Failsafe después del empaquetado.

## Fronteras y aislamiento

- GameSessionTest y CombatSessionRegressionTest: sesión, control, vida, enemigos y oleadas reales en mundos deterministas en memoria; cubren pausa, muerte, reaparición y prioridades temporales.
- LocalWorldPersistenceIT y WorldLifecycleIT: WorldApplicationService + JsonWorldStorage reales; guardar cambios, cerrar, reabrir con otro servicio y conservarlos. Un archivo inválido no sustituye el mundo activo. Cada prueba descarga WorldManager y usa una carpeta temporal propia.
- GameFlowSmokeIT: tres escenarios públicos de consola con procesos separados, JAR real, archivos temporales, timeout y comprobaciones de salida/disco. Crear/listar/guardar/reiniciar/cargar/eliminar; cancelación; JSON inválido.
- ArchitectureBoundaryTest: reglas de import y contrato de avance del ADR.
- RecoveryWorldStorageFailureTest: errores del caso de uso con stub sin IO; corresponde a unitarias, no a persistencia real.

## Ejecución

Desde Minecraft2/, JDK 17:

```bash
mvn clean verify -DskipUnitTests=true
mvn clean verify
```

El primer comando ejecuta 148 casos de integración y empaqueta primero el JAR. El segundo ejecuta además 198 casos unitarios/adaptadores. Ambos pasaron desde una copia sin target previo. Resultados por clase/caso, cobertura y fuentes comprobadas están en el [reporte canónico](../pruebas.md) y su [manifiesto](evidencias/integracion/manifest.json).

La infraestructura real es el almacenamiento JSON local; no requiere una BD ni contenedores. Los tres casos de consola no ejercitan la opción Jugar ni controles 3D. Esa frontera se comprobó por separado con el smoke OpenGL automatizado; no se presenta como evaluación de usabilidad.

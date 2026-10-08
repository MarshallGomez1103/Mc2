# Minecraft2

Videojuego voxel de escritorio desarrollado en Java 17, Maven y LibGDX por Elioth Thomas Gomez Morales, Jasub Sastre y Ethian Daniel White Ortiz.

## Recuperación del Corte 2

El trabajo actual completa la arquitectura, la trazabilidad y la evidencia del proyecto existente. Los cambios se organizan según la rúbrica del Corte 2; las tareas de implementación siguen pendientes en el [TODO](Minecraft2/TODO.md).

| Integrante | Rama de trabajo | Responsabilidad |
| --- | --- | --- |
| Thomas | `feature/c2-Thomas` | Frontera de aplicación, conexión gráfica, cobertura/configuración de pruebas, documentación canónica e integración |
| Jasub | `feature/c2-jasub` | Pruebas de integración, flujo público de caja negra y comprobación de fronteras arquitectónicas |
| Ethian | `feature/c2-Ethian` | Comparación de estilos, ADR, diagramas y evidencia de carga |

Las tres ramas parten de la misma base de preparación. `main` reúne los cambios revisados del equipo. El [plan de trabajo](Minecraft2/docs/recuperacion-c2/plan-equipo.md) define archivos exclusivos, dependencias, pruebas y criterios de aceptación.

## Trazabilidad del reto

El reto comunicado oralmente al equipo fue incorporar un enemigo. La conducta, coordinación y coste de actualización son aspectos de ese reto; no se presentan como retos adicionales asignados.

| Reto | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba | Resultado |
| --- | --- | --- | --- | --- | --- |
| Incorporar un enemigo con conducta y navegación integradas en la partida | Mantenibilidad, testabilidad y rendimiento de actualización | Separar FSM y navegación del render; coordinar los servicios en aplicación. La recuperación trasladará también la coordinación del control del jugador desde presentación a aplicación y comparará capas con puertos/adaptadores | [FSM](Minecraft2/src/domain/enemy/ZombieStateMachine.java), [A*](Minecraft2/src/domain/enemy/AStarPathfinder.java), [servicio de enemigos](Minecraft2/src/application/EnemyUpdateService.java), [sesión](Minecraft2/src/application/GameSession.java), [entrada actual](Minecraft2/src/presentation/game/GameInput.java) | [Pruebas FSM](Minecraft2/test/domain/enemy/ZombieStateMachineTest.java), [pruebas A*](Minecraft2/test/domain/enemy/AStarPathfinderTest.java), [pruebas de sesión](Minecraft2/test/application/GameSessionTest.java), [harness de carga](Minecraft2/test/loadtest/EnemyLoadHarness.java). La integración y el flujo público previstos se detallan en el plan | Evidencia histórica: suite de 313 pruebas aprobadas el 26 de septiembre, según el [reporte de cierre](Minecraft2/docs/corte2/testing/final-horde-20260926.md). La recuperación está pendiente de implementar y medir; este registro no acredita un resultado nuevo |

Los resultados nuevos incorporarán comandos, versión, pruebas ejecutadas, fallos/errores/omitidas, cobertura del dominio y métricas de carga. El ADR y los diagramas se enlazarán cuando estén terminados y revisados.

## Empezar a trabajar

Desde la raíz del clon, con los cambios locales previamente conservados:

```bash
git fetch origin
git switch feature/c2-Thomas
git pull --ff-only origin feature/c2-Thomas
```

Jasub usa `feature/c2-jasub` y Ethian `feature/c2-Ethian` en los dos últimos comandos. Si la rama todavía no existe localmente, se puede crear con `git switch --track origin/feature/c2-Thomas`, usando el nombre correspondiente. Si Git informa cambios locales o divergencia, revisarlos antes de continuar; no descartarlos.

Cada integrante modifica únicamente sus archivos del plan. Thomas integra el control de aplicación primero; Jasub y Ethian incorporan ese avance para validar sus consumidores. La integración final reúne Jasub y después Ethian y ejecuta la verificación combinada.

## Compilar, jugar y ejecutar la suite existente

Requisitos: JDK 17, Maven y entorno gráfico compatible con OpenGL para jugar. Desde la raíz del repositorio:

```bash
cd Minecraft2
mvn clean package
java -Dfile.encoding=UTF-8 -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

La suite existente se ejecuta desde `Minecraft2/` con:

```bash
mvn clean test
```

Esa suite contiene pruebas de distintos niveles; su total no se presenta íntegramente como pruebas unitarias aisladas. La separación de niveles y el reporte JaCoCo del dominio forman parte de la recuperación.

Controles, ejecución por IDE, guardados, limitaciones y evidencias históricas: [README de la aplicación](Minecraft2/README.md).

## Documentación

- [Plan de trabajo y reparto por archivos](Minecraft2/docs/recuperacion-c2/plan-equipo.md).
- [Checklist y registro de etapas](Minecraft2/TODO.md).
- [Arquitectura existente, pendiente de consolidar para la recuperación](Minecraft2/docs/arquitectura.md).
- [Vistas UML históricas, pendientes de contrastar con la nueva versión](Minecraft2/docs/uml.md).
- [Evidencia histórica de carga y límites de medición](Minecraft2/docs/corte2/testing/load-testing.md).

Los mundos son finitos y permanecen en memoria. Enemigos y oleadas son estado de sesión y no se guardan en JSON. Las métricas headless no demuestran FPS gráficos. Se conservan los controles y el formato JSON v1 durante la reorganización.

# Minecraft 2

Videojuego voxel universitario sencillo inspirado en Minecraft Classic, desarrollado en Java por tres estudiantes. El MVP del Corte 1 está completado; la recuperación del Corte 2 reorganiza los controles y completa evidencia arquitectónica sobre la base existente.

> Este proyecto no es todavía un videojuego completo. Incluye estructuras, contratos, conexiones entre capas, generación de terreno, movimiento, física, interacción por raycast, renderizado voxel, controles básicos y persistencia JSON.

## Estado del proyecto

### Corte 1 — Completado

Menú de consola, crear/listar/cargar/guardar/eliminar mundos, generación inicial de
cuatro chunks, bloques, jugador, cámara, movimiento, salto, gravedad, colisiones,
raycast e interacción, renderizado 3D y persistencia JSON. Incluye Factory,
Singleton, Observer y texturas de bloques ON/OFF. Baseline: 41 pruebas JUnit.

### Corte 2 — base funcional histórica

Biomas PLAINS/DESERT/MOUNTAINS, aldea, mundos finitos, zombies FSM + A* y oleadas
están integrados. El inicio normal abre el menú gráfico para crear/listar/cargar/
eliminar mundos; ESC pausa la partida y permite guardar, cambiar opciones y salir
con confirmación. F alterna pantalla completa conservando la partida.

Los zombies tienen cabeza, torso, brazos y piernas, UV por cara/parte y marcha.
Sus cuerpos se separan; spawn ocupado o a menos de 2.5 bloques del jugador se
rechaza. NORMAL: velocidad 4.3, detección 24, oleada inicial 4 a los 8 s.
VERY_HARD: velocidad 4.7, detección 28, oleada inicial 4 a los 4 s.
Ambas dificultades duplican cantidades: 4, 8, 16, 32, 64, 128… Las apariciones
fallidas se reintentan, sin descontarlas de la ronda.
Caminar sigue en 4.3; sprint 7.095 con estamina para aproximadamente 10 s.

Estamina: 100, consumo 10/s al moverse, regeneración 20/s, desbloqueo al 20% tras
agotarse. Pausa y muerte congelan la simulación; reaparecer restaura estamina.
La cámara añade un movimiento moderado y hasta 4 grados de FOV al correr.

Enemigos y texturas de zombies se aplican inmediatamente desde Opciones.
Dificultad y texturas de bloques se aplican en la próxima partida, con aviso.
Gráficos muestra el renderer OpenGL efectivo y permite Integrada/Dedicada/Automática en Linux
con dispositivos compatibles; requiere próximo inicio. Ver límites de GPU abajo.

Todos los menús son transparentes: un paisaje aleatorio temporal de 16 chunks sirve
de fondo sin partida; la pausa conserva el mundo de la partida. El menú muestra solo
Integrada o Dedicada en Intel/NVIDIA; los modelos quedan en la terminal.
El jugador tiene 100 de vida y una barra roja; cada golpe enemigo resta 25. El primero
es inmediato; siguientes golpes cada 0.6 s NORMAL / 0.4 s VERY_HARD. Al reaparecer
recupera toda la vida. Los zombis detectados conservan el objetivo mientras viva.
A* permite ocho direcciones con diagonales de coste sqrt(2) y protección de esquinas.
Los zombis ya no suben bloques instantáneamente: saltan con la gravedad y velocidad del
jugador, con mínimo 1 segundo entre saltos. Hay destellos rojos cortos al recibir daño.
La vida regenera 5/s después de 5 s sin golpes. Desde la oleada 3 aparece una pistola
sobre terreno aleatorio: acercarse la recoge; Q alterna, clic dispara y 1–7 vuelven a
bloques. Daño 3, alcance 36, intervalo 0.28 s, munición ilimitada.
Los zombis pueden entrar en pozos, de uno en uno, y recuerdan el borde cuando el jugador
se aleja por la cueva. Caída de 8 bloques o más: 70% se desarman y 30% sobreviven sin
piernas, con altura 0.8 y velocidad reducida al 28% (1.204 bloques/s en NORMAL);
persiguen por túneles bajos. Preparan cada salto al pozo durante 5 s en el borde;
el mismo hueco admite una entrada por segundo, siempre que esté libre.
Los cuerpos se desarman en 18 piezas texturizadas: chocan con suelo y paredes,
se asientan y permanecen durante toda la sesión, sin colisión ni bloquear oleadas.
Los restos no se guardan en JSON; se limpian al cerrar la partida.
La muerte por kamikaze dice «Te mató el zombi kamikaze»; por vacío «Aquí no hay piso».

El menú de pausa es transparente. La barra de estamina aparece arriba sin etiquetas.
El radio inicial de chunks es 2 (limitado por el tamaño del mundo). Los mensajes de
muerte recorren únicamente las tres frases restantes, una por muerte.
Detalle histórico del 26 de septiembre: [oleadas y restos](docs/corte2/testing/final-horde-20260926.md).
La validación anterior de combate se conserva como evidencia histórica.

Gate real 2026-09-26: **313 pruebas, cero fallos/errores/ignoradas**.
El registro histórico describe un smoke gráfico de menú, sprint, pausa, opciones,
fullscreen, resize y guardado/carga. Su harness de flujo no está versionado: no
se declara reproducible a partir del repositorio ni equivale a un playtest humano.
Resultados nuevos de PIT/carga y fronteras de evidencia:
[reporte de integración](docs/corte2/testing/integration-20260926.md).

### Recuperación — candidato integrado

La entrada sigue `GameInput.read()` → `GameSession.advance(delta, PlayerFrameInput)` → `PlayerControlService`. HUD/cámara consultan `session.controls()`, una instantánea inmutable. Pausa y muerte congelan el tick; R restablece estamina y flags; se conserva prioridad de combate y JSON v1.

Verificación actual: 199 unitarias/adaptadores aislados y 148 integración, sin fallos, errores u omitidas. [Pruebas, cobertura y evidencia seleccionada](docs/pruebas.md). [Tabla de trazabilidad obligatoria](../README.md#tabla-de-trazabilidad) e [índice de correcciones](docs/recuperacion-c2/correcciones.md).

ADR, vistas completas y tres escenarios de caja negra están integrados. Carga de recuperación medida: SLO de 80 zombis cumplido en las tres repeticiones (p95 ≤ 1,926 ms), 160 dentro del presupuesto y parada en 320; el perfilado identificó y corrigió el cuello de botella del índice de chunks ([resultados y análisis](docs/recuperacion-c2/carga.md#6-resultados)). Las evidencias de septiembre no se presentan como resultados de recuperación.

### Corte 3

**Plan y checklist de entrega continua y segura**

**Estado: Corte 3 en curso; implementación y evidencias pendientes.** Fecha límite: lunes 16 de noviembre de 2026, 23:59 (último commit de `main`). Las evidencias de los cortes anteriores conservan su alcance histórico y no completan automáticamente ningún punto de este checklist. Los frentes están asignados a las tres personas del equipo: reparto, ramas, archivos y calendario en el [README raíz](../README.md#asignación-del-corte-3) y en el [TODO](TODO.md). El alcance acordado con el profesor (front y back en contenedores separados, SLO de la comunicación front→back, ejecución local y multijugador en la misma red) prevalece sobre las premisas de «juego sin servidor» de C3-00, C3-01.4 y de la fila C3-04 hasta que el ADR-002 (`previsto`) lo registre.

El objetivo es entregar el sistema del Corte 2 mediante un flujo reproducible que compile, pruebe, verifique la arquitectura y la seguridad, publique versiones y despliegue automáticamente. El alcance incluye los tres adicionales: Kubernetes, observabilidad y DAST. Cada uno puede aportar hasta 5 puntos, pero la bonificación conjunta tiene un máximo de 10 puntos.

Las referencias de evaluación son **Proyecto Tercer Corte: Entrega continua y segura del sistema** y **Rúbrica Corte 3**. Los identificadores siguientes permiten repartir el trabajo y comprobar cada salida contra esos documentos.

#### C3-00. Primer hito: acordar la entrega de un juego sin servidor

Hasta el Corte 2, Minecraft2 es un juego de escritorio Java 17/Maven/LibGDX, ejecutado en un proceso, con mundos JSON locales, sin API, servidor multijugador ni base de datos. Para el Corte 3 el equipo acordó con el profesor desplegar front y back en contenedores separados, medir el SLO en la comunicación front→back, ejecutar el entorno de forma local y permitir multijugador en la misma red; el cambio se registra en el [ADR-002](docs/adr/ADR-002-despliegue.md). El enunciado establece: «Si el sistema no tiene ningún componente de servidor, el grupo acuerda con el profesor qué se despliega y cómo se verifica el SLO antes de empezar».

- [ ] **C3-00.1** Acordar con el profesor qué componente o modalidad se desplegará, dónde será verificable y qué demostrará el arranque con Docker Compose. Registrar el acuerdo antes de implementar la adaptación.

- [ ] **C3-00.2** Acordar cómo dirigir humo y carga al entorno desplegado conservando el significado del SLO del Corte 2. Definir preparación de datos, recursos, readiness, calentamiento, repeticiones, salida de error y evidencia de Actions.

- [ ] **C3-00.3** Confirmar la viabilidad de Kubernetes y del objetivo de DAST. ZAP necesita un objetivo HTTP pertinente; si no existe, acordar una alternativa o declarar la limitación y su efecto en la bonificación. No incorporar un endpoint sin función real solamente para escanearlo.

- [ ] **C3-00.4** Registrar el acuerdo, alternativas y consecuencias en el ADR de despliegue previsto; actualizar la arquitectura si el acuerdo exige una modificación real.

**Salida del hito:** alcance evaluable y SLO acordados por escrito. La consola existente con `-Dmc2.console=true` permite estudiar empaquetado, pero por sí sola no demuestra el cumplimiento de despliegue del proyecto. La evidencia de empaquetado local debe distinguirse de la evidencia del pipeline y del entorno desplegado.

#### Base que se conserva y pendientes del Corte 2

La base conserva las capas y contratos del [ADR-001](docs/adr/ADR-001-estilo-recuperacion.md), las pruebas en `test/`, fuentes en `src/` y el JAR sombreado `target/minecraft2-0.1.0-SNAPSHOT.jar`, todos dentro de `Minecraft2/` desde la raíz Git. El trabajo no requiere funcionalidades de negocio nuevas.

- [ ] **C3-01.1** Consolidar los pendientes declarados y la retroalimentación original del Corte 2, con requisito, fuente, estado, corrección y evidencia. Si falta retroalimentación, dejar su revisión pendiente; no inferirla.

- [ ] **C3-01.2** Clasificar cada punto como corrección exigida, límite conocido o decisión que requiere revisión. Revisar, entre otros, inversión incompleta de `WorldStorage`, ciclos del dominio, vistas históricas y límites de validación gráfica/humana.

- [ ] **C3-01.3** Resolver los pendientes exigidos por la retroalimentación y registrar cómo se resolvieron en `Minecraft2/docs/evolucion.md` —previsto—, con enlaces a pruebas y cambios reales. No declarar resueltos los límites que se conservan.

- [ ] **C3-01.4** Mantener JSON v1 y los mundos locales; usar datos temporales aislados para pruebas. El servidor headless y el multijugador son la excepción acordada y se justifican en el ADR-002; no añadir base de datos ni reorganizar el estilo sin necesidad y ADR correspondiente.

**Salida:** inventario trazable de pendientes y evolución de los tres cortes; los cambios de comportamiento quedan acompañados de pruebas pertinentes.

#### Frentes delegables, dependencias y entregables

Las rutas de esta tabla se expresan desde la raíz Git. **Previsto** significa que el archivo o la evidencia todavía debe producirse; no es un enlace a una entrega existente. Los documentos continúan bajo `Minecraft2/docs/`, y el README raíz será la entrada principal.

| Frente | Responsable | Depende de | Salidas previstas | Evidencia y condición de aceptación |
| --- | --- | --- | --- | --- |
| C3-00 · adaptación y despliegue | Thomas (acuerdo con el profesor, multijugador en dominio y aplicación); Jasub (adaptador de red y protocolo); Ethian (ADR-002) | Acuerdo docente | ADR de despliegue, contrato de humo/carga y objetivo de DAST | Alcance acordado (front y back separados, SLO front→back) registrado en el ADR-002 antes de la adaptación |
| C3-01 · cierre y evolución | Thomas | Fuentes del Corte 2 | `Minecraft2/docs/evolucion.md`, correcciones necesarias | Cada pendiente exigido tiene resolución reproducible; límites identificados |
| C3-02 · contenedores | Ethian | C3-00 | `Minecraft2/Dockerfile`, `Minecraft2/.dockerignore`, `docker-compose.yml`, `.env.example` | Arranque desde cero con Docker y un comando; readiness y ejecución sin root |
| C3-03 · integración continua | Thomas | C3-01 y C3-02 para la imagen | `.github/workflows/ci.yml`, ajuste de `Minecraft2/pom.xml`, `.github/dependabot.yml` | Checks obligatorios en push/PR, cobertura ≥70%, artefactos y duración <10 min |
| C3-04 · arquitectura y carga | Jasub (reglas de arquitectura y trazabilidad); Ethian (script de carga y umbrales en `Minecraft2/perf/`) | C3-00 para definir pruebas; C3-03/C3-06 para verificarlas desplegadas | Pruebas de fronteras existentes, `Minecraft2/perf/`, trazabilidad raíz | Dos reglas reales bloqueantes y SLO comprobado después de cada despliegue |
| C3-05 · seguridad | Jasub (STRIDE, seguridad, ADR de seguridad, PR negativo); Thomas (cableado de los cuatro gates) | C3-00, diagrama vigente y C3-02 | Gates CI, `Minecraft2/docs/seguridad.md`, ADR de seguridad | Cuatro controles bloqueantes y PR negativo cerrado sin fusionar |
| C3-06 · publicación y despliegue | Ethian | C3-02 a C3-05 | `.github/workflows/cd.yml`, imagen en registro, Releases | main despliega automáticamente; humo/carga bloqueantes; reversión probada |
| C3-07 · Kubernetes | Ethian | C3-00, C3-02 y C3-06 | `k8s/` | Manifiestos aplicados, probes, recursos y política justificada |
| C3-08 · observabilidad | Ethian | C3-00 y C3-06 | `observabilidad/` y documentación operativa | Métricas/tablero del SLO o logs centralizados con búsqueda verificable |
| C3-09 · DAST | Jasub (job ZAP y hallazgos); Ethian (cableado en `cd.yml`) | Objetivo viable en C3-00 y C3-06 | Job ZAP y hallazgos en seguridad | Escaneo del entorno desplegado en Actions; hallazgos tratados |
| C3-10 · documentación y guía | Thomas (README, evolución, guía); Jasub (seguridad, pruebas); Ethian (devops, vista de despliegue) | Se inicia con C3-00; se cierra con C3-06 a C3-09 | README, documentos y vista de despliegue | Pasos ejecutables, coherentes con la implementación y sin ayuda del equipo |
| C3-11 · entrega y presentación | Thomas (integración y presentación), con material de Jasub y Ethian | Todos los frentes | Enlaces de evidencia, presentación y demostraciones | Último main verde, entorno disponible y ambos casos demostrados en 15 min |

Las reglas y scripts de C3-04 se preparan antes de C3-06; su evidencia desplegada se completa al integrar ambos frentes. El empaquetado define primero los nombres de servicios, variables, comando de salud y artefactos; CI/CD, carga y documentación consumirán esos mismos nombres. La integración de workflows y del POM se realiza en secuencia para evitar cambios incompatibles. La documentación puede avanzar en paralelo, pero se cierra contra el sistema desplegado.

#### C3-02. Contenerización y arranque reproducible — 5 puntos

- [ ] **C3-02.1** Crear Dockerfile multi-etapa junto al POM: una etapa compila con Java 17/Maven y otra ejecuta el artefacto. Fijar versiones de las imágenes base; nunca `latest`.

- [ ] **C3-02.2** Ejecutar con usuario sin privilegios de root y permisos mínimos sobre los datos. Incorporar `.dockerignore` al contexto `Minecraft2/`, excluyendo guardados personales, `.env`, cachés y archivos innecesarios.

- [ ] **C3-02.3** Crear Compose en la raíz con contexto de construcción `./Minecraft2`, el sistema acordado y solo sus dependencias reales. Definir volúmenes y permisos para los datos que deban persistir.

- [ ] **C3-02.4** Configurar por variables de entorno; incluir todas en `.env.example` con valores ficticios. No incorporar contraseñas, tokens ni cadenas de conexión sensibles al código o a la imagen.

- [ ] **C3-02.5** Definir un endpoint o comando de readiness real, no una mera comprobación de proceso. Usarlo en Compose y en despliegue; ordenar las dependencias que existan con `depends_on` y condición de salud.

- [ ] **C3-02.6** Verificar el arranque desde cero en una máquina con Docker, sin Java/Maven instalados en el host ni pasos adicionales ocultos. La imagen de entrega debe construirse en Actions.

- [ ] **C3-02.7** Documentar y comprobar en el README raíz el contrato previsto: crear `.env` desde `.env.example` y ejecutar `docker compose up` desde la raíz Git. Añadir en la guía el resultado esperado, servicios y forma de detenerlos.

**Salida:** arranque reproducible de la modalidad aprobada, imagen sin root y healthcheck usado tanto localmente como al desplegar. Las instrucciones finales reflejarán el comando realmente comprobado.

#### C3-03. Integración continua y protección de main — 15 puntos

- [ ] **C3-03.1** Trabajar por ramas y pull requests; proteger main mediante ruleset o branch protection: PR obligatorio, aprobación de al menos otro integrante y todos los checks requeridos. No admitir push directo a main.

- [ ] **C3-03.2** Ejecutar GitHub Actions en **cada `push` y cada `pull_request`**. Hacer visible como check cada etapa y asegurar que pueda detener el pipeline. No ocultar fallos mediante `continue-on-error` en controles obligatorios.

- [ ] **C3-03.3** Ejecutar construcción, unitarias y pruebas de integración del Corte 2 con componentes reales o embebidos. Mantener la clasificación Surefire/Failsafe sin omitir ni duplicar clases; `package` por sí solo no reemplaza `verify`.

- [ ] **C3-03.4** Configurar en JaCoCo un **gate de cobertura de líneas de `domain/**` ≥70%** que rompa la construcción. Definir el conjunto de datos usado; no usar la cobertura global ni sumar porcentajes de unitarias e integración. El POM actual publica reportes, pero todavía no configura este gate.

- [ ] **C3-03.5** Publicar reportes de pruebas, cobertura, seguridad y carga como artefactos de cada ejecución, incluidos los reportes disponibles cuando un gate falle. Vincularlos al SHA y al enlace de ejecución.

- [ ] **C3-03.6** Configurar caché de dependencias Maven y medir el tiempo de integración: **menos de 10 minutos**, excluyendo despliegue y prueba de carga. Conservar duraciones reales; no confundir un timeout con el cumplimiento.

- [ ] **C3-03.7** Incorporar badge de estado en el README raíz. Comprobar el historial de push/PR, aprobaciones y bloqueo efectivo del merge; la configuración sin evidencia no basta.

- [ ] **C3-03.8** Preparar actualización de dependencias con Dependabot o equivalente; mantener SCA independiente y bloqueante.

Contrato previsto de etapas y salidas:

| Check | Entrada/salida | Condición que detiene el pipeline |
| --- | --- | --- |
| Construcción | Fuentes/POM → clases y artefacto | Error de compilación o resolución de dependencias |
| Unitarias y cobertura | Reglas aisladas → resultados y JaCoCo del dominio | Una prueba falla o cobertura de líneas del dominio <70% |
| Integración | Componentes y archivos temporales reales → reportes Failsafe | Una prueba falla |
| Arquitectura | Código y ADR-001 → reporte de fronteras | Cualquier dependencia prohibida |
| Seguridad | Código, dependencias e historial → reportes | Superar cualquiera de los cuatro umbrales de C3-05 |
| Imagen y escaneo | Artefacto/contexto → imagen identificada y reporte | Falla la construcción o aparece vulnerabilidad crítica con corrección disponible |
| Despliegue y verificación | Imagen aprobada → entorno, humo y carga | Despliegue, readiness, humo o SLO fallan |

Los PR verificarán en un entorno de prueba aislado la modalidad acordada, sin credenciales de producción. El despliegue accesible de C3-06 se ejecutará al llegar a main. En C3-00 se precisa la evidencia de despliegue/verificación exigida como check del PR (compose efímero con servidor y front en el runner del PR); no se contará como verificación un job que únicamente omita esos pasos.

**Salida:** un PR aprobado con checks bloqueantes, reportes y duración verificables, más main efectivamente protegida.

#### C3-04. Arquitectura, SLO y trazabilidad — 15 puntos

- [ ] **C3-04.1** Ejecutar en CI al menos dos reglas reales del [ADR-001](docs/adr/ADR-001-estilo-recuperacion.md), reutilizando [ArchitectureBoundaryTest](test/architecture/ArchitectureBoundaryTest.java): dominio sin dependencias a aplicación, presentación, persistencia, bootstrap ni LibGDX; aplicación sin presentación, bootstrap ni LibGDX y con `persistence.WorldStorage` como única dependencia admitida hacia persistencia.

- [ ] **C3-04.2** Conservar la regla de `GameInput` como traductor de entrada y el contrato de avance por datos. Comprobar que una infracción de las reglas hace fallar el job; reglas sobre mera existencia de paquetes no cuentan.

- [ ] **C3-04.3** Convertir el [SLO-CARGA-01](docs/recuperacion-c2/carga.md#2-slo-fijado-antes-de-ejecutar) en condición ejecutable: **80 zombis activos, p95 ≤16,67 ms por tick de IA, 0 ticks fallidos y 0 ticks con déficit de población, en cada una de tres repeticiones**. Conservar el protocolo o documentar la adaptación expresamente acordada.

- [ ] **C3-04.4** Hacer fallar el script si faltan resultados, no se alcanzó la población objetivo o se incumple cualquier umbral. No aprobar por un p95 bajo calculado con menos enemigos ni por promediar repeticiones que incumplen.

- [ ] **C3-04.5** Tras **cada despliegue**, esperar readiness con timeout, ejecutar humo y solo después carga corta **contra el entorno desplegado**. Si el proveedor se suspende, esperar su activación antes de medir. Conservar tiempos, recursos, parámetros y SHA del entorno.

- [ ] **C3-04.6** Ofrecer carga larga con `workflow_dispatch` o programación; no hacerla obligatoria en cada push. Publicar CSV, manifiesto y reporte como artefactos.

- [ ] **C3-04.7** Mantener la tabla del Corte 2 en el [README raíz](../README.md#tabla-de-trazabilidad) y añadir exactamente las dos columnas nuevas, conservando las seis anteriores: **Reto | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba | Resultado | Etapa del pipeline | Umbral que detiene el pipeline**.

- [ ] **C3-04.8** Completar las columnas nuevas para las cuatro filas existentes; actualizar resultados con fecha/SHA cuando exista evidencia nueva y conservar la distinción histórica. Declarar expresamente qué aspectos requieren revisión humana y por qué no tienen verificación automática.

Contenido previsto para las columnas nuevas; no reemplaza las seis columnas existentes:

| Aspecto del reto existente | Etapa del pipeline | Umbral que detiene el pipeline |
| --- | --- | --- |
| Actualizar muchos zombis por fotograma | Despliegue → readiness → humo → carga | Con 80 activos: p95 >16,67 ms, cualquier tick fallido o déficit de población; el tick se mide en el servidor desplegado (SLO-CARGA-01) y la adaptación se registra en el ADR-002 |
| Ajustar dificultad y oleadas | Unitarias/integración; reglas de arquitectura | Falla de una prueba vinculada al reto o infracción de frontera; no atribuir un gate PIT que aún no exista |
| Probar conducta y controles sin GPU | Unitarias/integración; reglas de arquitectura | Falla de pruebas o dependencia prohibida; cobertura del dominio <70% como gate transversal |
| Conservar mundo, guardado y estado | Integración y humo del flujo acordado | Falla de regresión de sesión, persistencia o recorrido público de crear/guardar/reabrir/eliminar |

**Límite de interpretación:** el SLO mide ticks de IA headless. No mide HTTP, FPS gráficos, GPU ni el tick completo de `GameSession`. Los resultados históricos de un portátil no acreditan el nuevo entorno desplegado. La validación humana de usabilidad y equilibrio debe declararse aparte, sin atribuirle resultados no realizados.

#### C3-05. DevSecOps — 20 puntos

- [ ] **C3-05.1** Construir STRIDE sobre el diagrama de contenedores del Corte 2 y actualizarlo a lo realmente desplegado: al menos **cinco amenazas específicas**, cada una con categoría, componente/flujo, mitigación y verificación.

- [ ] **C3-05.2** Revisar como candidatos concretos la manipulación de JSON en `JsonWorldStorage`, los nombres de mundo y acceso a archivos, agotamiento de memoria en generación/carga, divulgación mediante errores/logs y sustitución del JAR/imagen en la cadena de entrega. Son temas por evaluar, no vulnerabilidades confirmadas.

- [ ] **C3-05.3** Incluir autenticación y autorización donde existan usuarios/cuentas. Explicar el alcance del jugador local sin cuentas y el de las identidades de GitHub, registro y despliegue; no inventar login del juego.

- [ ] **C3-05.4** Implementar los cuatro gates de la tabla siguiente, con versiones/configuración fijadas y reportes retenidos. Justificar elección de herramientas y umbrales en el ADR de seguridad previsto.

| Control obligatorio | Alcance | Umbral mínimo literal que debe aplicarse |
| --- | --- | --- |
| SAST | Código propio | **Ningún hallazgo alto o crítico sin tratar** |
| SCA | Dependencias con CVE | **Ninguna vulnerabilidad alta o crítica con corrección disponible** |
| Detección de secretos | Código y **historial completo de Git** | **Cero secretos, incluido el historial** |
| Escaneo de imagen | SO y librerías dentro del contenedor | **Ninguna vulnerabilidad crítica con corrección disponible** |

- [ ] **C3-05.5** Asegurar un paso bloqueante que interprete el resultado cuando una herramienta solo reporte, por ejemplo SARIF. Publicar alertas en Security o abrir PR con Dependabot no sustituye los gates.

- [ ] **C3-05.6** Registrar **cada hallazgo** en `Minecraft2/docs/seguridad.md` —previsto—: corregido con enlace al commit; falso positivo con justificación técnica; o riesgo aceptado con justificación y trabajo necesario para corregirlo. No bajar umbrales ni deshabilitar reglas sin justificar; no usar excepciones para aprobar problemas que superen el umbral obligatorio.

- [ ] **C3-05.7** Crear un PR de prueba negativa con un problema deliberado y seguro, por ejemplo una credencial **ficticia** detectable. Conservar el check fallido y evidencia del bloqueo; **cerrarlo sin fusionar** y enlazarlo en seguridad. Nunca introducir un secreto real.

- [ ] **C3-05.8** Comprobar que no hay secretos reales en el repositorio ni en su historial. Si aparece uno, rotarlo y documentar la remediación; la rúbrica sitúa DevSecOps en Insuficiente cuando aparece un secreto real.

**Salida:** modelo específico, cuatro gates que realmente pueden fallar, registro completo y PR bloqueado accesible. Solo se endurecerán los umbrales; no se relajarán.

#### C3-06. Despliegue, versiones y reversión — 15 puntos

- [ ] **C3-06.1** Construir la imagen en Actions y publicarla en un registro con **SHA del commit** y **versión semántica cuando exista tag**. Desplegar una referencia identificable; nunca una imagen etiquetada únicamente como `latest`.

- [ ] **C3-06.2** Automatizar el despliegue de cada cambio que llegue a main al entorno accesible acordado. Si se usa aprobación previa a producción, configurarla expresamente con Environments; no introducir otros pasos manuales.

- [ ] **C3-06.3** Guardar secretos en GitHub Secrets o Environments, fuera del repositorio. Documentar permisos y configuración necesaria sin publicar valores reales.

- [ ] **C3-06.4** Ejecutar readiness, humo del flujo principal y carga en ese orden, con salida no cero si fallan. Exponer la URL activa o el mecanismo verificable del servidor propio, según el acuerdo C3-00.

- [ ] **C3-06.5** Definir rollback al tag o SHA anterior, tratamiento de los datos persistentes y verificación posterior. Ejecutarlo al menos una vez y conservar la ejecución de Actions o su registro reproducible en `Minecraft2/docs/devops.md` —previsto—.

- [ ] **C3-06.6** Crear tags semánticos y notas de cambios en GitHub Releases o `CHANGELOG.md` —previsto—. No confundir la versión `SNAPSHOT` del POM con una versión de entrega.

- [ ] **C3-06.7** Generar el cliente de escritorio en el pipeline y **publicar el JAR versionado en GitHub Releases con cada versión**, además de la imagen. Vincular versión, commit, JAR y despliegue.

- [ ] **C3-06.8** Verificar descarga y ejecución del cliente, y documentar su conexión al componente desplegado si aplica. Si el acuerdo conserva operación local sin servidor, explicar esa modalidad y su verificación, sin simular una conexión inexistente.

**Salida:** una versión trazable, desplegada automáticamente y verificable, cliente descargable y reversión efectivamente ejecutada.

#### C3-07, C3-08 y C3-09. Adicionales incluidos

- [ ] **C3-07.1 · Kubernetes:** crear en `k8s/` —previsto— `Deployment`, `Service`, `ConfigMap` y definición/plantilla de `Secret` sin credenciales reales; aplicar liveness/readiness y límites de recursos.

- [ ] **C3-07.2:** desplegar en kind, minikube o nube y demostrar el estado real de los recursos. Incluir al menos una `NetworkPolicy` o política RBAC justificada y efectiva; indicar cómo verificarla.

- [ ] **C3-07.3:** ejecutar el despliegue desde el pipeline o documentarlo paso a paso. Relacionarlo con la imagen/SHA, la configuración y el acuerdo de C3-00.

- [ ] **C3-08.1 · Observabilidad:** escoger y justificar una ruta evaluable: métricas de aplicación con Prometheus o equivalente y Grafana con p95, tasa de error y throughput del SLO; **o** logs centralizados con búsqueda.

- [ ] **C3-08.2:** guardar en `observabilidad/` —previsto— la configuración reproducible y tablero o consultas; mostrar datos reales del entorno. Si se usan métricas, las unidades serán ticks de IA y sus errores/throughput, sin denominarlas latencia HTTP ni FPS.

- [ ] **C3-08.3:** documentar acceso, consultas, retención y relación con la operación/SLO. No contar como observabilidad una captura de consola o un CSV aislado sin la solución requerida.

- [ ] **C3-09.1 · DAST:** resolver el objetivo HTTP relevante o alternativa acordada en C3-00 antes de implementar ZAP. Si la alternativa no satisface la bonificación, declarar esa limitación sin marcarla cumplida.

- [ ] **C3-09.2:** integrar OWASP ZAP baseline contra el entorno desplegado, retener el reporte y tratar los hallazgos con las mismas tres decisiones de seguridad. Definir el comportamiento del job y justificar su política; la rúbrica no fija aquí un umbral numérico adicional.

**Salida:** evidencia independiente de cada adicional. Presentar los tres no eleva la bonificación máxima por encima de +10.

#### C3-10. Documentación final y guía — 20 puntos

Todos los archivos nuevos indicados a continuación están **previstos**. Los enlaces existentes al Corte 2 son antecedentes, no evidencia de automatización del Corte 3.

- [ ] **C3-10.1 · README raíz:** badge, URL/mecanismo del entorno desplegado, arranque desde cero con Compose, eventos y ejecución del pipeline, versiones/cliente y tabla de trazabilidad de ocho columnas. Mantener este plan accesible desde la entrada principal.

- [ ] **C3-10.2 · `Minecraft2/docs/devops.md`:** diagrama del pipeline, etapa/check, entrada/salida, umbral y justificación de cada valor, publicación, despliegue y procedimiento de reversión con evidencia de ejecución.

- [ ] **C3-10.3 · `Minecraft2/docs/seguridad.md`:** STRIDE específico, controles, herramientas, umbrales, hallazgos y decisiones, enlace al PR bloqueado y tratamiento de DAST.

- [ ] **C3-10.4 · `Minecraft2/docs/arquitectura.md`:** actualizar la vista C4 de despliegue o vista física 4+1, indicando nodos, contenedores, comunicaciones, datos y límites de confianza. Debe coincidir con lo realmente desplegado, incluidos los adicionales que se presenten.

- [ ] **C3-10.5 · `Minecraft2/docs/adr/`:** crear al menos dos ADR nuevos: estrategia de despliegue y estrategia de seguridad del pipeline. Cada uno incluirá contexto, alternativas, decisión y consecuencias; el de seguridad justificará herramientas y umbrales. Conservar ADR-001 como antecedente.

- [ ] **C3-10.6 · `Minecraft2/docs/evolucion.md`:** narrar los tres cortes, decisiones mantenidas y cambiadas con razones verificables, resolución de pendientes del Corte 2 y límites del sistema final.

- [ ] **C3-10.7 · `Minecraft2/docs/pruebas.md`:** actualizar comandos, clasificación de pruebas, reportes, cobertura y relación con checks/artefactos reales; diferenciar pruebas gráficas, headless, humanas e históricas.

- [ ] **C3-10.8 · `Minecraft2/docs/implementacion.md`:** redactar la guía para un equipo técnico que conoce Docker/Git, desconoce el proyecto y debe operar sin consultar al grupo. Cubrir las nueve partes siguientes con pasos propios, no solo enlaces al README.

| Parte obligatoria de la guía | Contenido que debe poder verificarse |
| --- | --- |
| 1. Qué se va a implementar | Un párrafo del sistema, componentes instalados y enlace a vista de despliegue |
| 2. Requisitos previos | Cuentas/servicios, software y versiones, CPU/RAM/disco mínimos, puertos y dominio/HTTPS si aplica |
| 3. Costos estimados | Costo mensual por componente en **dos escenarios**: plan usado en el proyecto y plan recomendado para volumen real del negocio; explicitar supuestos y fuente/fecha de precios |
| 4. Configuración | Tabla completa: nombre, propósito, ejemplo, obligatoriedad, ubicación de configuración y generación de valores sensibles |
| 5. Instalación paso a paso | Desde cuenta/servidor vacío: entorno, repositorio, secretos, primer despliegue, datos iniciales y primer administrador si aplica; comandos/acciones exactos y resultado esperado en cada paso |
| 6. Verificación | Healthcheck, humo y recorrido corto del flujo principal; descarga, instalación y uso del cliente de Releases, con conexión si aplica |
| 7. Operación | Nueva versión, reversión, copia y restauración de datos, rotación de secretos, consulta de logs y reinicio de servicio |
| 8. Solución de problemas | **Al menos cinco** problemas con síntoma, causa y solución ejecutable |
| 9. Recomendaciones para producción | Credenciales, HTTPS, backups programados, capacidad/recursos y riesgos aceptados todavía abiertos |

- [ ] **C3-10.9** Aclarar los pasos no aplicables al juego local, como creación de administrador, sin inventar usuarios o infraestructura. Identificar exactamente los datos iniciales necesarios y el acuerdo correspondiente.

- [ ] **C3-10.10** Escribir comandos completos con rutas relativas y directorio de ejecución inequívoco; marcar parámetros variables como `<usuario-github>`. Usar los mismos nombres de variables, servicios y comandos que `.env.example`, Compose y workflows. Las capturas solo podrán ilustrar acciones en consolas web, no reemplazar ejecuciones del pipeline.

- [ ] **C3-10.11** Comprobar los pasos en un entorno limpio y registrar resultados reales. Revisar concordancia entre guías, diagramas, archivos, enlaces y evidencias.

**Salida:** documentación coherente y guía operativa autónoma, incluyendo instalación y operación posterior, con límites explícitos.

#### C3-11. Revisión final, presentación y coevaluación — 10 puntos

- [ ] **C3-11.1** Mantener el repositorio público o agregar al profesor como colaborador, con historial de Actions visible. Conservar enlaces a runs, artefactos, PR aprobados/bloqueados, registro, Releases y reversión.

- [ ] **C3-11.2** Comprobar el **último commit de main anterior a la fecha límite** y que su pipeline termine verde. Si no termina verde, integración continua, arquitectura verificada y despliegue continuo quedan como máximo en **Básico**.

- [ ] **C3-11.3** Mantener activa la URL durante la calificación. Para servidor propio, disponer de la ejecución de despliegue en Actions y la demostración aceptadas como evidencia. Comprobar de nuevo Compose desde cero siguiendo el README.

- [ ] **C3-11.4** Preparar una presentación de **15 minutos**: sistema/retos/evolución, vista de despliegue y pipeline, decisiones/umbrales, modelo de amenazas, límites y mejoras que el equipo realmente proponga.

- [ ] **C3-11.5** Demostrar en vivo **ambos casos**: cambio pequeño que entra por PR, supera el pipeline y queda desplegado; cambio que el pipeline bloquea por prueba, arquitectura o seguridad. Lanzar al inicio y revisar al final si hace falta para respetar el tiempo.

- [ ] **C3-11.6** Preparar material legible y participación de varios integrantes; todos deben poder responder sobre cualquier parte, además de su frente de trabajo.

- [ ] **C3-11.7** Revisar las cuatro dimensiones de coevaluación: claridad del flujo de entrega, justificación de decisiones, demostración en vivo y comunicación/material de apoyo. Cada una se califica de 1 a 5 y pesa igual: `(suma / 20) × 10`.

- [ ] **C3-11.8** Cada estudiante entrega las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación; no se modifican después. Omitirlas hace perder individualmente los puntos de esta categoría.

- [ ] **C3-11.9** Tener presentes las reglas: promedio de notas recibidas descartando la más alta y la más baja, redondeo a un decimal y posible ajuste docente por calificaciones sin sustento o sesgos.

La evaluación suma 90 puntos del profesor y 10 de los compañeros; salvo las condiciones individuales de coevaluación, la nota es común al grupo. La evidencia válida de automatización es **la ejecución en GitHub Actions**, no capturas de pantalla, archivos presentes sin ejecutar ni reportes que nunca puedan detener el pipeline.

#### Matriz de cierre por criterio

| Criterio | Máximo | Checklist | Artefacto/evidencia de cierre |
| --- | --- | --- | --- |
| Contenerización | 5 | C3-00, C3-02 | Dockerfile/Compose/env, arranque desde cero y healthcheck efectivo |
| Integración continua | 15 | C3-03 | Actions push/PR, gate JaCoCo, artefactos, duración y protección de main |
| Arquitectura verificada | 15 | C3-04 | Reglas derivadas de ADR, carga desplegada bloqueante y trazabilidad ampliada |
| DevSecOps | 20 | C3-05 | STRIDE, cuatro gates, hallazgos y PR negativo cerrado sin merge |
| Despliegue continuo | 15 | C3-06 | Registro SHA/semver, despliegue, humo/carga, rollback y Releases del cliente |
| Documentación y repositorio | 10 | C3-01, C3-10.1 a C3-10.7 | README, devops, seguridad, vista real, dos ADR nuevos y evolución |
| Guía de implementación | 10 | C3-10.8 a C3-10.11 | Nueve partes, dos escenarios de costos, ≥5 problemas y comandos comprobados |
| Presentación y demostración | 10 | C3-11 | Dos casos en vivo, 15 min, participación y coevaluaciones |
| Kubernetes | +5 | C3-07 | Clúster funcionando, probes/recursos/configuración y política justificada |
| Observabilidad | +5 | C3-08 | Métricas/tablero del SLO o logs centralizados con búsqueda |
| DAST | +5 | C3-00.3, C3-09 | Objetivo pertinente, ZAP en Actions y hallazgos tratados; máximo conjunto +10 |

Cada casilla se marcará únicamente cuando exista la salida y su evidencia verificable. El cierre exige concordancia entre el último main, los artefactos publicados, el entorno desplegado y la documentación.

## Problema

El equipo necesita iniciar un videojuego voxel sin mezclar la interfaz, las reglas del mundo y el acceso a archivos. Una base sin separación de responsabilidades dificultaría que tres personas trabajen en paralelo y que el proyecto evolucione sin dependencias circulares.

## Objetivo original del Corte 1

Ofrecer un esqueleto pequeño, claro y compilable que establezca:

- una arquitectura por capas;
- los modelos principales del dominio;
- los tres patrones elegidos por el equipo: Factory, Singleton y Observer;
- operaciones CRUD conceptuales para mundos guardados como JSON;
- un menú principal de consola desde el cual conectar los casos de uso.

## Alcance del MVP — Corte 1 completado

- Menú principal.
- Crear, listar, cargar, guardar y eliminar mundos.
- Uno o pocos chunks por mundo.
- Generación pseudoaleatoria sencilla.
- Bloques de aire, césped, tierra, piedra, arena, grava, madera y hojas.
- Movimiento hacia adelante, atrás, izquierda y derecha.
- Salto y gravedad.
- Colocación y eliminación de bloques mediante clic.
- Ventana 3D con renderizado de chunks y controles básicos de teclado y ratón.
- Persistencia local en archivos JSON.

## Fuera del alcance del Corte 2

- Un videojuego terminado o una réplica completa de Minecraft.
- Multijugador, agente LLM, survival completo, inventario complejo o crafting.
- Mundo infinito/streaming y carga dinámica de chunks; motor gráfico completo.
- ECS, behavior trees, machine learning, redes neuronales y arquitectura distribuida.
- Base de datos.
- Refactors masivos y reescritura de la arquitectura. La restricción original de
  patrones del Corte 1 se conserva como contexto histórico, no excluye la FSM acordada del Corte 2.
- Frameworks o librerías que no sean necesarios para esta base.

## Tecnologías

- Java 17.
- IntelliJ IDEA.
- Maven para compilación reproducible.
- LibGDX 1.12.1 con backend LWJGL3 para la ventana y el renderizado 3D.
- Archivos JSON locales, sin base de datos ni librería externa.

## Arquitectura

La versión actual mantiene capas con coordinación explícita en aplicación. `GameInput` convierte dispositivos en `PlayerFrameInput`; `GameSession` coordina el tick y `PlayerControlService` integra las reglas existentes de movimiento/física/colisión/estamina e interacción. Presentación conserva cámara, HUD y render; dominio no depende de capas superiores ni LibGDX. Persistencia conserva JSON v1.

`application` aún depende de la abstracción `persistence.WorldStorage`; no se presenta como una arquitectura hexagonal completa. Se conservan Factory, Observer, Singleton y los ciclos internos declarados. [Comparación, costes, antes/después y pendientes](docs/arquitectura.md).

## Patrones utilizados

### Factory

`BlockFactory` centraliza la creación de `Block` a partir de un `BlockType` y una `Position`. Reconoce AIR, GRASS, DIRT, STONE, SAND, GRAVEL, WOOD y LEAVES. No contiene todavía propiedades especiales por material. Véase [docs/factory.md](docs/factory.md).

### Singleton

`WorldManager` conserva la referencia al mundo actualmente cargado y ofrece un único punto de acceso con `getInstance()`. Es el único Singleton del proyecto. Véase [docs/singleton.md](docs/singleton.md).

### Observer

`World` actúa como sujeto de notificaciones `BlockChange`. Al colocar o eliminar un bloque, comunica un cambio `PLACED` o `REMOVED` a los observadores registrados. No hay bus global ni sistema complejo de eventos. Véase [docs/observer.md](docs/observer.md).

## Estructura resumida del proyecto

```text
Minecraft2/
├── src/bootstrap/        composición y arranque
├── src/presentation/     consola y juego gráfico
├── src/application/      sesión, controles y casos de uso
├── src/domain/           reglas, jugador, mundo e IA
├── src/persistence/      contrato y almacenamiento JSON
├── src/patterns/         Factory, Observer y Singleton
├── test/                 unitarias, integración y arneses
├── docs/arquitectura.md  documento canónico
├── docs/pruebas.md       estrategia, resultados y evidencia
├── docs/recuperacion-c2/ seguimiento y evidencias seleccionadas
├── pom.xml               build, Surefire, Failsafe, JaCoCo y PIT
└── TODO.md               tareas y registro histórico
```

Las carpetas ADR/diagramas de recuperación y perf/ están incorporadas. Esta vista resumida no enumera todos los archivos.

## Instrucciones de ejecución

### IntelliJ IDEA

1. Abrir la carpeta raíz `Minecraft2`.
2. Configurar un JDK 17 si IntelliJ lo solicita.
3. Esperar a que IntelliJ importe `pom.xml`.
4. Ejecutar el método `main` de `bootstrap.Minecraft2Application`.

### Terminal

```bash
mvn clean package
java -Dfile.encoding=UTF-8 -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

Se abre el menú gráfico; la consola no es necesaria para jugar.
Para mundos grandes puede usarse `java -Xmx2g -jar target/minecraft2-0.1.0-SNAPSHOT.jar`.
El mundo completo sigue en memoria; no hay streaming.

La consola histórica se conserva como compatibilidad y diagnóstico:

```bash
java -Dmc2.console=true -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

### Pruebas

Desde esta carpeta:

```bash
mvn clean test
mvn clean verify -DskipUnitTests=true
mvn clean verify
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

En orden: unitarias/adaptadores aislados; integración con JAR; verificación conjunta; mutación. Surefire/Failsafe y cobertura del dominio se documentan en el [README raíz](../README.md#ejecutar-pruebas-por-nivel). Allí también está el comando del arnés de controles con OpenGL real.

### Controles

| Acción | Control |
| --- | --- |
| Caminar / correr | WASD / Shift con estamina |
| Saltar / mirar | Espacio / ratón |
| Golpear zombie o eliminar bloque | Clic izquierdo |
| Colocar / elegir bloque | Clic derecho / 1–7 |
| Distancia visible | J/K |
| Equipar / guardar pistola | Q |
| Reaparecer | R |
| Pausa y menú | ESC |
| Pantalla completa / ventana | F |

ESC libera el cursor y congela movimiento, física, enemigos, oleadas y estamina.
Los clics de menús no editan el mundo. Salir ofrece Guardar y salir / Salir sin
guardar / Cancelar. No hay guardado automático silencioso.

### Guardados locales

Crear un mundo ya guarda su estado inicial. Guardar desde pausa conserva los
cambios posteriores. Mundos muestra la carpeta absoluta y los errores de lectura.
Un archivo inválido permanece visible, y cargarlo informa el error sin sustituir
el mundo actual. JSON versión 1 se conserva; enemigos y estamina son de sesión.

La ruta se resuelve desde IDE/JAR hasta la raíz del proyecto, no desde el cwd.
Puede fijarse `-Dmc2.worlds.dir=target/mundos-prueba`. Copiar el JAR fuera del
proyecto puede cambiar la carpeta por defecto. Los archivos inválidos se conservan y se muestran con su error de lectura.

### Gráficos y GPU

LibGDX usa OpenGL. Opciones → Gráficos/GPU muestra la tarjeta en uso y ofrece
**Integrada · menor consumo**, **Dedicada · mayor rendimiento** y **Automática**.
Elige una y vuelve a abrir el juego: la selección se realiza antes del contexto.
La integrada selecciona por PCI el adaptador de arranque Mesa compatible; la dedicada
solicita PRIME NVIDIA o DRI_PRIME Mesa cuando Linux expone varios dispositivos.
Un fallo de arranque permite continuar con la selección del sistema.
La preferencia se guarda en graphics.properties, fuera de los mundos.
La terminal conserva GL_VENDOR, GL_RENDERER, GL_VERSION y backend.
También puede usarse `-Dmc2.gpu=INTEGRATED` o `-Dmc2.gpu=DEDICATED` sin guardar.

Pruebas físicas de la retroalimentación: Integrada → Intel/Mesa; Dedicada → NVIDIA RTX 4060.
AMD tiene ruta Mesa implementada y tests de selección; no hubo hardware AMD
para comprobarla. Windows/macOS delegan selección al sistema y no ofrecen aquí
el relanzamiento dedicado. Java 17 es el objetivo; el JDK 26 local mostró avisos
LWJGL/Unsafe aunque las pruebas gráficas pasaron.

Las comprobaciones que todavía no están en JUnit se ejecutan una a una:

```bash
java -cp target/classes manualtest.WorldBlockOperationsTest
java -cp target/classes manualtest.PlayerMovementManualTest
java -cp target/classes manualtest.PlayerPhysicsManualTest
java -cp target/classes manualtest.CollisionManualTest
java -cp target/classes manualtest.BlockInteractionAndPatternsManualTest
```

Los archivos creados desde el menú se guardan en la carpeta local `worlds/`. Esa carpeta está ignorada por Git.

## Limitaciones y revisión pendiente

- Mundos finitos de 4/100/256 chunks en memoria; no hay streaming, crafting ni multiplayer.
- Vida de sesión con daño y regeneración; sin survival completo ni persistencia de salud.
- Percepción del zombie por distancia; detecta a través de paredes, pero navegación
  consulta terreno y el golpe cuerpo a cuerpo exige línea despejada.
- Separación local; no es pathfinding multiagente. Corredores pueden formar filas.
- Biomas/aldea/interacción están cubiertos por tests existentes; la revisión humana
  prolongada de equilibrio, cámara, melee, salto, R/J/K y estructuras queda pendiente.
- La carga histórica de septiembre y la de recuperación son headless: no acreditan medición térmica/GPU ni FPS. La de recuperación se midió en un solo equipo Windows.

## Integrantes

- Estudiante 1: **Elioth Thomas Gomez Morales**.
- Estudiante 2: **Jasub Sastre**.
- Estudiante 3: **Ethian Daniel White Ortiz**.

## Modelado UML

El modelado UML de la asignatura está en [docs/uml.md](docs/uml.md). Incluye el diagrama de clases obligatorio y vistas complementarias de casos de uso, patrones, componentes y secuencias, de las etapas anteriores con ventana 3D, controles y observador visual; deben contrastarse con el código antes de una nueva entrega.

## Registro del Corte 2

Consultar [TODO.md](TODO.md) para tareas por estudiante y dependencias, y
el registro de fases y validaciones del Corte 2 (documento histórico retirado del árbol de entrega).
La arquitectura vigente está en `docs/arquitectura.md`; los documentos de `docs/corte2/` son históricos; el cierre funcional
está registrado en `docs/corte2/testing/final-horde-20260926.md`. Los resultados históricos
se conservan identificados por fecha. `docs/uml.md` conserva vistas de las etapas anteriores; no se considera actualizado para una nueva entrega.

## Actualizar desde main y jugar

Desde la raíz del repositorio Mc2, con JDK 17 y Maven instalados:

```bash
git pull origin main
./Minecraft2/jugar.sh
```

El lanzador compila el código actualizado antes de abrir el juego. En IntelliJ:
actualizar main, recargar Maven y ejecutar `bootstrap.Minecraft2Application`.
`worlds/` y `graphics.properties` son locales y no se publican; cada computador
conserva sus mundos y elige su propia tarjeta gráfica. La recuperación reúne las tres ramas en el candidato común, con su evidencia de carga en `perf/results/`.

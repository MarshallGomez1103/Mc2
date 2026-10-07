> Registro histórico del Corte 2. Los conteos, parámetros y métricas corresponden a la etapa descrita; no son resultados del Corte 3.

# Sprint y estamina — implementación y evidencia

> Evidencia de la primera integración. Los ajustes posteriores y su validación están en
> [feedback-20260926.md](feedback-20260926.md); el README y el registro final de oleadas describen la última etapa del Corte 2.

## Implementado

- `domain.player.Stamina`: componente de sesión sin LibGDX ni persistencia. Máximo100,
  consumo20/s (5s de sprint continuo), regeneración14/s. Al agotarse bloquea sprint
  hasta recuperar20 (aproximadamente1.43s); recuperar desde0 a100 tarda7.14s.
- Shift expresa intención. `GameInput` autoriza velocidad con estamina; caminar4.3,
  sprint1.65× (7.095 bloques/s) preservados. Al quedar poca energía el último paso
  mezcla duración de caminar/sprint y no concede tiempo de sprint gratuito.
- Estamina se consume sobre movimiento horizontal realmente aplicado por colisión;
  quieto, teclas opuestas o pared completamente bloqueante regeneran. Con deslizamiento
  parcial el consumo escala por distancia aplicada respecto al desplazamiento propuesto.
- `SprintCameraEffect` no mueve al jugador: FOV adicional hasta4°, desplazamiento vertical
  máximo0.035 bloques mientras sprinta en suelo, transición exponencial suave. En aire
  conserva FOV y amortigua el bob anterior.
- Nueva sesión y `GameInput.resetAfterRespawn()` restauran energía100 y estados de movimiento.
  La pausa/muerte congelan simulación omitiendo updates (responsabilidad del loop).
- Estamina no modifica el JSON ni introduce nuevos singleton/dependencias.

## APIs para integración

- `GameInput.stamina()`, `isSprinting()`, `isMoving()`, `resetAfterRespawn()`.
- `Stamina.current()`, `maximum()`, `fraction()`, `exhausted()`, `canSprint()`.
- `SprintCameraEffect.update(delta,sprinting,moving,grounded)`, `verticalOffset()`,
  `fovOffset()`, `reset()`.
- El HUD dibuja `fraction()`. La cámara agrega `verticalOffset()` a altura de ojo y
  `fovOffset()` a FOV70. Loop llama efecto sólo RUNNING y reset al reaparecer.
- `GameInput.updateMovement(...)` expone paso sin dispositivos para pruebas. Sigue
  preservando el límite de física0.05s existente; no implica simulación ilimitada
  durante tirones. Comparación deFPS cubre30/60/144FPS dentro de este límite.

## Probado automáticamente

Ejecución real durante esta actualización:

```text
mvn -q -Dtest=StaminaTest,GameInputStaminaTest,SprintCameraEffectTest,PlayerMovementSprintTest test
```

Exit0:12 tests,0 fallos/errores/omitidos (5+3+3+1).

Casos: consumo equivalente30/60/144FPS, distancia sprint equivalente, último paso
limitado por energía, agotamiento/umbral20%, recuperación, máximos, reset, delta0,
rechazo de tiempos inválidos, Shift quieto, teclas contrarias, pared bloqueante,
velocidad de caminar tras agotamiento, offset moderado/atenuación/aire y regresión
original de normalización diagonal. `git diff --check` sin errores.

El gate completo pertenece
al responsable de integración.

## Revisión visual / limitaciones

Esta evidencia focalizada no afirma un playtest manual. La barra y el efecto de cámara
requieren verificar la integración final en ventana: Shift yW, 5s de consumo, recuperación,
pared, salto, pausa sin consumo/regeneración, respawn, bob/FOV moderado y volver a caminar.
Validar comodidad visual del usuario; los límites pequeños no sustituyen esa revisión.
Balance de dificultad/hordas pertenece al frente de enemigos y no se cambió en estos archivos.

## Smoke gráfico automatizado de integración

Se ejecutó un harness de flujo gráfico externo, no versionado, con OpenGL real y backendLWJGL3,
Input/Graphics delegados al backend salvo teclas programadas y delta1/60. El harness
dispara callbacks reales `ChangeEvent` de botones Scene2D y escribe TextField. No es
un playtest humano ni verifica ergonomía subjetiva. Resultado final exit0,
`FLOW_SMOKE_SUCCESS`,224frames. Renderer observado Intel Mesa RPL-S/OpenGL4.6.

- Crear y jugar desde UI, listar archivo creado.
- W+Shift durante121frames: estamina59.6666646, Z22.8082507; se dibujó barra.
- ESC pausa: posición, estamina y timer horda invariables; cursor liberado.
- Clic izquierdo sintético durante UI no cambia bloques/salud de zombi.
- Opciones enemigos/texturas se aplican ahora, dificultad/bloques quedan para próxima partida.
- Continuar recaptura cursor. F cambia fullscreen y vuelve a ventana.
- Resize640×480 y1280×720: dimensiones ventana/cámara/Stage coinciden.
- Guardar y salir, crear servicio/shell nuevos en mismo JVM/contexto y cargar desde JSON:
  bloqueWOOD editado persiste. No equivale a reiniciar proceso del sistema operativo.
- Cancelar salida preserva partida, cancelar eliminación preserva JSON; confirmación
  elimina únicamente mundo temporal seleccionado.

Se usaron mundos temporales; ningún guardado personal fue modificado. Intentos previos
del harness corrigieron su propia instalación de proxy porframe y selección de lista,
sin cambios al producto. Capturas inspeccionadas: pausa/opciones legibles.
Artefactos externos no versionados: `game-flow-smoke.log`, `main-menu.png`,
`gameplay.png`, `stamina.png`, `pause-menu.png`, `options.png`, `gameplay-resized.png`.
El playtest humano completo permanece pendiente.

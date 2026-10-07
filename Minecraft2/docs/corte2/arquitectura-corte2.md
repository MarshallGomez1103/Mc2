# Arquitectura Corte 2 — estado integrado 2026-09-26

Los tres frentes y la ampliación R1–R10 están implementados localmente.
Registro histórico de pruebas y límites: [integración](testing/integration-20260926.md).
Se conservan Java objetivo 17, Maven, LibGDX 1.12.1, JSON v1 y capas existentes.
No se introdujeron dependencias, singletons ni una reescritura global.

## Dependencias y composición

```text
bootstrap.Minecraft2Application
  ├─ WorldDirectory + GpuPreference (antes del contexto)
  ├─ JsonWorldStorage + BlockFactory
  ├─ WorldApplicationService
  └─ GameWindow → GraphicalGame
                       ├─ Stage/MenuSkin: pantallas y CRUD delegado
                       ├─ GraphicsDiagnostics + FullscreenController
                       └─ VoxelGame: render/input y Observer
                              ├─ GameSession: estado, vida, EnemyUpdateService, HordeManager
                              ├─ GameInput: traducción de input, Stamina, física/interacción
                              ├─ SprintCameraEffect + GameHud
                              ├─ ChunkMeshBuilder + BlockTextureAtlas
                              └─ ZombieRenderer + ZombieAtlas
```

`application` coordina servicios de dominio/persistencia; el dominio no importa
LibGDX ni capas superiores. Persiste el ciclo interno domain.world ↔ domain.player,
por World/Player y CollisionResolver; no se afirma ausencia de todo ciclo.
WorldManager sigue siendo el único Singleton. BlockFactory centraliza bloques y
World publica BlockChange al Observer VoxelGame para reconstruir mallas vecinas.

## Responsabilidades

| Componente | Responsabilidad |
| --- | --- |
| GraphicalGame | UI principal/mundos/pausa/opciones; ciclo de vida de la vista; llama casos de uso |
| VoxelGame | Loop de presentación, cámara, mallas, render y conexión de input; no CRUD |
| GameSession | RUNNING/PAUSED/DEAD; snapshot dificultad y avance de vida/IA/hordas |
| GameInput | WASD/Shift/ratón a servicios existentes; consumo según movimiento aplicado |
| Stamina | Energía, consumo, regeneración, agotamiento y reset, sin gráficos |
| GameHud / SprintCameraEffect | Barra, overlays, proyecciones, bob/FOV moderados |
| EnemyUpdateService | FSM, A*, repath, ataques, despawn y separación local |
| ZombieSeparation | Resolución de AABB sobre terreno; no pathfinding multiagente |
| HordeManager | Scheduling y spawn determinista, ocupado/seguro, no IA individual |
| WorldApplicationService | CRUD; usa WorldStorage; UI no duplica persistencia |
| JsonWorldStorage | Snapshot JSON compatible, escritura temporal y movimiento atómico |

## Sesión, pausa y settings

- RUNNING ejecuta input/física/estamina, vida, horda y enemigos con delta limitado a .05.
- PAUSED omite el avance completo, libera cursor y deja Stage activo.
- DEAD congela avance; R reaparece, conserva mundo, restaura estamina y efecto.
- Continuar se difiere al frame siguiente y la vista omite un frame de input:
  el clic UI no golpea zombies ni edita bloques.
- Enemigos ON/OFF y textura zombie se aplican inmediatamente de forma coherente.
- Dificultad y textura bloque son preferencias de próxima partida; GameSession
  conserva Difficulty y parámetros vigentes. El menú muestra el aviso y dificultad activa.
- GPU se solicita antes del contexto en el próximo inicio; renderer efectivo visible.
- Preferencia GPU se guarda fuera del JSON. Otras opciones siguen siendo de la sesión del programa.

## Mundo, navegación y cuerpos

World conserva chunks 16×64×16 y Player. Tamaños finitos 2×2/10×10/16×16;
se liberan mallas, nunca bloques del snapshot. Biomas y aldea siguen deterministas.
Zombies no heredan Player ni se persisten. Cuerpo lógico 0.6×1.8×0.6;
modelo visual seis piezas, UV por parte/cara, frente +Z girado con movimiento.
FSM decide conducta y A* decide ruta; vecinos cuatro, subida uno, caída tres,
World actual y chunk ausente no caminable. Transiciones comprueban techo sobre
ambas columnas; movimiento consume distancia por subpasos para conservar velocidad.
Ruta bloqueada espera cooldown, evitando retries por frame.

Separación local AABB determinista, máximo 32 pasadas con correcciones acotadas,
validadas contra terreno; muertos no bloquean. Spawn evita cuerpo ocupado y
rechaza posiciones a menos de 2.5 bloques del jugador. Corredores forman filas;
no existe navegación global multiagente. Detección sigue por distancia sin visión,
pero el golpe enemigo verifica segmento despejado por terreno.

## Persistencia, UX y recursos

Crear ya persiste estado inicial. Guardar conserva bloques/posición/orientación;
salir ofrece guardar/sin guardar/cancelar, sin autosave oculto. Archivo inválido
sigue en lista, su error se muestra al cargar y conserva el mundo actual.
WorldDirectory resuelve ruta absoluta desde IDE/JAR; override mc2.worlds.dir.
Mover una copia del JAR fuera del proyecto puede cambiar su ruta por defecto.

La shell mantiene una sola ventana. Crear vista es transaccional a nivel de
presentación: si falla, libera recursos parciales y vuelve al menú con error.
Dispose retira Observer, modelos/mallas, texturas, Stage/Skin/font/batches.
F conserva tamaño windowed/cursor; resize actualiza cámara, HUD y Stage.
No se instalaron drivers ni hay CUDA/cambio de GPU en caliente.

## Validación y límites

238 pruebas automatizadas en la primera integración; 313 en la etapa final y smoke OpenGL programado con entradas simuladas.
PIT seleccionado y carga headless nuevos en el reporte. Capturas zombie de
frente/espalda/lateral/superior/marcha y OFF revisadas. Prueba GPU Intel + NVIDIA
real; AMD y otros SO no físicamente probados. Playtest humano de diversión y
sesión gráfica prolongada siguen pendientes. ENDURANCE actual: un minuto real.

## Ampliación de combate

- Dominio: ZombiePhysics integra saltos/caídas y cajas; KamikazePolicy decide el
  resultado de un aterrizaje profundo. PlayerLife guarda vida fraccional para regenerar
  independientemente de FPS, causa de muerte y reloj del golpe. No dependen de LibGDX.
- Aplicación: PistolService guarda aparición/recogida/equipado y cooldown de una sesión;
  ZombieTargeting aplica el mismo rayo/oclusión a pistola y puños. EnemyUpdateService
  coordina caminos normales, entrada de pozos y ataques. GameSession es el único reloj.
- Presentación: PistolRenderer tiene modelos reutilizados para suelo/mano y destello.
  ZombiePose describe transformaciones puras, ZombieRenderer las dibuja. GameHud
  presenta vida, daño y controles; DeathMessages selecciona por causa concreta.
- Persistencia: no cambia JSON; armas, oleadas, salud y enemigos se reinician al crear
  otra sesión de juego. Muerte/reaparición conserva la pistola de la sesión.

Ver evidencia actual en testing/combat-kamikaze-20260926.md.

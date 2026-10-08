# C4 nivel 3 — Componentes al cierre del Corte 2 (histórica, antes de la recuperación)

> **Vista histórica.** Corresponde al commit `4d33d6a`, cierre del Corte 2. `4220c40` solo añade documentación y tiene el mismo código fuente. Es el estado de partida de la recuperación, no el resultado.

Contenedor: aplicación de escritorio en la JVM. Solo se muestran los componentes del juego y del reto. Menús, skins, diagnóstico de GPU y renderers auxiliares se agrupan.

```mermaid
flowchart LR
    subgraph boot["bootstrap"]
        main["Minecraft2Application"]
        gpu["GpuPreference<br/>WorldDirectory"]
    end

    subgraph pres["presentation.game (LibGDX)"]
        gui["GameWindow → GraphicalGame<br/><i>menú gráfico, opciones, pausa</i>"]
        voxel["VoxelGame<br/><i>loop, cámara, mallas, Observer</i>"]
        input["GameInput<br/><i>teclado/ratón + coordinación</i>"]
        hud["GameHud<br/>SprintCameraEffect"]
        zr["ZombieRenderer<br/>PistolRenderer"]
    end

    subgraph app["application"]
        session["GameSession<br/><i>RUNNING/PAUSED/DEAD, reloj</i>"]
        enemies["EnemyUpdateService<br/><i>IA por tick</i>"]
        horde["HordeManager<br/><i>oleadas</i>"]
        pistol["PistolService"]
        melee["ZombieMeleeService<br/>ZombieTargeting"]
        inter["PlayerInteractionService"]
        wsvc["WorldApplicationService"]
    end

    subgraph dom["domain"]
        enemy["domain.enemy<br/><i>Zombie, ZombieStateMachine,<br/>AStarPathfinder, NavigationGrid,<br/>ZombiePhysics, KamikazePolicy,<br/>WaveRules, Difficulty</i>"]
        player["domain.player<br/><i>PlayerMovementService, PlayerPhysics,<br/>CollisionResolver, Stamina, PlayerLife</i>"]
        world["domain.world<br/><i>World, Chunk, biomas</i>"]
    end

    subgraph pers["persistence"]
        storage["«interface» WorldStorage"]
        json["JsonWorldStorage"]
    end

    main --> gpu & json & wsvc & gui
    gui --> voxel & wsvc
    gui -.-> gpu
    voxel --> session & input & hud & zr & world
    voxel -- "advance(delta, () -> input.update(...))" --> session
    input == "construye y coordina física,<br/>colisión, movimiento, estamina" ==> player
    input --> enemies & pistol & melee & inter
    hud --> session
    session --> enemies & horde & pistol & player
    horde --> enemies
    enemies --> enemy & world & player
    pistol --> enemy
    wsvc --> storage
    json -. implementa .-> storage

    classDef d fill:#dbe9ff,stroke:#1168bd,color:#000
    classDef a fill:#d9f2d9,stroke:#2e7d32,color:#000
    classDef p fill:#ffe5cc,stroke:#e67e22,color:#000
    classDef g fill:#eeeeee,stroke:#777,color:#000
    class enemy,player,world d
    class session,enemies,horde,pistol,melee,inter,wsvc a
    class gui,voxel,input,hud,zr p
    class main,gpu,storage,json g
    linkStyle 6,13 stroke:#c62828,stroke-width:2px,stroke-dasharray:6 4
```

## Secuencia de un tick (antes)

```mermaid
sequenceDiagram
    participant V as VoxelGame.render (presentación)
    participant S as GameSession (aplicación)
    participant I as GameInput (presentación)
    participant D as PlayerPhysics / Collision / Stamina (dominio)
    participant H as HordeManager
    participant E as EnemyUpdateService

    V->>S: advance(delta, Runnable)
    alt RUNNING y delta válido
        S->>I: playerUpdate.run()
        I->>I: lee Gdx.input (teclado/ratón)
        I->>D: mover, gravedad, colisión, gastar estamina
        S->>S: life.update/advance
        S->>H: update(delta, x, z)
        S->>E: update(life, delta)
    else PAUSED, DEAD o delta inválido
        S-->>V: no avanza
    end
```

## Reglas de dependencia observadas en `4d33d6a`

| Regla (ADR-001 §5) | Estado |
| --- | --- |
| 1. `domain` sin capas superiores ni LibGDX | Se cumple. Persiste el ciclo `domain.world` ↔ `domain.player`. |
| 2. `application` sin presentación ni LibGDX; solo `persistence.WorldStorage` | Se cumple. |
| 3. `GameInput` sin coordinadores de física, movimiento ni estamina | **No se cumple** (REC-04). |
| 4. Una sola entrada de avance con datos | **Parcial.** `GameSession` es el único reloj, pero recibe un `Runnable` de la vista. |
| 5. La vista solo lee el modelo | **No se cumple** para el jugador. `GameInput` también llama a `EnemyUpdateService`, `PistolService` y `ZombieMeleeService` para el combate. |

Límite adicional: `presentation.game.GraphicalGame` → `bootstrap.GpuPreference`, una dependencia hacia la raíz de composición (línea discontinua).

Lo que sí resolvió el Corte 2 para el reto: FSM, A\*, física del zombi y reglas de oleada están en `domain.enemy`, sin LibGDX. `GameSession` es el único reloj de vida, hordas, enemigos y pistola.

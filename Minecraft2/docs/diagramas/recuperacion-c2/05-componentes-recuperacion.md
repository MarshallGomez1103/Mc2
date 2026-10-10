# C4 nivel 3 — Componentes de la recuperación

> **Vista implementada.** Contrasta el contrato con las clases de `e730e9c` y la integración de `jasub/final-a` y `feature/c2-Ethian`. Los hashes exactos del candidato comprobado están en el [manifiesto integrado](../../recuperacion-c2/evidencias/integracion/manifest.json).

Respecto al [cierre del Corte 2](04-componentes-corte2-cierre.md) cambia lo siguiente: `GameInput` solo traduce dispositivos, la coordinación del jugador pasa a aplicación y `GameSession` recibe datos en lugar de un callback.

```mermaid
flowchart LR
    subgraph boot["bootstrap"]
        main["Minecraft2Application"]
        gpu["GpuPreference<br/>WorldDirectory"]
    end

    subgraph pres["presentation.game (LibGDX)"]
        gui["GameWindow → GraphicalGame"]
        voxel["VoxelGame<br/><i>loop gráfico, cámara, mallas</i>"]
        input["GameInput<br/><i>solo dispositivos → datos</i>"]
        hud["GameHud<br/>SprintCameraEffect"]
        zr["ZombieRenderer<br/>PistolRenderer"]
    end

    subgraph app["application"]
        frame["PlayerFrameInput<br/><i>dato inmutable</i>"]
        session["GameSession<br/><i>advance(delta, PlayerFrameInput)<br/>controls()</i>"]
        control["PlayerControlService<br/><i>movimiento, física, colisión,<br/>estamina, interacción</i>"]
        cstate["PlayerControlState<br/><i>consulta para HUD/cámara</i>"]
        enemies["EnemyUpdateService"]
        horde["HordeManager"]
        pistol["PistolService"]
        melee["ZombieMeleeService<br/>ZombieTargeting"]
        inter["PlayerInteractionService"]
        wsvc["WorldApplicationService"]
    end

    subgraph dom["domain (sin cambios)"]
        enemy["domain.enemy"]
        player["domain.player"]
        world["domain.world"]
    end

    subgraph pers["persistence (sin cambios)"]
        storage["«interface» WorldStorage"]
        json["JsonWorldStorage"]
    end

    main --> gpu & json & wsvc & gui
    gui --> voxel & wsvc
    gui -.-> gpu
    voxel --> session & input & hud & zr & world
    input -- "produce" --> frame
    voxel -- "advance(delta, frame)" --> session
    hud -- "consulta controls()" --> session
    hud -- "usa instantánea" --> cstate
    session --> control & enemies & horde & pistol
    session -- "controls()" --> cstate
    control --> player & inter & melee & pistol & enemies & world
    frame --> player
    session --> player & world
    horde --> enemies
    enemies --> enemy & world & player
    wsvc --> storage & world
    json -. implementa .-> storage
    json --> world

    classDef d fill:#dbe9ff,stroke:#1168bd,color:#000
    classDef a fill:#d9f2d9,stroke:#2e7d32,color:#000
    classDef p fill:#ffe5cc,stroke:#e67e22,color:#000
    classDef g fill:#eeeeee,stroke:#777,color:#000
    class enemy,player,world d
    class frame,session,control,cstate,enemies,horde,pistol,melee,inter,wsvc a
    class gui,voxel,input,hud,zr p
    class main,gpu,storage,json g

```

El combate lo coordina PlayerControlService: mirada → movimiento/física/colisión/estamina → arma → material → acción primaria/secundaria. GameSession conserva después la secuencia vida → hordas → enemigos → pistola mientras el jugador sigue vivo. HUD y cámara reciben PlayerControlState mediante controls().

## Secuencia de un tick (implementada)

```mermaid
sequenceDiagram
    participant V as VoxelGame.render (presentación)
    participant I as GameInput (presentación)
    participant S as GameSession (aplicación)
    participant C as PlayerControlService (aplicación)
    participant D as Movimiento / Física / Colisión / Stamina (dominio)
    participant H as HordeManager
    participant E as EnemyUpdateService

    V->>I: leer dispositivos
    I-->>V: PlayerFrameInput (o neutro)
    V->>S: advance(delta, frame)
    alt RUNNING, delta finito > 0
        S->>S: delta = min(delta, 0.05)
        S->>C: update(delta, frame)
        C->>D: mirar, mover, gravedad, colisión, gastar estamina
        C->>C: alternar arma, seleccionar material, combate/interacción
        S->>S: life.update/advance
        opt El jugador sigue vivo tras actualizar vida
            S->>H: update(delta, x, z)
            S->>E: update(life, delta)
            opt El jugador sigue vivo tras actualizar enemigos
                S->>S: pistol.update(wave, delta)
            end
        end
    else PAUSED, DEAD o delta inválido
        S-->>V: no avanza nada (ni entrada, ni física, ni IA)
    end
    V->>S: controls()
    S-->>V: PlayerControlState (energía, sprint, agotamiento)
```

## Reglas de dependencia comprobadas

Las reglas 1 a 3 y la firma de avance del ADR-001 §5 se comprueban en `test/architecture/ArchitectureBoundaryTest.java`. GameSessionTest y el smoke OpenGL comprueban el avance y la conexión visual. La revisión de las llamadas completa la comprobación de la ruta normal. Siguen como límites declarados `GraphicalGame` → `bootstrap.GpuPreference`, `application` → `persistence.WorldStorage` y el ciclo `domain.world` ↔ `domain.player`.

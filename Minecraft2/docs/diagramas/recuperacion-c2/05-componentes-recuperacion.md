# C4 nivel 3 — Componentes de la recuperación (BORRADOR)

> **Borrador según el contrato**, todavía sin verificar contra el código. Se basa en la API propuesta en `plan-equipo.md` §3 y en [ADR-001](../../adr/ADR-001-estilo-recuperacion.md) §5. Cuando se integre `feature/c2-Thomas` (REC-T1/T2), Ethian actualizará este diagrama con los `import` reales y anotará aquí el commit. Hasta entonces **no describe código existente**.

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
    voxel --> session & input & hud & zr
    input -- "produce" --> frame
    voxel -- "advance(delta, frame)" --> session
    hud -- "lee controls()" --> cstate
    session --> control & enemies & horde & pistol
    session -- "controls()" --> cstate
    control --> player & inter & melee & pistol
    horde --> enemies
    enemies --> enemy & world & player
    wsvc --> storage
    json -. implementa .-> storage

    classDef d fill:#dbe9ff,stroke:#1168bd,color:#000
    classDef a fill:#d9f2d9,stroke:#2e7d32,color:#000
    classDef p fill:#ffe5cc,stroke:#e67e22,color:#000
    classDef g fill:#eeeeee,stroke:#777,color:#000
    class enemy,player,world d
    class frame,session,control,cstate,enemies,horde,pistol,melee,inter,wsvc a
    class gui,voxel,input,hud,zr p
    class main,gpu,storage,json g
    linkStyle 6 stroke:#c62828,stroke-width:2px,stroke-dasharray:6 4
```

**Pendiente de confirmar al integrar.** No sabemos todavía si el combate (pistola y puños) lo coordina `PlayerControlService` o se queda en otra ruta de aplicación. El plan habla de «acción primaria/secundaria» y «alternancia de pistola» en `PlayerFrameInput`. Las flechas `control → melee/pistol` reflejan esa intención y se corregirán según el código real.

## Secuencia de un tick (después, según el contrato)

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
        S->>C: apply(frame, delta)
        C->>D: mover, gravedad, colisión, gastar estamina según desplazamiento real
        S->>S: life.update/advance
        S->>H: update(delta, x, z)
        S->>E: update(life, delta)
    else PAUSED, DEAD o delta inválido
        S-->>V: no avanza nada (ni entrada, ni física, ni IA)
    end
    V->>S: controls()
    S-->>V: PlayerControlState (energía, sprint, agotamiento)
```

## Reglas de dependencia objetivo

Son las reglas 1 a 5 de ADR-001 §5. Las verifica automáticamente `test/architecture/ArchitectureBoundaryTest.java` (REC-J6, de Jasub). Siguen como límites declarados, en rojo, `GraphicalGame` → `bootstrap.GpuPreference`, `application` → `persistence.WorldStorage` y el ciclo `domain.world` ↔ `domain.player`.

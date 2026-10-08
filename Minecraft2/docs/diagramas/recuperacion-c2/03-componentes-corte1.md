# C4 nivel 3 — Componentes del Corte 1 (histórica)

> **Vista histórica.** Corresponde al commit `5501b02` (24-08-2026), el último del Corte 1. No describe el código actual.

Contenedor: aplicación de escritorio en la JVM. El menú era **solo de consola**. Elegir «jugar» abría la ventana LibGDX.

```mermaid
flowchart LR
    subgraph boot["bootstrap"]
        main["Minecraft2Application<br/><i>composición</i>"]
    end

    subgraph pres["presentation"]
        menu["MainMenu + ConsoleIO<br/><i>menú de consola</i>"]
        win["GameWindow<br/><i>Lwjgl3Application</i>"]
        voxel["VoxelGame<br/><i>loop, cámara, Observer</i>"]
        input["GameInput<br/><i>teclado/ratón</i>"]
        mesh["ChunkMeshBuilder<br/>+ BlockTextureAtlas"]
    end

    subgraph app["application"]
        wsvc["WorldApplicationService<br/><i>CRUD de mundos</i>"]
        inter["PlayerInteractionService<br/><i>romper/colocar</i>"]
        gen["ChunkGenerationService"]
    end

    subgraph dom["domain"]
        world["World / Chunk / Block<br/><i>Subject (Observer)</i>"]
        pmove["PlayerMovementService<br/>PlayerPhysics<br/>CollisionResolver"]
        terrain["SimpleTerrainGenerator"]
    end

    subgraph pers["persistence"]
        storage["«interface» WorldStorage"]
        json["JsonWorldStorage<br/>+ WorldJsonCodec"]
    end

    subgraph pat["patterns"]
        factory["BlockFactory"]
        singleton["WorldManager<br/><i>Singleton</i>"]
    end

    main --> menu & win & wsvc & json & factory
    menu --> wsvc & win
    win --> voxel
    voxel --> input & mesh & world
    input --> inter
    input == "construye y coordina<br/>física/movimiento" ==> pmove
    wsvc --> storage & gen & singleton & factory & world
    gen --> terrain & factory
    inter --> world & factory
    json -. implementa .-> storage
    json --> factory & world

    classDef d fill:#dbe9ff,stroke:#1168bd,color:#000
    classDef a fill:#d9f2d9,stroke:#2e7d32,color:#000
    classDef p fill:#ffe5cc,stroke:#e67e22,color:#000
    classDef g fill:#eeeeee,stroke:#777,color:#000
    class world,pmove,terrain d
    class wsvc,inter,gen a
    class menu,win,voxel,input,mesh p
    class main,storage,json,factory,singleton g
    linkStyle 12 stroke:#c62828,stroke-width:2px,stroke-dasharray:6 4
```

## Reglas de dependencia observadas en `5501b02`

| Regla | Estado en el Corte 1 |
| --- | --- |
| `domain` sin LibGDX ni capas superiores | Se cumple. El único acoplamiento es `domain.world` → `patterns.observer`. |
| `application` sin LibGDX | Se cumple. Depende de `persistence.WorldStorage`, que es una interfaz. |
| Presentación sin coordinación de reglas | **No se cumple.** `GameInput` crea `PlayerMovementService`, `PlayerPhysics` y `CollisionResolver` y avanza la física cada fotograma (flecha roja). |

Todavía no existían enemigos, sesión de juego, estamina ni menú gráfico. Esta es la base sobre la que el Corte 2 añadió el reto del enemigo.

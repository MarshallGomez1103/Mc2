# C4 nivel 1 — Contexto

Versión: `4220c40`. Es la vista actual y la recuperación no la cambia.

```mermaid
flowchart TB
    jugador["👤 Jugador<br/><i>Persona</i><br/>Explora, construye, combate<br/>zombis y guarda mundos"]

    subgraph limite["Límite del sistema"]
        mc2["<b>Minecraft2</b><br/><i>Sistema de software</i><br/>Juego voxel de escritorio con<br/>mundos finitos, oleadas de zombis<br/>y guardado local"]
    end

    so["🖥️ Sistema operativo + driver gráfico<br/><i>Sistema externo</i><br/>Ventana, teclado/ratón,<br/>OpenGL 3.x, GPU"]
    fs["📁 Sistema de archivos local<br/><i>Sistema externo</i><br/>Carpeta de mundos"]

    jugador -- "Juega con teclado y ratón;<br/>ve la escena 3D y el HUD" --> mc2
    jugador -. "Modo consola opcional<br/>(-Dmc2.console=true)" .-> mc2
    mc2 -- "Abre la ventana, lee la entrada,<br/>dibuja con OpenGL" --> so
    mc2 -- "Lee y escribe JSON v1<br/>y graphics.properties" --> fs

    classDef persona fill:#08427b,color:#fff,stroke:#052e56
    classDef sistema fill:#1168bd,color:#fff,stroke:#0b4884
    classDef externo fill:#999,color:#fff,stroke:#6b6b6b
    class jugador persona
    class mc2 sistema
    class so,fs externo
```

## Notas

- Hay un solo actor humano. No hay multijugador, red ni servicios remotos: todo ocurre en la máquina del jugador.
- El **reto oral**, incorporar un enemigo, vive por completo dentro del sistema. Los zombis no son un sistema externo: son modelo del dominio (`domain.enemy`) que coordina la aplicación (`EnemyUpdateService` y `HordeManager`).
- La carpeta de mundos se resuelve con `bootstrap.WorldDirectory`, por defecto junto al proyecto o al JAR, y se puede cambiar con `-Dmc2.worlds.dir`. La preferencia de GPU se guarda en `graphics.properties`, junto a esa carpeta y fuera del JSON.

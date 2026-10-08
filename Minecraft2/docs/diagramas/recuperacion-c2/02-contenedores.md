# C4 nivel 2 — Contenedores

Versión: `4220c40`. Es la vista actual y la recuperación no la cambia.

Un contenedor en C4 es algo que se ejecuta o que guarda datos. Minecraft2 tiene **un solo proceso**: no hay Docker, servidor ni base de datos.

```mermaid
flowchart TB
    jugador["👤 Jugador"]

    subgraph maquina["Equipo del jugador"]
        subgraph mc2["Minecraft2 (límite del sistema)"]
            app["<b>Aplicación de escritorio</b><br/><i>Contenedor: proceso JVM 17</i><br/>JAR con dependencias (LibGDX 1.12.1, LWJGL3)<br/>Menú gráfico o de consola, simulación<br/>del juego, render y guardado"]
            worlds[("<b>Carpeta de mundos</b><br/><i>Contenedor: archivos</i><br/>worlds/*.json (formato v1)")]
            prefs[("<b>graphics.properties</b><br/><i>Contenedor: archivo</i><br/>Preferencia de GPU")]
        end
        gl["Driver OpenGL / GPU<br/><i>Externo</i>"]
        stdio["Terminal: stdin/stdout<br/><i>Externo, solo modo consola</i>"]
    end

    jugador -- "Teclado/ratón" --> app
    jugador -.-> stdio
    stdio -. "Opciones del menú<br/>-Dmc2.console=true" .-> app
    app -- "Llamadas OpenGL vía LWJGL3" --> gl
    app -- "JsonWorldStorage:<br/>escritura temporal + movimiento atómico" --> worlds
    app -- "GpuPreference<br/>(antes de crear el contexto)" --> prefs

    classDef cont fill:#438dd5,color:#fff,stroke:#2e6295
    classDef ext fill:#999,color:#fff,stroke:#6b6b6b
    class app,worlds,prefs cont
    class gl,stdio,jugador ext
```

## Notas

- La IA de enemigos, las oleadas y la física se ejecutan **en el hilo de render** de LibGDX, dentro de `VoxelGame.render`, una vez por fotograma. Por eso su coste por tick compite con el dibujo. Ver [carga.md](../../recuperacion-c2/carga.md).
- El modo consola (`-Dmc2.console=true`) usa la misma aplicación y el mismo `WorldApplicationService`. Es la interfaz pública que ejercita la prueba de caja negra de Jasub (REC-J4).
- `-Dmc2.gpu=AUTO|INTEGRATED|DEDICATED` y la preferencia guardada se aplican antes de crear el contexto OpenGL. No hay cambio de GPU en caliente.

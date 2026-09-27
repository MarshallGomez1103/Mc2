# UI integrada, fullscreen y diagnóstico gráfico

> Evidencia de la primera integración. Los ajustes posteriores y su validación están en
> [feedback-20260926.md](feedback-20260926.md); ese documento y el README describen el estado actual.

## Implementado

- Arranque normal `bootstrap.Minecraft2Application` abre una ventana LibGDX con menú principal y mundos. Consola histórica opcional `-Dmc2.console=true`.
- CRUD se delega a `WorldApplicationService`: listado de archivos, crear por tamaño, cargar y eliminar con confirmación. Ruta absoluta de guardados y excepciones visibles.
- ESC pausa; menú ofrece continuar, guardar, opciones y salir. Salida de partida, incluido botón X, requiere Guardar y salir / Salir sin guardar / Cancelar. No hay autosave.
- Si create/resize de una partida falla, se libera su estado parcial y se devuelve control al menú con el error; el mundo cargado permanece en el servicio sin guardarlo automáticamente.
- Stage recibe input mientras está pausado. Continuar difiere la reanudación un frame y VoxelGame también protege el primer input para consumir el clic de UI. Focus perdido pausa.
- Opciones de enemigos/texturas del enemigo se aplican por `applySettings`. Dificultad y textura de bloques muestran explícitamente próxima partida.
- F alterna fullscreen/ventana en el monitor actual mediante `FullscreenController`, restaura tamaño anterior y cursor. Scene2D usa viewport y scroll con ancho adaptable; cámara/HUD se delegan a resize de VoxelGame.
- Tras crear contexto se registran GL_VENDOR, GL_RENDERER, GL_VERSION y backend. Pantalla Gráficos muestra el renderer efectivo.
- AUTO/DEDICATED se guarda en `graphics.properties` junto a `worlds/`, fuera de los JSON. Solo Linux con múltiples dispositivos DRM permite solicitar DEDICATED. Se aplica en próximo arranque, nunca en caliente.
- En siguiente arranque DEDICATED relanza una sola JVM conservando flags y classpath; NVIDIA solicita PRIME, otros drivers Mesa DRI_PRIME. El driver selecciona el dispositivo: confirmar GL_RENDERER. Un error de arranque intenta AUTO sin bucle. No se instalan drivers.

## Probado automáticamente

`mvn -q -Dtest=FullscreenControllerTest,GpuPreferenceTest,GameWindowTextureModeTest test`: 8 casos, 0 fallos.

- Ida/vuelta conserva tamaño ventana incluso tras tamaño fullscreen distinto.
- Cambio no soportado y fallo fullscreen.
- Preferencia almacenada fuera del directorio JSON y leída para próximo inicio.
- Valor desconocido cae a AUTO.
- Entornos PRIME NVIDIA y Mesa separados, preservando otras variables.
- Compatibilidad con toggle de texturas de GameWindow legado.

Estos tests usan proxy Graphics sin contexto OpenGL. No demuestran resultado físico del monitor ni el driver.

## Pruebas gráficas

Integración y smoke final se registran por el agente coordinador. `-Dmc2.screenshot=/ruta.png` captura menú y termina; `-Dmc2.play.world=id` permite cargar directamente un mundo para smoke de sesión. Las propiedades están desactivadas en arranque normal.

## Limitaciones

- CRUD/generación/guardado síncronos en hilo gráfico; mundos grandes pueden congelar temporalmente UI y requieren memoria suficiente. No se añadió threading sin medir.
- DEDICATED es solicitud al driver, no garantía; GPUs AMD físicas y otros sistemas operativos no probados aquí. En Windows/macOS seleccionar GPU desde sistema/driver, manteniendo AUTO en menú.
- Opciones generales duran sesión, excepto preferencia gráfica externa.
- Cargar archivo inválido presenta excepción y carpeta; el archivo no se borra ni se interpreta como lista vacía.

## Fuentes técnicas oficiales

- [LibGDX configuración/display modes](https://libgdx.com/wiki/graphics/querying-and-configuring-graphics)
- [Mesa environment variables: DRI_PRIME](https://docs.mesa3d.org/envvars.html)
- [NVIDIA Optimus y múltiples GPUs](https://docs.nvidia.com/datacenter/tesla/driver-installation-guide/optimus-laptops-and-multi-gpu-desktop-systems.html)

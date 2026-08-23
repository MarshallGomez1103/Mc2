# CT-11 — Flujo MVP: crear → guardar → cerrar → cargar → eliminar

**Criterio de terminado:** crear → generar → guardar → cerrar → cargar conserva los cambios de
colocar y eliminar bloques (`docs/decisiones-compartidas.md`, tabla de la sección 4).

Este criterio se comprueba por dos caminos que se complementan.

## 1. Comprobación automática

`test/application/WorldLifecycleTest.java` ejecuta el ciclo entero por código, que es la única
manera de cubrir la parte de **colocar y eliminar bloques**: el menú de consola todavía no ofrece
esas acciones, porque llegan con la integración gráfica (teclado y ratón).

La prueba hace, sobre una carpeta temporal:

1. crear el mundo, que genera 4 chunks de terreno;
2. colocar un bloque de `WOOD` en el aire, en `(3, 40, 3)`;
3. eliminar un bloque del terreno generado;
4. guardar y descargar el mundo del `WorldManager`;
5. cargar de nuevo y comprobar que **el bloque colocado sigue ahí**, que **el eliminado no
   reaparece**, y que la semilla y la fecha de creación sobrevivieron;
6. eliminar el mundo y comprobar que desaparece del listado y deja de estar cargado.

Se ejecuta con el resto de la batería:

```bash
mvn clean package
```

## 2. Comprobación manual guiada

Cubre el flujo tal como lo vive el usuario desde el menú, incluida la confirmación de borrado.

### Cómo reproducirla

```bash
mvn clean package
java -Dfile.encoding=UTF-8 -cp target/classes bootstrap.Minecraft2Application
```

Y elegir en orden: `1` (crear `mundo_demo`) → `4` (guardar) → `5` y responder `n` →
`2` (listar) → `3` (cargar) → `5` y responder `s` → `2` (listar) → `0` (salir).

### Transcripción obtenida

Salida real, con las cabeceras y el menú recortados para que se lean solo los resultados:

```text
  Crear mundo
Nombre del mundo: Mundo creado y cargado: mundo_demo

  Guardar mundo actual
Mundo actual guardado: mundo_demo

  Eliminar mundo
Nombre del mundo: Se eliminará "mundo_demo" de forma permanente. ¿Continuar? [s/N]: n
Operación cancelada. No se eliminó ningún mundo.

  Mundos guardados
  - mundo_demo
Total: 1

  Cargar mundo
Nombre del mundo: Mundo cargado: mundo_demo

  Eliminar mundo
Nombre del mundo: Se eliminará "mundo_demo" de forma permanente. ¿Continuar? [s/N]: s
Mundo eliminado: mundo_demo

  Mundos guardados
No hay mundos guardados.

Hasta luego.
```

### Qué demuestra

- El mundo se crea con terreno y se guarda en `worlds/mundo_demo.json`, de alrededor de 1,1 MB.
- **Cancelar la confirmación no borra nada:** tras responder `n`, el listado sigue mostrando el
  mundo.
- El mundo se carga de nuevo en una operación posterior sin pérdida.
- Confirmar con `s` sí elimina, y el listado queda vacío.
- Al final la carpeta `worlds/` no conserva ningún archivo.

## Nota sobre acentos en Windows

Sin `-Dfile.encoding=UTF-8`, Java 17 escribe en la codificación de la plataforma (`Cp1252` en este
equipo) y la terminal muestra `Opci�n` en vez de `Opción`. No es un fallo del programa; el flag lo
resuelve. En IntelliJ no hace falta, porque la consola ya usa UTF-8.

## Entorno

- Windows 11, JDK 17.0.10 (Microsoft Build of OpenJDK), Apache Maven 3.9.9.
- Entrada enviada por tubería desde Git Bash. Desde PowerShell, la tubería antepone un BOM a la
  primera línea y el menú la rechaza como opción no válida; conviene escribir las opciones a mano o
  usar Git Bash.

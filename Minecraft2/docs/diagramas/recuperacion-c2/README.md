# Diagramas de la recuperación del Corte 2

Corrección REC-03 del [plan de recuperación](../../recuperacion-c2/plan-equipo.md). La decisión que explican está en [ADR-001](../../adr/ADR-001-estilo-recuperacion.md).

Los diagramas están en Mermaid y GitHub los renderiza directamente. Siguen los niveles de C4 (contexto, contenedor y componente). En C4, un **contenedor** es una unidad que se ejecuta o almacena datos: aquí es la aplicación de escritorio en la JVM o la carpeta de mundos. **No** implica Docker ni microservicios.

| Vista | Archivo | Versión del código | Vigencia |
| --- | --- | --- | --- |
| Contexto | [01-contexto.md](01-contexto.md) | `4220c40` | Actual; no cambia con la recuperación |
| Contenedores | [02-contenedores.md](02-contenedores.md) | `4220c40` | Actual; no cambia con la recuperación |
| Componentes, Corte 1 | [03-componentes-corte1.md](03-componentes-corte1.md) | `5501b02` (24-08-2026) | **Histórica** |
| Componentes, cierre del Corte 2 | [04-componentes-corte2-cierre.md](04-componentes-corte2-cierre.md) | `4d33d6a` (mismo código fuente que `4220c40`) | **Histórica**: estado previo a la recuperación |
| Componentes, recuperación | [05-componentes-recuperacion.md](05-componentes-recuperacion.md) | Contrato de `plan-equipo.md` §3 | **Borrador**: se actualiza con el código integrado (REC-T1/T2) |

## Cómo se obtuvieron

Las dependencias salen de los `import` reales de `Minecraft2/src` en cada commit (`git show <commit>:<ruta>`) y de las llamadas `new`/`advance` en la raíz de composición y en `VoxelGame`. No se dibujan dependencias que el código no tenga. Las flechas se leen como «usa» o «depende de», en la dirección del `import`.

Convenciones de color en las vistas de componentes:

- Azul: dominio, sin LibGDX.
- Verde: aplicación, casos de uso y coordinación.
- Naranja: presentación, LibGDX.
- Gris: persistencia y bootstrap.
- Línea roja discontinua: dependencia que viola, o violaba, una regla de la decisión.

# REC-07 — clasificación y evidencia integrada

Las cifras vigentes pertenecen al [candidato integrado](../../pruebas.md): 198 casos Surefire y 148 Failsafe, sin fallos, errores ni omitidas. El [inventario](../../recuperacion-c2/evidencias/integracion/clasificacion.csv) clasifica 68 clases sin duplicación.

Jasub renombró EnemyUpdateServiceTest, LocalWorldPersistenceTest, SaveEdgeCasesTest y WorldLifecycleTest a IT. Se conservaron sus 18 casos y aserciones. Thomas ya los seleccionaba por contenido en Failsafe; el merge adaptó los patrones a los nombres nuevos. No se suman 18 pruebas nuevas por cambiar su nombre.

Otras clases que terminan en Test también conectan componentes/archivos reales y siguen en Failsafe mediante la selección explícita del POM. Surefire incluye además adaptadores/métricas aislados; no todos son reglas de negocio. La cobertura solicitada se limita a domain y se informa por nivel en el reporte canónico.

El aporte inicial de Jasub tenía un total distinto sobre su rama anterior; no se usa como conteo de esta integración. Las equivalencias y límites se verifican en FSM/A*, navegación, oleadas, controles y tiempos. El caso aislado de error usa un stub de WorldStorage; los archivos y procesos reales se ejecutan en integración.

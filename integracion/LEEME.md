# Integración del servidor de Jasub con la carga de Ethian

El servidor inicial implementa el arranque `-Dmc2.server=true`, `/health`, `/metrics` y protocolo v1 con `type` y `ack` numérico, confirmado tras aplicar cada entrada. Capacidad actual de B: un jugador. Después de C se verifica el escenario oficial de tres clientes.

## Calentamiento observado

`MC2_LOAD_WARMUP_SECONDS` en el consumidor actual limita cuánto se espera para alcanzar población; no asegura una duración mínima de calentamiento si los 80 zombis ya están presentes. `CLIENT_WARMUP` añade solo dos segundos con los clientes conectados. En este equipo hubo fallos en frío y mejores resultados posteriores; el perfil mostró trabajo de rutas y pausas GC. Esto no demuestra una causa única de todos los picos.

Se conserva una propuesta independiente en `ethian-calentamiento.patch`, basada en `c3/ethian` `fb28706`. **No está aplicada en el código de carga del repositorio de Jasub ni se declara probada en Actions.** La incorpora/revisa Ethian en sus archivos, conforme al reparto.

El parche agrega `MC2_LOAD_CLIENT_WARMUP_SECONDS` (30 s por defecto) a Settings, espera ese tiempo con clientes enviando entradas antes de comenzar la ventana medida y anota el valor en el manifiesto. Mantiene la espera de población por separado, los umbrales existentes y la medición de todas las repeticiones. Las IT contra dobles conservan su calentamiento de 2 s para no convertirlas en benchmarks lentos. Incluye dos pruebas de configuración. El parche pasó `git apply --check` y esas dos pruebas se ejecutaron en el worktree aislado con JDK 17 (log `27-propuesta-calentamiento-test.log`); esto no verifica todavía toda la suite de Ethian, su pipeline ni el SLO con el parche aplicado.

Para revisarlo desde la raíz de una copia de `c3/ethian`:

```bash
git apply --check integracion/ethian-calentamiento.patch
```

Para TDD, incorporar primero `ClientWarmupTest`, comprobar su fallo, y después el resto. Después verificar las pruebas de carga, suite completa, carga contra el despliegue en frío y artefactos de Actions. Revisar usos adicionales del constructor Settings si la rama ha cambiado. Este parche requiere revalidación sobre la versión integrada; no sustituye la prueba final de SLO ni garantiza por sí solo cumplirlo.

## Evidencia y publicación

Los resultados locales de B se resumen en `docs/servidor.md` y en el informe personal `FASE_B_ESTADO.md`. Las ejecuciones fallidas se conservan. La comprobación con calentamiento explícito es una medición de un cliente en un servidor ya calentado; no debe presentarse como resultado de tres jugadores ni como gate de CD en un despliegue frío.

Los workflows, Dockerfiles y scripts de Ethian permanecen bajo su responsabilidad. El fragmento de ZAP se entrega en la fase F; este archivo no lo anticipa.

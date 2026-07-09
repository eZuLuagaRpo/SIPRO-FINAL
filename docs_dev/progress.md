# Progress Log

- 2026-05-20: Fix crítico de constraint DB — `uq_planilla_activa_fecha_archivo` eliminada (bloqueaba Full IFRS con mismo nombre de archivo que Seg 1). Nueva constraint `uq_planilla_activa_por_producto (fecha_corte, id_producto) WHERE activo=true` creada en `PartitionInitializer.java` de forma idempotente.
- 2026-05-20: Eliminado el bloque de validación de archivo control `.txt` de `FileValidationService.java`. La validación pasa a ser solo responsabilidad del frontend (visual/bloqueante en UI, transparente en backend).
- 2026-05-20: Ajuste de espaciado CSS en `cargar.component.scss` — márgenes y padding del formulario de carga compactados, gap entre hint de nombre y uploaders IFRS añadido.

- 2026-05-06: Se revisaron AGENTS.md, CONTEXT.md, MEMORY.md, el flujo real de consolidación y la maqueta [preview.html](c:/Users/junortiz/Grupo%20Bancolombia/Alexander%20Group%20-%20General/33.%20GCIA%20SERVICIOS%20FINANCIEROS%20IFRS9/SIPRO/Pantallas/Pagina%20de%20Administrador/M%C3%B3vil/preview.html).
- 2026-05-06: Se definió el plan técnico del panel admin: backend con endpoints nuevos para dashboard/SQL/logs y frontend con nueva ruta Angular, retirando la consolidación temporal de Inicio.

- 2026-04-06: Se revisaron skills aplicables, AGENTS.md, DETALLES_PROYECTO.md y memorias del repositorio.
- 2026-04-06: Se identificó que la brecha principal está en ConsolidacionResumenService y ResumenComponent.
- 2026-04-06: Se confirmó soporte existente de SXSSF para exportar y SAX para lectura de XLSX grandes.
- 2026-04-13: Se endureció la validación estructural de planillas para fallar ante cualquier desalineación de encabezado respecto a data_validation_rule, incluyendo orden incorrecto y columnas extra.
- 2026-04-13: Se actualizaron pruebas unitarias de DynamicExcelValidationService para cubrir fail-fast estructural y se ejecutó la suite completa :services:validation-service:test con resultado OK.
- 2026-04-13: El frontend Angular compiló correctamente tras dejar Descripción como opcional y publicar reglas visibles del esquema en la pantalla de carga.

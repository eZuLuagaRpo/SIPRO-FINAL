# Task: Comparacion CREFFSOS masiva y rediseño de resumen

## Sesión 2026-05-06: Panel de administrador funcional

### Objetivo
Llevar la maqueta de panel de administrador al monorepo SIPRO, moviendo la consolidación manual fuera de Inicio y agregando una pantalla Angular real con datos backend para consolidación, consola SQL restringida y monitoreo de logs en tiempo real.

### Fases
- [x] Fase 1: Investigación del flujo actual de consolidación, autenticación y maqueta preview.html
- [x] Fase 2: Diseño técnico del contrato admin backend/frontend
- [ ] Fase 3: Implementación backend admin (dashboard, SQL, logs, autorización)
- [ ] Fase 4: Implementación frontend Angular del panel admin
- [ ] Fase 5: Retiro del bloque temporal de consolidación desde Inicio
- [ ] Fase 6: Validación con build/test

### Decisiones
| Decisión | Rationale | Fecha |
|----------|-----------|------|
| Reusar el flujo existente de consolidación manual y extenderlo para persistir observación desde el panel admin | Evita reescribir la lógica crítica del consolidado y reduce riesgo funcional | 2026-05-06 |
| Proteger el panel admin con autenticación Entra ya existente y validación RBAC/legado en backend | El backend ya autentica bearer tokens; no hace falta inventar otro mecanismo | 2026-05-06 |
| Restringir la consola SQL a tablas maestras/métricas y a operaciones SELECT/UPDATE/INSERT | Cumple el caso operativo sin abrir DDL/DROP/TRUNCATE desde el panel | 2026-05-06 |
| Implementar logs en tiempo real con buffer en memoria y polling HTTP | El proyecto no persiste logs a archivo hoy; el buffer evita tocar infraestructura externa | 2026-05-06 |

## Objetivo
Implementar comparación operativa entre PostgreSQL (sipro_detalle_consolidado_registros) y el archivo CREFFSOS generado por periodo, soportando hasta ~900k filas en XLSX con lectura streaming, y exponer el resultado en la pantalla de resumen.

## Fases
- [x] Fase 1: Investigación de consolidación y resumen actual
- [ ] Fase 2: Diseño técnico de comparación masiva y DTOs
- [ ] Fase 3: Implementación backend
- [ ] Fase 4: Implementación frontend
- [ ] Fase 5: Validación con pruebas y build

## Decisiones
| Decisión | Rationale | Fecha |
|----------|-----------|------|
| Comparar por agregados por producto y total del periodo, no fila a fila | Reduce memoria y tiempo; responde al requerimiento operativo de diferencias en cantidad y valor | 2026-04-06 |
| Leer CREFFSOS en streaming SAX | Ya existe utilidad XlsxStreamingReader y el repositorio documenta que POI DOM no escala | 2026-04-06 |
| Resolver archivo CREFFSOS desde storage key por periodo y fallback a ruta compartida | La generación actual publica en ambos destinos y el storage key ya incluye fecha de corte | 2026-04-06 |

## Errores Encontrados
| Error | Attempt | Resolution |
|-------|---------|------------|
| Pendiente | - | - |

## Nota 2026-05-20

- [x] Fix crítico de constraint `uq_planilla_activa_fecha_archivo` — reemplazada por `uq_planilla_activa_por_producto (fecha_corte, id_producto) WHERE activo=true` en `PartitionInitializer.java`
- [x] Eliminación de validación backend del archivo control `.txt` para Full IFRS — responsabilidad solo del frontend
- [x] Ajuste de espaciado CSS en `cargar.component.scss` — formulario de carga compactado

## Nota 2026-04-13

- [x] Hardening incremental de validación de planillas basado en data_validation_rule
- Alcance:
	- fail-fast cuando el encabezado no coincide exactamente en orden o cantidad de columnas
	- CLASIFICACION mantiene semántica opcional con vacío/0 permitido y valores informados entre 1 y 4
	- DOCUMENTO solo puede repetirse si cambia MONEDA; colisión de la llave compuesta se trata como error de negocio
	- la pantalla de carga deja de exigir Descripción y ahora informa explícitamente las reglas estructurales del archivo

# Seguridad

Resumen de la postura de seguridad actual del repositorio SIPRO y de los pendientes de endurecimiento conocidos.

## Controles vigentes

- Credenciales y secretos reales no deben almacenarse en Git.
- El backend usa Spring Security, pero en el perfil de desarrollo la configuracion funcional actual esta en modo permitAll para no bloquear los flujos locales.
- El login operativo se resuelve contra PostgreSQL y retorna permisos RBAC al frontend.
- El frontend protege navegacion con guards por autenticacion, carga y aprobacion.
- La sesion del frontend vive en sessionStorage y aplica timeout con extensiones controladas durante actividades largas.
- La conectividad a Impala usa truststore y debe manejarse con criterios de secreto por ambiente.

## Limitaciones actuales que deben asumirse como conocidas

- Los guards Angular no sustituyen autorizacion backend endurecida.
- No hay JWT corporativo ni resource server activo en este momento.
- SecurityConfig esta orientado a desarrollo local; cualquier fortalecimiento debe hacerse de forma incremental y compatible.

## Lineamientos obligatorios

- No commitear secretos, passwords, tokens, certificados privados ni llaves.
- No debilitar trazabilidad, auditoria ni segregacion de ambientes.
- No introducir cambios de seguridad que rompan de forma abrupta el login o los flujos operativos vigentes.
- Cualquier endurecimiento futuro debe preservar el desarrollo local y la transicion controlada entre ambientes.

## Recomendaciones operativas

- Usa gestores corporativos para secretos y certificados sensibles.
- Revisa el archivo backend/services/validation-service/src/main/resources/certificates/README.md antes de tocar truststores.
- Verifica permisos RBAC y filtros por lider asignado cuando hagas cambios en aprobacion.
- Ejecuta pruebas y build despues de cualquier ajuste de seguridad.

## Referencias

- [GOVERNANCE.md](GOVERNANCE.md)
- [AGENTS.md](AGENTS.md)
- [backend/services/validation-service/LOGIN_README.md](backend/services/validation-service/LOGIN_README.md)

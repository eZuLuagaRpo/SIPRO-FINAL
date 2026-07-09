# SIPRO - Sistema de Validacion de Productos

![Angular](https://img.shields.io/badge/Angular-20.0.0-red?logo=angular)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-green?logo=springboot)
![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.1-blue?logo=postgresql)

SIPRO es el monorepo del Sistema de Validacion de Productos de Bancolombia. Hoy integra un backend Spring Boot para validacion, aprobacion, consolidacion y conciliacion contra CREFFSOS, un frontend Angular con componentes standalone y los activos de infraestructura para empaquetado Docker y despliegue Helm.

## 📍 Mapa de evidencia rapida

- 📍 Java 17 de compilacion: [backend/services/validation-service/build.gradle](backend/services/validation-service/build.gradle) y [backend/gradle.properties](backend/gradle.properties)
- 📍 Spring Boot 3.4.0: [backend/services/validation-service/build.gradle](backend/services/validation-service/build.gradle)
- 📍 Gradle wrapper del repo: [backend/gradle/wrapper/gradle-wrapper.properties](backend/gradle/wrapper/gradle-wrapper.properties)
- 📍 PostgreSQL como base principal: [backend/services/validation-service/build.gradle](backend/services/validation-service/build.gradle) y [backend/services/validation-service/src/main/resources/application.yml](backend/services/validation-service/src/main/resources/application.yml)
- 📍 Angular 20, TypeScript 5.8.3 y RxJS 7.8.0: [frontend/package.json](frontend/package.json)
- 📍 Docker y runtime de contenedor: [infra/docker/validation-service/Dockerfile](infra/docker/validation-service/Dockerfile) y [infra/README.md](infra/README.md)
- 📍 Helm para despliegue: [infra/charts/validation-service/Chart.yaml](infra/charts/validation-service/Chart.yaml) y [infra/README.md](infra/README.md)
- 📍 S3, LocalStack e integracion LZ/Impala: [backend/services/validation-service/src/main/resources/application.yml](backend/services/validation-service/src/main/resources/application.yml)

## Estado actual

El sistema ya cubre estos flujos principales:

- Autenticacion contra PostgreSQL y consulta de permisos RBAC.
- Carga de archivos Excel con validacion sincrona o asincrona por job.
- Solicitud de aprobacion, consulta de pendientes, aprobacion, rechazo y descarga de adjuntos.
- Dashboard de inicio con pendientes de carga o aprobacion y consolidacion manual temporal por periodo.
- Tablero de Control por periodo con estados de cargue y aprobacion por producto en ambos segmentos (Colgaap/Modificado y Full IFRS).
- Resumen consolidado por periodo con comparacion entre PostgreSQL y CREFFSOS.
- Exportacion del resumen consolidado a Excel con formato profesional, boton "Exportar" en la pantalla Resumen y descarga con nombre conciliacion_planillas_manuales_YYYYMMDD.xlsx.
- Envio de planillas manuales a rutas compartidas
- Integracion con Landing Zone / Impala y almacenamiento local o S3 para archivos.

## Estructura del repo

```text
VSC_EUC00xxx_SIPRO/
├── backend/
│   ├── README.md
│   └── services/
│       └── validation-service/
├── frontend/
│   └── README.md
├── infra/
│   └── README.md
├── AGENTS.md
├── DETALLES_PROYECTO.md
├── GOVERNANCE.md
├── SECURITY.md
├── install-dependencies.bat
├── run-backend.bat
├── run-frontend.bat
└── run-fullstack.bat
```

## Requisitos previos

- Java 17.0.17 o compatible con el wrapper de Gradle.
- Node.js 20.20.0 o superior.
- npm 10.8.2 o superior.
- PostgreSQL local disponible para el perfil dev.
- Truststore de Impala si vas a usar integracion LZ real.
- LocalStack opcional para desarrollo con almacenamiento tipo S3.

## Arranque rapido

### Opcion 1: scripts del monorepo

```powershell
.\install-dependencies.bat
.\run-fullstack.bat
```

### Opcion 2: por separado

```powershell
# Backend
cd backend
.\gradlew.bat :services:validation-service:bootRun

# Frontend
cd frontend
npm install
npm start
```

## URLs utiles

- Frontend Angular: http://localhost:4200
- Login: http://localhost:4200/login
- Inicio: http://localhost:4200/inicio
- Tablero de control: http://localhost:4200/tablero
- Resumen consolidado: http://localhost:4200/resumen
- Backend API: http://localhost:8080/api
- Health backend: http://localhost:8080/api/health
- Actuator: http://localhost:8080/actuator/health

## Comandos utiles

### Backend

```powershell
cd backend
.\gradlew.bat :services:validation-service:compileJava
.\gradlew.bat :services:validation-service:test
.\gradlew.bat clean build
```

### Frontend

```powershell
cd frontend
npm start
npm run build
npm test
```

## Modulos funcionales

### Backend

- Autenticacion y permisos: login, consulta de permisos, health del modulo auth.
- Validacion: endpoints sync y async, polling por job y descarga de errores por lote.
- Planillas: solicitud de aprobacion, resumenes, pendientes y flujo de aprobacion/rechazo.
- Consolidacion: resumen mensual, comparacion CREFFSOS y consolidacion manual asincrona por periodo.
- Landing Zone: ingesta, verificacion de conexion y estado del proceso.

### Frontend

- Login con sesion en sessionStorage y timeout controlado.
- Inicio con cards de pendientes, calendario y consolidacion manual temporal.
- Cargar con validacion asincrona, reutilizacion de archivo validado y solicitud de aprobacion.
- Aprobacion con filtros por lider asignado y acciones de aprobar o rechazar.
- Tablero de Control con selector de anio/mes, tabla por producto y estados de proceso por segmento.
- Resumen con tablas de productos, CREFFSOS y diferencias reales de conciliacion.
- Exportacion del resumen consolidado a XLSX desde ResumenComponent, con parsing de Content-Disposition y descarga del archivo generado por backend.

## Documentacion del repo

- [backend/README.md](backend/README.md): guia del backend y endpoints actuales.
- [backend/services/validation-service/README.md](backend/services/validation-service/README.md): detalle del servicio principal.
- [backend/services/validation-service/INICIO_RAPIDO.md](backend/services/validation-service/INICIO_RAPIDO.md): arranque rapido del servicio y del front.
- [backend/services/validation-service/LOGIN_README.md](backend/services/validation-service/LOGIN_README.md): login, permisos y sesion.
- [frontend/README.md](frontend/README.md): rutas, guards y modulos Angular.
- [infra/README.md](infra/README.md): Docker y Helm.
- [DETALLES_PROYECTO.md](DETALLES_PROYECTO.md): contexto tecnico consolidado.
- [AGENTS.md](AGENTS.md): reglas operativas para agentes y politica de cambio incremental.
- [SECURITY.md](SECURITY.md): postura de seguridad actual y pendientes de endurecimiento.

## Notas operativas importantes

- El backend sigue en modo de seguridad relajada para desarrollo; los guards Angular y RBAC ayudan a la experiencia, pero no sustituyen el endurecimiento de API.
- La aprobacion operativa se resuelve por lider asignado en la planilla, no solo por RBAC de producto.
- La consolidacion manual temporal se ejecuta en segundo plano y expone estado por periodo.
- El resumen consolidado separa diferencias reales PostgreSQL vs CREFFSOS de observaciones internas de calidad.
- La exportacion del resumen consolidado se genera en backend con Apache POI para mantener el estilo visual (negrilla, gris 25% y bordes) igual a la vista.
- Por politica del repositorio, los cambios deben ser incrementales y no se deben crear nuevas tablas PostgreSQL salvo una instruccion explicita fuera de la politica actual.

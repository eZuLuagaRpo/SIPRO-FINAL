# Infraestructura SIPRO

Activos de infraestructura para empaquetado y despliegue del validation-service. Esta carpeta no es la via principal de desarrollo local; para trabajar dia a dia se usan los scripts del monorepo y bootRun/ng serve.

## 📍 Mapa de evidencia

- 📍 Docker y runtime Temurin 21: [docker/validation-service/Dockerfile](docker/validation-service/Dockerfile)
- 📍 Java 17 de compilacion del backend empaquetado: [../backend/services/validation-service/build.gradle](../backend/services/validation-service/build.gradle) y [../backend/gradle.properties](../backend/gradle.properties)
- 📍 Gradle wrapper del repo: [../backend/gradle/wrapper/gradle-wrapper.properties](../backend/gradle/wrapper/gradle-wrapper.properties)
- 📍 Helm chart de despliegue: [charts/validation-service/Chart.yaml](charts/validation-service/Chart.yaml)
- 📍 Datasource, storage y variables tecnicas: [../backend/services/validation-service/src/main/resources/application.yml](../backend/services/validation-service/src/main/resources/application.yml)

## Estructura

```text
infra/
├── docker/
│   └── validation-service/
│       └── Dockerfile
└── charts/
    └── validation-service/
        ├── Chart.yaml
        ├── values.yaml
        └── templates/
            ├── configmap.yaml
            ├── deployment.yaml
            ├── ingress.yaml
            ├── secret.yaml
            └── service.yaml
```

### Lectura rapida por carpeta

- `docker/validation-service`: empaqueta el backend en una imagen ejecutable para ambientes containerizados.
- `charts/validation-service`: chart Helm base para desplegar el servicio y parametrizar imagen, red y variables.
- `localstack`: soporte de desarrollo para emular S3 sobre Docker + WSL2 con datos persistentes.

## Docker

El Dockerfile construye el JAR del backend y lo ejecuta en un contenedor separado.

### Build

```powershell
docker build -f infra/docker/validation-service/Dockerfile -t sipro-validation-service .
```

### Run

```powershell
docker run -p 8080:8080 sipro-validation-service
```

### Notas

- El build usa imagen Temurin 21 y compila el subproyecto backend completo desde la carpeta backend/.
- La aplicacion sigue compilando con toolchain Java 17 dentro de Gradle.
- Si necesitas variables o secretos reales, debes inyectarlos al contenedor; el Dockerfile no los embebe.

## Helm

El chart disponible es una base operativa, pero varios valores siguen siendo placeholders corporativos y deben revisarse antes de un despliegue real.

### Que hace cada template

- `templates/configmap.yaml`: publica configuracion no sensible como URL JDBC y parametros de Active Directory.
- `templates/secret.yaml`: entrega credenciales sensibles al contenedor usando `stringData`.
- `templates/deployment.yaml`: declara replicas, imagen y variables de entorno que consume Spring Boot.
- `templates/service.yaml`: expone el puerto HTTP interno del pod dentro del cluster.
- `templates/ingress.yaml`: habilita entrada HTTP solo cuando `ingress.enabled=true`.

### Comandos

```powershell
cd infra/charts/validation-service
helm install sipro-validation-service .
helm upgrade sipro-validation-service .
helm uninstall sipro-validation-service
```

### Valores que suelen ajustarse

- image.repository
- image.tag
- service.port y service.targetPort
- ingress.enabled y hosts
- env.SERVER_PORT
- env.JDBC_URL / DB_USER / DB_PASS

## Recomendaciones

- Usa estos activos para empaquetado y despliegue, no como unica fuente de verdad funcional.
- Mantén sincronizados los valores de imagen y variables con los perfiles reales del backend.
- No guardes secretos reales en values.yaml; usa el template de secret y la herramienta corporativa correspondiente.

## LocalStack

El script [localstack/start-localstack.ps1](localstack/start-localstack.ps1) automatiza un escenario que en Windows suele fallar si se hace manualmente: levantar LocalStack en Docker dentro de WSL2, mantener vivo el forwarding a `localhost:4566`, asegurar que exista el bucket `sipro-bucket` y restaurar un seed minimo cuando el volumen queda vacio.

### Modos soportados

- `Start`: levanta o reutiliza el contenedor y deja corriendo un keepalive en segundo plano.
- `Status`: muestra contenedor, health, hooks de init y conteo de objetos del bucket.
- `Stop`: detiene el contenedor y mata el keepalive de Windows.
- `Recreate`: elimina contenedor y datos runtime, pero conserva `init-scripts` y `seed` para reconstruir el ambiente.

### Por que existe tanta logica operativa

- WSL2 no siempre publica de inmediato `localhost:4566` hacia Windows aunque el contenedor ya este sano.
- LocalStack puede quedar en crash loop si persisten datos dañados en el volumen.
- El backend espera que el bucket exista y que el endpoint local responda antes de inicializar el cliente S3.

## Referencias

- [../README.md](../README.md)
- [../backend/README.md](../backend/README.md)
- [../SECURITY.md](../SECURITY.md)

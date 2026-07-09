A nivel práctico, tener LocalStack “con WSL” no cambia lo que LocalStack puede o no puede hacer: lo que manda es (1) cómo corres Docker (Docker Desktop con backend WSL2 o Docker Engine dentro de WSL) y (2) cómo configuras el almacenamiento (volúmenes) y la persistencia (feature de LocalStack). [docs.docker.com], [learn.microsoft.com], [docs.local...tack.cloud]
Abajo te lo explico enfocado en S3, con escenarios de “si cierro”, gratis vs pago, y cómo configurarlo desde WSL.

1) ¿Qué pasa cuando usas LocalStack + WSL (arquitectura real)?
Opción A (más común): Docker Desktop usando motor WSL2

Docker Desktop puede correr usando el WSL 2 based engine, y entonces tú puedes ejecutar docker desde tu distro WSL (Ubuntu, Debian, etc.) como si fuera Linux. [docs.docker.com], [learn.microsoft.com]
Docker Desktop con WSL2 trae mejoras de rendimiento/arranque y uso de recursos (memoria dinámica), y guarda datos del engine en ubicaciones administradas por Docker/WSL. [docs.docker.com]

Opción B: Docker Engine instalado dentro de tu distro WSL

Microsoft explica que si necesitas preferir un motor dentro de Linux/WSL (por ejemplo por simultaneidad o preferencia), puedes instalar Docker “nativo” en tu distro en lugar de Docker Desktop. [learn.microsoft.com]

✅ En ambas opciones, LocalStack normalmente corre como un contenedor Docker y expone el endpoint principal (edge) en el puerto 4566. [hub.docker.com]

2) LocalStack + S3 en local: ¿qué “estado” existe?
Cuando creas en LocalStack:

un bucket S3,
subes objetos,
creas colas, tablas, etc.

Eso vive en el estado interno del emulador y/o en el filesystem del contenedor. [docs.local...tack.cloud], [docs.local...tack.cloud]
La ruta importante (dentro del contenedor) es el “volume directory”:

/var/lib/localstack (raíz del volumen)
/var/lib/localstack/state (estado de servicios si la persistencia está habilitada) [docs.local...tack.cloud]

Y la propia doc dice que para que funcione correctamente en contenedor, debes montar un volumen del host hacia /var/lib/localstack. [docs.local...tack.cloud]

3) Persistencia en S3: ¿es gratis o de pago?
3.1 Persistencia “real” (snapshot-based) = de pago
La guía oficial de Persistence indica que esta capacidad está incluida en los planes Base y Ultimate. [docs.local...tack.cloud], [localstack.cloud]
Y el pricing remarca que en planes pagos se incluye “Local state persistence”. [localstack.cloud]
➡️ Con persistencia oficial, LocalStack puede guardar y restaurar el estado (incluyendo recursos/datos) usando snapshots y reanudar al reiniciar. [docs.local...tack.cloud]
3.2 En Free: por defecto no tienes esa persistencia snapshot
En Free tienes un set de servicios emulados (p.ej. “30+”) pero la persistencia local como feature aparece como parte de planes pagos. [localstack.cloud], [docs.local...tack.cloud]
3.3 Alternativa para Free: localstack-persist (comunidad)
Existe un proyecto open-source (imagen Docker) llamado gresau/localstack-persist que afirma añadir persistencia “out-of-the-box” para Community, porque la persistencia oficial “as of LocalStack 1.0” pasó a ser “pro-only”. [github.com], [hub.docker.com]

Importante: esto es una solución comunitaria, no la feature oficial; úsala si tu caso lo permite. [github.com], [hub.docker.com]


4) ¿Qué pasa si “cierro” o “reinicio”? (S3)
Esto depende de dos cosas:

si el contenedor se elimina/recrea
si hay volumen montado y/o persistencia habilitada [docs.local...tack.cloud], [docs.local...tack.cloud], [hub.docker.com]

Caso A — Solo cierras la terminal WSL

Si corriste docker compose up -d (detached), cerrar la terminal no apaga los contenedores; siguen corriendo en segundo plano (esto es comportamiento normal de Docker). [docs.docker.com]
Si corriste docker compose up sin -d, cerrar la terminal normalmente detiene lo que estaba “attachado” (depende de cómo se cierre), pero el punto clave es que la vida del contenedor determina si el estado se mantiene. [docs.docker.com]

Caso B — docker stop o reinicias Docker Desktop

Si el contenedor se vuelve a levantar sin haber sido eliminado, y además tienes volumen montado, es más probable que el estado permanezca en disco del host (si tu persistencia y/o almacenamiento están bien configurados). [docs.local...tack.cloud], [docs.local...tack.cloud]

Caso C — docker run --rm ... (modo efímero)

El ejemplo típico de Docker para LocalStack usa --rm, lo que significa que cuando el contenedor para, se elimina; y además el ejemplo advierte que no monta volúmenes, así que el estado no queda persistido. 
✅ Resultado: pierdes buckets/objetos (S3) al detener. [hub.docker.com]

Caso D — docker compose down

Docker documenta que docker compose down para y remueve contenedores y redes, y puede remover volúmenes si usas -v; además recalca que para datos que deban persistir debes usar bind mounts o named volumes explícitos. 
✅ Resultado típico: si haces down y no tienes un volumen persistente bien configurado, tu S3 local “se va”. [docs.docker.com] [docs.docker.com], [docs.local...tack.cloud]


5) ¿Cómo se configura desde WSL para S3? (Docker + WSL)
Recomendación de performance en WSL
Docker recomienda que, para mejor performance al bind-mount, guardes el código/datos en el filesystem Linux (WSL) y no en /mnt/c/...; además, eventos de file-watch (inotify) funcionan mejor desde filesystem Linux. [docs.docker.com], [learn.microsoft.com]
Esto aplica si montas carpetas del proyecto o un directorio de datos desde tu distro WSL hacia contenedores. [docs.docker.com], [learn.microsoft.com]

6) Configuración concreta para S3 (con y sin persistencia)

Nota: no pongo código en tablas para que lo puedas copiar fácil.

6.1 (PAGO) LocalStack con persistencia oficial (S3 persistente)
La doc oficial dice:

habilita con PERSISTENCE=1
monta el volumen del host a /var/lib/localstack [docs.local...tack.cloud], [docs.local...tack.cloud]

Ejemplo (conceptual) en docker-compose.yml:
YAMLservices:  localstack:    image: localstack/localstack-pro    ports:      - "4566:4566"    environment:      - LOCALSTACK_AUTH_TOKEN=${LOCALSTACK_AUTH_TOKEN}      - PERSISTENCE=1      - SERVICES=s3    volumes:      - ./volume:/var/lib/localstackMostrar más líneas

El montaje ./volume:/var/lib/localstack sigue la recomendación oficial del layout/volumen. [docs.local...tack.cloud]
PERSISTENCE=1 activa snapshots para guardar/restaurar estado. [docs.local...tack.cloud]

¿Qué pasa si reinicias?: Con persistencia, al volver a iniciar debería restaurar el estado desde el volumen (según compatibilidad de versiones y estrategia de guardado). [docs.local...tack.cloud], [docs.local...tack.cloud]

6.2 (GRATIS) Sin persistencia oficial: dos caminos
Camino 1: recrear recursos al iniciar (init scripts)
LocalStack soporta hooks de init en /etc/localstack/init (en el layout), lo que te permite crear buckets al arrancar (no es “persistir”, es “reconstruir siempre”). [docs.local...tack.cloud]
✅ Ventaja: 100% free. 
⚠️ Desventaja: los objetos/datos subidos se vuelven a perder si no los re-cargas. [localstack.cloud] [hub.docker.com], [docs.docker.com]
Camino 2: usar localstack-persist (comunidad)
Ejemplo conceptual:
YAMLservices:  localstack:    image: gresau/localstack-persist:4    ports:      - "4566:4566"    environment:      - SERVICES=s3    volumes:      - ./my-localstack-data:/persisted-dataMostrar más líneas

La imagen localstack-persist está diseñada como drop-in replacement y guarda datos en /persisted-data (montable). [github.com], [hub.docker.com]
También permite activar/desactivar persistencia por servicio con variables PERSIST_*. [github.com], [hub.docker.com]


7) Credenciales y accesos (AWS CLI/SDK) cuando usas LocalStack
7.1 Necesitas credenciales “tipo AWS” en las llamadas
LocalStack requiere que se envíen credenciales en las operaciones API (como AWS). [docs.local...tack.cloud]
7.2 Pero el “Secret Access Key” actualmente se ignora
La doc indica que el secret access key se ignora y recomiendan usar “test” o el mismo valor. [docs.local...tack.cloud]
7.3 Protección para que no uses AKIA/ASIA reales por accidente
Por defecto LocalStack intenta aceptar keys con ciertos prefijos (LSIA/LKIA) y rechaza AKIA/ASIA (o cae a un account por defecto) como safeguard para evitar conexiones accidentales a AWS real. [docs.local...tack.cloud]

8) OJO: cambio importante en 2026 (autenticación)
Varias páginas oficiales muestran el aviso: desde March 23, 2026, LocalStack for AWS se consolidará en una sola imagen que requiere autenticación. [docs.local...tack.cloud], [docs.local...tack.cloud], [docs.local...tack.cloud]
Esto se relaciona con la parte de licencias/tokens (workspace + usuario) descrita en “Licensing & Tiers”. [docs.local...tack.cloud]

9) Resumen ultra claro (S3 en WSL)

WSL solo es el entorno: lo importante es Docker + volúmenes + tier (free/pago). [docs.docker.com], [docs.local...tack.cloud], [localstack.cloud]
S3 persistente “de verdad” (snapshot) en LocalStack = pago (Base/Ultimate) + PERSISTENCE=1 + montar /var/lib/localstack. [docs.local...tack.cloud], [localstack.cloud], [docs.local...tack.cloud]
En Free, por defecto no tienes esa persistencia; puedes:

reconstruir recursos con init hooks, o [docs.local...tack.cloud]
usar localstack-persist (comunidad). [github.com], [hub.docker.com]


Si haces docker run --rm o docker compose down sin volumen persistente, pierdes buckets/objetos. [hub.docker.com], [docs.docker.com]
Para mejor performance en WSL, guarda tu proyecto/volúmenes en el filesystem Linux (no /mnt/c). [docs.docker.com], [learn.microsoft.com]


Dos preguntas rápidas para aterrizarlo a TU setup (y te doy el compose exacto)

¿Estás usando Docker Desktop con WSL2 o instalaste Docker Engine dentro de WSL? [docs.docker.com], [learn.microsoft.com]
¿Tu objetivo es que S3 mantenga objetos y buckets aunque hagas down/reinicio, o solo que al iniciar se recreen los buckets? [docs.docker.com], [docs.local...tack.cloud]

Si me dices eso, te dejo un docker-compose.yml final (free/pago) y comandos awslocal/AWS CLI para verificar persistencia paso a paso

Perfecto — lo que escribiste describe un síntoma muy común cuando se usa LocalStack en Windows/WSL con Docker, especialmente en Free/Community: el directorio state/ dentro de /var/lib/localstack/ queda vacío y S3 no “sobrevive” reinicios, aunque montes un bind mount. A continuación te dejo toda la información y el razonamiento, incluyendo por qué pasa, qué es realmente /var/lib/localstack/state, qué hace (y qué NO hace) PERSISTENCE=1, y por qué tu “fix” de seed idempotente es una estrategia válida.

1) Qué es /var/lib/localstack/state y por qué esperabas ver algo allí
LocalStack, cuando corre en contenedor, organiza su almacenamiento interno bajo /var/lib/localstack (el “LocalStack volume directory”). Ahí vive: logs/, tmp/, cache/ y state/. La documentación oficial indica explícitamente que /var/lib/localstack/state contiene el estado de los servicios si la persistencia está habilitada. [tutorialpedia.org]
Además, la misma página dice que para que LocalStack funcione correctamente en Docker, debe montarse un volumen del host hacia /var/lib/localstack (bind mount o named volume). [tutorialpedia.org]
✅ Conclusión: tu expectativa (ver archivos en state/) es lógica solo si la “persistencia” está realmente activa y soportada en tu edición/tier. [tutorialpedia.org], [stackoverflow.com]

2) Por qué en LocalStack Free suele quedar vacío (la razón #1)
Persistencia “snapshot-based” = feature de pago (Base/Ultimate)
La guía oficial de Persistence (mecanismo de snapshots) indica que está incluida en planes Base y Ultimate. 
Y la página de precios lista “Local state persistence” como capacidad incluida en los planes pagos (no en Free). [stackoverflow.com], [docs.docker.com] [docs.docker.com]
Entonces, en Free, aunque configures PERSISTENCE=1, la persistencia “real” (guardar y restaurar estado completo tipo snapshot) no está garantizada como feature. [stackoverflow.com], [docs.docker.com]
✅ Esto explica directamente tu observación: /var/lib/localstack/state vacío porque el mecanismo de persistencia que escribe snapshots ahí no se habilita (o no funciona de forma soportada) en tu tier. [stackoverflow.com], [tutorialpedia.org]

3) Por qué PERSISTENCE=1 “solo genera confusión” en Free
La doc oficial de Persistence explica que para activar snapshots se usa PERSISTENCE=1 y se guarda en el “LocalStack Volume Directory”. 
Pero si tu plan no incluye la funcionalidad, lo común es que: [stackoverflow.com], [tutorialpedia.org]

el contenedor no escriba snapshots, o
el comportamiento sea parcial/no confiable.

Por eso tu decisión de quitar PERSISTENCE=1 en Free tiene sentido operativo: evita una configuración que sugiere “persistencia garantizada” cuando realmente no lo está para ese tier. [docs.docker.com], [stackoverflow.com]

4) “Cross-filesystem bind mounts” (Windows ↔ WSL) = razón #2 (y por qué afecta tanto)
Aunque el punto principal es el tier, en Windows/WSL se suman problemas prácticos con bind mounts desde el filesystem de Windows (/mnt/c/...) hacia contenedores Linux.
Docker recomienda explícitamente que, para mejor rendimiento y comportamiento correcto al bind-mount, guardes el código/datos en el filesystem Linux (WSL), no en el filesystem Windows montado (/mnt/c). Además, los contenedores Linux solo reciben correctamente eventos tipo inotify si los archivos están en el filesystem Linux. [dev.to], [github.com]
Microsoft también recomienda no trabajar “cross-OS filesystem” salvo necesidad, y que para mejor performance en terminal Linux uses rutas tipo /home/<user>/... (WSL filesystem), no /mnt/c/Users/.... [github.com]
✅ Resultado: incluso si montas un bind mount para datos, hacerlo desde /mnt/c puede traer:

I/O lento,
watchers que no detectan cambios,
y en algunos casos comportamientos raros con archivos/locks. [dev.to], [github.com]


Importante: esto no “convierte” Free en persistente, pero sí reduce problemas cuando montas volúmenes y ejecutas scripts de init/seed desde WSL. [dev.to], [github.com]


5) Entonces, ¿qué estrategia sí funciona en Free? → “Seed idempotente” (tu fix)
Tu fix es una práctica estándar: en vez de persistir el estado, reconstruyes el estado de forma determinista al arrancar. Es decir:

Cada arranque del contenedor ejecuta scripts que:

crean buckets si no existen
configuran policies/CORS/notificaciones
suben fixtures si aplica
crean colas/tablas relacionadas, etc.



¿Dónde enganchar el “seed” en LocalStack?
La documentación del filesystem layout menciona /etc/localstack/init como raíz para initialization hooks. 
(En la práctica, mucha gente también usa mecanismos de “init scripts” montados al contenedor según el método de arranque, pero el punto importante es que LocalStack contempla una carpeta de init/hook). [tutorialpedia.org]
✅ Esto encaja perfecto con “seed idempotente”: tu sistema no depende de snapshots, sino de scripts repetibles. [tutorialpedia.org], [w3tutorials.net]

6) Cómo diseñar un seed idempotente para S3 (recomendación concreta)
Principios (para que nunca falle en reinicios)

Idempotencia: “si existe, no lo recrees” (o recrea sin error).
Orden: primero infraestructura (buckets), luego configuración (CORS/policies), luego datos (objetos).
Endpoint siempre local: usa --endpoint-url=http://localhost:4566 o awslocal para evitar pegarle a AWS real. [web-v2.pro...radius.com], [docs.docker.com]
Credenciales dummy: LocalStack requiere credenciales “tipo AWS” en las operaciones, pero el Secret suele ignorarse y se recomienda usar valores de prueba. [docs.docker.com]

Ejemplo de script (bash) idempotente para S3

Esto lo puedes montar como init hook o ejecutarlo en un “sidecar” que espere a que LocalStack esté “healthy” y luego corra el seed.

Shell#!/usr/bin/env bashset -euo pipefailENDPOINT="http://localhost:4566"REGION="us-east-1"BUCKET="mi-bucket-local"# Crea bucket si no existe (idempotente)if ! aws --endpoint-url="$ENDPOINT" s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then  aws --endpoint-url="$ENDPOINT" s3api create-bucket --bucket "$BUCKET" --region "$REGION"fi# Configura CORS (si aplica)aws --endpoint-url="$ENDPOINT" s3api put-bucket-cors --bucket "$BUCKET" --cors-configuration '{  "CORSRules": [{    "AllowedOrigins": ["*"],    "AllowedMethods": ["GET","PUT","POST","DELETE","HEAD"],    "AllowedHeaders": ["*"],    "ExposeHeaders": ["ETag"],    "MaxAgeSeconds": 3000  }]}' || true# Sube fixtures si no existenecho "hola localstack" > /tmp/hello.txtaws --endpoint-url="$ENDPOINT" s3api head-object --bucket "$BUCKET" --key "fixtures/hello.txt" 2>/dev/null \  || aws --endpoint-url="$ENDPOINT" s3 cp /tmp/hello.txt "s3://$BUCKET/fixtures/hello.txt"Mostrar más líneas
(La forma exacta de engancharlo depende de tu compose/run, pero el patrón idempotente es el core.)

7) Alternativa “Free pero persistente”: localstack-persist (comunidad)
Si tu prioridad absoluta es persistir sin pagar, existe el proyecto gresau/localstack-persist, una imagen que actúa como drop-in replacement y guarda estado en /persisted-data montable como volumen. También permite activar/desactivar persistencia por servicio con variables PERSIST_*. [learn.microsoft.com], [cmas.dev]
Esto se creó precisamente porque “as of LocalStack 1.0, persistence is a pro-only feature” según su README. [learn.microsoft.com], [cmas.dev]

8) Checklist de diagnóstico rápido (para tu caso exacto)

¿Estás en Free? Entonces asume que no hay snapshot persistence oficial (aunque pongas PERSISTENCE=1). [docs.docker.com], [stackoverflow.com]
¿Montas volumen a /var/lib/localstack? (si no, todo vive dentro del contenedor y se pierde al recrearlo). [tutorialpedia.org], [web-v2.pro...radius.com]
¿Tu bind mount viene de /mnt/c/...? Si sí, muévelo a /home/<user>/... en WSL para evitar problemas cross-filesystem. [dev.to], [github.com]
¿Usas docker run --rm o docker compose down? Eso destruye contenedores; sin persistencia/volumen estable pierdes estado. [web-v2.pro...radius.com], [w3tutorials.net]


Conclusión (validando tu afirmación)

✅ “state/ siempre vacío” es coherente si estás en Free y esperas persistencia snapshot oficial. [stackoverflow.com], [docs.docker.com]
✅ Quitar PERSISTENCE=1 en Free evita confusión, porque la persistencia real está ligada a planes pagos. [docs.docker.com], [stackoverflow.com]
✅ “Seed idempotente en cada arranque” es un workaround sólido y muy usado para S3 en entornos locales. [tutorialpedia.org], [w3tutorials.net]
✅ Si además estás montando desde Windows (/mnt/c), migrar el directorio a WSL (/home/...) mejora mucho la estabilidad y performance del bind mount

¡Hecho! Te traigo mecanismos reales (probados por la comunidad) para tener “S3 local” en modo Free con algún tipo de persistencia (o equivalente funcional) y que sobreviva cuando el contenedor se recrea. Voy a separar claramente:

Persistencia real del estado de LocalStack CE (free) usando un “wrapper”
Persistencia “práctica” por reconstrucción idempotente en cada arranque (init hooks)
Persistencia real de objetos S3 usando un reemplazo S3-compatible (MinIO) — recomendado si solo te importa S3

Además te explico dónde se guardan los archivos, por qué /var/lib/localstack/state queda vacío en Free, y cómo hacerlo bien en WSL (evitar cross-filesystem).

0) Contexto: por qué en LocalStack Free “no persiste” S3 (y el state queda vacío)

La persistencia snapshot-based oficial de LocalStack (la que guarda/restaura estado completo, “pause & resume”) está documentada como incluida en planes Base/Ultimate y se habilita con PERSISTENCE=1. [docs.local...tack.cloud], [docs.docker.com]
El mismo concepto (“persistencia como feature”) aparece en pricing como “Local state persistence” en planes pagos, no en el tier Free. [docs.docker.com], [docs.local...tack.cloud]
La comunidad reporta exactamente tu síntoma: montas ./localstack-data:/var/lib/localstack, creas buckets/objetos, pero localstack-data queda vacío (solo cache/), y al reiniciar “desaparece todo”. [stackoverflow.com], [tutorialpedia.org]

✅ Conclusión: En Free, no puedes depender de que LocalStack escriba/restaure estado “oficial” en /var/lib/localstack/state. [docs.local...tack.cloud], [stackoverflow.com]

1) Opción A (REAL + Free): LocalStack Community con persistencia vía localstack-persist (wrapper OSS)
1.1 ¿Qué es y de dónde sale?

localstack-persist es un proyecto open-source de GREsau que crea una imagen Docker “drop-in replacement” para LocalStack Community. [github.com], [hub.docker.com]
El propio README explica el motivo: “As of LocalStack 1.0, persistence is a pro-only feature… localstack-persist adds out-of-the-box persistence … saved whenever a resource is modified and automatically restored on container startup.” [github.com], [github.com]
En Docker Hub se repite lo mismo y añade detalle clave: no necesitas PERSISTENCE=1, porque esa bandera controla la persistencia interna de LocalStack (que “no funciona” en Community), y este wrapper usa su propio mecanismo. [hub.docker.com], [github.com]

1.2 ¿Dónde guarda los datos?

Guarda el estado persistente dentro del contenedor en /persisted-data, y tú montas un volumen del host hacia esa ruta para que sobreviva a recreaciones del contenedor. [github.com], [hub.docker.com]

1.3 Ejemplo funcional (docker-compose) enfocado SOLO en S3

Esto es literalmente el patrón recomendado por el README/DockerHub.

YAMLservices:  localstack:    image: gresau/localstack-persist:4    container_name: localstack    ports:      - "4566:4566"    environment:      - SERVICES=s3      # opcional: persiste solo S3 (en vez de todo)      - PERSIST_DEFAULT=0      - PERSIST_S3=1    volumes:      - ./my-localstack-data:/persisted-dataMostrar más líneas

La configuración PERSIST_DEFAULT=0 y PERSIST_S3=1 existe para persistir solo servicios específicos, según README/DockerHub. [github.com], [hub.docker.com]
El volumen ./my-localstack-data:/persisted-data es el camino estándar para que la data persista en host. [github.com], [hub.docker.com]

1.4 Prueba “real” de persistencia (pasos)

Levanta: docker compose up -d
Crea bucket y sube objeto (con AWS CLI apuntando a LocalStack):

aws --endpoint-url=http://localhost:4566 s3 mb s3://demo-bucket
echo hola > file.txt
aws --endpoint-url=http://localhost:4566 s3 cp file.txt s3://demo-bucket/file.txt


Baja y recrea contenedor:

OJO: docker compose down elimina contenedores pero no borra volúmenes nombrados/bind mounts por defecto; solo los borra si usas -v. [w3tutorials.net], [w3tutorials.net]


Sube de nuevo: docker compose up -d
Verifica:

aws --endpoint-url=http://localhost:4566 s3 ls
aws --endpoint-url=http://localhost:4566 s3 ls s3://demo-bucket/



Deberías ver el bucket/objeto, porque el estado queda en ./my-localstack-data montado en /persisted-data. [github.com], [hub.docker.com]
Pros / Contras
✅ Pros

Persistencia “real” sin pagar (para recursos/datos) usando un mecanismo adicional. [github.com], [hub.docker.com]
Se restaura automáticamente al iniciar. [github.com], [github.com]

⚠️ Contras

No es feature oficial de LocalStack; depende del proyecto OSS y compatibilidad con versiones (aunque el tag mayor sigue al LocalStack base). [github.com], [hub.docker.com]
Si tu equipo busca soporte/garantías, el camino “oficial” es el plan pago. [docs.local...tack.cloud], [docs.docker.com]


2) Opción B (Free, “funciona siempre”): Seed idempotente en cada arranque (Init Hooks)
(No es persistencia, pero te da el MISMO resultado práctico: al recrear, reaparece tu “S3 esperado”)
2.1 Origen “real” y por qué es el workaround recomendado

LocalStack tiene Initialization Hooks (hooks de ciclo de vida): boot.d, start.d, ready.d, shutdown.d bajo /etc/localstack/init/. [docs.local...tack.cloud], [tutorialpedia.org]
Esta feature está marcada como incluida en planes Free/Base/Ultimate (o sea: sí está disponible en Free). [docs.local...tack.cloud], [docs.local...tack.cloud]
La comunidad documenta el cambio histórico: antes se usaba /docker-entrypoint-initaws.d, pero desde v1.1.0 se deprecó y se reemplazó por /etc/localstack/init/<stage>.d. [stackoverflow.com], [devlo.io]

2.2 Ejemplo real: crear buckets al arrancar (READY stage)
docker-compose.yml
YAMLservices:  localstack:    image: localstack/localstack:latest    container_name: localstack    ports:      - "4566:4566"    environment:      - SERVICES=s3      - AWS_ACCESS_KEY_ID=test      - AWS_SECRET_ACCESS_KEY=test      - AWS_DEFAULT_REGION=us-east-1    volumes:      - ./init-scripts:/etc/localstack/init/ready.dMostrar más líneas

Montar scripts en /etc/localstack/init/ready.d es exactamente lo que recomienda la doc y lo que la comunidad usa en ejemplos. [docs.local...tack.cloud], [stackoverflow.com]

./init-scripts/10-seed-s3.sh
Shell#!/usr/bin/env bashset -euo pipefail# crea bucket si no existe (idempotente)awslocal s3api head-bucket --bucket demo-bucket 2>/dev/null \  || awslocal s3 mb s3://demo-bucket --region us-east-1# sube un fixture si no existeecho "hola" > /tmp/hello.txtawslocal s3api head-object --bucket demo-bucket --key fixtures/hello.txt 2>/dev/null \  || awslocal s3 cp /tmp/hello.txt s3://demo-bucket/fixtures/hello.txtMostrar más líneas

awslocal es el wrapper típico (aws + endpoint local) citado en guías de arranque/scripts. [devlo.io], [rieckpil.de]
Los scripts se ejecutan en orden alfanumérico en el stage READY según la doc. [docs.local...tack.cloud], [docs.local...tack.cloud]

¿Qué obtienes?

Cada vez que recreas el contenedor, los buckets/fixtures vuelven a existir sin depender de /var/lib/localstack/state. [docs.local...tack.cloud], [stackoverflow.com]
Incluso si docker compose down te elimina el contenedor, al up se reconstruye. [w3tutorials.net], [docs.local...tack.cloud]

✅ Pros

100% Free, soportado oficialmente (hooks). [docs.local...tack.cloud], [tutorialpedia.org]
Excelente para equipos: repositorio versionado con infraestructura “seed”. [rieckpil.de], [docs.local...tack.cloud]

⚠️ Contras

Si necesitas conservar objetos dinámicos (subidos “a mano” durante debug), esto no los guarda, solo los recrea si tu script lo contempla. [docs.local...tack.cloud], [stackoverflow.com]


3) Opción C (la más “limpia” si SOLO quieres S3 persistente): MinIO (S3-compatible)
Si tu objetivo real es: “necesito un endpoint S3 local que guarde objetos persistentes y sobreviva siempre”, entonces MinIO suele ser la mejor solución porque es un storage real (no emulador de estado).
3.1 Origen y prueba real

MinIO es un servidor de almacenamiento de objetos compatible con la API S3 y se usa para desarrollo local sin AWS. [datacamp.com], [oneuptime.com]
Los ejemplos típicos lo levantan con Docker y persisten datos montando un volumen a /data. [oneuptime.com], [pliutau.com]

3.2 Ejemplo funcional (docker compose) con persistencia + creación de bucket
YAMLservices:  minio:    image: quay.io/minio/minio:latest    container_name: minio    ports:      - "9000:9000"   # API S3      - "9001:9001"   # consola web    command: ["server", "--console-address", ":9001", "/data"]    environment:      - MINIO_ROOT_USER=minioadmin      - MINIO_ROOT_PASSWORD=minioadmin    volumes:      - minio_data:/data  minio-buckets:    image: quay.io/minio/mc:latest    depends_on:      - minio    restart: on-failure    entrypoint: >      /bin/sh -c "      /usr/bin/mc alias set local http://minio:9000 minioadmin minioadmin;      /usr/bin/mc mb local/demo-bucket;      exit 0;      "volumes:  minio_data: {}Mostrar más líneas

Este patrón (MinIO + mc para crear buckets al inicio) aparece en guías reales y funciona porque el volumen minio_data:/data conserva los objetos aunque recrees contenedor. [pliutau.com], [oneuptime.com]

✅ Pros

Persistencia real y simple (volumen Docker). [oneuptime.com], [pliutau.com]
Muy estable si tu foco es S3 (subir/listar/descargar). [datacamp.com], [oneuptime.com]

⚠️ Contras

No emula “AWS completo” (solo S3-compatible). Si necesitas SQS/Dynamo/Lambda, entonces LocalStack sigue siendo útil. [datacamp.com], [dev.to]


4) Recomendación crítica para WSL: evita “cross-filesystem” en los volúmenes
Esto te ahorra muchos “misterios” de I/O, permisos y rendimiento:

Docker recomienda bind-mount desde el filesystem Linux (WSL) y evitar /mnt/c/... por rendimiento y eventos (inotify). [dev.to], [github.com]
Microsoft también recomienda: si trabajas desde Linux (WSL), guarda los proyectos en /home/<user>/... en vez de /mnt/c/... para mejor performance. [github.com], [dev.to]

✅ Traducción práctica:

Si vas a usar ./my-localstack-data o ./init-scripts, crea esas carpetas en tu home de WSL, por ejemplo:

/home/tuusuario/localstack/my-localstack-data
/home/tuusuario/localstack/init-scripts




5) ¿Cuál te conviene? (decisión rápida)
Si quieres seguir usando LocalStack Free y además “guardar estado”
➡️ Opción A: localstack-persist (persistencia real sin pagar, pero OSS externo). [github.com], [hub.docker.com]
Si lo que necesitas es que “al recrear vuelva a existir lo básico” (buckets + fixtures)
➡️ Opción B: Init Hooks + seed idempotente (oficial + Free + súper estable). [docs.local...tack.cloud], [stackoverflow.com]
Si SOLO necesitas S3 realmente persistente y te da igual emular AWS completo
➡️ Opción C: MinIO (más simple y persistente)

Entiendo perfecto tu escenario: solo tienes WSL (Ubuntu) y LocalStack, pero NO tienes Docker Desktop ni un “Docker local” en Windows. En esa situación, la clave es esta:

LocalStack (la forma estándar) corre como contenedor, así que sin un runtime de contenedores (Docker Engine/Podman) dentro de WSL, LocalStack no puede ejecutarse. [mundobytes.com], [learn.microsoft.com]

A continuación te dejo un análisis muy detallado y basado en casos reales sobre limitaciones, problemas típicos, y soluciones prácticas cuando trabajas así (WSL-only).

1) Lo primero: ¿qué opciones reales existen si NO tienes Docker Desktop?
Opción 1 — Instalar Docker Engine dentro de WSL (Ubuntu) (lo más común)
Microsoft menciona explícitamente que si necesitas ejecutar contenedores de Linux “por fuera de Docker Desktop”, puedes instalar y correr una instancia de Docker dentro de WSL. 
Guías reales (DEV/otros) describen el mismo enfoque: WSL2 como runtime + Docker Engine instalado en Ubuntu, evitando Docker Desktop (y su licenciamiento/cliente). [learn.microsoft.com], [dev.to] [dev.to], [gist.github.com]
Opción 2 — Usar Podman dentro de WSL (alternativa a Docker)
Microsoft también sugiere “instalar Podman” como alternativa para contenedores Linux si prefieres no usar Docker Desktop. 
(Pero LocalStack y muchas herramientas están más documentadas con Docker; Podman funciona, pero a veces requiere más ajustes). [learn.microsoft.com]
✅ Conclusión: WSL-only sí es viable, pero necesitas un motor de contenedores en WSL para correr LocalStack en contenedor. [learn.microsoft.com], [mundobytes.com]

2) Limitaciones reales cuando tu Docker “vive” solo dentro de WSL (sin Desktop)
2.1 No tienes integración “bonita” con Windows (GUI, actualizaciones, etc.)
Docker Desktop aporta conveniencia (GUI dashboard, upgrades, etc.). Sin Docker Desktop, tú gestionas todo en Linux (WSL) como en un servidor. [superuser.com], [dev.to]
2.2 (Muy común) No puedes usar docker desde PowerShell/CMD “por defecto”
Un caso real documentado: al correr Docker “nativo” en WSL, no obtienes automáticamente que Windows use ese daemon (no hay “daemon sharing”). 
Hay workarounds (scripts proxy wsl docker … o exponer el daemon por TCP), pero eso ya es configuración adicional y tiene implicaciones de seguridad. [dev.to], [superuser.com] [gist.github.com], [blog.miniasp.com]
2.3 Docker Engine no se “comparte” entre distribuciones WSL por defecto
Otro punto repetido en guías/casos: si tienes varias distros (Ubuntu + Debian), el Docker Engine instalado en Ubuntu no queda automáticamente accesible desde otras distros sin configurar sockets/hosts. [dev.to], [superuser.com]

3) Limitación crítica: systemd y el arranque del daemon (dockerd)
3.1 Si no tienes systemd, Docker no se inicia solo (y a veces ni arranca bien)
WSL hoy soporta systemd y Microsoft explica cómo habilitarlo con /etc/wsl.conf ([boot] systemd=true). 
Muchas guías de Docker-in-WSL (sin Desktop) dependen de systemd para auto-arranque de Docker y manejo con systemctl. [learn.microsoft.com], [learn.microsoft.com] [dev.to], [woshub.com]
3.2 Caso típico: “Docker funciona solo si ejecuto sudo service docker start”
Es exactamente el tipo de fricción que systemd resuelve: sin systemd, tienes que arrancar el servicio “a mano” o con hacks. Microsoft documenta systemd como el camino oficial para servicios persistentes. [learn.microsoft.com], [learn.microsoft.com]

4) Networking en WSL2 (casos reales) — por qué “a veces no me conecta”
4.1 WSL2 por defecto usa NAT y eso trae “quirks”
Microsoft documenta que WSL usa arquitectura NAT por defecto y que hay consideraciones especiales (IP interna, cambios tras reinicio, etc.). [learn.microsoft.com], [learn.microsoft.com]
Qué te afecta en LocalStack:

LocalStack expone servicios en un puerto (típico 4566) y tu app necesita llegar ahí.
En WSL2 normalmente puedes acceder desde Windows usando localhost, pero hay casos (VPN, firewalls, LAN) donde no es tan directo. [learn.microsoft.com], [mundobytes.com]

4.2 Exponer servicios a tu LAN (otro PC/móvil) requiere pasos extra
Casos reales muestran que para que un servicio en WSL2 sea visible desde la red local, suele requerirse:

portproxy (netsh interface portproxy) o
usar mirrored networking mode (Windows 11 + WSL moderno) + reglas del firewall Hyper-V. [iangge.github.io], [hy2k.dev]

Microsoft recomienda probar el nuevo Mirrored networking mode para mejores features y evitar algunas complicaciones de NAT. [learn.microsoft.com], [learn.microsoft.com]

5) Filesystem: el “mata-rendimiento” clásico en WSL (y afecta volúmenes, LocalStack y S3)
5.1 Montar volúmenes desde /mnt/c/... suele ser peor (lento / raro)
Docker recomienda que, para bind-mounts, guardes el código/datos en el filesystem Linux (WSL) en vez del filesystem Windows montado (/mnt/c), porque el rendimiento y eventos tipo inotify son mejores. 
Microsoft también recomienda: si trabajas desde Linux (WSL), guarda tus proyectos en /home/<user> (Linux FS) y evita /mnt/c/... para mejor rendimiento. [elest.io], [web-v2.pro...radius.com] [web-v2.pro...radius.com], [elest.io]
Impacto práctico en LocalStack/S3:

Si intentas persistir datos (volúmenes) en rutas cross-filesystem, es más común ver problemas de I/O, permisos y “comportamientos raros” en el stack local. [elest.io], [web-v2.pro...radius.com]


6) Cgroups / rootless / límites de recursos (casos reales cuando haces “Docker serio” en WSL)
6.1 Rootless Docker existe, pero tiene prerequisitos y límites
Docker documenta “Rootless mode” y sus prerequisitos (uidmap, subuid/subgid, etc.). [docs.docker.com]
6.2 Control de recursos real (CPU/memoria por contenedor) depende de cgroup v2
Guías de rootless containers explican que cgroup v2 suele ser necesario para límites como --memory, y requiere kernel y systemd suficientemente recientes. 
En WSL han existido fricciones históricas con cgroups; hay discusiones sobre habilitar/usar cgroup v2 en WSL2. [rootlesscontaine.rs], [learn.microsoft.com] [stackoverflow.com], [github.com]
6.3 Networking en rootless puede romper cosas avanzadas
Proyectos como kind documentan que en rootless pueden faltar módulos iptables y eso rompe componentes de red; esto aplica cuando empiezas a correr stacks más complejos en WSL. [kind.sigs.k8s.io], [rootlesscontaine.rs]

7) Limitaciones específicas para LocalStack en WSL-only (sin Docker Desktop)
7.1 LocalStack (estándar) necesita Docker para correr como contenedor
El “Quickstart” típico de LocalStack es docker run ... con puertos. Si no tienes Docker Engine en WSL, no hay contenedor que ejecutar. [mundobytes.com], [learn.microsoft.com]
7.2 Persistencia en LocalStack Free: no esperes “snapshots oficiales”
La persistencia snapshot-based se documenta como parte de planes pagos (Base/Ultimate) y se habilita con PERSISTENCE=1. 
En Free, el síntoma real que reporta la gente es que ./volume:/var/lib/localstack queda “casi vacío” y el estado no reaparece tras reinicio. [github.com], [github.com] [dockerpros.com], [docs.docker.com]
7.3 Workarounds reales usados en Free (sin pagar)
Tienes dos caminos reales y muy usados:


Init Hooks (seed idempotente)
LocalStack documenta init hooks en /etc/localstack/init/<stage>.d (incluido en Free) para crear buckets/colas/tablas en el arranque. [docs.docker.com], [rootlesscontaine.rs]


Imagen comunitaria con persistencia (localstack-persist)
Proyecto OSS que persiste recursos en /persisted-data y restaura al iniciar (drop-in replacement). [gist.github.com], [docs.docker.com]


(Esto ya lo veníamos conversando; aquí lo reafirmo como “casos reales”) [gist.github.com], [docs.docker.com]

8) Casos típicos (“me pasa X”) y diagnóstico rápido en WSL-only
Caso A: “No tengo Docker Desktop y LocalStack no arranca”

Causa: no hay daemon de contenedores (Docker Engine/Podman) corriendo dentro de WSL. [learn.microsoft.com], [mundobytes.com]
Solución real: instalar Docker Engine en WSL y habilitar systemd para autostart. [dev.to], [learn.microsoft.com]

Caso B: “Docker funciona pero al reiniciar WSL se muere”

Causa: Docker daemon no está habilitado como servicio (systemd apagado). [learn.microsoft.com], [learn.microsoft.com]

Caso C: “Desde Windows no puedo llegar a LocalStack en WSL”

Causa: NAT + firewall + puertos no publicados / binding no adecuado.
Soluciones reales: revisar modelo NAT/mirrored, y para LAN usar portproxy o mirrored + reglas Hyper-V firewall. [learn.microsoft.com], [iangge.github.io], [hy2k.dev]

Caso D: “Volúmenes/persistencia raros, lento, archivos que no cambian”

Causa: bind mounts desde /mnt/c/... (Windows FS) en vez de Linux FS.
Recomendación oficial: usar filesystem Linux para el proyecto/volúmenes. [elest.io], [web-v2.pro...radius.com]


9) Recomendación “mínima y realista” para tu caso (WSL-only + LocalStack)
Si tu objetivo es tener LocalStack (S3) funcionando estable en WSL-only, el stack más realista es:

Instalar Docker Engine dentro de Ubuntu WSL (sin Desktop) [learn.microsoft.com], [dev.to]
Habilitar systemd para que dockerd arranque siempre [learn.microsoft.com], [learn.microsoft.com]
Guardar todo en /home/<user>/... (no /mnt/c) [web-v2.pro...radius.com], [elest.io]
Para persistencia S3 en Free:

o seed idempotente con init hooks [docs.docker.com], [rootlesscontaine.rs]
o localstack-persist [gist.github.com], [docs.docker.com]

1) ¿Qué significa “Connection refused” en localhost:4566 según reportes comunes?
En discusiones y issues, “connection refused” significa que no hay un proceso escuchando en ese host/puerto en el contexto donde se ejecuta el cliente (tu app), o que el puerto no está expuesto/alcanzable desde ese contexto. Esto se ve mucho cuando el endpoint es LocalStack en el puerto “edge” 4566 y el contenedor/VM/host no coincide con el localhost que tu app está usando. [docs.local...tack.cloud], [blog.local...tack.cloud]
LocalStack documenta que, para acceder desde la misma máquina, debes exponer el puerto 4566 y conectarte a localhost (o un dominio que apunte a localhost); pero cuando se ejecuta desde otros contextos (otro contenedor, Lambdas, etc.), localhost deja de representar lo mismo. [docs.local...tack.cloud], [blog.local...tack.cloud]

2) Causa #1 reportada: LocalStack no está escuchando (no está corriendo / se cayó / se reinició)
En issues de LocalStack (por ejemplo “Failed to connect to localhost port 4566: Connection refused”), los usuarios reportan que el contenedor puede “verse arriba” pero el endpoint no responde a /health o no acepta conexiones en 4566, lo cual se interpreta como servicio no listo, caída o problema de arranque del gateway. [github.com], [docs.local...tack.cloud]
En el blog oficial de LocalStack también se menciona que LocalStack “publica su edge port (usualmente 4566) al host”; si algo interrumpe esa publicación o el proceso, las llamadas a localhost:4566 fallan. [blog.local...tack.cloud], [docs.local...tack.cloud]

3) Causa #2 (muy frecuente): “localhost no es local” cuando tu app corre en contenedor
Múltiples fuentes repiten el mismo patrón:

Si tu aplicación corre en otro contenedor, localhost:4566 apunta al mismo contenedor de tu app, no al contenedor de LocalStack. Esto produce exactamente “connection refused”. [discourse....odered.org], [stackoverflow.com]
En Stack Overflow se remarca que, en escenarios multi-contenedor, hay que usar el nombre del servicio/host del contenedor (o redes de Docker) porque localhost no referencia al otro contenedor. [stackoverflow.com], [stackoverflow.com]

LocalStack lo documenta formalmente: cuando el código corre “desde un contenedor”, recomiendan usar localhost.localstack.cloud y/o configurar DNS/red (y en algunos casos redes Docker user-managed) para que el contenedor resuelva bien el endpoint de LocalStack. [docs.local...tack.cloud], [blog.local...tack.cloud]

Incluso hay ejemplos reales donde el fix fue “dejar de usar localhost y usar el nombre del contenedor de localstack”, exactamente por este motivo. [discourse....odered.org], [stackoverflow.com]


4) Causa #3: Cambios de puertos / edge port unificado (4566) y confusión con puertos antiguos
LocalStack tuvo un cambio relevante (muy citado): todos los servicios pasan por el “edge” port 4566 y se “jubilan” puertos específicos por servicio (4572, 4576, etc.). Cuando el proyecto o scripts usan puertos viejos, aparecen errores y confusión de endpoints. [github.com], [docs.local...tack.cloud]
Hay hilos donde el equipo cree estar apuntando a otro puerto, pero el error menciona 4566; y se discuten ajustes como hostname/HOSTNAME_EXTERNAL/networking para estabilizar URLs generadas por servicios (SQS/SNS/S3). [stackoverflow.com], [github.com]

5) Causa #4: Problemas de DNS/hostnames para S3 (virtual-host style) y resolución de subdominios
En LocalStack, S3 suele generar URLs tipo:

http://<bucket>.s3.localhost.localstack.cloud:4566/...

Esto está relacionado con “virtual-hosted style requests”. LocalStack explica que han promovido localhost.localstack.cloud (y subdominios) porque resuelve a 127.0.0.1 y evita parte del caos de hostnames/subdominios, especialmente con TLS y routing. [blog.local...tack.cloud], [docs.local...tack.cloud]
Cuando la app/SDK o el entorno no resuelve esos subdominios, aparecen errores tipo UnknownHost o fallos de resolución (no exactamente “refused”, pero se reportan en el mismo paquete de “problemas de conectividad a LocalStack”). [w3tutorials.net], [blog.local...tack.cloud]

6) Causa #5: WSL2 / “localhost forwarding” que se rompe o cambia (intermitente)
Tu log menciona “Reactivando forward…”, y en Internet hay muchos reportes de que en WSL2 la conectividad por localhost puede fallar por temas de red (NAT/modo espejo), reanudación del sistema, etc.

Microsoft explica que WSL2 tiene consideraciones de red y, por defecto, usa una arquitectura NAT; en algunos casos recomiendan “mirrored networking mode” para mejorar integración. [github.com], [superuser.com]
Hay artículos que describen el síntoma “localhost refused to connect” en WSL2 como algo que a veces ocurre tras hibernación/fast startup o “quirks” del stack de red, y mencionan como “soluciones” reinicios del subsistema/red, revisar conflictos de puertos e inconsistencias IPv4/IPv6, etc. [technofossy.com], [codegenes.net]

En resumen, en WSL2 se reporta que “localhost forwarding” puede ser intermitente: a veces funciona y luego deja de funcionar, lo que cuadra con escenarios donde al inicio el servicio parece alcanzable y luego aparece “connection refused”. [technofossy.com], [github.com]

7) Causa #6: IPv4 vs IPv6 (127.0.0.1 vs ::1) y binding
En tu excepción aparece tanto 127.0.0.1 como 0:0:0:0:0:0:0:1 (IPv6 loopback). En writeups de WSL2 “localhost refused”, se menciona que a veces hay desajustes entre cómo el servicio escucha (solo IPv4, solo IPv6, o interfaces específicas) y cómo el cliente resuelve localhost. [technofossy.com], [codegenes.net]
Esto se cita como una de las “posibles causas” de “refused” o “no puedo acceder por localhost pero sí por IP interna”. [codegenes.net], [github.com]

8) Causa #7 (menos directa, pero aparece en reportes): credenciales / cadena de proveedores y endpoints “equivocados”
Aunque tu stacktrace apunta a localhost:4566, en la misma familia de problemas hay casos donde el SDK intenta usar rutas no esperadas si faltan credenciales o configuración, por ejemplo intentando consultar metadatos de EC2 (169.254.169.254) en contenedores. En Node-RED lo reportan como “ECONNREFUSED 169.254.169.254:80” y se mezclaba con el hecho de que “localhost” dentro del contenedor no era el de LocalStack. [discourse....odered.org], [docs.local...tack.cloud]

9) Qué “soluciones” menciona Internet (sin aplicarlas a tu caso)
Solo listando lo que otros sugieren en fuentes públicas (sin decirte “haz esto”):

Verificar que LocalStack realmente exponga 4566 y que el endpoint correcto sea accesible (LocalStack docs). [docs.local...tack.cloud], [github.com]
Si el cliente corre en contenedor, evitar localhost:4566 y usar hostname/red/DNS correctos (LocalStack docs, SO, foros). [docs.local...tack.cloud], [stackoverflow.com], [discourse....odered.org]
Usar el dominio localhost.localstack.cloud para resolver subdominios y simplificar conectividad (blog + docs). [blog.local...tack.cloud], [docs.local...tack.cloud]
En WSL2, revisar el modo de red / forwarding y casos donde “localhost refused” aparece por reanudación o NAT/mirrored mode (Microsoft Learn + artículos). [github.com], [superuser.com], [technofossy.com]
Asegurar consistencia con el “edge port” 4566 vs puertos legacy (issue de breaking change)
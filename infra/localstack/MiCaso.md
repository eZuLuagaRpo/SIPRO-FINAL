1) Tu estado actual (confirmado)
✅ LocalStack Community 3.8.1 (Free) con S3 “running” y lo demás “disabled”. 
✅ Contenedor localstack/localstack:3 está Up (healthy). 
✅ El puerto 4566 está escuchando y lo atiende docker-proxy dentro de WSL. 
✅ Dentro del contenedor existe /var/lib/localstack con carpetas como logs/, state/, minio-data/, etc. 
⚠️ /var/lib/localstack/state está vacío, típico cuando esperas “persistencia snapshot” en Community. [bancolombi...epoint.com], [stackoverflow.com] [bancolombi...epoint.com] [bancolombi...epoint.com], [github.com] [bancolombi...epoint.com], [woshub.com], [bancolombi...epoint.com]
📌 Además: en tu WSL no tienes AWS CLI ni Java instalados (solo están en Windows). No es obligatorio para persistencia, pero sí te complica validar rápido. [bancolombi...epoint.com]

2) Qué condiciones deben cumplirse para que “los Excel no se pierdan”
Para que tus Excel (objetos S3) persistan incluso tras reinicios:
A) Los datos deben estar en un volumen (o bind mount) persistente
En LocalStack, el “volume directory” estándar es /var/lib/localstack. Ahí quedan logs, cache, state, etc. 
Si ese directorio no está montado a un volumen del host, al recrear contenedor se pierde. [github.com], [bancolombi...epoint.com] [github.com], [blog.richy.net]
B) El contenedor debe reiniciarse automáticamente después de reboot
Como tú estás en WSL-only, cuando apagas/enciendes:

WSL se apaga → Docker daemon se apaga → contenedores se detienen
al volver, necesitas que Docker daemon arranque y que el contenedor se levante.

Para esto normalmente se usa:

systemd en WSL para que el servicio docker inicie solo [docs.local...tack.cloud], [cmas.dev]
y restart: unless-stopped en docker compose (o equivalente).

C) Evitar guardar volúmenes en /mnt/c/... (cross-filesystem)
Docker recomienda bind-mount desde el filesystem Linux de WSL (p.ej. /home/...) por rendimiento y menos problemas; Microsoft también recomienda lo mismo. [cmas.dev], [learn.microsoft.com]

3) Lo más importante: “Persistencia S3 Free” — 3 métodos reales (elige el mejor)
Tu objetivo: subir Excel y que sobrevivan. Eso implica persistencia real de objetos, no solo “recrear bucket”.
✅ Método 1 (Mejor si quieres seguir con LocalStack S3): Volumen real a /var/lib/localstack
Como en tu contenedor ya existe /var/lib/localstack/minio-data (muy probable que allí se almacenen objetos S3 internamente), la persistencia práctica depende de montar /var/lib/localstack a un volumen. [bancolombi...epoint.com], [github.com]
Cómo validarlo (WSL):
Shelldocker inspect localstack --format '{{json .Mounts}}'Mostrar más líneas

Si ves un mount tipo Destination:/var/lib/localstack con Type=volume o bind mount, vas bien.

Qué mejora:

Los objetos deberían quedar en el volumen del host aunque reinicies el contenedor (si no borras el volumen). [blog.richy.net], [github.com]

Qué suele romperlo:

Usar docker compose down -v (borra volúmenes) [blog.richy.net]
Tener bind mount apuntando a rutas Windows (/mnt/c/...) que causan issues de IO/perm. [cmas.dev], [learn.microsoft.com]


✅ Método 2 (Persistencia “real” en Free): localstack-persist (imagen OSS)
Este método existe precisamente porque la persistencia oficial “snapshot-based” es Pro/plan pago; localstack-persist agrega persistencia en Community guardando en /persisted-data y restaurando al iniciar. [gist.github.com], [linkedin.com]
Qué mejora:

Persiste estado/recursos en Free sin depender del mecanismo Pro. [gist.github.com], [linkedin.com]

Qué considerar:

Es comunidad (no oficial), pero muy usado como workaround real. [gist.github.com], [linkedin.com]


✅ Método 3 (El más robusto si SOLO quieres almacenar Excel): MinIO (S3-compatible)
Si tu web solo necesita un S3 para guardar Excel, MinIO suele ser lo más estable porque es almacenamiento real y persiste con -v ...:/data. Hay ejemplos probados con Docker Compose + volumen minio_data:/data. [superuser.com], [learn.microsoft.com]
Qué mejora:

Persistencia real, simple, y muy confiable para objetos. [superuser.com], [learn.microsoft.com]
Menos sorpresas que LocalStack CE para persistencia.

Tradeoff:

No emula AWS completo; pero si solo usas S3, es perfecto. [superuser.com], [blog.miniasp.com]

👉 Para tu caso (Excel = objetos), el “mejor” suele ser MinIO si no dependes de otros servicios AWS. [superuser.com], [learn.microsoft.com]

4) Qué te falta instalar/configurar para que “no se te caiga” al apagar/encender
4.1 Habilitar systemd en WSL (para que Docker arranque solo)
Microsoft documenta que WSL soporta systemd y se habilita con /etc/wsl.conf ([boot] systemd=true) y luego wsl --shutdown. [docs.local...tack.cloud], [cmas.dev]
Por qué te sirve:

Después de un reinicio del PC, Docker daemon puede iniciar automáticamente dentro de WSL (según config) y luego el contenedor con restart policy vuelve. [docs.local...tack.cloud], [cmas.dev]

4.2 Asegurar política de reinicio del contenedor
Cuando no hay Docker Desktop, dependes del Docker Engine en WSL. Microsoft indica que para usar contenedores de Linux sin Desktop puedes instalar Docker directamente en WSL. 
En ese escenario, restart policy es importante. [rafaltruszkowski.com], [bancolombi...epoint.com]
Validación (WSL):
Shelldocker inspect localstack --format '{{.HostConfig.RestartPolicy.Name}}'Mostrar más líneas

Si sale vacío o “no”, no reinicia solo.

4.3 Guardar datos en filesystem Linux (no /mnt/c)
Esto reduce problemas raros con volúmenes/IO. Docker y Microsoft lo recomiendan para performance y estabilidad. [cmas.dev], [learn.microsoft.com]

5) Qué instalar (opcional pero MUY útil) para validar rápido
5.1 AWS CLI o awslocal dentro de WSL
Hoy en WSL te falta aws (y eso te impide verificar con 2 comandos si tus buckets/archivos existen). 
awslocal es el wrapper típico para LocalStack. [bancolombi...epoint.com] [devlo.io], [mymiller.name]
✅ Esto no “da persistencia”, pero te quita 80% del dolor de troubleshooting.

6) Validaciones concretas (para decidir cuál método es “mejor” en tu caso)
Prueba 1: ¿Hoy ya estás persistiendo en volumen o no?
En WSL:
Shelldocker inspect localstack --format '{{json .Mounts}}'docker exec -it localstack sh -lc 'du -sh /var/lib/localstack/* 2>/dev/null | sort -h'Mostrar más líneas

Si minio-data crece cuando subes Excel, ahí están cayendo los objetos. [bancolombi...epoint.com]

Prueba 2: ¿Sobrevive a reinicio del contenedor?

Subes un Excel (desde tu app)
docker restart localstack
verificas que el objeto siga (con AWS CLI/awslocal o desde app)

Prueba 3: ¿Sobrevive a reboot (apagado/encendido)?
Para esto lo crítico es:

systemd/docker autostart [docs.local...tack.cloud], [cmas.dev]
restart policy del contenedor
volumen persistente


7) Mi recomendación “mejor” según tu objetivo (Excel deben sobrevivir SIEMPRE)
Si tu web SOLO usa S3 para guardar Excel
✅ Mejor: MinIO con volumen /data → persistencia real. [superuser.com], [learn.microsoft.com]
Si necesitas seguir usando LocalStack porque tu app está “amarrada” a LocalStack endpoints
✅ Mejor: asegurar volumen persistente a /var/lib/localstack + restart policy + systemd. [github.com], [docs.local...tack.cloud], [blog.richy.net]
Si quieres persistencia real en LocalStack pero sin pagar
✅ Mejor: localstack-persist (OSS). [gist.github.com], [linkedin.com]
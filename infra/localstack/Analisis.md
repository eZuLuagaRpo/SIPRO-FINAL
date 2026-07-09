1) Lo que tu script hace bien ✅
1.1 Asegura que LocalStack esté accesible por el endpoint correcto
Usa http://localhost:4566/_localstack/health para validar estado. Esa es la ruta recomendada para health/servicios en LocalStack. [naiyerasif.com], [docs.local...tack.cloud]
1.2 Limita servicios a S3 (mejor para performance)
Estás intentando levantar solo S3. La config SERVICES=s3 existe y “si SERVICES está definido, solo se cargan esos servicios y el resto queda disabled”. 
Y además LocalStack permite prefijo LOCALSTACK_ para variables equivalentes (interoperabilidad), así que usar prefijos tipo LOCALSTACK_* sí es válido en Docker. [docs.local...tack.cloud]
1.3 Tiene “restart policy”
--restart unless-stopped es correcto para que el contenedor vuelva si Docker daemon está vivo y el host reinicia. (Peeero en WSL-only hay una condición extra; la explico más abajo). [dev.to], [learn.microsoft.com]

2) Problemas/restricciones reales que te pueden estar rompiendo la persistencia ⚠️
2.1 La persistencia “oficial” de LocalStack (snapshots) NO es parte de Free
Tu script dice “LocalStack Persist”, pero estás corriendo localstack/localstack:3 (Community). En Community, la persistencia snapshot-based no es fiable/garantizada, y esto se refleja en la realidad: mucha gente ve /var/lib/localstack/state vacío y que al reiniciar se pierde el estado. [gist.github.com], [bancolombi...epoint.com], [stackoverflow.com]
Clave: aunque montes /var/lib/localstack, eso no garantiza que LocalStack CE escriba/recupere estado como en Pro. [gist.github.com], [stackoverflow.com]

Esto es exactamente lo que tuviste: state/ vacío. En tu inventario anterior ya lo viste. [bancolombi...epoint.com]


2.2 S3 en Community puede guardar objetos en rutas que NO son /var/lib/localstack/state
En reportes reales, usuarios han encontrado que los objetos de S3 aparecen dentro del contenedor en rutas como /tmp/localstack-s3-storage, y no en /var/lib/localstack (o no persisten como esperaban). [stackoverflow.com]
Entonces, incluso montando /var/lib/localstack, podrías:

ver datos en el filesystem del contenedor,
pero no persistirlos donde tú crees,
o no re-cargarlos al iniciar.

Por eso tu script intenta “seed restore”: porque sabe que no siempre vuelve el contenido. Esa idea es válida.

2.3 Estás montando tu “volumen persistente” desde Windows (C:\s3mock2) → /mnt/c/...
Esto es MUY importante: estás bind-mounteando desde el filesystem Windows hacia Linux. En WSL/Docker esto puede generar:

I/O lento,
permisos raros,
comportamientos inconsistentes con archivos,
y problemas con scripts (ejecución, CRLF, etc.).

Docker recomienda explícitamente que los bind mounts vayan desde el filesystem Linux (WSL) y evitar /mnt/c/.... Microsoft dice lo mismo: si trabajas desde Linux, pon los archivos en /home/.... [docs.docker.com], [stackoverflow.com]
➡️ En tu script, esto impacta directamente:

-v '$volData' (tu “persistencia”)
-v '$volInit' (tus init scripts)

Si el bind mount viene de C:\... (Windows), es donde más se ve “a veces funciona, a veces no”.

2.4 El “Recreate” de tu script BORRA la carpeta persistente
Tu modo Recreate elimina todo dentro de C:\s3mock2 excepto init-scripts y seed.
Eso significa que si tus objetos de S3 están dentro de /var/lib/localstack (por ejemplo en minio-data, cache, etc.), con Recreate los puedes borrar sin querer.
O sea: “Recreate” por definición te borra persistencia (y luego toca restaurar desde seed).
Eso puede explicar por qué “no persiste”: porque un recreate limpia lo que sería el “disco”.

2.5 Tus init scripts pueden fallar por CRLF/permisos (Windows)
La comunidad reporta que scripts en init hooks fallan por line endings (CRLF) y permisos cuando se montan desde Windows; recrearlos con LF soluciona. [stackoverflow.com], [tutorialesdevops.es]
Tu script crea el directorio init-scripts, pero no veo que cree/garantice:

que el archivo .sh sea ejecutable,
que tenga LF,
que esté realmente presente.

LocalStack init hooks existen en /etc/localstack/init/<stage>.d (ready.d, start.d, etc.) y se ejecutan como parte del lifecycle. [tutorialesdevops.es], [codingtechroom.com]

3) El bloque “KeepAlive / portproxy” — por qué existe y por qué es frágil
Tu script implementa 3 cosas para “que Windows llegue a localhost:4566”:

Warmup TCP (10 intentos conectando a 127.0.0.1:4566)
Si falla, netsh portproxy como fallback
Luego un KeepAlive que pega a 4566 cada 10s

Esto coincide con lo que explica Internet: WSL2 tiene NAT y el “localhost forwarding” puede ser inconsistente; una solución clásica es netsh interface portproxy (pero el IP de WSL cambia y hay que reconfigurarlo). [stackoverflow.com], [jwstanly.com], [dev.to]
✅ Tu script está alineado con “la práctica común” (portproxy). 
⚠️ Pero es frágil porque: [iangge.github.io], [stackoverflow.com]

requiere permisos admin,
el IP de WSL cambia después de reboot,
y terminas “manteniendo vivo” el forwarding con tráfico artificial.

Alternativa más “limpia” (internet/Microsoft): usar modo de red “mirrored” (cuando está disponible), que reduce la necesidad de portproxy/complicaciones de NAT. [dev.to], [dev.to]

4) El gran problema para “apago/prendo servidor”: Docker en WSL no arranca solo si no tienes systemd
Tu script hace:
PowerShellRun-WslQuiet "service docker start ..."Mostrar más líneas
Eso significa que dependes de ejecutar el script para levantar Docker daemon.
Si apagas/enciendes el PC y nadie corre este script, Docker daemon no inicia, por ende:

contenedores no corren,
--restart unless-stopped no sirve,
y tu S3 “se cae”.

Microsoft documenta que puedes habilitar systemd en WSL (/etc/wsl.conf, [boot] systemd=true) para manejar servicios como en Linux real. [dev.to], [learn.microsoft.com]
✅ Esto sí es una mejora real para tu caso “apago/prendo”.

5) ¿Qué dice internet sobre “lo mejor” para tu caso (Excel que deben persistir)?
Opción A (seguir con LocalStack CE): Persistencia por volumen + evitar /mnt/c

Monta el volumen en /var/lib/localstack como recomienda el layout oficial. [docs.local...tack.cloud], [codingtechroom.com]
Pero hazlo desde filesystem Linux (WSL) o mejor con “named volume”, porque Docker recomienda evitar /mnt/c. [docs.docker.com], [stackoverflow.com]

Opción B (persistencia real en Free): localstack-persist (proyecto OSS)
Existe exactamente para “persistencia en Community”, guardando en /persisted-data y restaurando al iniciar. [dev.to], [woshub.com]

Tu script se llama “Persist”, pero hoy corre localstack/localstack:3, no esa imagen. Eso es una discrepancia real.

Opción C (si SOLO quieres S3): MinIO (S3-compatible)
La comunidad suele recomendar MinIO para persistencia “real” de objetos en local: se levanta con Docker y persiste con un volumen en /data. [learn.microsoft.com], [gist.github.com]
Para “subo Excel y quiero que quede aunque reinicie”, MinIO suele ser lo más estable si no dependes de otros servicios AWS.

6) Qué puedes mejorar / instalar (sin “solucionarte”, solo recomendaciones concretas)
(1) Cambiar tu “persistencia” a un almacenamiento estable (no /mnt/c)

Cambia $winDir a una ruta dentro de WSL (por ejemplo /home/<user>/s3mock2) o usa un named volume en Docker.
Esto reduce los problemas de I/O y permisos descritos por Docker/Microsoft. [docs.docker.com], [stackoverflow.com]

(2) Habilitar systemd en WSL
Para que Docker daemon arranque y el contenedor vuelva solo tras reboot. [dev.to], [learn.microsoft.com]
(3) Si te quedas con init hooks: cuidar LF + permisos
Porque en Windows es típico que el script falle por CRLF/permisos. [stackoverflow.com], [tutorialesdevops.es]
(4) Instalar AWS CLI dentro de WSL (para validar rápido)
No es obligatorio, pero reduce muchísimo el tiempo de diagnosticar si el Excel “está o no está”. (Hoy tu WSL no lo tiene). [bancolombi...epoint.com]
(5) Replantear el “KeepAlive/portproxy”
Es un workaround normal en WSL2 NAT, pero frágil y dependiente de IP; la propia comunidad usa netsh portproxy como solución pero reconoce que el IP cambia y hay que automatizar. [jwstanly.com], [stackoverflow.com]

7) Diagnóstico rápido de tu script (por qué “a veces no funciona”)

Persistencia depende de un bind mount en Windows (C:\s3mock2) → esto es el tipo de montaje que Docker y Microsoft recomiendan evitar para I/O estable. [docs.docker.com], [stackoverflow.com]
Estás en LocalStack Community, donde “persistencia de estado” no es garantizada y hay reportes de que S3 no persiste como esperaban (datos en /tmp/...). [stackoverflow.com], [gist.github.com]
Tu modo Recreate limpia la carpeta persistente → puede “borrar” los objetos y obligar a seed restore.
Tu conectividad Windows→WSL depende de port forwarding (auto-forward) y portproxy (clásico en WSL2 NAT), que es inestable por IP variable. [stackoverflow.com], [jwstanly.com], [dev.to]
Docker daemon no necesariamente arranca en reboot sin systemd → contenedor no vuelve solo. [dev.to], [learn.microsoft.com]


Recomendación final (práctica) para tu caso “Excel deben sobrevivir” ✅
Si tu app solo usa S3 para guardar Excel y lo importante es que NO se pierdan nunca:
✅ Mejor camino (más estable): MinIO con volumen persistente
Porque es almacenamiento real y persistente con volumen /data. [learn.microsoft.com], [gist.github.com]
Si necesitas mantener LocalStack sí o sí:

usa almacenamiento persistente que no sea /mnt/c (o usa named volume) [docs.docker.com], [docs.local...tack.cloud]
habilita systemd para auto-arranque [dev.to], [learn.microsoft.com]
y considera localstack-persist si requieres persistencia real sin pagar
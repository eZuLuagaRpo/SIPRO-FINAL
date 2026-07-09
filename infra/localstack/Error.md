1) Lo que Internet confirma sobre tu script (y por qué esa lógica existe)
1.1 LocalStack: endpoint correcto de health y filtro de servicios

LocalStack documenta que el endpoint de salud para ver servicios es /_localstack/health, y ahí se listan servicios válidos (como s3). [docs.local...tack.cloud], [deepwiki.com]
También documenta la variable SERVICES como el mecanismo para cargar solo ciertos servicios; si está definida, el resto se deshabilita. [deepwiki.com], [docs.local...tack.cloud]
Además, LocalStack explica que en Docker puedes prefijar variables con LOCALSTACK_ para interoperabilidad; por ejemplo LOCALSTACK_PERSISTENCE=1 equivale a PERSISTENCE=1. (Eso sugiere que LOCALSTACK_SERVICES puede funcionar como alias de SERVICES, aunque el nombre “canónico” en docs sea SERVICES). [deepwiki.com], [docs.local...tack.cloud]

✅ Conclusión: tu uso de /_localstack/health y el intento de limitar a S3 está alineado con documentación. [docs.local...tack.cloud], [deepwiki.com]

1.2 WSL2: “localhost forwarding” es real… pero puede ser inconsistente
Microsoft describe que WSL2 usa NAT por defecto, y que se puede acceder desde Windows a servicios dentro de WSL usando localhost, pero advierte que hay consideraciones de red y recomienda probar Mirrored mode para mejoras. [learn.microsoft.com], [learn.microsoft.com]
En comunidad se reporta lo que te pasa: “localhost refused to connect” de forma intermitente, donde a veces funciona y otras no, incluso siguiendo los mismos pasos. [stackoverflow.com], [technofossy.com]
✅ Conclusión: tu “warm-up TCP” + “keepalive” tiene sentido porque hay evidencia real de que el forwarding puede “romperse” o volverse inconsistente. [technofossy.com], [learn.microsoft.com]

1.3 netsh portproxy: es el workaround “clásico” (pero con desventajas conocidas)
Muchos artículos/answers explican que, por NAT en WSL2, a veces necesitas netsh interface portproxy ... para enrutar tráfico del host a la IP interna de WSL. [iangge.github.io], [stackoverflow.com]
También se recalca la “trampa” típica: la IP de WSL cambia y toca reconfigurar/automatizar (o usar alternativas). [stackoverflow.com], [tech2geek.net]
✅ Conclusión: tu fallback con netsh portproxy coincide con lo que se recomienda en internet para WSL2 NAT. [iangge.github.io], [stackoverflow.com]

1.4 Mirrored networking mode: alternativa más “estable” para varios casos
Hay guías recientes y respuestas técnicas que recomiendan networkingMode=mirrored para simplificar conectividad y evitar algunos problemas de NAT/forwarding, especialmente para exponer servicios. [hy2k.dev], [superuser.com]
Microsoft también menciona “Mirrored mode” como recomendación moderna para mejoras. [learn.microsoft.com], [learn.microsoft.com]
✅ Conclusión: cuando dices “¿tengo que mantener WSL activo?”, internet sugiere que el problema es más bien el modo NAT/forwarding, y mirrored es una vía para reducir esa fragilidad. [learn.microsoft.com], [hy2k.dev]

2) Entonces… ¿por qué te sigue fallando aunque el script “arranque bien”?
Tu log muestra un patrón muy importante:

Al iniciar: localhost:4566 alcanzable (intento 1/5) ✅
Más tarde, al subir archivo: “Warm-up TCP no logró abrir localhost:4566…” y luego Connection refused ❌

Eso es exactamente el tipo de falla “intermitente” descrita en WSL2: el forwarding puede funcionar inicialmente y luego dejar de funcionar (por cambios de red, suspensión/idle, o inconsistencias IPv4/IPv6). [technofossy.com], [stackoverflow.com]
2.1 “Connection refused” indica que en ese momento no hay nada accesible en localhost:4566 desde el contexto del backend
Ese error, en general, significa “no hay proceso escuchando en ese host/puerto desde tu contexto de red”. En LocalStack y Docker esto aparece cuando:

el puerto no está expuesto,
el contenedor no es alcanzable desde donde se ejecuta el cliente,
o el forwarding/route dejó de existir. [docs.local...tack.cloud], [technofossy.com]

Como tu backend corre en Windows, depende totalmente de que Windows→WSL mantenga el acceso a localhost:4566, y justo eso es lo que se reporta como frágil en WSL2 NAT. [learn.microsoft.com], [stackoverflow.com]
2.2 Tu “KeepAlive” intenta mitigar, pero no evita las causas típicas reportadas
La comunidad reporta que los problemas pueden dispararse por:

hibernación/fast startup,
inconsistencias de stack de red,
conflictos,
IPv4 vs IPv6,
o que el forwarding “simplemente se rompe”. [technofossy.com], [stackoverflow.com]

Tu keepalive es un workaround, pero si el mecanismo de forwarding se cae, el keepalive puede no ser suficiente (y tú mismo lo ves: “Warm-up TCP no logró abrir…”). [technofossy.com], [learn.microsoft.com]
2.3 run-fullstack.bat no “rompe” LocalStack por sí mismo, pero sí crea una condición de carrera
Tu .bat:

llama el script
luego inicia backend
espera 15s
luego frontend

Aunque tu script espera health “estable”, el problema reportado en WSL2 es que la conectividad por localhost puede ser inconsistente en el tiempo, no solo al arranque. O sea: 15s después puede estar bien, y 1 minuto después no. Eso coincide con reportes de “funciona a veces, luego deja de funcionar”. [stackoverflow.com], [technofossy.com]

3) Qué revisar / mejorar según lo que dice internet (sin tocar tu código exacto)
3.1 Usar el nombre canónico de configuración: SERVICES (y verificar que realmente limitó S3)
La doc oficial resalta SERVICES como variable para limitar servicios, y que puedes validar por /_localstack/health. [deepwiki.com], [docs.local...tack.cloud]
Tu script usa LOCALSTACK_SERVICES=s3. Como LocalStack dice que LOCALSTACK_ puede prefijar variables, debería funcionar, pero si quieres alinear con lo “más documentado”, el nombre “SERVICES” es el que aparece de forma explícita. [deepwiki.com], [docs.local...tack.cloud]

Además, en threads de issues se discuten cambios históricos y confusión con SERVICES en versiones viejas, así que es importante validar con health en tu versión actual. [github.com], [deepwiki.com]

3.2 Evitar bind mounts desde /mnt/c para estabilidad/performance
Aunque tu pregunta aquí es sobre red, hay un tema grande que internet repite: bind mounts desde Windows (/mnt/c/...) tienden a dar problemas; Docker recomienda guardar datos montados en el filesystem Linux. [stackoverflow.com], [refactorfirst.com]
Esto afecta persistencia y a veces estabilidad del contenedor, especialmente si scripts init/seed están montados desde Windows (CRLF/permisos). La comunidad reporta fallos de init scripts por CRLF. [stackoverflow.com], [stackoverflow.com]
3.3 Para el problema exacto (localhost intermitente): considerar mirrored networking mode
Microsoft recomienda probar Mirrored mode para mejoras en networking y menor necesidad de “trucos” de IP/portproxy. 
Hay guías paso a paso (y respuestas técnicas) donde mirrored mode reduce la dependencia de portproxy/forwarding frágil. [learn.microsoft.com], [learn.microsoft.com] [hy2k.dev], [superuser.com]
3.4 Portproxy: si lo usas, hay que aceptar su “costo operativo”
Los guides de portproxy dicen claramente:

necesitas admin,
hay que abrir firewall,
y si cambia la IP WSL, toca reconfigurar. [tech2geek.net], [stackoverflow.com]

Tu script ya lo intenta, pero el hecho de que igual te falle indica que la conectividad no está quedando “fija” en el tiempo (lo cual también está reportado). [technofossy.com], [stackoverflow.com]

4) ¿Tienes que “iniciar WSL/Ubuntu” a mano? ¿O mantenerlo activo?
Según internet, el problema no es “abrir Ubuntu en una terminal”, sino:

la arquitectura NAT,
el estado de la VM WSL,
y el forwarding/portproxy.

En otras palabras: abrir Ubuntu “ayuda” solo porque mantiene la VM viva, pero no es la solución estructural; lo estructural es red (mirrored/portproxy) y servicios (systemd) si quieres autostart. [learn.microsoft.com], [learn.microsoft.com], [superuser.com]
Además, Microsoft explica que cambios de config (.wslconfig, wsl.conf) requieren reiniciar WSL (wsl --shutdown) y esperar para aplicar (“regla de 8 segundos”). [learn.microsoft.com], [woshub.com]

5) “S3 persistente Free” y lo que tu script realmente está logrando (y lo que no)
Tu script habla de “Persist”, pero al usar LocalStack Community, la persistencia “snapshot” no es un feature garantizado; la doc de filesystem layout dice que state/ contiene estado si persistence está habilitada, pero en Community frecuentemente no aplica como esperas. [tutorialpedia.org], [stackoverflow.com]
Por eso tu estrategia real es:

“persistencia” como volumen + “seed restore” (reconstrucción), no snapshot.
Eso es coherente con el enfoque de init hooks y seeds. [tutorialpedia.org], [labex.io]


6) Lo que necesito para buscar “resultados aún más parecidos a tu caso”
Ya encontré fuentes que explican:

forwarding intermitente WSL2,
portproxy como workaround,
mirrored mode,
configuración LocalStack.

Si quieres que lo refine al 100% (casos idénticos con LocalStack:4566 y Spring Boot en Windows), dime solo esto:

¿Tu backend (Spring Boot) corre en Windows (parece que sí por el path C:\...) o lo has corrido dentro de WSL alguna vez?
¿Tu Windows entra en suspensión/hibernación durante la jornada? (muchos reportes vinculan el fallo de localhost forwarding con eso). [technofossy.com], [stackoverflow.com]

Con esas respuestas hago otra búsqueda ultra dirigida (“Spring Boot AWS SDK LocalStack WSL2 localhost forwarding breaks”) y te traigo 10-15 casos con causas y soluciones reportadas (sin tocar tu implementación).

En resumen (lo que “dice internet” en una línea)
Tu síntoma coincide con un problema ampliamente reportado: WSL2 NAT + localhost forwarding puede ser intermitente, y los workarounds típicos son portproxy (con mantenimiento por IP) o mirrored networking mode para reducir la fragilidad
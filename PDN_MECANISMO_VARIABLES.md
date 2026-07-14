# Mecanismo de configuración para PDN: SIPRO vs. el otro proyecto ya desplegado

Este documento explica cómo cada proyecto resuelve "¿de dónde saca el código los valores reales al desplegar?", qué implicaría adoptar el mecanismo del otro proyecto, y qué decisiones y preguntas hay que cerrar con el equipo de provisión antes de salir a PDN.

---

## 1. Cómo lo hace SIPRO hoy: variables de entorno

SIPRO usa el mecanismo estándar de Spring Boot: **se compila el programa una sola vez, y el valor real se le entrega al momento de arrancar**, no antes.

En los archivos `.yml` (`application.yml`, `application-dev.yml`, `application-qa.yml`, `application-prd.yml`), cada valor configurable se ve así:

```yaml
lz:
  host: ${LZ_HOST:impala.bancolombia.corp}
```

Esto se lee: *"deja un espacio para una variable llamada `LZ_HOST`; si al arrancar el programa el sistema operativo tiene definida esa variable, úsala; si no existe ninguna, usa `impala.bancolombia.corp` por defecto."*

El JAR que se compila **es siempre el mismo**, sin importar si va a correr en desarrollo, QA o PDN. Lo único que cambia entre ambientes es qué variables de entorno le configuras al contenedor/servidor antes de arrancarlo (en Kubernetes esto se hace con ConfigMap/Secret — ver `infra/charts/validation-service/` — o en Azure App Service con "Application Settings").

**Ejemplo concreto:** si en PDN defines la variable de entorno `LZ_HOST=impala.bancolombia.corp` antes de arrancar el contenedor, el programa la lee en ese momento y se conecta ahí. Si no la defines, usa el valor por defecto que ya viene escrito en el `.yml` (que en este caso coincide con el valor real, así que funcionaría igual).

---

## 2. Cómo lo hace el otro proyecto: Replace Tokens

El otro proyecto (el ya desplegado, que están usando de referencia) resuelve el mismo problema de una forma distinta: **construye un JAR distinto para cada ambiente, con los valores ya escritos dentro, antes de compilar.**

En vez de un `.yml` con `${VAR:default}`, tienen clases Java como `LZConnection.java` con placeholders escritos directamente en el código:

```java
return LZSecret.builder()
    .username("#{lzusr}#")
    .password("#{lzpss}#")
    .host("#{lzhost}#")
    .build();
```

El pipeline de Azure DevOps usa una tarea llamada **"Replace Tokens"**, que antes de compilar:

1. Abre el archivo como texto plano (le da igual si es `.java`, `.yml`, `.properties`, lo que sea).
2. Busca el texto literal `#{lzhost}#` y lo reemplaza por el valor real, por ejemplo `impala.bancolombia.corp`, sacado de un Variable Group / Library de Azure DevOps.
3. El archivo queda así, con el valor ya fijo: `.host("impala.bancolombia.corp")`.
4. **Recién ahí** se compila.

El JAR final ya no tiene ningún `#{...}#` en ningún lado — el valor quedó horneado dentro del artefacto compilado, para siempre, para ese ambiente específico. Si necesitas el mismo programa para otro ambiente, hay que repetir el reemplazo con otros valores y compilar de nuevo — es un JAR distinto por ambiente, no uno solo reutilizado.

Este proyecto también tiene un `AwsConfig.java` para SES con, muy probablemente, el mismo patrón de tokens — es su forma general de resolver *cualquier* configuración, no algo exclusivo de la LZ.

---

## 3. ¿De dónde saca Replace Tokens los valores reales? Quién escribe qué

Este es el punto que más se presta a confusión, así que vale la pena dejarlo aparte, bien explícito. Replace Tokens **no inventa ni guarda ningún valor por sí mismo** — solo conecta dos cosas que ya tienen que existir de antemano, escritas por dos equipos distintos:


| Quién                         | Qué escribe                                                                                                                                                                                                                 | Dónde queda                                                |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| **SIPRO (nosotros)**          | La llave/placeholder, por ejemplo `#{LZ_HOST}#`, en el archivo de configuración — en el lugar donde hoy escribimos `${LZ_HOST:...}`                                                                                         | En nuestro propio repositorio de código                    |
| **Equipo de provisión**       | El valor real correspondiente a ese mismo nombre, por ejemplo "`LZ_HOST` = `impala.bancolombia.corp`", dentro de un **Variable Group / Library** de Azure DevOps (a veces conectado a Azure Key Vault para datos sensibles) | En Azure DevOps — nunca tocan nuestro código               |
| **La tarea "Replace Tokens"** | Nada propio — durante el pipeline, abre nuestro archivo, encuentra `#{LZ_HOST}#`, busca en el Variable Group de ellos algo llamado `LZ_HOST`, y si lo encuentra, borra el placeholder y escribe el valor real en su lugar   | Corre automático dentro del pipeline, nadie lo hace a mano |


**Las llaves (los nombres) las decidimos nosotros, al escribir el placeholder en nuestro código.** El equipo de provisión no las "tiene" de antemano — nosotros se las tenemos que comunicar (por ejemplo, entregándoles la lista de `PDN_VARIABLES.md`), para que ellos sepan con qué nombres exactos deben crear sus propias entradas en el Variable Group.

Si cualquiera de las dos partes falta — nosotros no escribimos la llave, o ellos no tienen un valor con ese nombre exacto — Replace Tokens no tiene nada que reemplazar (deja el archivo igual, o falla, según cómo esté configurada la tarea). **Ambas partes tienen que hacer su tarea de antemano, por separado, para que el mecanismo funcione el día del build.**

Importante: esto significa que, si se adopta este mecanismo, **el equipo de provisión también necesita armar su propia lista** (un Variable Group, o varios — uno por ambiente) con el valor real de cada una de las variables que hoy tenemos documentadas en `PDN_VARIABLES.md`. No es trabajo que se resuelva solo por definir las llaves de nuestro lado — ellos tienen una tarea equivalente pendiente del suyo.

---

## 4. Comparación directa


|                                                            | SIPRO hoy (variables de entorno)                     | El otro proyecto (Replace Tokens)                                                |
| ---------------------------------------------------------- | ---------------------------------------------------- | -------------------------------------------------------------------------------- |
| ¿Cuándo se define el valor real?                           | Al arrancar el programa                              | Antes de compilarlo                                                              |
| ¿Cuántos JAR distintos hay?                                | Uno solo, reutilizado en dev/qa/prd                  | Uno por cada ambiente                                                            |
| ¿Dónde vive el placeholder?                                | En archivos `.yml` (`${VAR:default}`)                | En cualquier archivo de texto, incluyendo `.java` (`#{VAR}#`)                    |
| ¿El código necesita saber del mecanismo?                   | Sí — Spring Boot interpreta `${...}` de forma nativa | No — el reemplazo pasa *antes* de que exista el programa compilado               |
| ¿Qué tan fácil es tener un default seguro para desarrollo? | Muy fácil (`${VAR:valor-por-defecto}`)               | No aplica — el token no tiene default, siempre necesita que alguien lo reemplace |


---

## 5. ¿Replace Tokens sería solo para la LZ, o para todas las variables?

**Depende, pero técnicamente podría ser para cualquier cosa.** Replace Tokens no sabe ni le importa qué está reemplazando — solo busca texto y lo cambia. Si el equipo de provisión adopta este mecanismo como estándar, en teoría podrían tokenizar **todas** las variables de SIPRO (CORS, S3, correo, base de datos, LZ, todo), no solo la LZ.

Lo que sí importa es que **es una decisión de alcance que hay que definir explícitamente**, no algo que se resuelve solo. Las opciones son:

- **Todo con Replace Tokens** — se cambiaría `${VAR:default}` por `#{VAR}#` en los 4 archivos `.yml` completos.
- **Solo la LZ con Replace Tokens, el resto con variables de entorno** — mezcla rara, pero posible si por alguna razón puntual la LZ necesita un tratamiento especial (por ejemplo, si el problema real es que el contenedor no puede llamar a AWS Secrets Manager, ver sección 6).
- **Nada con Replace Tokens** — si el pipeline puede simplemente definir variables de entorno reales, SIPRO sigue exactamente como está, sin ningún cambio.

---

## 6. Qué habría que hacer en cada escenario

### Escenario A: el pipeline puede definir variables de entorno reales

**No hay que cambiar nada.** SIPRO ya está listo. Solo hace falta que, al desplegar, alguien configure las variables de entorno reales (la lista completa está en `PDN_VARIABLES.md`) — vía Application Settings de Azure App Service, ConfigMap/Secret de Kubernetes, o el mecanismo equivalente de donde sea que corra el contenedor.

### Escenario B: el pipeline exige Replace Tokens como estándar obligatorio

Cambios necesarios, acotados:

1. En cada `.yml` (`application.yml`, `application-prd.yml`, etc.), cambiar la sintaxis de `${VAR:default}` a `#{VAR}#` para las variables que el equipo de provisión decida tokenizar.
2. **No hace falta crear clases Java nuevas** como `LZConnection.java` o `AwsConfig.java` — el código Java de SIPRO (`LzJdbcService`, `S3Config`, etc.) sigue exactamente igual, porque solo lee la propiedad ya resuelta (`lz.host`, `app.storage.s3.bucket`, etc.), sin importarle si el valor llegó por variable de entorno o por reemplazo de texto.
3. Implicación operativa a tener en cuenta: si se tokeniza toda la configuración, habría que compilar un JAR distinto por ambiente (dev/qa/prd), en vez del JAR único que se usa hoy — esto cambia cómo se arma el pipeline de build, aunque no cambia el código de la aplicación.

---

## 7. Ya existe un archivo plantilla listo — pero "entregarlo" no alcanza por sí solo

Ya dejamos preparado, por si acaso, el archivo:

```
backend/services/validation-service/src/main/resources/application-prd-replacetokens.yml
```

Es una copia de `application-prd.yml`, pero con la sintaxis `#{VAR}#` en vez de `${VAR:default}` — mismos nombres de variable que ya están en `PDN_VARIABLES.md`. Hoy está **inerte**: Spring Boot no lo carga a menos que alguien active explícitamente el perfil `prd-replacetokens`, así que no afecta en nada lo que ya funciona. De paso, tokeniza `lz.dev.user`/`lz.dev.password` directamente (en vez de pasar por AWS Secrets Manager), lo que evita por completo el bug de la sección 7 sin tener que arreglarlo.

**Pero ojo: no es tan simple como "les paso el archivo y ya funciona".** Para que este archivo realmente sirva de algo, tienen que estar **las 4 cosas siguientes** al mismo tiempo — si falta una sola, no funciona:

1. **Su tarea de Replace Tokens tiene que apuntar a este archivo específico.** La tarea escanea solo los archivos que le digan que escanee (un patrón configurado, por ejemplo `**/*.java` hoy). Si no agregan `application-prd-replacetokens.yml` a ese patrón, la tarea corre pero nunca toca nuestro archivo.
2. **Algo tiene que definir** `SPRING_PROFILES_ACTIVE=prd-replacetokens` **al arrancar el programa (pipeline releas).** Aunque Replace Tokens llene perfecto los valores dentro del archivo, Spring Boot no lo va a usar a menos que se le diga explícitamente que cargue ese perfil. Este dato en particular casi seguro **sigue siendo una variable de entorno real** (no algo que reemplace Replace Tokens) — es el interruptor maestro que normalmente configura la plataforma de despliegue, incluso en proyectos que usan Replace Tokens para todo lo demás. Si por accidente queda `SPRING_PROFILES_ACTIVE=prd` (el de siempre), se cargaría el `application-prd.yml` viejo, que espera variables de entorno y no tokens.
3. **El Variable Group de ellos tiene que estar lleno de verdad**, con un valor real para cada nombre que aparece en el archivo (`LZ_USER`, `JDBC_URL`, etc. — ver sección 3).
4. **Los delimitadores de su tarea Replace Tokens tienen que coincidir con `#{...}#`.** Es prácticamente seguro que sí, porque es lo que ya vimos en `LZConnection.java`, pero vale la pena confirmarlo — es un valor configurable dentro de la tarea, no viene fijo por defecto en todas las instalaciones.

Una vez esas 4 cosas estén resueltas del lado de ellos, ahí sí — `gradlew build` no necesita ningún ajuste adicional de nuestra parte. El código Java sigue exactamente igual; desde el punto de vista de Gradle, es un `.yml` normal como cualquier otro.

---

## 8. Un hallazgo aparte, encontrado en el camino: la conexión a AWS Secrets Manager de la LZ está rota hoy

Independientemente de qué se decida sobre Replace Tokens, encontré que `LzSecretsService.java` **no puede conectarse al Secrets Manager real de AWS tal como está escrito hoy**:

- Arma la petición HTTP a mano, con una firma de autenticación **hardcodeada y falsa** (`Signature=dummy`), en vez de calcular la firma real que AWS exige (AWS Signature V4).
- El proyecto **no tiene** la librería oficial de AWS para Secrets Manager como dependencia (`build.gradle` solo incluye la de S3).
- Esto funciona hoy en desarrollo porque siempre se prueba contra LocalStack (que no valida firmas de verdad) o contra el modo de credenciales directas (`LZ_DEV_USER`/`LZ_DEV_PASSWORD`), nunca contra el Secrets Manager real.

Esto es un bug real que hay que arreglar antes de PDN, sin importar si el contenedor termina en AWS o en Azure, y sin importar qué se decida sobre Replace Tokens. Si el contenedor se queda en AWS, la solución es agregar la librería real del SDK y usar el cliente oficial (trabajo acotado, no una reescritura grande). Si en cambio se decide que la LZ se resuelve por Replace Tokens (como en el otro proyecto), este problema se evita por completo, porque ya no haría falta llamar a Secrets Manager desde el código.

---

## 9. Lo que reveló el pipeline real que compartió el equipo de provisión

Nos compartieron los pasos de su agente de build, tal como los tienen hoy para el proyecto de referencia:

```
Use Java 17 → Replace tokens → gradlew build → Archive SIPRO_BACKEND
→ Artifactory Generic Upload → Publish Artifact: Artifact Backend
→ Publish Artifact: Route → Publish Artifact: test
```

Esto confirma y agrega varias cosas:

- **"Replace tokens" ya es un paso real, no una posibilidad teórica** — y corre **antes** de `gradlew build`. Esto confirma el Escenario B: su modelo es "hornear los valores antes de compilar", no "compilar una vez y configurar después".
- **"Archive SIPRO_BACKEND"** — el pipeline ya está nombrado específicamente para este proyecto, no es solo una plantilla genérica de referencia.
- **"Artifactory Generic Upload"** — usan JFrog Artifactory como repositorio de artefactos compilados. Dato útil, pero no dice nada sobre si el contenedor corre en AWS o Azure.
- **"Publish Artifact: Route"** — el nombre sugiere fuertemente que están generando/publicando una configuración de enrutamiento (gateway o reverse-proxy). Podría responder directamente la pregunta de si frontend y backend van a compartir dominio — vale la pena preguntar puntualmente qué contiene este artefacto.

### Importante: esto es solo la mitad de la historia — falta el pipeline de Release

Lo que compartieron es un pipeline de **Build**: compila, empaqueta y publica el artefacto — nunca instala nada en un servidor real. Es la fábrica que arma el paquete y lo deja listo, no el camión que lo entrega. El pipeline de **Release** es la otra mitad: toma ese artefacto ya armado y lo despliega de verdad en algún ambiente (dev, qa, prd), generalmente con etapas y aprobaciones intermedias. **Es en el Release donde se resolvería dónde corre realmente el contenedor (AWS o Azure)**, y si además de Replace Tokens también se configuran variables de entorno reales en ese momento. Sin ver el Release, no podemos confirmar del todo el panorama completo.

### Un riesgo operativo si este pipeline se usara hoy tal cual, sin ajustar nada de SIPRO antes

El paso "Replace tokens" busca el patrón `#{...}#`. Los `.yml` de SIPRO hoy usan `${VAR:default}` — **no hay ningún `#{...}#` que Replace Tokens pueda encontrar**. Eso no haría fallar el build (simplemente no reemplazaría nada), pero el JAR se compilaría y subiría a Artifactory usando los valores por defecto de Spring, muchos de ellos vacíos a propósito en `application-prd.yml` (CORS, S3, etc.). Si nada más adelante (en el Release) configura variables de entorno reales, el resultado sería un despliegue que **arranca sin errores pero queda mal configurado en silencio**. Por eso es importante resolver el mecanismo antes de correr este pipeline contra SIPRO de verdad.

---

## 10. Decisiones que hay que cerrar antes de seguir

1. **¿Dónde corre el contenedor de SIPRO en producción?** ¿AWS (ECS/EKS/EC2) o Azure (App Service/AKS)? — determina si llamar a AWS Secrets Manager desde el código tiene sentido o es fricción innecesaria.
2. **¿S3, SES y Secrets Manager siguen siendo servicios reales de AWS, o se reemplazan por sus equivalentes de Azure** (Blob Storage, Azure Communication/otro servicio de correo, Key Vault)? — si cambian a Azure, hay trabajo de código real en el storage (no es solo configuración).
3. **¿El pipeline de despliegue puede definir variables de entorno reales, o su único mecanismo estándar es Replace Tokens?**
4. **Si es Replace Tokens: ¿para todas las variables, o solo para un subconjunto (por ejemplo, solo LZ)?**
5. **¿El equipo de provisión ya tiene armado (o va a armar) su propio Variable Group con los valores reales de cada nombre que usemos?** — sin esto, aunque nosotros escribamos las llaves perfectas, no hay ningún valor real que Replace Tokens pueda poner ahí.

---

## 11. Preguntas concretas para llevar al equipo de provisión

> **1.** "Nuestro proyecto ya usa variables de entorno estándar de Spring Boot para su configuración por ambiente (no necesita que le reescriban archivos de código antes de compilar). ¿Su pipeline puede simplemente definir esas variables de entorno en el despliegue (Application Settings / variables de contenedor), o su estándar obligatorio es Replace Tokens sobre archivos?"

> **2.** "¿Dónde va a correr exactamente el contenedor de SIPRO — un servicio de cómputo de AWS o uno de Azure?"

> **3.** "Si el contenedor corre en Azure: ¿la idea es mantener S3 y AWS Secrets Manager reales de todas formas (arquitectura multi-nube), o migrar esas piezas a sus equivalentes nativos de Azure?"

> **4.** "Si terminamos usando Replace Tokens: ¿aplicaría solo para la conexión a la LZ, o para toda la configuración del proyecto (base de datos, S3, CORS, correo)?"

> **5.** "¿Nos pueden mostrar también el pipeline de Release (no solo el de Build)? Necesito ver dónde se despliega realmente y si ahí también se configuran variables de entorno."

> **6.** "¿Qué contiene exactamente el artefacto 'Route' que publican? ¿Es la configuración de un gateway que rutea entre frontend y backend?"

> **7.** "Si usamos Replace Tokens: ¿ya tienen (o van a crear) un Variable Group con los valores reales de cada variable? Les vamos a entregar la lista completa de nombres que necesitamos (ver `PDN_VARIABLES.md`), para que ustedes carguen ahí el valor real de cada uno."

Con las respuestas a estas 7 preguntas, queda completamente definido qué (si acaso algo) hay que ajustar en el código antes de que el equipo de provisión pueda desplegar SIPRO con confianza.
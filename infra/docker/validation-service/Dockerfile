# Dockerfile base para empaquetar validation-service como imagen autocontenida.
# Mantiene una separacion simple entre build y runtime para no llevar Gradle al contenedor final.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# El contexto copia solo backend porque el artefacto publicado por esta imagen es el JAR del servicio Spring Boot.
COPY backend/ .

# bootJar resuelve el subproyecto validation-service y deja el artefacto listo en build/libs.
RUN ./gradlew :services:validation-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

# Se copia el JAR generado en la etapa anterior sin arrastrar el resto del workspace de compilacion.
COPY --from=build /workspace/services/validation-service/build/libs/*.jar /app/app.jar

# El servicio HTTP de Spring Boot expone 8080 tanto para ejecucion local como para Kubernetes.
EXPOSE 8080

# La imagen arranca en modo simple con java -jar; variables y secretos se inyectan en runtime.
ENTRYPOINT ["java","-jar","/app/app.jar"]

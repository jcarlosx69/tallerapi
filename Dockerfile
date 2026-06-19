# =============================================================
# Dockerfile - TallerAPI
# Fase 3 — Dockerización
#
# Build multi-stage: una etapa compila el proyecto con Maven + JDK 21
# completos; la etapa final solo contiene el JRE 21 (sin compilador,
# sin Maven, sin código fuente) más el .jar ya construido. El
# resultado es una imagen final mucho más ligera que si copiáramos
# directamente sobre una imagen con el JDK completo.
#
# Justificación detallada y tamaños aproximados en
# fase3-notas-tecnicas.md.
# =============================================================

# ---------- Etapa 1: build ----------
# Imagen oficial de Maven con JDK 21 ya instalado (Eclipse Temurin).
# Solo se usa para compilar; no llega a la imagen final.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copiamos primero SOLO el pom.xml. Mientras no cambien las
# dependencias declaradas en él, Docker reutiliza la cache de esta
# capa (incluida la descarga de dependencias) en builds posteriores,
# aunque cambiemos el código en src/. Si copiáramos todo de golpe,
# cualquier cambio en una clase Java invalidaría también la descarga
# de dependencias.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Ahora sí copiamos el código fuente y empaquetamos el .jar ejecutable.
COPY src ./src

# Omitimos los tests aquí a propósito: los 94 tests del proyecto
# (47 unitarios + 47 de integración) ya se ejecutan como puerta de
# calidad en el pipeline de CI (Fase 5) antes de que el código llegue
# a construirse en una imagen. Repetirlos en cada build de imagen
# alargaría innecesariamente el tiempo de build sin aportar una
# verificación nueva en este punto del flujo.
RUN mvn -B package -DskipTests


# ---------- Etapa 2: imagen final ----------
# Solo el runtime (JRE), no el JDK ni Maven. Alpine como base porque
# es una distribución mínima (~5 MB) en comparación con una base
# Ubuntu/Debian completa.
FROM eclipse-temurin:21-jre-alpine

# Usuario y grupo de sistema dedicados, sin privilegios de root y sin
# shell de login interactivo. Justificación completa en
# fase3-notas-tecnicas.md.
RUN addgroup -S tallerapi && adduser -S tallerapi -G tallerapi

WORKDIR /app

# Copiamos ÚNICAMENTE el .jar ya construido desde la etapa "build".
# Nada de código fuente, nada de Maven, nada del JDK de compilación
# termina en esta imagen.
COPY --from=build /build/target/*.jar app.jar

# El propietario del fichero debe coincidir con el usuario que lo va
# a ejecutar; si no, el contenedor fallaría al arrancar por permisos.
RUN chown tallerapi:tallerapi app.jar

# A partir de aquí, todo lo que se ejecute en el contenedor (incluido
# el ENTRYPOINT) corre como "tallerapi", nunca como root.
USER tallerapi

EXPOSE 8080

# Forma "exec" (array), no forma "shell": así Java recibe directamente
# las señales del sistema (por ejemplo SIGTERM al hacer
# "docker compose down") y puede apagarse de forma ordenada, en lugar
# de que la señal se la quede un proceso de shell intermedio.
ENTRYPOINT ["java", "-jar", "app.jar"]

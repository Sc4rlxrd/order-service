# syntax=docker/dockerfile:1.7

ARG JAVA_VERSION=21

# ESTÁGIO 1 — BUILD
# Este estágio contém JDK, Maven Wrapper e ferramentas de compilação.

FROM eclipse-temurin:${JAVA_VERSION}-jdk-jammy AS build

WORKDIR /workspace

# Copia primeiro os arquivos do Maven.
# Como eles mudam menos que o código, o Docker consegue reutilizar o cache.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

# Baixa as dependências antes de copiar o código-fonte.
# O cache mantém o repositório local do Maven entre os builds.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw \
      --batch-mode \
      --no-transfer-progress \
      dependency:go-offline

# Agora copia o código da aplicação.
COPY src/ src/

# Compila e empacota o Spring Boot.
# Os testes devem ser executados antes, separadamente.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw \
      --batch-mode \
      --no-transfer-progress \
      clean package \
      -DskipTests

# Localiza o JAR executável.
# Ignora o arquivo .jar.original criado pelo Spring Boot.
RUN set -eux; \
    JAR_FILE="$(find target \
      -maxdepth 1 \
      -type f \
      -name '*.jar' \
      ! -name '*.jar.original' \
      -print \
      -quit)"; \
    test -n "${JAR_FILE}"; \
    cp "${JAR_FILE}" /workspace/application.jar



# ESTÁGIO 2 — RUNTIME
# Este estágio contém somente o JRE e o JAR da aplicação.

FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime

# Metadados que podem ser informados durante o docker build.
ARG APP_NAME=order-service
ARG APP_VERSION=dev
ARG BUILD_DATE=unknown
ARG VCS_REF=unknown
ARG SOURCE_URL=unknown

LABEL org.opencontainers.image.title="${APP_NAME}" \
      org.opencontainers.image.description="BookCommerce ${APP_NAME}" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.source="${SOURCE_URL}" \
      org.opencontainers.image.vendor="Sc4rlxrd"

# Cria um grupo e um usuário sem privilégios.
# A aplicação não será executada como root.
RUN groupadd \
      --system \
      --gid 10001 \
      appgroup \
    && useradd \
      --system \
      --uid 10001 \
      --gid appgroup \
      --no-create-home \
      --shell /usr/sbin/nologin \
      appuser

WORKDIR /application

# Copia somente o JAR gerado no estágio de build.
COPY --from=build \
     --chown=10001:10001 \
     /workspace/application.jar \
     /application/application.jar

#  o processo não possui privilégios de root.
USER 10001:10001


EXPOSE 8082

# Informa que o processo deve receber SIGTERM durante o encerramento.
STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "/application/application.jar"]
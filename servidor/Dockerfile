# syntax=docker/dockerfile:1
# Servidor de ranking do Missão Marte (Fase 5) em contêiner.
# Fase 8 (DA-39): o servidor compila o módulo nucleo do jogo, então o contexto do build é a RAIZ do repositório
# (o que entra no contexto está em servidor/Dockerfile.dockerignore). Na raiz:
#   docker build -f servidor/Dockerfile -t missao-marte-servidor .
#   docker run -p 8080:8080 -e MISSAOMARTE_JWT_SEGREDO=... -v missao-marte-dados:/dados missao-marte-servidor
# Ou, para desenvolvimento, use o compose.yaml da pasta servidor (cd servidor && docker compose up --build).

# ---- 1) compila com o Gradle Wrapper do servidor num JDK 17 -----------------------------------------------------
FROM eclipse-temurin:17-jdk AS compilacao
# A mesma disposição de pastas do repositório: o servidor/settings.gradle inclui ../nucleo.
WORKDIR /fonte/servidor
# Primeiro só o que define as dependências: esta camada fica em cache enquanto os build.gradle não mudarem.
COPY servidor/gradlew servidor/settings.gradle servidor/build.gradle ./
COPY servidor/gradle gradle
COPY nucleo/build.gradle /fonte/nucleo/build.gradle
RUN ./gradlew --no-daemon --quiet dependencies > /dev/null
# Depois o código. Os testes rodam fora do contêiner (./gradlew build); aqui só o jar executável.
COPY servidor/src src
COPY nucleo/src/main /fonte/nucleo/src/main
RUN ./gradlew --no-daemon --quiet bootJar

# ---- 2) imagem final: só o JRE 17 e o jar, rodando com um usuário sem privilégios ---------------------------------
FROM eclipse-temurin:17-jre
RUN groupadd --system missao && useradd --system --gid missao --home /app missao \
    && mkdir -p /app /dados && chown missao:missao /dados
WORKDIR /app
COPY --from=compilacao /fonte/servidor/build/libs/missao-marte-servidor.jar app.jar
# Publicação: o APK e o jogo desktop que a página oferece em /downloads. O GitHub Actions os gera (já apontando para
# o servidor publicado) e coloca em servidor/downloads antes do build; fora dele, a pasta só tem o .gitkeep e a
# página avisa.
COPY servidor/downloads /app/downloads

# Banco H2 no volume /dados (sobrevive à recriação do contêiner). O segredo do token NÃO tem valor aqui:
# vem de fora (MISSAOMARTE_JWT_SEGREDO), nunca da imagem.
ENV MISSAOMARTE_DADOS=/dados/missaomarte \
    MISSAOMARTE_DOWNLOADS=/app/downloads \
    PORT=8080
VOLUME ["/dados"]
EXPOSE 8080
USER missao

# Saúde pelo actuator, sem instalar curl: o bash abre a conexão TCP e procura "UP" na resposta.
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${PORT} && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

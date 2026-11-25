# syntax=docker/dockerfile:1
# Servidor de ranking do Missão Marte (Fase 5) em contêiner.
#   docker build -t missao-marte-servidor .
#   docker run -p 8080:8080 -e MISSAOMARTE_JWT_SEGREDO=... -v missao-marte-dados:/dados missao-marte-servidor
# Ou, para desenvolvimento, use o compose.yaml desta pasta (docker compose up --build).

# ---- 1) compila com o Gradle Wrapper do projeto num JDK 17 -------------------------------------------------------
FROM eclipse-temurin:17-jdk AS compilacao
WORKDIR /fonte
# Primeiro só o que define as dependências: esta camada fica em cache enquanto o build.gradle não mudar.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon --quiet dependencies > /dev/null
# Depois o código. Os testes rodam fora do contêiner (./gradlew build); aqui só o jar executável.
COPY src src
RUN ./gradlew --no-daemon --quiet bootJar

# ---- 2) imagem final: só o JRE 17 e o jar, rodando com um usuário sem privilégios ---------------------------------
FROM eclipse-temurin:17-jre
RUN groupadd --system missao && useradd --system --gid missao --home /app missao \
    && mkdir -p /app /dados && chown missao:missao /dados
WORKDIR /app
COPY --from=compilacao /fonte/build/libs/missao-marte-servidor.jar app.jar
# Publicação: o APK e o jogo desktop que a página oferece em /downloads. O GitHub Actions os gera (já apontando para
# o servidor publicado) e coloca nesta pasta antes do build; fora dele, ela só tem o .gitkeep e a página avisa.
COPY downloads /app/downloads

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

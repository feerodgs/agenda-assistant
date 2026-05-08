# ─── Etapa 1: build ───────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copia somente o pom.xml primeiro para aproveitar o cache de dependências do Maven
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Baixa dependências (camada cacheável — só re-executa se pom.xml mudar)
RUN ./mvnw dependency:go-offline -q

# Copia o restante e compila
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ─── Etapa 2: imagem final (só JRE, menor) ────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/bot-agendamento-*.jar app.jar

# Variáveis de ambiente com valores padrão (sobrescreva no docker-compose ou no run)
ENV SPRING_PROFILES_ACTIVE=default
ENV BOT_CHECKER_MODE=fake

ENTRYPOINT ["java", "-jar", "app.jar"]

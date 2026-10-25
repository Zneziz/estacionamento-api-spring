# ==========================================
# ETAPA 1: Build (Compilação com Maven e Java 17)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia o pom.xml e baixa as dependências primeiro (otimiza o cache do Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia todo o código-fonte do projeto e gera o pacote .jar (pulando os testes no build)
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: Run (Execução leve apenas com JRE 17)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Define o fuso horário do container para São Paulo/Brasília (importante para os cálculos de permanência e JWT)
ENV TZ=America/Sao_Paulo

# Copia o arquivo .jar gerado na Etapa 1 (no seu pom.xml o projeto gera est-0.0.1-SNAPSHOT.jar)
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Comando que inicia a API quando o container subir
ENTRYPOINT ["java", "-jar", "app.jar"]
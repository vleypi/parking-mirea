# Сборка: Maven + JDK 21 собирает исполняемый jar.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package && cp target/parking-server-*.jar app.jar

# Запуск: только JRE 21 и готовый jar, приложение работает не от root.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S parking && adduser -S parking -G parking
COPY --from=build /workspace/app.jar app.jar
USER parking
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=30 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

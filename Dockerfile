
FROM maven:3.9-eclipse-temurin-17 AS build


WORKDIR /app


COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -B -DskipTests

FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="Notification Service Team"
LABEL description="Notification Service Application"

RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

COPY --from=build /app/target/notification-svc-*.jar app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8081

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]

# ============================================
# Usage Notes:
# ============================================
# To build the Docker image:
#   docker build -t notification-svc:latest .
#
# To run the container:
#   docker run -p 8081:8081 notification-svc:latest
#
# To run with a MySQL database:
#   docker run -p 8081:8081 \
#     -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/notification_svc_sept_2025 \
#     -e SPRING_DATASOURCE_USERNAME=root \
#     -e SPRING_DATASOURCE_PASSWORD=yourpassword \
#     notification-svc:latest
#
# To run with docker-compose (recommended):
#   Create a docker-compose.yml file with the application and the MySQL database


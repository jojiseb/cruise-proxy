FROM openjdk:8-jre-slim
WORKDIR /app

ARG JAR_FILE=target/offshore-proxy-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} offshore-proxy.jar

# Expose both the HTTP port (8090) and the TCP tunnel port (9999)
EXPOSE 8090 9999

ENTRYPOINT ["java","-jar","offshore-proxy.jar"]

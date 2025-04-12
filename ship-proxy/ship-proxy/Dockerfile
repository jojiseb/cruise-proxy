FROM openjdk:8-jre-slim

WORKDIR /app

RUN apt-get update && \
    apt-get install -y netcat-openbsd && \
    rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=target/ship-proxy-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} ship-proxy.jar

COPY wait-for-offshore.sh /wait-for-offshore.sh
RUN chmod +x /wait-for-offshore.sh

EXPOSE 8080

ENTRYPOINT ["/wait-for-offshore.sh", "offshore", "9999", "--", "java", "-jar", "ship-proxy.jar"]
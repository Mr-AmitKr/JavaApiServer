FROM eclipse-temurin:17-jdk-focal

WORKDIR /app
COPY ItemApiServer.java .
RUN javac ItemApiServer.java

CMD ["java", "ItemApiServer"]
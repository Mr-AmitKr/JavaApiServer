FROM eclipse-temurin:17
WORKDIR /app
COPY . .
RUN javac ItemApiServer.java
EXPOSE 8000
CMD ["java", "ItemApiServer"]

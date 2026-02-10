FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac ItemApiServer.java

CMD ["java", "ItemApiServer"]

EXPOSE 8000

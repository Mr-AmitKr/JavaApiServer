FROM openjdk:17-jdk-slim
WORKDIR /app
COPY ItemApiServer.java .
RUN javac ItemApiServer.java
# Render will override this EXPOSE, but it's good practice
EXPOSE 8000 
CMD ["java", "ItemApiServer"]
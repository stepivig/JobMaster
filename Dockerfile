FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build/libs/*.jar jobmaster.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "jobmaster.jar"]
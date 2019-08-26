FROM openjdk:8-alpine

COPY target/uberjar/blogger.jar /blogger/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/blogger/app.jar"]

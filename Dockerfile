FROM openjdk:8
WORKDIR /usr/src/app
COPY target/scala-2.12/syn-bot-assembly-0.1.jar /usr/src/app
CMD ["java", "-jar", "syn-bot-assembly-0.1.jar"]

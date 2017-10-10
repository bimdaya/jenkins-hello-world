FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD .fetchr.sample-0.0.1-SNAPSHOT.war app.war
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar

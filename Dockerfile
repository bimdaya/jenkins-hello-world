FROM openjdk:8-jdk-alpine
RUN apk add maven --update-cache --repository http://dl-4.alpinelinux.org/alpine/edge/community/ --allow-untrusted \
	&& rm -rf /var/cache/apk/*
RUN apk add --update git
ENV MAVEN_HOME /usr/share/java/maven-3
ENV PATH $PATH:$MAVEN_HOME/bin
RUN git clone https://github.com/talal-shobaita/hello-world.git 
WORKDIR "/hello-world" 
#EXPOSE 8080
ENTRYPOINT mvn package && mvn spring-boot:run

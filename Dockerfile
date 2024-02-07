FROM openjdk:11-jre
WORKDIR /channelfinder
ADD https://repo1.maven.org/maven2/org/phoebus/ChannelFinder/4.7.2/ChannelFinder-4.7.2.jar .

CMD ["java", "-jar", "./ChannelFinder-*.jar", "--spring.config.name=application"]


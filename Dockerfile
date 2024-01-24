FROM openjdk:11

# deployment unit
COPY target/ChannelFinder-*.jar /channelfinder/ChannelFinder-*.jar

CMD ["java", "-jar", "/channelfinder/ChannelFinder-*.jar", "--spring.config.name=application"]

FROM openjdk:11-jre
WORKDIR /channelfinder
ADD https://github.com/ChannelFinder/ChannelFinderService/releases/download/service-cf-4.0.0/ChannelFinder-4.0.0.jar .
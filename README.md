# Channel Finder

ChannelFinder is a directory server, implemented as a REST style web service.
Its intended use is for control systems, namely the EPICS Control system.

- [Documentation](https://channelfinder.readthedocs.io/en/latest/)
- [Releases](https://github.com/ChannelFinder/ChannelFinderService/releases)
- [Docker Containers](https://github.com/ChannelFinder/ChannelFinderService/pkgs/container/channelfinderservice)

## Description

* **Motivation and Objectives**

  High level applications tend to prefer a hierarchical view of the control system name space. They group channel names
  by location or physical function. The name space of the EPICS Channel Access protocol, on the other hand, is flat. A
  good and thoroughly enforced naming convention may solve the problem of creating unique predictable names. It does not
  free every application from being configured explicitly, so that it knows all channel names it might be interested in
  beforehand.

  ChannelFinder tries to overcome this limitation by implementing a generic directory service, which applications can
  query for a list of channels that match certain conditions, such as physical functionality or location. It also
  provides mechanisms to create channel name aliases, allowing for different perspectives of the same set of channel
  names.

* **Directory Data Structure**

  Each directory entry consists of a channel `<name>`, an arbitrary set of `<properties>` (name-value pairs), and an
  arbitrary set of `<tags>` (names).

* **Basic Operation**

  An application sends an HTTP query to the service, specifying an expression that references tags, properties and their
  values, or channel names. The service returns a list of matching channels with their properties and tags, as JSON
  documents.

## Installation

ChannelFinder is a Java EE5 REST-style web service. The directory data is held in a ElasticSearch index.

### Docker Compose

For using docker containers there is a barebones [docker compose file](./compose.yml).

### Manual Installation

* Prerequisites

    * JDK 17
    * Elastic version 8.11.x
    * **For authN/authZ using LDAP:** LDAP server, e.g. OpenLDAP

#### Setup Elasticsearch

Options:

- Download and install elasticsearch (version 8.11.0)
  from [elastic.com](https://www.elastic.co) following the instructions for
  your platform.
- Install the elastic server from your distribution using a package manager.
- Run Elasticsearch in a [docker container](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html)

#### Running

```bash
sudo apt-get install openjdk-17-jre git curl wget
sudo systemctl start elasticsearch # Or other command to run elastic search

# Replace verison with the release you want
wget https://github.com/ChannelFinder/ChannelFinderService/releases/download/ChannelFinder-{version}/ChannelFinder-{version}.jar 
java -jar target/ChannelFinder-*.jar
``` 

Other installation recipes can be found
on [the wiki pages](https://github.com/ChannelFinder/ChannelFinder-SpringBoot/wiki).

### Configuration

By default, the channelfinder service will start on port 8080 with the default settings. To start with a
different `application.properties` file:

```bash
java -Dspring.config.location=file:./application.properties -jar ChannelFinder-*.jar
```

The default authentication includes an embedded ldap server with users and roles defined in
the [`cf.ldif`](src/main/resources/cf.ldif) file.
Note that `cf.ldif` contains **default credentials** and should only be used during testing and evaluation.

### Verification

To check that the server is running correctly, visit [the default homepage](http://localhost:8080/). For more
information on the api see the [swagger docs endpoint](http://localhost:8080/swagger-ui/index.html). 

## Development

It's strongly encouraged to use a modern IDE such as [Intelij](https://www.jetbrains.com/idea/)
and [Eclipse](https://eclipseide.org/).

* Prerequisites

    * JDK 17
    * Maven (via package manager or via the wrapper `./mvnw`) (version specified
      in [the wrapper properties](./.mvn/wrapper/maven-wrapper.properties))

For the following commands `mvn` can be interchangeably used instead via `./mvnw`

To build:

```bash
mvn clean install
```

To test:

```bash
mvn test
```

To run the server in development (you need a running version of Elasticsearch)

```bash
mvn spring-boot:run
```

### Integration tests with Docker containers

Purpose is to have integration tests for ChannelFinder API with Docker.

See `src/test/java` and package

* `org.phoebus.channelfinder.docker`

Integration tests start docker containers for ChannelFinder and Elasticsearch and run http requests (GET, POST, PUT,
DELETE) towards the application to test behavior (read, list, query, create, update, remove) and
replies are received and checked if content is as expected.

There are tests for properties, tags and channels separately and in combination.

Integration tests can be run in IDE and via Maven.

```
mvn failsafe:integration-test -DskipITs=false
```

See also

* [How to run Integration test with Docker](src/test/resources/INTEGRATIONTEST_DOCKER_RUN.md)
* [Tutorial for Integration test with Docker](src/test/resources/INTEGRATIONTEST_DOCKER_TUTORIAL.md)

### Release ChannelFinder Server binaries to maven central

The Phoebus ChannelFinder service uses the maven release plugin to prepare the publish the ChannelFinder server binaries
to maven central
using the sonatype repositories.

#### Setup

Create a sonatype account and update the maven settings.xml file with your sonatype credentials

```xml
<servers>
    <server>
        <id>phoebus-releases</id>
        <username>username</username>
        <password>*******</password>
    </server>
</servers>
```

#### Prepare the release

```bash
mvn release:prepare
```  

In this step will ensure there are no uncommitted changes, ensure the versions number are correct, tag the scm, etc..
A full list of checks is
documented [here](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html):

#### Perform the release

```bash
mvn release:perform
```

Checkout the release tag, build, sign and push the build binaries to sonatype.

#### Publish

Open the staging repository in [sonatype](https://s01.oss.sonatype.org/#stagingRepositories) and hit the *publish*
button

### Channel Finder

#### A simple directory service

  ChannelFinder is a directory server, implemented as a REST style web service.
Its intended use is within control systems, namely the EPICS Control system, for which it has been written.

* Motivation and Objectives

  High level applications tend to prefer an hierarchical view of the control system name space. They group channel names by location or physical function. The name space of the EPICS Channel Access protocol, on the other hand, is flat. A good and thoroughly enforced naming convention may solve the problem of creating unique predictable names. It does not free every application from being configured explicitly, so that it knows all channel names it might be interested in beforehand.

  ChannelFinder tries to overcome this limitation by implementing a generic directory service, which applications can query for a list of channels that match certain conditions, such as physical functionality or location. It also provides mechanisms to create channel name aliases, allowing for different perspectives of the same set of channel names.

* Directory Data Structure

 Each directory entry consists of a channel `<name>`, an arbitrary set of `<properties>` (name-value pairs), and an arbitrary set of `<tags>` (names).

* Basic Operation

 An application sends an HTTP query to the service, specifying an expression that references tags, properties and their values, or channel names. The service returns a list of matching channels with their properties and tags, as JSON documents.


#### API reference guide

https://channelfinder.readthedocs.io/en/latest/

#### Installation

ChannelFinder is a Java EE5 REST-style web service. The directory data is held in a ElasticSearch index.

Collected installation recipes and notes may be found on [wiki pages](https://github.com/ChannelFinder/ChannelFinder-SpringBoot/wiki).

* Prerequisites

  * JDK 17
  * Elastic version 8.2.x
  * <For authN/authZ using LDAP:> LDAP server, e.g. OpenLDAP

* setup elastic search  
  **Install**  
  Download and install elasticsearch (verision 8.2.0) from [elastic.com](https://www.elastic.co/downloads/past-releases/elasticsearch-8-2-0)
  following the instructions for your platform.\
  <Alternatively:> Install the elastic server from your distribution using a package manager.  
  
* Build 
```
# Debian 10
sudo apt-get install openjdk-17-jdk maven git curl wget
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.2.0-amd64.deb
sudo dpkg -i elasticsearch-8.2.0-amd64.deb
sudo systemctl start elasticsearch

#### Checkout and build ChannelFinder service source
git clone https://github.com/ChannelFinder/ChannelFinderService.git
cd ChannelFinderService
mvn clean install

``` 

#### Start the service  

* Using spring boot via Maven

```
mvn spring-boot:run
```

* or using the jar

```
java -jar target/ChannelFinder-4.7.0.jar
```

The above command will start the channelfinder service on port 8080 with the default settings,
which use embedded ldap server with users and roles defined in the [`cf.ldif`](src/main/resources/cf.ldif) file.
Note that `cf.ldif` contains **default credentials** and should only be used during testing and evaluation.

#### Verification

To check that the server is running correctly.

```
$ curl --fail-with-body http://localhost:8080/ChannelFinder
{
  "name" : "ChannelFinder Service",
  "version" : "4.7.0",
  "elastic" : {
    "status" : "Connected",
    "clusterName" : "elasticsearch",
    "clusterUuid" : "sA2L_cpoRD-H46c_Mya3mA",
    "version" : "8.2.0"
  }
}
$ curl --fail-with-body http://localhost:8080/ChannelFinder/resources/tags
[]
$ curl --basic -u admin:1234 --fail-with-body -H 'Content-Type: application/json' \
  -X PUT -d '{"name":"foo", "owner":"admin"}' \
  http://localhost:8080/ChannelFinder/resources/tags/foo
...
$ curl --fail-with-body http://localhost:8080/ChannelFinder/resources/tags
[{"name":"foo","owner":"admin","channels":[]}]
$ curl --basic -u admin:1234 --fail-with-body -X DELETE \
  http://localhost:8080/ChannelFinder/resources/tags/foo
```

#### Start up options  

You can start the channelfinder service with your own applications.properties file as follows:  

```
mvn spring-boot:run -Dspring.config.location=file:./application.properties
```
or  
```
java -Dspring.config.location=file:./application.properties -jar ChannelFinder-4.7.0.jar
```

You can also start up channelfinder with demo data using the command line argument `demo-data` followed by an integer number `n`. For example, `--demo-data=n`. With this argument, `n*1500` channels will be created to simulate some of the most common types of devices found in accelerators like magnets, power supplies, etc...  

```
java -jar target/ChannelFinder-4.7.0.jar --demo-data=1
java -jar target/ChannelFinder-4.7.0.jar --cleanup=1
```
  
```
mvn spring-boot:run -Dspring-boot.run.arguments="--demo-data=1"
mvn spring-boot:run -Dspring-boot.run.arguments="--cleanup=1"
```

#### Integration tests with Docker containers

Purpose is to have integration tests for ChannelFinder API with Docker.

See `src/test/java` and package
* `org.phoebus.channelfinder.docker`

Integration tests start docker containers for ChannelFinder and Elasticsearch and run http requests (GET) and curl commands (POST, PUT, DELETE) towards the application to test behavior (read, list, query, create, update, remove) and replies are received and checked if content is as expected.

There are tests for properties, tags and channels separately and in combination.

Integration tests can be run in IDE and via Maven.

```
mvn failsafe:integration-test -DskipITs=false
```

See
* [How to run Integration test with Docker](src/test/resources/INTEGRATIONTEST_DOCKER_RUN.md)
* [Tutorial for Integration test with Docker](src/test/resources/INTEGRATIONTEST_DOCKER_TUTORIAL.md)

#### ChannelFinder data managment

The [cf-manager](https://github.com/ChannelFinder/cf-manager) project provides tools to perform operations on large queries ( potentially the entire directory ).
Some examples of these operations include running checks to validate the pv names or producing reports about the number of active PVs, a list of IOC names, etc..

### Release ChannelFinder Server binaries to maven central

The Phoebus ChannelFinder service uses the maven release plugin to prepare the publish the ChannelFinder server binaries to maven central
using the sonatype repositories.

**Setup**

Create a sonatype account and update the maven settings.xml file with your sonatype credentials

```
  <servers>
   <server>
      <id>phoebus-releases</id>
      <username>shroffk</username>
      <password>*******</password>
   </server>
  </servers>
```

**Prepare the release**  
`mvn release:prepare`  
In this step will ensure there are no uncommitted changes, ensure the versions number are correct, tag the scm, etc..
A full list of checks is documented [here](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html):

**Perform the release**  
`mvn release:perform`  
Checkout the release tag, build, sign and push the build binaries to sonatype.

**Publish**  
Open the staging repository in [sonatype](https://s01.oss.sonatype.org/#stagingRepositories) and hit the *publish* button

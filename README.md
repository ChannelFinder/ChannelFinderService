### Channel Finder

#### A simple directory service


  ChannelFinder is a directory server, implemented as a REST style web service.
Its intended use is within control systems, namely the EPICS Control system, for which it has been written.

* Motivation and Objectives

  High level applications tend to prefer an hierarchical view of the control system name space.
They group channel names by location or physical function. The name space of the EPICS Channel Access protocol,
on the other hand, is flat. A good and thoroughly enforced naming convention may solve the problem of creating
unique predictable names. It does not free every application from being configured explicitly,
so that it knows all channel names it might be interested in beforehand.

  ChannelFinder tries to overcome this limitation by implementing a generic directory service,
which applications can query for a list of channels that match certain conditions,
such as physical functionality or location. It also provides mechanisms to create channel name aliases,
allowing for different perspectives of the same set of channel names.

* Directory Data Structure

 Each directory entry consists of a channel <name>, an arbitrary set of <properties> (name-value pairs),
and an arbitrary set of <tags> (names).

* Basic Operation

 An application sends an HTTP query to the service, specifying an expression that references tags,
properties and their values, or channel names. The service returns a list of matching channels with their
properties and tags, as JSON documents.


#### API reference guide

https://github.com/shroffk/ChannelFinder-SpringBoot/blob/master/src/main/resources/api_description.rst

#### Installation

ChannelFinder is a Java EE5 REST-style web service. The directory data is held in
a ElasticSearch index.

* Prerequisites

  * JDK 8 or newer
  * Elastic erion 6.3
  * <For authN/authZ using LDAP:> LDAP server, e.g. OpenLDAP

* setup elastic search  
  **Install**  
  Download and install elasticsearch (verision 6.3) from [elastic.com](https://www.elastic.co/downloads/past-releases/elasticsearch-6-3-1)  
  following the instructions for your platform.\
  <Alternatively:> Install the elastic server from your distribution using a package manager.  
  **Create the elastic indexes and set up their mapping**  
  The Mapping_definitions script (which is avaiable under /channelfinder/src/main/resources) contains the curl commands to setup the 3 elastic indexes associated with channelfinder. 

* Build 
```
mvn clean install
``` 

#### Start the service

1. Run the jar

```
java -jar target/alarm-logger-<version>.jar
```

2. Using spring boot  

```
mvn spring-boot:run
```

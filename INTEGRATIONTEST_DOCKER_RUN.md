### Prerequisites

##### Tools

* Docker - engine 18.06.0+ or later, compose 1.29.2 or later, compose file version 3.7 to be supported

##### Build ChannelFinder service

```
mvn clean install
```

### Run tests

##### IDE

All or individual integration tests (including methods) can be run in IDE as JUnit tests.

##### Maven

All integration tests can be run via Maven.

```
mvn failsafe:integration-test
```

Individual integration tests (classes) can also be run via Maven.

```
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderChannelsIT
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderIT
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderPropertiesIT
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderScrollIT
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderTagsIT
```

### Note

##### Build

* (Re) Build after change in `src/main/java` in order for change to be tested
* `Dockerfile.integrationtest` relies on built code and not on Maven central

##### Configuration

* Configuration in folder `src/test/java` and package `org.phoebus.channelfinder.docker`, e.g. urls and port numbers, is coupled to files `Dockerfile.integrationtest` and `docker-compose-integrationtest.yml` (beside `src/main/resources/application.properties`)

##### Debug

* Docker containers can be inspected when debugging integration tests

##### Performance

* It may take a minute to run a test. This includes time to set up the test environment, perform the test and tear down the test environment. Setting up the test environment takes most of that time.

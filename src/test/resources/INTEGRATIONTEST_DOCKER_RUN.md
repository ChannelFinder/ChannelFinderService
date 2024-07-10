### Prerequisites

##### Tools

* Docker - engine 18.06.0+ or later, compose 2.21.0 or later, compose file version 3.7 to be supported

##### Build ChannelFinder service

```
mvn clean install
```

### Run tests

##### IDE

All or individual integration tests (including methods) can be run in IDE as JUnit tests.

##### Maven

To run all integration tests via Maven.

```
mvn failsafe:integration-test -DskipITs=false
mvn failsafe:integration-test -DskipITs=false -DskipITCoverage=false
```

To run individual integration tests (classes) via Maven.

```
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderChannelsIT
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderIT
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderPropertiesIT
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderScrollIT
mvn test -Dtest=org.phoebus.channelfinder.docker.ChannelFinderTagsIT
```

##### Code coverage

Run integration tests with property `-DskipITCoverage=false` in order to have code coverage analysis. By default, code coverage for integration tests is disabled.

After integration tests have been run, run below command to process coverage data. This applies for all and individual integration tests (including methods).

```
mvn verify -Djacoco.skip=false
```

Result is available in `target/site/jacoco` folder and includes code coverage execution data and reports.

```
index.html
jacoco.exec
jacoco.csv
jacoco.xml
```

##### Summary

To build and run all unit tests and integration tests (Docker).

```
mvn clean install test-compile failsafe:integration-test failsafe:verify --batch-mode --fail-at-end -DskipITs=false -Pintegrationtest-docker
```

To build and run all unit tests and integration tests (Docker) with code coverage.

```
mvn clean install test-compile failsafe:integration-test failsafe:verify --batch-mode --fail-at-end -Djacoco.skip=false -DskipITs=false -DskipITCoverage=false -Pintegrationtest-docker
```

### Note

##### Build

* (Re) Build after change in `src/main/java` in order for change to be tested
* `Dockerfile.integrationtest` relies on built code and not on Maven central
* Requires a deployable jar

##### Configuration

* Configuration in folder `src/test/java` and package `org.phoebus.channelfinder.docker`, e.g. urls and port numbers, is coupled to files `Dockerfile.integrationtest` and `docker-compose-integrationtest.yml` (beside `src/main/resources/application.properties`)

##### Debug

* Docker containers can be inspected when debugging integration tests

##### Performance

* It may take a minute to run a test. This includes time to set up the test environment, perform the test and tear down the test environment. Setting up the test environment takes most of that time.
* It may take additional time to run an integration test with code coverage.

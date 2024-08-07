### About

Describe ability to develop and run integration tests for ChannelFinder API with Docker.

In other words, how to use `src/test/java` to test `src/main/java` with integration tests using Docker.

##### Background

ChannelFinder with Elasticsearch together with the environment in which the applications run, is complex and usually heavily relied on by other applications and environments. Outside interface is to ChannelFinder but ChannelFinder and Elasticsearch go together. Therefore, there is need to test ChannelFinder and Elasticsearch together.

It is possible to test ChannelFinder API by running ChannelFinder and Elasticsearch applications together as Docker containers and executing a series of requests and commands to test their behavior. This tutorial will show how it works and give examples.

##### Content

* [Prerequisites](#prerequisites)
* [Examples](#examples)
* [How it works - big picture](#how-it-works-big-picture)
* [How it works - in detail](#how-it-works-in-detail)
* [How to run](#how-to-run)
* [Reference](#reference)

### Prerequisites

##### Tools

* Docker - engine 18.06.0+ or later, compose 2.21.0 or later, compose file version 3.7 to be supported

##### Dependencies

* JUnit 5
* Testcontainers

##### Files

* folder `src/test/java` and package `org.phoebus.channelfinder.docker`
* [docker-compose-integrationtest.yml](docker-compose-integrationtest.yml)
* [Dockerfile.integrationtest](Dockerfile.integrationtest)

### Examples

##### Simple

[ChannelFinderIT.java](src/test/java/org/phoebus/channelfinder/docker/ChannelFinderIT.java)

```
@Test
void channelfinderUp()
```

Purpose
* verify that ChannelFinder is up and running

How
* Http request (GET) is run towards ChannelFinder base url and response code is verified to be 200

##### Medium

[ChannelFinderPropertiesIT.java](src/test/java/org/phoebus/channelfinder/docker/ChannelFinderPropertiesIT.java)

```
@Test
void handleProperty()
```

Purpose
* verify behavior for single property that include commands - list, create property, list, retrieve, delete (unauthorized), delete, list

How
* a series of Http requests (GET) and curl commands (POST, PUT, DELETE) are run towards the application to test behavior

##### Complex

[ChannelFinderChannelsIT.java](src/test/java/org/phoebus/channelfinder/docker/ChannelFinderChannelsIT.java)

```
@Test
void handleChannels3QueryByPattern()
```

Purpose
* set up test fixture - properties, tags, channels, channels with properties & tags
* query by pattern - search for a list of channels based on their name, tags, and/or properties
* tear down test fixture - reverse to set up

How
* a series of Http requests (GET) and curl commands (POST, PUT, DELETE) are run towards the application to test behavior

### How it works - big picture

Integration tests are implemented in test class annotated with `@Testcontainers`. Test class starts a docker container for the application (ChannelFinder service) and another docker container for elastic (Elasticsearch) through `docker-compose-integrationtest.yml` and `Dockerfile.integrationtest` after which JUnit tests are run.

```
@Testcontainers
class ChannelFinderIT {

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @Test
    void channelfinderUp() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_CHANNELFINDER);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }
```

Http requests (GET) and curl commands (POST, PUT, DELETE) are run towards the application to test behavior (read, list, query, create, update, remove) and replies are received and checked if content is as expected.

There are tests for properties, tags and channels separately and in combination.

##### Note

* Docker containers (ChannelFinder, Elasticsearch) are shared for tests within test class. Order in which tests are run is not known. Therefore, each test is to leave ChannelFinder, Elasticsearch in a clean state to not disturb other tests.

### How it works - in detail

##### Anatomy of an integration test

```
@Testcontainers
class ChannelFinderPropertiesIT {

    static Property property_p1_owner_o1;

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @BeforeAll
    public static void setupObjects() {
        property_p1_owner_o1 = new Property("p1", "o1", null);
    }

    @AfterAll
    public static void tearDownObjects() {
        property_p1_owner_o1 = null;
    }

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty() {
        // what
        //     user with required role PropertyMod
        //     create property
        //         list, create property, list, retrieve, delete (unauthorized), delete, list

        try {
            ITUtilProperties.assertListProperties(0);

            ITUtilProperties.assertCreateReplaceProperty(AuthorizationChoice.ADMIN, "/t1", property_p1_owner_o1);

            ITUtilProperties.assertListProperties(1, property_p1_owner_o1);

            ITUtilProperties.assertRetrieveProperty("/p1",                    property_p1_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p1?withChannels=true",  property_p1_owner_o1);
            ITUtilProperties.assertRetrieveProperty("/p1?withChannels=false", property_p1_owner_o1);

            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.NONE,  "/p1", HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.USER,  "/p1", HttpURLConnection.HTTP_UNAUTHORIZED);
            ITUtilProperties.assertRemoveProperty(AuthorizationChoice.ADMIN, "/p1", HttpURLConnection.HTTP_OK);

            ITUtilProperties.assertListProperties(0);
        } catch (Exception e) {
            fail();
        }
    }
```

##### What happens at runtime

The test environment is started with through test class annotated with `@Testcontainers` and constant `ENVIRONMENT` annotated with `@Container`. Containers are started (Ryuk, ChannelFinder, Elasticsearch). Then one time setup is run (method annotated with `@BeforeAll`), after which individual tests are run (methods annotated with `@Test`) after which one time tear down is run (method annotated with `@AfterAll`). Finally tasks are done and test class is closed.

Note the extensive use of test utility classes (in more detail below) in which are shared code for common tasks.

* authorization
* serialization and deserialization of properties, tags and channels
* Http requests (GET) and curl commands (POST, PUT, DELETE) corresponding to endpoints in ChannelFinder API
* assert response

##### Examining `handleProperty`

1.  A GET request is made to ChannelFinder to list all properties and ensure there are no properties available.
2.  A PUT request is made to ChannelFinder to create the property listed by the path parameter. Request is made with ADMIN authority.
3.  A GET request is made to ChannelFinder to list all properties and ensure there is one (given) property available.
4.  A GET request is made to ChannelFinder to retrieve property with given name.
5.  A GET request is made to ChannelFinder to retrieve property with given name with associated channel information.
6.  A GET request is made to ChannelFinder to retrieve property with given name without associated channel information.
7.  A DELETE request is made to ChannelFinder to delete property. Request is made without authority.
8.  A DELETE request is made to ChannelFinder to delete property. Request is made with USER authority.
9.  A DELETE request is made to ChannelFinder to delete property. Request is made with ADMIN authority.
10. A GET request is made to ChannelFinder to list all properties and ensure there are no properties available.


* 1, 3, 10 - Request corresponds to PropertyManager method

```
    @GetMapping
    public Iterable<Property> list() {
```

* 2 - Request corresponds to PropertyManager method

```
    @PutMapping("/{propertyName}")
    public Property create(@PathVariable("propertyName") String propertyName, @RequestBody Property property) {
```

* 4, 5, 6 - Request corresponds to PropertyManager method

```
    @GetMapping("/{propertyName}")
    public Property read(@PathVariable("propertyName") String propertyName,
                         @RequestParam(value = "withChannels", defaultValue = "true") boolean withChannels) {
```

* 7, 8, 9 - Request corresponds to PropertyManager method

```
    @DeleteMapping("/{propertyName}")
    public void remove(@PathVariable("propertyName") String propertyName) {
```

##### Test classes

See `src/test/java` and `org.phoebus.channelfinder.docker`.

* files with suffix IT.java

##### Test utilities

See `src/test/java` and `org.phoebus.channelfinder.docker`.

* files with prefix ITTestFixture
* files with prefix ITUtil

##### Test utilities - example

With the help of test utitilies, the tests themselves may be simplified and made more clear.

```
public class ITUtilChannels {

    public static Channel[] assertListChannels(int expectedEqual) {
        return assertListChannels("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, CHANNELS_NULL);
    }
    public static Channel[] assertListChannels(int expectedEqual, Channel... expected) {
        return assertListChannels("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    public static Channel[] assertListChannels(String queryString, Channel... expected) {
        return assertListChannels(queryString, HttpURLConnection.HTTP_OK, -1, -1, expected);
    }
    public static Channel[] assertListChannels(String queryString, int expectedEqual) {
        return assertListChannels(queryString, HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, CHANNELS_NULL);
    }
    public static Channel[] assertListChannels(String queryString, int responseCode, int expectedEqual) {
        return assertListChannels(queryString, responseCode, expectedEqual, expectedEqual, CHANNELS_NULL);
    }

    /**
     * Utility method to return the list of channels which match all given expressions, i.e. the expressions are combined in a logical AND.
     *
     * @param queryString query string
     * @param responseCode response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response channels
     * @return number of channels
     */
    public static Channel[] assertListChannels(String queryString, int responseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Channel... expected) {
        Channel[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_CHANNELFINDER_RESOURCES_CHANNELS + queryString);

            ITUtil.assertResponseLength2Code(response, responseCode);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                actual = mapper.readValue(response[1], Channel[].class);
            }
            // expected number of items in list
            //     (if non-negative number)
            //     expectedGreaterThanOrEqual <= nbr of items <= expectedLessThanOrEqual
            if (expectedGreaterThanOrEqual >= 0) {
                assertTrue(actual.length >= expectedGreaterThanOrEqual);
            }
            if (expectedLessThanOrEqual >= 0) {
                assertTrue(actual.length <= expectedLessThanOrEqual);
            }
            if (expected != null) {
                assertEqualsChannels(actual, expected);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }
```

Above methods can be used like shown below.

```
@Testcontainers
public class ChannelFinderChannelsIT {

    @Test
    void handleChannels3QueryByPattern() {

            ITUtilChannels.assertListChannels("?~name=asdf", 0);

            ITUtilChannels.assertListChannels("?~name=ABC:DEF-GHI:JKL:001", ITTestFixture.channel_ghi001_properties_tags);

            ITUtilChannels.assertListChannels("?~name=*001",
                    ITTestFixture.channel_ghi001_properties_tags,
                    ITTestFixture.channel_xyz001_properties_tags);

```

##### Note

* (Re) Build after change in `src/main/java` is needed in order for change to be tested as `Dockerfile.integrationtest` relies on built code.
* Configuration in folder `src/test/java` and package `org.phoebus.channelfinder.docker`, e.g. urls and port numbers, is coupled to files `Dockerfile.integrationtest` and `docker-compose-integrationtest.yml` (beside `src/main/resources/application.properties`).
* Both positive and negative tests are important to ensure validation works as expected.

### How to run

See [How to run Integration test with Docker](INTEGRATIONTEST_DOCKER_RUN.md).

### Reference

##### ChannelFinder

* [ChannelFinder - Enhanced Directory Service](https://channelfinder.readthedocs.io/en/latest/api.html)

##### Testcontainers

* [Testcontainers](https://testcontainers.com/)

/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.channelfinder.docker;

import org.springframework.http.HttpHeaders;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch with focus on support common behavior for tests.
 *
 * @author Lars Johansson
 */
public class ITUtil {

    // note
    //     port numbers can be exposed differently to avoid interference with any running instance

    private static final String AUTH_USER     = "user:userPass";
    private static final String AUTH_ADMIN    = "admin:adminPass";
    private static final String HEADER_JSON   = "application/json";
    private static final String HEADER_BASIC  = "Basic ";

    private static final String RESOURCES_CHANNELS    = "/resources/channels";
    private static final String RESOURCES_PROPERTIES  = "/resources/properties";
    private static final String RESOURCES_SCROLL      = "/resources/scroll";
    private static final String RESOURCES_TAGS        = "/resources/tags";

    public static final String HTTP_IP_PORT_CHANNELFINDER                      = "http://127.0.0.1:8080/ChannelFinder";
    public static final String HTTP_IP_PORT_CHANNELFINDER_RESOURCES_CHANNELS   = ITUtil.HTTP_IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_CHANNELS;
    public static final String HTTP_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES = ITUtil.HTTP_IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_PROPERTIES;
    public static final String HTTP_IP_PORT_CHANNELFINDER_RESOURCES_SCROLL     = ITUtil.HTTP_IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_SCROLL;
    public static final String HTTP_IP_PORT_CHANNELFINDER_RESOURCES_TAGS       = ITUtil.HTTP_IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_TAGS;

    // integration test - docker

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String CHANNELFINDER = "channelfinder";
    public static final String INTEGRATIONTEST_DOCKER_COMPOSE = "src/test/resources/compose.yml";

    // code coverage

    public static final String JACOCO_EXEC_PATH      = "/channelfinder/jacoco.exec";
    public static final String JACOCO_TARGET_PREFIX  = "target/jacoco_";
    public static final String JACOCO_TARGET_SUFFIX  = ".exec";
    public static final String JACOCO_SKIPITCOVERAGE = "skipITCoverage";

    /**
     * This class is not to be instantiated.
     */
    private ITUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Provide a default compose setup for testing.
     * For Docker Compose V2.
     *
     * Intended usage is as field annotated with @Container from class annotated with @Testcontainers.
     *
     * @return compose container
     */
    public static ComposeContainer defaultComposeContainers() {
        return new ComposeContainer(new File(ITUtil.INTEGRATIONTEST_DOCKER_COMPOSE))
                .withEnv(ITUtil.JACOCO_SKIPITCOVERAGE, System.getProperty(ITUtil.JACOCO_SKIPITCOVERAGE))
                .withLocalCompose(true)
                .waitingFor(ITUtil.CHANNELFINDER, Wait.forHealthcheck());
    }

    /**
     * Extract coverage report from compose container to file system.
     *
     * @param environment compose container
     * @param destinationPath destination path, i.e. where in file system to put coverage report
     * that has been extracted from container
     */
    public static void extractJacocoReport(ComposeContainer environment, String destinationPath) {
        // extract jacoco report from container file system
        //     stop jvm to make data available

        if (!Boolean.FALSE.toString().equals(System.getProperty(ITUtil.JACOCO_SKIPITCOVERAGE))) {
            return;
        }

        Optional<ContainerState> container = environment.getContainerByServiceName(ITUtil.CHANNELFINDER);
        if (container.isPresent()) {
            ContainerState cs = container.get();
            DockerClient dc = cs.getDockerClient();
            dc.stopContainerCmd(cs.getContainerId()).exec();
            try {
                cs.copyFileFromContainer(ITUtil.JACOCO_EXEC_PATH, destinationPath);
            } catch (Exception e) {
                // proceed if file cannot be copied
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Assert that response object is as expected, an array with 2 elements
     * of which first contains response code OK (200).
     *
     * @param response string array with response of http request, response code and content
     *
     * @see HttpURLConnection#HTTP_OK
     */
    static void assertResponseLength2CodeOK(String[] response) {
        assertResponseLength2Code(response, HttpURLConnection.HTTP_OK);
    }

    /**
     * Assert that response object is as expected, an array with 2 elements
     * of which first element contains given response code.
     *
     * @param response string array with response of http request, response code and content
     * @param expectedResponseCode expected response code
     *
     * @see HttpURLConnection for available response codes
     */
    static void assertResponseLength2Code(String[] response, int expectedResponseCode) {
        assertNotNull(response);
        assertEquals(2, response.length);
        assertEquals(expectedResponseCode, Integer.parseInt(response[0]));
    }

    /**
     * Assert that response object is as expected, an array with 2 elements
     * of which first element contains response code OK (200) and second element contains given response content.
     *
     * @param response string array with response of http request, response code and content
     * @param expectedResponseContent expected response content
     *
     * @see HttpURLConnection#HTTP_OK
     */
    static void assertResponseLength2CodeOKContent(String[] response, String expectedResponseContent) {
        assertResponseLength2CodeContent(response, HttpURLConnection.HTTP_OK, expectedResponseContent);
    }

    /**
     * Assert that response object is as expected, an array with 2 elements
     * of which first element contains given response code and second element contains given response content.
     *
     * @param response string array with response of http request, response code and content
     * @param expectedResponseCode expected response code
     * @param expectedResponseContent expected response content
     *
     * @see HttpURLConnection for available response codes
     */
    static void assertResponseLength2CodeContent(String[] response, int expectedResponseCode, String expectedResponseContent) {
        assertResponseLength2Code(response, expectedResponseCode);
        assertEquals(expectedResponseContent, response[1]);
    }

    // ----------------------------------------------------------------------------------------------------

    // enum for http methods
    static enum MethodChoice        {POST, GET, PUT, DELETE};

    // enum for different authorizations
    static enum AuthorizationChoice {NONE, USER, ADMIN};

    // enum for different endpoints
    static enum EndpointChoice      {CHANNELS, PROPERTIES, SCROLL, TAGS};

    /**
     * Utility method to build http request for test to run.
     *
     * @param methodChoice method choice
     * @param authorizationChoice authorization choice
     * @param endpointChoice endpoint choice
     * @param path particular path
     * @param json json data
     * @return http request to run
     * @throws URISyntaxException If request is created with non-legal URI characters
     */
    static HttpRequest buildRequest(MethodChoice methodChoice, AuthorizationChoice authorizationChoice, EndpointChoice endpointChoice, String path, String json) throws URISyntaxException  {
        String pathstr = !StringUtils.isEmpty(path)
                ? path
                : "";

        String str =
                  ITUtil.HTTP_IP_PORT_CHANNELFINDER
                + ITUtil.buildUri(endpointChoice)
                + pathstr;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(new URI(str))
                .header(HttpHeaders.CONTENT_TYPE, HEADER_JSON);

        builder = buildAuthorization(builder, authorizationChoice);

        return buildMethodJson(builder, methodChoice, json).build();
    }

    /**
     * Utility method to build http request for method and json data.
     *
     * @param builder http request builder
     * @param methodChoice method choice, i.e. POST, GET, PUT, DELETE, PATCH
     * @param json json data
     * @return http request builder
     */
    private static HttpRequest.Builder buildMethodJson(HttpRequest.Builder builder, MethodChoice methodChoice, String json) {
        switch (methodChoice) {
        case POST:
           return builder.POST(HttpRequest.BodyPublishers.ofString(json));
        case GET:
           return builder.GET();
        case PUT:
           return builder.PUT(HttpRequest.BodyPublishers.ofString(json));
        case DELETE:
           return builder.DELETE();
        default:
           return builder.GET();
        }
    }

    /**
     * Utility method to build http client for authentication and authorization.
     * Http basic authorization is used.
     *
     * @param builder http request builder
     * @param authorizationChoice authorization choice
     * @return http request builder
     */
    private static HttpRequest.Builder buildAuthorization(HttpRequest.Builder builder, AuthorizationChoice authorizationChoice) {
        switch (authorizationChoice) {
        case ADMIN:
            return builder.headers(HttpHeaders.AUTHORIZATION, HEADER_BASIC + Base64.getEncoder().encodeToString(AUTH_ADMIN.getBytes()));
        case USER:
            return builder.headers(HttpHeaders.AUTHORIZATION, HEADER_BASIC + Base64.getEncoder().encodeToString(AUTH_USER.getBytes()));
        case NONE:
            return builder;
        default:
            return builder;
        }
    }

    /**
     * Utility method to build string for endpoint. To be used when constructing uri to send query to server.
     *
     * @param endpointChoice endpoint choice
     * @return string for endpoint
     */
    private static String buildUri(EndpointChoice endpointChoice) {
        switch (endpointChoice) {
        case CHANNELS:
            return ITUtil.RESOURCES_CHANNELS;
        case PROPERTIES:
            return ITUtil.RESOURCES_PROPERTIES;
        case SCROLL:
            return ITUtil.RESOURCES_SCROLL;
        case TAGS:
            return ITUtil.RESOURCES_TAGS;
        default:
            return StringUtils.EMPTY;
        }
    }

    /**
     * Send GET request with given string as URI and return response status code.
     *
     * @param str string to parse as URI
     * @return response status code
     * @throws URISyntaxException If request is created with non-legal URI characters
     * @throws InterruptedException
     * @throws IOException
     */
    static int sendRequestStatusCode(String str) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(str))
                .GET()
                .build();

        return Integer.parseInt(sendRequest(request)[0]);
    }

    /**
     * Send GET request with given string as URI and return string array with response status code and response body.
     *
     * @param str string to parse as URI
     * @return string array with response status code and response body
     * @throws URISyntaxException If request is created with non-legal URI characters
     * @throws InterruptedException
     * @throws IOException
     */
    static String[] sendRequest(String str) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(str))
                .GET()
                .build();

        return sendRequest(request);
    }

    /**
     * Send request and return response with string array with response code and response string.
     *
     * @param request request
     * @return string array with response code and response string
     * @throws InterruptedException
     * @throws IOException
     */
    static String[] sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = CLIENT.send(request, BodyHandlers.ofString());

        return new String[] {String.valueOf(response.statusCode()), response.body()};
    }

}

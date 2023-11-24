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

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import com.github.dockerjava.api.DockerClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Utility class to help (Docker) integration tests for ChannelFinder and Elasticsearch with focus on support common behavior for tests.
 *
 * @author Lars Johansson
 */
public class ITUtil {

    static final String CHANNELFINDER = "channelfinder";

    static final String AUTH_USER     = "user:userPass";
    static final String AUTH_ADMIN    = "admin:adminPass";
    static final String EMPTY_JSON    = "[]";
    static final String HTTP          = "http://";
    static final String HEADER_JSON   = "'Content-Type: application/json'";

    // port numbers
    //     can be exposed differently externally to avoid interference with any running instance

    static final String IP_PORT_CHANNELFINDER = "127.0.0.1:8080/ChannelFinder";
    static final String IP_PORT_ELASTICSEARCH = "127.0.0.1:9201";

    static final String RESOURCES_CHANNELS = "/resources/channels";
    static final String RESOURCES_PROPERTIES = "/resources/properties";
    static final String RESOURCES_SCROLL = "/resources/scroll";
    static final String RESOURCES_TAGS = "/resources/tags";

    static final String HTTP_IP_PORT_CHANNELFINDER = HTTP + IP_PORT_CHANNELFINDER;

    static final String HTTP_IP_PORT_CHANNELFINDER_RESOURCES_CHANNELS              = ITUtil.HTTP +                           ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_CHANNELS;
    static final String HTTP_AUTH_USER_IP_PORT_CHANNELFINDER_RESOURCES_CHANNELS    = ITUtil.HTTP + ITUtil.AUTH_USER  + "@" + ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_CHANNELS;
    static final String HTTP_AUTH_ADMIN_IP_PORT_CHANNELFINDER_RESOURCES_CHANNELS   = ITUtil.HTTP + ITUtil.AUTH_ADMIN + "@" + ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_CHANNELS;

    static final String HTTP_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES            = ITUtil.HTTP +                           ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_PROPERTIES;
    static final String HTTP_AUTH_USER_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES  = ITUtil.HTTP + ITUtil.AUTH_USER  + "@" + ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_PROPERTIES;
    static final String HTTP_AUTH_ADMIN_IP_PORT_CHANNELFINDER_RESOURCES_PROPERTIES = ITUtil.HTTP + ITUtil.AUTH_ADMIN + "@" + ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_PROPERTIES;

    static final String HTTP_IP_PORT_CHANNELFINDER_RESOURCES_SCROLL                = ITUtil.HTTP + ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_SCROLL;

    static final String HTTP_IP_PORT_CHANNELFINDER_RESOURCES_TAGS                  = ITUtil.HTTP +                           ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_TAGS;
    static final String HTTP_AUTH_USER_IP_PORT_CHANNELFINDER_RESOURCES_TAGS        = ITUtil.HTTP + ITUtil.AUTH_USER  + "@" + ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_TAGS;
    static final String HTTP_AUTH_ADMIN_IP_PORT_CHANNELFINDER_RESOURCES_TAGS       = ITUtil.HTTP + ITUtil.AUTH_ADMIN + "@" + ITUtil.IP_PORT_CHANNELFINDER + ITUtil.RESOURCES_TAGS;

    private static final String BRACKET_BEGIN     = "[";
    private static final String BRACKET_END       = "]";
    private static final String CURLY_BRACE_BEGIN = "{";
    private static final String CURLY_BRACE_END   = "}";
    private static final String HTTP_REPLY        = "HTTP";

    // integration test - docker

    public static final String INTEGRATIONTEST_DOCKER_COMPOSE = "docker-compose-integrationtest.yml";
    public static final String INTEGRATIONTEST_LOG_MESSAGE    = ".*Started Application.*";

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
                .waitingFor(ITUtil.CHANNELFINDER, Wait.forLogMessage(ITUtil.INTEGRATIONTEST_LOG_MESSAGE, 1));
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

    /**
     * Do GET request with given string as URL and return response code.
     *
     * @param spec string to parse as URL
     * @return response code
     *
     * @throws IOException
     */
    static int doGet(String spec) throws IOException {
        URL url = new URL(spec);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        return con.getResponseCode();
    }

    /**
     * Do GET request with given string as URL and return response with string array with response code and response string.
     *
     * @param spec string to parse as URL
     * @return string array with response code and response string
     *
     * @throws IOException
     */
    static String[] doGetJson(String spec) throws IOException {
        URL url = new URL(spec);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int responseCode = con.getResponseCode();

        String line;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = responseCode == HttpURLConnection.HTTP_OK
                ? new BufferedReader(new InputStreamReader(con.getInputStream()))
                : new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
            while((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        return new String[] {String.valueOf(responseCode), sb.toString().trim()};
    }

    /**
     * Run a shell command and return response with string array with response code and response string.
     *
     * @param command shell command
     * @return string array with response code and response string
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws Exception
     */
    static String[] runShellCommand(String command) throws IOException, InterruptedException, Exception {
        // run shell command & return http response code if available

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);

        String responseCode = null;
        String responseContent = null;
        try {
            final Process process = processBuilder.start();
            final BufferedReader errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            final BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final boolean processFinished = process.waitFor(30, TimeUnit.SECONDS);

            String line = null;
            while ((line = inputStream.readLine()) != null) {
                if (line.startsWith(HTTP_REPLY)) {
                    // response code, e.g. "HTTP/1.1 200", "HTTP/1.1 401", "HTTP/1.1 500"
                    String[] s = line.trim().split(" ");
                    if (s != null && s.length == 2) {
                        responseCode = s[1];
                    }
                } else if ((line.startsWith(BRACKET_BEGIN) && line.endsWith(BRACKET_END))
                        || (line.startsWith(CURLY_BRACE_BEGIN) && line.endsWith(CURLY_BRACE_END))) {
                    // response string, json
                    responseContent = line;
                }
            }

            if (!processFinished) {
                throw new Exception("Timed out waiting to execute command: " + command);
            }
            if (process.exitValue() != 0) {
                throw new Exception(
                        String.format("Shell command finished with status %d error: %s",
                                process.exitValue(),
                                errorStream.lines().collect(Collectors.joining())));
            }
        } catch (IOException | InterruptedException e) {
            throw e;
        }
        return new String[] {responseCode, responseContent};
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
     * Prepare curl command for test to run for contacting server.
     *
     * @param methodChoice method choice
     * @param authorizationChoice authorization choice
     * @param endpointChoice endpoint choice
     * @param path particular path
     * @param json json data
     * @return curl command to run
     */
    static String curlMethodAuthEndpointPathJson(MethodChoice methodChoice, AuthorizationChoice authorizationChoice, EndpointChoice endpointChoice, String path, String json) {
        String pathstr = !StringUtils.isEmpty(path)
                ? path
                : "";

        String data = !StringUtils.isEmpty(json)
                ? " -d '" + json + "'"
                : "";

        return "curl"
            + " -H " + ITUtil.HEADER_JSON
            + " -X"  + ITUtil.getMethodString(methodChoice)
            + " -i "
            + ITUtil.HTTP
            + ITUtil.getAuthorizationString(authorizationChoice)
            + ITUtil.IP_PORT_CHANNELFINDER
            + ITUtil.getEndpointString(endpointChoice)
            + pathstr
            + data;
    }

    /**
     * Utility method to return string for http method. To be used when constructing url to send query to server.
     *
     * @param methodChoice method choice, i.e. POST, GET, PUT, DELETE, PATCH
     * @return string for http method
     */
    private static String getMethodString(MethodChoice methodChoice) {
        switch (methodChoice) {
        case POST:
            return "POST";
        case GET:
            return "GET";
        case PUT:
            return "PUT";
        case DELETE:
            return "DELETE";
        default:
            return "GET";
        }
    }

    /**
     * Utility method to return string for authorization. To be used when constructing url to send query to server.
     *
     * @param authorizationChoice authorization choice
     * @return string for authorization
     */
    private static String getAuthorizationString(AuthorizationChoice authorizationChoice) {
        switch (authorizationChoice) {
        case ADMIN:
            return ITUtil.AUTH_ADMIN  + "@";
        case USER:
            return ITUtil.AUTH_USER  + "@";
        case NONE:
            return StringUtils.EMPTY;
        default:
            return StringUtils.EMPTY;
        }
    }

    /**
     * Utility method to return string for endpoint. To be used when constructing url to send query to server.
     *
     * @param endpointChoice endpoint choice
     * @return string for endpoint
     */
    private static String getEndpointString(EndpointChoice endpointChoice) {
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


}

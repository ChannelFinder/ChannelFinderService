package org.phoebus.channelfinder.api.v0;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * HTTP client for the API, so that the tests see exactly what a remote client sees: status codes,
 * raw bodies and the query string as the client encoded it.
 *
 * <p>It sends with {@link RestTestClient} and returns the status and body for the tests to check
 * with JUnit. Spring's own assertions throw {@link AssertionError}, and a test marked
 * {@code @ExpectedToFail} accepts only JUnit's {@code AssertionFailedError}.
 */
final class ApiClient {

  static final String ADMIN = "admin:adminPass";
  static final String USER = "user:userPass";
  static final String GUEST = "guest:guestPass";

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final RestTestClient http;
  private final String root;

  ApiClient(String root) {
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
    // The service compresses responses. Without this, every request asks for gzip.
    requestFactory.enableCompression(false);
    this.http = RestTestClient.bindToServer(requestFactory).build();
    this.root = root;
  }

  record Response(int status, String body) {

    JsonNode json() {
      return MAPPER.readTree(body);
    }
  }

  Response get(String pathAndQuery) {
    return send(HttpMethod.GET, pathAndQuery, null, null);
  }

  /** Return the {@code Content-Type} header of the response to a GET, or null when it is absent. */
  String contentTypeOfGet(String pathAndQuery) {
    return exchange(HttpMethod.GET, pathAndQuery, null, null)
        .getResponseHeaders()
        .getFirst(HttpHeaders.CONTENT_TYPE);
  }

  Response put(String pathAndQuery, String json, String credentials) {
    return send(HttpMethod.PUT, pathAndQuery, json, credentials);
  }

  Response post(String pathAndQuery, String json, String credentials) {
    return send(HttpMethod.POST, pathAndQuery, json, credentials);
  }

  Response delete(String pathAndQuery, String credentials) {
    return send(HttpMethod.DELETE, pathAndQuery, null, credentials);
  }

  Response delete(String pathAndQuery, String json, String credentials) {
    return send(HttpMethod.DELETE, pathAndQuery, json, credentials);
  }

  private Response send(HttpMethod method, String pathAndQuery, String json, String credentials) {
    EntityExchangeResult<byte[]> result = exchange(method, pathAndQuery, json, credentials);
    byte[] body = result.getResponseBody();
    return new Response(
        result.getStatus().value(), body == null ? "" : new String(body, StandardCharsets.UTF_8));
  }

  private EntityExchangeResult<byte[]> exchange(
      HttpMethod method, String pathAndQuery, String json, String credentials) {
    RestTestClient.RequestBodySpec request =
        http.method(method)
            .uri(URI.create(root + encodeQuery(pathAndQuery)))
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    if (credentials != null) {
      String[] userAndPassword = credentials.split(":", 2);
      request =
          request.headers(headers -> headers.setBasicAuth(userAndPassword[0], userAndPassword[1]));
    }
    RestTestClient.RequestHeadersSpec<?> ready =
        json == null ? request : request.body(json.getBytes(StandardCharsets.UTF_8));
    return ready.exchange().returnResult(byte[].class);
  }

  /**
   * Encode each query key and value, so a test can be written with the literal query a client
   * means, e.g. {@code /channels?~name=a|b&prop!=*}. A key without {@code =} is sent bare.
   */
  private static String encodeQuery(String pathAndQuery) {
    int start = pathAndQuery.indexOf('?');
    if (start < 0) {
      return pathAndQuery;
    }
    StringJoiner query = new StringJoiner("&");
    for (String pair : pathAndQuery.substring(start + 1).split("&")) {
      int split = pair.indexOf('=');
      if (split < 0) {
        query.add(encode(pair));
      } else {
        query.add(encode(pair.substring(0, split)) + "=" + encode(pair.substring(split + 1)));
      }
    }
    return pathAndQuery.substring(0, start) + "?" + query;
  }

  private static String encode(String text) {
    return URLEncoder.encode(text, StandardCharsets.UTF_8);
  }
}

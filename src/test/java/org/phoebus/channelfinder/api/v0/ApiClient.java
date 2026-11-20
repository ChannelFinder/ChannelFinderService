package org.phoebus.channelfinder.api.v0;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.StringJoiner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plain HTTP client for the API, so that the tests see exactly what a remote client sees: status
 * codes, raw bodies and the query string as the client encoded it.
 */
final class ApiClient {

  static final String ADMIN = "admin:adminPass";
  static final String USER = "user:userPass";
  static final String GUEST = "guest:guestPass";

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final HttpClient http = HttpClient.newHttpClient();
  private final String root;

  ApiClient(String root) {
    this.root = root;
  }

  record Response(int status, String body) {

    JsonNode json() {
      return MAPPER.readTree(body);
    }
  }

  Response get(String pathAndQuery) {
    return send(request(pathAndQuery, null).GET());
  }

  /** Return the {@code Content-Type} header of the response to a GET, or null when it is absent. */
  String contentTypeOfGet(String pathAndQuery) {
    return exchange(request(pathAndQuery, null).GET())
        .headers()
        .firstValue("Content-Type")
        .orElse(null);
  }

  Response put(String pathAndQuery, String json, String credentials) {
    return send(request(pathAndQuery, credentials).PUT(body(json)));
  }

  Response post(String pathAndQuery, String json, String credentials) {
    return send(request(pathAndQuery, credentials).POST(body(json)));
  }

  Response delete(String pathAndQuery, String credentials) {
    return send(request(pathAndQuery, credentials).DELETE());
  }

  Response delete(String pathAndQuery, String json, String credentials) {
    return send(request(pathAndQuery, credentials).method("DELETE", body(json)));
  }

  private HttpRequest.Builder request(String pathAndQuery, String credentials) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(root + encodeQuery(pathAndQuery)))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json");
    if (credentials != null) {
      builder.header(
          "Authorization",
          "Basic "
              + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
    }
    return builder;
  }

  private Response send(HttpRequest.Builder builder) {
    HttpResponse<String> response = exchange(builder);
    return new Response(response.statusCode(), response.body());
  }

  private HttpResponse<String> exchange(HttpRequest.Builder builder) {
    try {
      return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static HttpRequest.BodyPublisher body(String json) {
    return HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
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

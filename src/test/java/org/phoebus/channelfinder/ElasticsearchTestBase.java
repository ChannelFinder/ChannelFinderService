package org.phoebus.channelfinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need Elasticsearch.
 *
 * <p>One container is started on first use and shared by every test class in the JVM; the
 * Testcontainers reaper removes it when the JVM exits. Setting the system property {@code
 * elasticsearch.host_urls} skips the container and points the tests at that cluster instead.
 *
 * <p>The image version is the Maven property {@code elasticsearch.server.version}. Maven copies it
 * into {@code elasticsearch-test.properties} when it processes the test resources.
 */
public abstract class ElasticsearchTestBase {

  private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch";
  private static final int ELASTICSEARCH_PORT = 9200;
  private static final Map<String, String> ELASTICSEARCH_SETTINGS =
      Map.of(
          "discovery.type", "single-node",
          "xpack.security.enabled", "false",
          // A nearly full developer disk would otherwise turn the indices read-only.
          "cluster.routing.allocation.disk.threshold_enabled", "false");
  private static final String JVM_HEAP_OPTIONS = "-Xms512m -Xmx512m";
  private static final String HEALTH_CHECK_PATH = "/_cluster/health?wait_for_status=yellow";
  private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

  private static GenericContainer<?> elasticsearch;

  @DynamicPropertySource
  static void elasticsearchProperties(DynamicPropertyRegistry registry) {
    registry.add("elasticsearch.host_urls", ElasticsearchTestBase::hostUrls);
  }

  private static synchronized String hostUrls() {
    String external = System.getProperty("elasticsearch.host_urls");
    if (external != null && !external.isBlank()) {
      return external;
    }
    if (elasticsearch == null) {
      elasticsearch = startElasticsearch();
    }
    return "http://"
        + elasticsearch.getHost()
        + ":"
        + elasticsearch.getMappedPort(ELASTICSEARCH_PORT);
  }

  /** Return the image for the version in {@code elasticsearch.server.version} in the POM. */
  private static DockerImageName elasticsearchImage() {
    Properties properties = new Properties();
    try (InputStream in =
        ElasticsearchTestBase.class.getResourceAsStream("/elasticsearch-test.properties")) {
      properties.load(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return DockerImageName.parse(ELASTICSEARCH_IMAGE)
        .withTag(properties.getProperty("elasticsearch.server.version"));
  }

  private static GenericContainer<?> startElasticsearch() {
    GenericContainer<?> container =
        new GenericContainer<>(elasticsearchImage())
            .withEnv(ELASTICSEARCH_SETTINGS)
            .withEnv("ES_JAVA_OPTS", JVM_HEAP_OPTIONS)
            .withExposedPorts(ELASTICSEARCH_PORT)
            .waitingFor(
                Wait.forHttp(HEALTH_CHECK_PATH)
                    .forStatusCode(200)
                    .withStartupTimeout(STARTUP_TIMEOUT));
    container.start();
    return container;
  }
}

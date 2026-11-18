package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests an empty service root. The endpoints are then served directly under {@code /}, and the
 * default root is not used.
 */
@TestPropertySource(properties = "channelfinder.legacy.service-root=")
class ServiceRootEmptyApiIT extends ApiTestBase {

  @Override
  String serviceUrl() {
    return "http://localhost:" + port;
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "/",
        "/resources/channels",
        "/resources/properties",
        "/resources/tags",
        "/resources/scroll"
      })
  void endpointsAreServedWithoutARoot(String path) {
    assertEquals(200, cf.get(path).status());
  }

  @Test
  void defaultRootIsNotServed() {
    assertEquals(404, cf.get("/ChannelFinder/resources/channels").status());
  }
}

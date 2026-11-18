package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests a service root configured with slashes. {@code LegacyApiProperties} strips them, so {@code
 * /cf/api/} works like {@code cf/api}.
 */
@TestPropertySource(properties = "channelfinder.legacy.service-root=/cf/api/")
class ServiceRootSlashesApiIT extends ApiTestBase {

  @Override
  String serviceUrl() {
    return "http://localhost:" + port;
  }

  @Test
  void resourcesAreServedUnderTheStrippedRoot() {
    assertEquals(200, cf.get("/cf/api/resources/channels").status());
  }
}

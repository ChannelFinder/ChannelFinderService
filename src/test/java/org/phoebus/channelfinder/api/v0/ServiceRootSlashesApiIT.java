package org.phoebus.channelfinder.api.v0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests a service root configured with slashes. {@code LegacyApiProperties} strips them, so the
 * evident intent is that {@code /cf/api/} works like {@code cf/api}. The controllers build their
 * mappings from the raw property. The resources end up behind a double slash, which the security
 * firewall rejects with 400.
 */
@TestPropertySource(properties = "channelfinder.legacy.service-root=/cf/api/")
class ServiceRootSlashesApiIT extends ApiTestBase {

  @Override
  String serviceUrl() {
    return "http://localhost:" + port;
  }

  @Test
  @Tag("known-defect")
  @ExpectedToFail(
      value = "the controllers map the raw service root, not the one LegacyApiProperties strips",
      withExceptions = AssertionFailedError.class)
  void resourcesAreServedUnderTheStrippedRoot() {
    // today: returns 404
    assertEquals(200, cf.get("/cf/api/resources/channels").status());
  }
}

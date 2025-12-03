package org.phoebus.channelfinder.performance;

import java.net.URL;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.configuration.PopulateDBConfiguration;
import org.phoebus.channelfinder.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

/** An class for creating the example database and testing the speed. */
@WebMvcTest(AuthorizationService.class)
@TestPropertySource(value = "classpath:performance_application.properties")
class PopulateDBConfigurationIT {

  @Autowired PopulateDBConfiguration populateDBConfiguration;

  @Test
  @WithMockUser(username = "admin", roles = "CF-ADMINS")
  void testCreateTagsAndProperties() {
    final URL tagResource = getClass().getResource("/perf_tags.json");
    final URL propertyResource = getClass().getResource("/perf_properties.json");

    populateDBConfiguration.createTagsAndProperties(tagResource, propertyResource);
  }

  @Test
  void testCreateDB() {
    populateDBConfiguration.createDB();
  }

  @Test
  void testCleanUpDB() {
    populateDBConfiguration.cleanupDB();
  }
}

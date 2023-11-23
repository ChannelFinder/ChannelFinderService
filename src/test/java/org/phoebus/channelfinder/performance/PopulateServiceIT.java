package org.phoebus.channelfinder.performance;

import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.AuthorizationService;
import org.phoebus.channelfinder.example.PopulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import java.net.URL;

/**
 * An class for creating the example database and testing the speed.
 */
@WebMvcTest(AuthorizationService.class)
@TestPropertySource(value = "classpath:performance_application.properties")
class PopulateServiceIT {

    @Autowired
    PopulateService populateService;
    @Test
    @WithMockUser(username = "admin", roles = "CF-ADMINS")
    void testCreateTagsAndProperties() {
        final URL tagResource = getClass().getResource("/perf_tags.json");
        final URL propertyResource = getClass().getResource("/perf_properties.json");

        populateService.createTagsAndProperties(tagResource, propertyResource);
    }

    @Test
    void testCreateDB() {
        populateService.createDB();
    }

    @Test
    void testCleanUpDB() {
        populateService.cleanupDB();
    }


}

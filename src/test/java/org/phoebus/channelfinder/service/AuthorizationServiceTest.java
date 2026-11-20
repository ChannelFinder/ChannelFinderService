package org.phoebus.channelfinder.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.service.AuthorizationService.ROLES;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

class AuthorizationServiceTest {

  private static AuthorizationService serviceWithChannelGroup(String group) {
    AuthorizationService service = new AuthorizationService();
    service.initializeAdminRoles("cf-admins");
    service.initializeChannelModRoles(group);
    service.initializePropertyRoles("cf-properties");
    service.initializeTagRoles("cf-tags");
    return service;
  }

  @Test
  void eachServiceUsesItsOwnGroups() {
    AuthorizationService first = serviceWithChannelGroup("first-group");
    AuthorizationService second = serviceWithChannelGroup("second-group");
    Authentication user = new TestingAuthenticationToken("user", "password", "ROLE_FIRST-GROUP");

    assertTrue(first.isAuthorizedRole(user, ROLES.CF_CHANNEL));
    assertFalse(second.isAuthorizedRole(user, ROLES.CF_CHANNEL));
  }
}

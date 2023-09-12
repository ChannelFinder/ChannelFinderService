package org.phoebus.channelfinder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.AuthorizationService.ROLES;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(AuthorizationService.class)
@TestPropertySource(value = "classpath:application_test.properties")
public class AuthorizationIT { 
    @Autowired AuthorizationService authorizationService;
    
    Tag testTag = new Tag("testTag","valid");
    Property testProperty = new Property("testProperty","valid");
    Channel testChannel = new Channel("testChannel","valid");
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "invalid", roles = "CF-ADMINS")
    public void adminIsAuthorizedOwner() {
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag), "failed to authorize user that is admin (tag)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty), "failed to authorize user that is admin (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel), "failed to authorize user that is admin (channel)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "valid", roles = "CF-ADMINS")
    public void adminAndDirectOwnerIsAuthorizedOwner() {
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag), "failed to authorize user that is admin and direct owner (tag)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty), "failed to authorize user that is admin and direct owner (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel), "failed to authorize user that is admin and direct owner (channel)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "invalid", roles = {"CF-ADMINS","VALID"})
    public void adminAndGroupOwnerIsAuthorizedOwner() {
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag), "failed to authorize user that is admin and group owner (tag)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty), "failed to authorize user that is admin and group owner (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel), "failed to authorize user that is admin and group owner (channel)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "valid", roles = {"CF-ADMINS","VALID"})
    public void adminAndDirectOwnerAndGroupOwnerIsAuthorizedOwner() {
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag), "failed to authorize user that is admin and direct owner and group owner (tag)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty), "failed to authorize user that is admin and direct owner and group owner (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel), "failed to authorize user that is admin and direct owner and group owner (channel)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "valid", roles = "")
    public void directOwnerIsAuthorizedOwner() {
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag), "failed to authorize user that is direct owner (tag)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty), "failed to authorize user that is direct owner (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel), "failed to authorize user that is direct owner (channel)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "invalid", roles = "VALID")
    public void groupOwnerIsAuthorizedOwner() {
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag), "failed to authorize user that is group owner (tag)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty), "failed to authorize user that is group owner (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel), "failed to authorize user that is group owner (channel)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "valid", roles = "VALID")
    public void directOwnerAndGroupOwnerIsAuthorizedOwner() {
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag), "failed to authorize user that is direct owner and group owner (tag)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty), "failed to authorize user that is direct owner and group owner (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel), "failed to authorize user that is direct owner and group owner (channel)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "invalid", roles = "")
    public void isNotAuthorizedOwner() {
        Assertions.assertFalse(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag), "authorized user that shouldn't be authorized (tag)");
        Assertions.assertFalse( authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty), "authorized user that shouldn't be authorized (property)");
        Assertions.assertFalse( authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel), "authorized user that shouldn't be authorized (channel)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "CF-ADMINS")
    public void adminIsAuthorizedRole() {
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN), "failed to authorize user that is admin (admin)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL), "failed to authorize user that is admin (channel)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY), "failed to authorize user that is admin (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG), "failed to authorize user that is admin (tag)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = {"CF-ADMINS","CF-TAGS"})
    public void adminAndTagIsAuthorizedRole() {
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN), "failed to authorize user that is admin and tag (admin)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL), "failed to authorize user that is admin and tag (channel)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY), "failed to authorize user that is admin and tag (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG), "failed to authorize user that is admin and tag (tag)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "CF-CHANNELS")
    public void channelIsAuthorizedRole() {
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN), "authorized user that is channel (admin)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL), "failed to authorize user that is channel (channel)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY), "failed to authorize user that is channel (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG), "failed to authorize user that is channel (tag)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = {"CF-CHANNELS","CF-TAGS"})
    public void channelAndTagIsAuthorizedRole() {
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN), "authorized user that is channel and tag (admin)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL), "failed to authorize user that is channel and tag (channel)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY), "failed to authorize user that is channel and tag (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG), "failed to authorize user that is channel and tag (tag)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "CF-PROPERTIES")
    public void propertyIsAuthorizedRole() {
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN), "authorized user that is property (admin)");
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL), "authorized user that is property (channel)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY), "failed to authorize user that is property (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG), "failed to authorize user that is property (tag)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = {"CF-PROPERTIES","CF-TAGS"})
    public void propertyAndTagIsAuthorizedRole() {
        Assertions.assertFalse( authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN), "authorized user that is property and tag (admin)");
        Assertions.assertFalse( authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL), "authorized user that is property and tag (channel)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY), "failed to authorize user that is property and tag (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG), "failed to authorize user that is property and tag (tag)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "CF-TAGS")
    public void TagIsAuthorizedRole() {
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN), "authorized user that is tag (admin)");
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL), "authorized user that is tag (channel)");
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY), "authorized user that is tag (property)");
        Assertions.assertTrue(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG), "failed to authorize user that is tag (tag)");
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "")
    public void noneIsNotAuthorizedRole() {
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN), "authorized user that is admin (admin)");
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL), "authorized user that is admin (channel)");
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY), "authorized user that is admin (property)");
        Assertions.assertFalse(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG), "authorized user that is admin (tag)");
    }
}








package org.phoebus.channelfinder;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.channelfinder.AuthorizationService.ROLES;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
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
        assertTrue("failed to authorize user that is admin (tag)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag));
        assertTrue("failed to authorize user that is admin (property)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty));
        assertTrue("failed to authorize user that is admin (channel)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "valid", roles = "CF-ADMINS")
    public void adminAndDirectOwnerIsAuthorizedOwner() {
        assertTrue("failed to authorize user that is admin and direct owner (tag)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag));
        assertTrue("failed to authorize user that is admin and direct owner (property)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty));
        assertTrue("failed to authorize user that is admin and direct owner (channel)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "invalid", roles = {"CF-ADMINS","VALID"})
    public void adminAndGroupOwnerIsAuthorizedOwner() {
        assertTrue("failed to authorize user that is admin and group owner (tag)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag));
        assertTrue("failed to authorize user that is admin and group owner (property)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty));
        assertTrue("failed to authorize user that is admin and group owner (channel)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "valid", roles = {"CF-ADMINS","VALID"})
    public void adminAndDirectOwnerAndGroupOwnerIsAuthorizedOwner() {
        assertTrue("failed to authorize user that is admin and direct owner and group owner (tag)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag));
        assertTrue("failed to authorize user that is admin and direct owner and group owner (property)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty));
        assertTrue("failed to authorize user that is admin and direct owner and group owner (channel)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "valid", roles = "")
    public void directOwnerIsAuthorizedOwner() {
        assertTrue("failed to authorize user that is direct owner (tag)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag));
        assertTrue("failed to authorize user that is direct owner (property)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty));
        assertTrue("failed to authorize user that is direct owner (channel)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "invalid", roles = "VALID")
    public void groupOwnerIsAuthorizedOwner() {
        assertTrue("failed to authorize user that is group owner (tag)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag));
        assertTrue("failed to authorize user that is group owner (property)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty));
        assertTrue("failed to authorize user that is group owner (channel)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "valid", roles = "VALID")
    public void directOwnerAndGroupOwnerIsAuthorizedOwner() {
        assertTrue("failed to authorize user that is direct owner and group owner (tag)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag));
        assertTrue("failed to authorize user that is direct owner and group owner (property)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty));
        assertTrue("failed to authorize user that is direct owner and group owner (channel)",authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(username = "invalid", roles = "")
    public void isNotAuthorizedOwner() {
        assertTrue("authorized user that shouldn't be authorized (tag)",!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testTag));
        assertTrue("authorized user that shouldn't be authorized (property)",!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testProperty));
        assertTrue("authorized user that shouldn't be authorized (channel)",!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), testChannel));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "CF-ADMINS")
    public void adminIsAuthorizedRole() {
        assertTrue("failed to authorize user that is admin (admin)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN));
        assertTrue("failed to authorize user that is admin (channel)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL));
        assertTrue("failed to authorize user that is admin (property)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY));
        assertTrue("failed to authorize user that is admin (tag)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = {"CF-ADMINS","CF-TAGS"})
    public void adminAndTagIsAuthorizedRole() {
        assertTrue("failed to authorize user that is admin and tag (admin)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN));
        assertTrue("failed to authorize user that is admin and tag (channel)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL));
        assertTrue("failed to authorize user that is admin and tag (property)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY));
        assertTrue("failed to authorize user that is admin and tag (tag)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "CF-CHANNELS")
    public void channelIsAuthorizedRole() {
        assertTrue("authorized user that is channel (admin)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN));
        assertTrue("failed to authorize user that is channel (channel)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL));
        assertTrue("failed to authorize user that is channel (property)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY));
        assertTrue("failed to authorize user that is channel (tag)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = {"CF-CHANNELS","CF-TAGS"})
    public void channelAndTagIsAuthorizedRole() {
        assertTrue("authorized user that is channel and tag (admin)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN));
        assertTrue("failed to authorize user that is channel and tag (channel)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL));
        assertTrue("failed to authorize user that is channel and tag (property)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY));
        assertTrue("failed to authorize user that is channel and tag (tag)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "CF-PROPERTIES")
    public void propertyIsAuthorizedRole() {
        assertTrue("authorized user that is property (admin)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN));
        assertTrue("authorized user that is property (channel)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL));
        assertTrue("failed to authorize user that is property (property)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY));
        assertTrue("failed to authorize user that is property (tag)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = {"CF-PROPERTIES","CF-TAGS"})
    public void propertyAndTagIsAuthorizedRole() {
        assertTrue("authorized user that is property and tag (admin)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN));
        assertTrue("authorized user that is property and tag (channel)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL));
        assertTrue("failed to authorize user that is property and tag (property)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY));
        assertTrue("failed to authorize user that is property and tag (tag)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "CF-TAGS")
    public void TagIsAuthorizedRole() {
        assertTrue("authorized user that is tag (admin)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN));
        assertTrue("authorized user that is tag (channel)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL));
        assertTrue("authorized user that is tag (property)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY));
        assertTrue("failed to authorize user that is tag (tag)",authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG));
    }
    
    /**
     * 
     */
    @Test
    @WithMockUser(roles = "")
    public void noneIsNotAuthorizedRole() {
        assertTrue("authorized user that is admin (admin)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_ADMIN));
        assertTrue("authorized user that is admin (channel)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL));
        assertTrue("authorized user that is admin (property)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY));
        assertTrue("authorized user that is admin (tag)",!authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG));
    }
}








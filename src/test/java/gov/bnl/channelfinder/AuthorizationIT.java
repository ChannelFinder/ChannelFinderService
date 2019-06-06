package gov.bnl.channelfinder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@RunWith(SpringRunner.class)
@WebMvcTest(AuthorizationService.class)
public class AuthorizationIT { 
    @Autowired AuthorizationService authorizationService;
    //@Autowired AuthenticationManager authenticationManager;
    @Resource(name="authenticationManager")
    private AuthenticationManager authManager;
    /**
     * 
     */
    @Test
    public void isAuthorizedOwner() {
        //auth
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin","1234");
        Authentication adminAuth = authManager.authenticate(auth);
        
        //tag
        XmlTag testTag = new XmlTag();
        testTag.setName("test-tag");
        testTag.setOwner("test-owner");
        assertTrue("failed",authorizationService.isAuthorizedOwner(adminAuth,testTag));
        
        
        //property
        
        
        //channel
        
    }
    
}























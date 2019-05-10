package gov.bnl.channelfinder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    public static List<String> admin_groups;

    @Value("${admin-groups:cf-admins}")
    void initializeAdmins(String groups) {
        this.admin_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }
    
    public enum ROLES {
        CF_ADMIN(admin_groups),
        CF_CHANNEL(admin_groups),
        CF_PROPERTY(admin_groups),
        CF_TAG(admin_groups);
        
        private final List<String> groups;

        private ROLES(List<String> groups) {
            this.groups = groups;
        }

    };

    public boolean isAuthorizedOwner(Authentication authentication, XmlTag data) {
        return false;
    }
    
    public boolean isAuthorizedOwner(Authentication authentication, XmlProperty data) {
        return false;
    }
    
    public boolean isAuthorizedOwner(Authentication authentication, XmlChannel data) {
        return false;
    }

    public boolean isAuthorizedRole(Authentication authentication, ROLES expectedRole) {
        authentication.getAuthorities();
        return false;
    }
}

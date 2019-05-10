package gov.bnl.channelfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    public static List<String> admin_groups;
    public static List<String> channel_groups;
    public static List<String> property_groups;
    public static List<String> tag_groups;

    @Value("${admin-groups:cf-admins}")
    void initializeAdminRoles(String groups) {
        this.admin_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }
    @Value("${channel-groups:cf-channels}")
    void initializeChannelModRoles(String groups) {
        this.channel_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }
    @Value("${property-groups:cf-properties}")
    void initializePropertyRoles(String groups) {
        this.property_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }
    @Value("${tag-groups:cf-tags}")
    void initializeTagRoles(String groups) {
        this.tag_groups = Arrays.asList(groups.split(",")).stream().map(g -> {
            return "ROLE_" + g.trim().toUpperCase();
        }).collect(Collectors.toList());
    }

    public enum ROLES {
        CF_ADMIN(admin_groups),
        CF_CHANNEL(channel_groups),
        CF_PROPERTY(property_groups),
        CF_TAG(tag_groups);

        private final List<String> groups;

        private ROLES(List<String> groups) {
            this.groups = groups;
        }

    };

    public boolean isAuthorizedOwner(Authentication authentication, XmlTag data) {
        ArrayList<String> auth = new ArrayList<String>();
        Collection auths = authentication.getAuthorities();
        for(Object a: auths)
            auth.add(((GrantedAuthority)(a)).getAuthority());
        
        if(!Collections.disjoint(auth,ROLES.CF_ADMIN.groups))
            return true;
        if(authentication.getName().equals(data.getOwner()) || auth.contains("ROLE_" + data.getOwner().trim().toUpperCase()))
            return true;
        return false;
    }

    public boolean isAuthorizedOwner(Authentication authentication, XmlProperty data) {
        ArrayList<String> auth = new ArrayList<String>();
        Collection auths = authentication.getAuthorities();
        for(Object a: auths)
            auth.add(((GrantedAuthority)(a)).getAuthority());
        
        if(!Collections.disjoint(auth,ROLES.CF_ADMIN.groups))
            return true;
        if(authentication.getName().equals(data.getOwner()) || auth.contains("ROLE_" + data.getOwner().trim().toUpperCase()))
            return true;
        return false;
    }

    public boolean isAuthorizedOwner(Authentication authentication, XmlChannel data) {
        ArrayList<String> auth = new ArrayList<String>();
        Collection auths = authentication.getAuthorities();
        for(Object a: auths)
            auth.add(((GrantedAuthority)(a)).getAuthority());
        
        if(!Collections.disjoint(auth,ROLES.CF_ADMIN.groups))
            return true;
        if(authentication.getName().equals(data.getOwner()) || auth.contains("ROLE_" + data.getOwner().trim().toUpperCase()))
            return true;
        return false;
    }

    public boolean isAuthorizedRole(Authentication authentication, ROLES expectedRole) {
        ArrayList<String> auth = new ArrayList<String>();
        Collection auths = authentication.getAuthorities();
        for(Object a: auths)
            auth.add(((GrantedAuthority)(a)).getAuthority());

        if(!Collections.disjoint(auth,ROLES.CF_ADMIN.groups))
            return true;
        else if(!Collections.disjoint(auth,ROLES.CF_CHANNEL.groups) && expectedRole != ROLES.CF_ADMIN)
            return true;
        else if(!Collections.disjoint(auth,ROLES.CF_PROPERTY.groups) && (expectedRole == ROLES.CF_PROPERTY || expectedRole == ROLES.CF_TAG))
            return true;
        else if(!Collections.disjoint(auth,ROLES.CF_TAG.groups) && expectedRole == ROLES.CF_TAG)
            return true;
        return false;
    }
}

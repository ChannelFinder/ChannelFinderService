package gov.bnl.channelfinder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.directory.Attributes;

import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

@Configuration
public class CustomUserDetailsContextMapper extends LdapUserDetailsMapper implements UserDetailsContextMapper {

    private Logger log = Logger.getLogger(this.getClass().getName());

    @Override
    public LdapUserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {

        LdapUserDetailsImpl details = (LdapUserDetailsImpl) super.mapUserFromContext(ctx, username, authorities);
        log.info("DN from ctx: " + ctx.getDn()); // return correct DN
        log.info("Attributes size: " + ctx.getAttributes().size()); // always returns 0

        return new CustomUserDetails(details);
    }
//    @Override
//    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) 
//    {
//
//        Attributes attributes = ctx.getAttributes();
//        Object[] groups = new Object[100];
//        groups = ctx.getObjectAttributes("memberOf");
//
//        //LOGGER.debug("Attributes: {}", attributes);
//
//        Set<GrantedAuthority> authority = new HashSet<GrantedAuthority>();
//
//        for(Object group: groups)
//        {
//
//            if (group.toString().toLowerCase().contains("GROUP_NAME".toLowerCase()) == true)
//            {
//                authority.add(new SimpleGrantedAuthority("ROLE_USER"));
//                break;          
//            }
//        }
//
//        User userDetails = new User(username, "", false, false, false, false, authority);
//        return userDetails;
//    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        // default
    }
}
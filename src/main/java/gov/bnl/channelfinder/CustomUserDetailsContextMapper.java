package gov.bnl.channelfinder;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

@Configuration
public class CustomUserDetailsContextMapper extends LdapUserDetailsMapper implements UserDetailsContextMapper {

    private Log log = LogFactory.getLog(this.getClass());

    @Override
    public LdapUserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {

        LdapUserDetailsImpl details = (LdapUserDetailsImpl) super.mapUserFromContext(ctx, username, authorities);
        log.info("DN from ctx: " + ctx.getDn()); // return correct DN
        log.info("Attributes size: " + ctx.getAttributes().size()); // always returns 0

        return new CustomUserDetails(details);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        // default
    }
}
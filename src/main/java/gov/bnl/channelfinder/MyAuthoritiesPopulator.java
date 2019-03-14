package gov.bnl.channelfinder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("myAuthPopulator")
public class MyAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    @Autowired
    private UserDetailsService userServices;
    //static final Logger LOG = LoggerFactory.getLogger(MyAuthoritiesPopulator.class);

    @Transactional(readOnly=true)
    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
        try{
            UserDetails user = userServices.loadUserByUsername(username);
            if (user==null){
                //LOG.error("Threw exception in MyAuthoritiesPopulator::getGrantedAuthorities : User doesn't exist into DART database" );
            }
            else{
                //Use this if a user can have different roles
//              for(Role role : user.getRole()) {
//                  authorities.add(new SimpleGrantedAuthority(role.getRole()));
//              }
                authorities = (Set<GrantedAuthority>) (user.getAuthorities());
                return (Collection<? extends GrantedAuthority>)authorities;
            }
        }catch(Exception e){
            //LOG.error("Threw exception in MyAuthoritiesPopulator::getGrantedAuthorities : " + ErrorExceptionBuilder.buildErrorResponse(e)); }
        return (Collection<? extends GrantedAuthority>)authorities;
    }         return (Collection<? extends GrantedAuthority>)authorities;

    }
}

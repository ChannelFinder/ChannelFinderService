package gov.bnl.channelfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapRdn;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("myAuthPopulator")
public class MyAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    @Autowired
    private UserDetailsService userServices;
    static final Logger log = Logger.getLogger(MyAuthoritiesPopulator.class.getName());

    @Transactional(readOnly = true)
    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
//        try {
//            UserDetails user = userServices.loadUserByUsername(username);
//            if (user == null) {
//                log.severe(
//                        "Threw exception in MyAuthoritiesPopulator::getGrantedAuthorities : User doesn't exist into DART database");
//            } else {
//                // Use this if a user can have different roles
////              for(Role role : user.getRole()) {
////                  authorities.add(new SimpleGrantedAuthority(role.getRole()));
////              }
//                authorities = (Set<GrantedAuthority>) (user.getAuthorities());
//                return (Collection<? extends GrantedAuthority>) authorities;
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Threw exception in MyAuthoritiesPopulator::getGrantedAuthorities : ", e.getCause());
//        }
        String[] groups = userData.getStringAttributes("memberOf");
        for (String group: groups) {
        	LdapRdn authority = new DistinguishedName(group).removeLast();
        	authorities.add(new SimpleGrantedAuthority (authority.getValue()));
        }
//        return (Collection<? extends GrantedAuthority>) authorities;
        return authorities;
    }

}

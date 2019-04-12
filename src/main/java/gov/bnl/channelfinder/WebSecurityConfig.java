package gov.bnl.channelfinder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    @Qualifier("myAuthPopulator")
    MyAuthoritiesPopulator myAuthPopulator;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.authorizeRequests().anyRequest().authenticated();
        http.httpBasic();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(HttpMethod.GET, "/**");
        // TODO a temporary workaround for lbnl test installation
//      web.ignoring().anyRequest();
    }
    /**
     * LDAP configuration properties
     */
    @Value("${ldap.enabled:false}")
    boolean ldap_enabled;
    @Value("${ldap.urls:ldaps://localhost:389/}")
    String ldap_url;
    @Value("${ldap.base.dn}")
    String ldap_base_dn;
    @Value("${ldap.user.dn.pattern}")
    String ldap_user_dn_pattern;

    /**
     * File based authentication
     */
    @Value("${file.auth.enabled:true}")
    boolean file_enabled;

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {

        boolean ldap_enabled = true;

        if (ldap_enabled) {
            auth.ldapAuthentication()
                    .userDnPatterns(ldap_user_dn_pattern)
                    .ldapAuthoritiesPopulator(myAuthPopulator)
                    .groupSearchBase("ou=Groups")
                    .contextSource()
                    .url(ldap_url);
//                    .and()
//                    .userDetailsContextMapper(userDetailsContextMapper());
        }
        auth.inMemoryAuthentication().withUser("admin").password(encoder().encode("adminPass")).roles("ADMIN").and()
                .withUser("user").password(encoder().encode("userPass")).roles("USER");

        auth.userDetailsService(new MyUserDetailsService());
    }

    @Bean
    public UserDetailsContextMapper userDetailsContextMapper() {
        return new CustomUserDetailsContextMapper();
    }
    
    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

}
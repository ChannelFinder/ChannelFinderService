package org.phoebus.channelfinder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.ldap.EmbeddedLdapServerContextSourceFactoryBean;
import org.springframework.security.config.ldap.LdapBindAuthenticationManagerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.PersonContextMapper;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig  {

    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.authorizeRequests().anyRequest().authenticated();
        http.httpBasic();
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // Authentication and Authorization is only needed for non search/query operations
        return (web) -> web.ignoring().antMatchers(HttpMethod.GET, "/**");
    }

    /**
     * External LDAP configuration properties
     */
    @Value("${ldap.enabled:false}")
    boolean ldap_enabled;
    @Value("${ldap.urls:ldaps://localhost:389/}")
    String ldap_url;
    @Value("${ldap.user.dn.pattern}")
    String ldap_user_dn_pattern;
    @Value("${ldap.groups.search.base}")
    String ldap_groups_search_base;
    @Value("${ldap.groups.search.pattern}")
    String ldap_groups_search_pattern;

    /**
     * Embedded LDAP configuration properties
     */
    @Value("${embedded_ldap.enabled:false}")
    boolean embedded_ldap_enabled;
    @Value("${embedded_ldap.urls:ldaps://localhost:389/}")
    String embedded_ldap_url;
    @Value("${embedded_ldap.user.dn.pattern}")
    String embedded_ldap_user_dn_pattern;
    @Value("${embedded_ldap.groups.search.base}")
    String embedded_ldap_groups_search_base;
    @Value("${embedded_ldap.groups.search.pattern}")
    String embedded_ldap_groups_search_pattern;

    /**
     * Demo authorization based on in memory user credentials
     */
    @Value("${demo_auth.enabled:false}")
    boolean demo_auth_enabled;
    @Value("${demo_auth.delimiter.roles::}")
    String demo_auth_delimiter_roles;
    @Value("${demo_auth.users}")
    String[] demo_auth_users;
    @Value("${demo_auth.pwds}")
    String[] demo_auth_pwds;
    @Value("${demo_auth.roles}")
    String[] demo_auth_roles;

    /**
     * File based authentication
     */
    @Value("${file.auth.enabled:true}")
    boolean file_enabled;

    @Bean
    public EmbeddedLdapServerContextSourceFactoryBean contextSourceFactoryBean() {
        EmbeddedLdapServerContextSourceFactoryBean contextSourceFactoryBean =
                EmbeddedLdapServerContextSourceFactoryBean.fromEmbeddedLdapServer();
        contextSourceFactoryBean.setPort(0);
        return contextSourceFactoryBean;
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration auth) {

        if (ldap_enabled) {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
            contextSource.afterPropertiesSet();
            DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
            myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
            myAuthPopulator.setSearchSubtree(true);
            myAuthPopulator.setIgnorePartialResultException(true);

            LdapBindAuthenticationManagerFactory factory =
                    new LdapBindAuthenticationManagerFactory(contextSource);
            factory.setLdapAuthoritiesPopulator(myAuthPopulator);
            factory.setUserDnPatterns(ldap_user_dn_pattern);
            factory.setUserDetailsContextMapper(new PersonContextMapper());
            return factory.createAuthenticationManager();
        }

        if (embedded_ldap_enabled) {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(embedded_ldap_url);
            contextSource.afterPropertiesSet();
            DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
            myAuthPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
            myAuthPopulator.setSearchSubtree(true);
            myAuthPopulator.setIgnorePartialResultException(true);

            LdapBindAuthenticationManagerFactory factory =
                    new LdapBindAuthenticationManagerFactory(contextSource);
            factory.setLdapAuthoritiesPopulator(myAuthPopulator);
            factory.setUserDnPatterns(embedded_ldap_user_dn_pattern);
            factory.setUserSearchBase("ou=Group");
            factory.setUserDetailsContextMapper(new PersonContextMapper());
            return factory.createAuthenticationManager();

        }
        return null;
    }
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {

        if (demo_auth_enabled &&  (demo_auth_users != null
                && demo_auth_pwds != null
                && demo_auth_roles != null
                && demo_auth_users.length > 0
                && demo_auth_users.length == demo_auth_pwds.length
                && demo_auth_pwds.length == demo_auth_roles.length)) {

            List<UserDetails> users = new ArrayList<>();
            for (int i=0; i<demo_auth_users.length; i++) {
                String[] userroles = demo_auth_roles[i].split(demo_auth_delimiter_roles);
                if (userroles.length > 0) {
                    users.add(User.withUsername(demo_auth_users[i]).password(encoder().encode(demo_auth_pwds[i])).roles(userroles).build());
                }
            }
            return new InMemoryUserDetailsManager(users);

        }
        return null;
    }
    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

}
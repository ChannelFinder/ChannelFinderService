package org.phoebus.channelfinder;

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
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.authorizeRequests().anyRequest().authenticated();
        http.httpBasic();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Authentication and Authorization is only needed for non search/query operations
        web.ignoring().antMatchers(HttpMethod.GET, "/**");
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

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {

        if (ldap_enabled) {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
            contextSource.afterPropertiesSet();

            DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
            myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
            myAuthPopulator.setSearchSubtree(true);
            myAuthPopulator.setIgnorePartialResultException(true);

            auth.ldapAuthentication()
                .userDnPatterns(ldap_user_dn_pattern)
                .ldapAuthoritiesPopulator(myAuthPopulator)
                .contextSource(contextSource);
        }

        if (embedded_ldap_enabled) {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(embedded_ldap_url);
            contextSource.afterPropertiesSet();

            DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
            myAuthPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
            myAuthPopulator.setSearchSubtree(true);
            myAuthPopulator.setIgnorePartialResultException(true);


            auth.ldapAuthentication()
                    .userDnPatterns(embedded_ldap_user_dn_pattern)
                    .ldapAuthoritiesPopulator(myAuthPopulator)
                    .groupSearchBase("ou=Group")
                    .contextSource(contextSource);

        }

        if (demo_auth_enabled) {
            // read from configuration, no default content
            //     interpret users, pwds, roles
            //     user may have multiple roles

            if (demo_auth_users != null
                    && demo_auth_pwds != null
                    && demo_auth_roles != null
                    && demo_auth_users.length > 0
                    && demo_auth_users.length == demo_auth_pwds.length
                    && demo_auth_pwds.length == demo_auth_roles.length) {

                for (int i=0; i<demo_auth_users.length; i++) {
                    String[] userroles = demo_auth_roles[i].split(demo_auth_delimiter_roles);
                    if (userroles != null && userroles.length > 0) {
                        auth.inMemoryAuthentication()
                                .withUser(demo_auth_users[i])
                                .password(encoder().encode(demo_auth_pwds[i]))
                                .roles(userroles);
                    }
                }
            }
        }
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

}
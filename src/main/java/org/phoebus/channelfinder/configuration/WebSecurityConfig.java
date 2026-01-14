package org.phoebus.channelfinder.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // CSRF disabled: application is a stateless REST API using HTTP Basic auth.
    // No session or cookie-based authentication is used, so CSRF attacks are not applicable.
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(withDefaults())
        .build();
  }

  @Bean
  public WebSecurityCustomizer ignoringCustomizer() {
    // Authentication and Authorization is only needed for non search/query operations
    return web -> web.ignoring().requestMatchers(HttpMethod.GET, "/**");
  }

  /** External LDAP configuration properties */
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

  /** Embedded LDAP configuration properties */
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

  /** Demo authorization based on in memory user credentials */
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

  /** File based authentication */
  @Value("${file.auth.enabled:true}")
  boolean file_enabled;

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration configuration, List<AuthenticationProvider> providers) {
    return new ProviderManager(providers);
  }

  @Bean
  @ConditionalOnProperty(name = "ldap.enabled", havingValue = "true")
  public AuthenticationProvider ldapAuthProvider() {

    DefaultSpringSecurityContextSource contextSource =
        new DefaultSpringSecurityContextSource(ldap_url);
    contextSource.afterPropertiesSet();

    DefaultLdapAuthoritiesPopulator authPopulator =
        new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
    authPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
    authPopulator.setSearchSubtree(true);
    authPopulator.setIgnorePartialResultException(true);

    BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
    bindAuthenticator.setUserDnPatterns(new String[] {ldap_user_dn_pattern});

    return new LdapAuthenticationProvider(bindAuthenticator, authPopulator);
  }

  @Bean
  @ConditionalOnProperty(name = "embedded_ldap.enabled", havingValue = "true")
  public AuthenticationProvider embeddedLdapAuthProvider() {

    DefaultSpringSecurityContextSource contextSource =
        new DefaultSpringSecurityContextSource(embedded_ldap_url);
    contextSource.afterPropertiesSet();

    DefaultLdapAuthoritiesPopulator authPopulator =
        new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
    authPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
    authPopulator.setSearchSubtree(true);
    authPopulator.setIgnorePartialResultException(true);

    BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
    bindAuthenticator.setUserDnPatterns(new String[] {embedded_ldap_user_dn_pattern});

    return new LdapAuthenticationProvider(bindAuthenticator, authPopulator);
  }

  @Bean
  @Conditional(EmbeddedLdapCondition.class)
  public AuthenticationProvider demoAuthProvider() {

    InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    PasswordEncoder encoder = encoder();

    for (int i = 0; i < demo_auth_users.length; i++) {
      String[] userroles = demo_auth_roles[i].split(demo_auth_delimiter_roles);

      manager.createUser(
          User.withUsername(demo_auth_users[i])
              .password(encoder.encode(demo_auth_pwds[i]))
              .roles(userroles)
              .build());
    }

    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(manager);
    provider.setPasswordEncoder(encoder);
    return provider;
  }

  @Bean
  public PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }

  private static class EmbeddedLdapCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

      Environment environment = context.getEnvironment();

      Boolean isDemoAuthEnabled =
          environment.getProperty("demo_auth.enabled", Boolean.class, false);
      String[] demoAuthPwds = environment.getProperty("demo_auth.pwds", String[].class);
      String[] demoAuthRoles = environment.getProperty("demo_auth.roles", String[].class);
      String[] demoAuthUsers = environment.getProperty("demo_auth.users", String[].class);

      return isDemoAuthEnabled
          && !ArrayUtils.isEmpty(demoAuthUsers)
          && !ArrayUtils.isEmpty(demoAuthPwds)
          && !ArrayUtils.isEmpty(demoAuthRoles)
          && demoAuthUsers.length == demoAuthPwds.length
          && demoAuthPwds.length == demoAuthRoles.length;
    }
  }
}

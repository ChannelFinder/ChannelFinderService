package gov.bnl.channelfinder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Override
	public void configure(WebSecurity web) throws Exception {
	    web
	        .ignoring()
	        .antMatchers(HttpMethod.GET, "/tag**");
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().anyRequest().authenticated();
		http.httpBasic();
		http.formLogin().permitAll().and().logout().logoutSuccessUrl("/");
	}

	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.ldapAuthentication()
				.userDnPatterns("uid={0},ou=people")
				.groupSearchBase("ou=groups")
				.contextSource()
				.url("ldap://localhost:8389/dc=springframework,dc=org")
				//.url("ldap://localhost:8080")
				.and()
				.passwordCompare()
					.passwordEncoder(new LdapShaPasswordEncoder())
					.passwordAttribute("userPassword");
		auth.inMemoryAuthentication()
        .withUser("admin").password(encoder().encode("adminPass")).roles("ADMIN")
        .and()
        .withUser("user").password(encoder().encode("userPass")).roles("USER");
	}
	
	@Bean
	public PasswordEncoder  encoder() {
	    return new BCryptPasswordEncoder();
	}
	
}
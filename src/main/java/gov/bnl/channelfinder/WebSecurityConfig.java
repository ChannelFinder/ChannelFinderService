package gov.bnl.channelfinder;

import java.io.IOException;
import java.util.function.Supplier;

import javax.naming.directory.InitialDirContext;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.UserDetailsServiceLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
    @Qualifier("myAuthPopulator")
    LdapAuthoritiesPopulator myAuthPopulator;
	
	@Autowired
	private MyBasicAuthenticationEntryPoint authenticationEntryPoint;

	//	@Bean
	//	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
	//	    http
	//	        // ...
	//	        .redirectToHttps();
	//	    return http.build();
	//	}

	@Override
	public void configure(WebSecurity web) throws Exception {
//		web.ignoring().antMatchers(HttpMethod.GET, "/**");
		// TODO a temporary workaround for lbnl test installation
		web.ignoring().anyRequest();
	}

	//	//@Bean
	//    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
	//         http.authorizeExchange().anyExchange().authenticated();
	//         http.httpBasic();
	//         //.authenticationEntryPoint(authenticationEntryPoint);
	//         http.formLogin();
	//         //.permitAll().and().logout().logoutSuccessUrl("/");
	//         http.redirectToHttps();
	//         return http.build();
	//    }

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();
		http.authorizeRequests().anyRequest().authenticated();
		//http.requiresChannel().anyRequest().requiresSecure();
//		http.portMapper().http(8080).mapsTo(8443);
		http.httpBasic().authenticationEntryPoint(authenticationEntryPoint);
		http.formLogin().successHandler(new AuthenticationLoginSuccessHandler()).permitAll().and().logout().logoutSuccessUrl("/");
        //http.successHandler(new AuthenticationLoginSuccessHandler());
		//http.headers().httpStrictTransportSecurity();
		//http.portMapper().http(80).mapsTo(443);
		//http.antMatcher("/**").requiresChannel().anyRequest().requiresSecure();
	}
	
    private class AuthenticationLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
                throws IOException, ServletException {
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {

		boolean ldap_enabled = true;
				
		if(ldap_enabled) {
			auth.ldapAuthentication()
			.userDnPatterns("uid={0},ou=People")
			.groupSearchBase("ou=Groups")
			.contextSource()
			//.url("ldap://localhost:8389/dc=springframework,dc=org")
			//.url("ldap://localhost:8389/dc=cf-test,dc=local")
			//.url("ldap://ldap01.nsls2.bnl.gov/dc=bnl,dc=gov")
			.url("ldaps://controlns02.nsls2.bnl.gov/dc=bnl,dc=gov")
			.and()
			.ldapAuthoritiesPopulator(myAuthPopulator);
//			.port(639)
//			.managerDn("cn=manager,ou=institution,ou=people,dc=bnl,dc=gov") 
//			.managerPassword("password")
//			.and()
//			.passwordCompare()
//			.passwordEncoder(new LdapShaPasswordEncoder())
//			.passwordAttribute("userPassword");
		}
		auth.inMemoryAuthentication()
		.withUser("admin").password(encoder().encode("adminPass")).roles("ADMIN")
		.and().withUser("user").password(encoder().encode("userPass")).roles("USER");

		auth.userDetailsService(new MyUserDetailsService());
	}

	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}
	
//	private String allPassword = "password";
//
//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder) throws Exception {
//
//        SSLContext sslContext = SSLContextBuilder
//                .create()
//                .loadKeyMaterial(ResourceUtils.getFile("classpath:keystore/cf.p12"), allPassword.toCharArray(), allPassword.toCharArray())
//                .loadTrustMaterial(ResourceUtils.getFile("classpath:certs"), allPassword.toCharArray())
//                .build();
//
//        HttpClient client = HttpClients.custom()
//                .setSSLContext(sslContext)
//                .build();
//
//        return builder
//                .requestFactory(new HttpComponentsClientHttpRequestFactory(client))
//                .build();
//    }

}
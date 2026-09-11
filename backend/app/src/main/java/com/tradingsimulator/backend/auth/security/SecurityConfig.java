package com.tradingsimulator.backend.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tradingsimulator.backend.auth.AuthPaths;
import com.tradingsimulator.backend.web.ApiPaths;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'";
	private static final String ACTUATOR_HEALTH = "/actuator/health";
	private static final String ACTUATOR_HEALTH_GROUPS = ACTUATOR_HEALTH + "/**";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(headers -> headers
						.frameOptions(frame -> frame.deny())
						.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(HttpMethod.POST, AuthPaths.PUBLIC_ENDPOINTS.toArray(String[]::new)).permitAll()
						.requestMatchers(ApiPaths.API + ApiPaths.HEALTH).permitAll()
						.requestMatchers(ACTUATOR_HEALTH, ACTUATOR_HEALTH_GROUPS).permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}

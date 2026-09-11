package com.tradingsimulator.backend.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = bearerToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				ParsedAccessToken principal = jwtService.verify(token);
				var authentication = new UsernamePasswordAuthenticationToken(
						principal, null,
						List.of(new SimpleGrantedAuthority(principal.role().authority())));
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			catch (JwtException invalidToken) {
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}

	private static String bearerToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return header.substring(BEARER_PREFIX.length());
	}
}

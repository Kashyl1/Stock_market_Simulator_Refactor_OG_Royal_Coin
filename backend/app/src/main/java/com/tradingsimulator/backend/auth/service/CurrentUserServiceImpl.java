package com.tradingsimulator.backend.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.security.ParsedAccessToken;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.CommonError;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

	private final UserRepository users;

	@Override
	@Transactional(readOnly = true)
	public AuthenticatedUser current() {
		User user = users.findById(principal().userId()).orElseThrow(() -> new AppException(CommonError.AUTHENTICATION_REQUIRED));
		user.getStatus().requireSignInAllowed();
		return AuthenticatedUser.of(user);
	}

	private static ParsedAccessToken principal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof ParsedAccessToken principal)) {
			throw new AppException(CommonError.AUTHENTICATION_REQUIRED);
		}
		return principal;
	}
}

package com.tradingsimulator.backend.auth.token;

import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.common.error.ErrorCode;

public interface OneTimeTokenService {

	OneTimeToken issue();

	String hash(String rawToken);

	String issueFor(Long userId, TokenType tokenType);

	UserToken consume(String rawToken, TokenType tokenType, ErrorCode invalid, ErrorCode expired);
}

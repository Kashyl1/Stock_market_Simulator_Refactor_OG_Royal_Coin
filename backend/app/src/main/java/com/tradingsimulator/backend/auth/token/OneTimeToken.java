package com.tradingsimulator.backend.auth.token;

import com.tradingsimulator.backend.common.Masking;

public record OneTimeToken(String raw, String hash) {

	@Override
	public String toString() {
		return "OneTimeToken[raw=" + Masking.MASK + ", hash=" + hash + "]";
	}
}

package com.tradingsimulator.backend.auth.token;

public record OneTimeToken(String raw, String hash) {
}

package com.tradingsimulator.backend.auth.service;

public interface LoginService {

	LoginResult login(String email, String password, ClientDetails client);
}

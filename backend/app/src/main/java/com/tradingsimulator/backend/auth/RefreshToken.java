package com.tradingsimulator.backend.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_token")
public class RefreshToken extends RefreshTokenJpa {
}

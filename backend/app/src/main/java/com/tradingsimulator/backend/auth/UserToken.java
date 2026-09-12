package com.tradingsimulator.backend.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_token")
public class UserToken extends UserTokenJpa {
}

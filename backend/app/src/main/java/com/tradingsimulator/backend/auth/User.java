package com.tradingsimulator.backend.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User extends UserJpa {
}

package com.tradingsimulator.backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface User extends JpaRepository<UserJpa, Long> {

	@Query("select u from UserJpa u where lower(u.email) = lower(:email)")
	Optional<UserJpa> findByEmailIgnoreCase(String email);

	@Query("select case when count(u) > 0 then true else false end from UserJpa u where lower(u.email) = lower(:email)")
	boolean existsByEmailIgnoreCase(String email);
}

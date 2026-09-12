package com.tradingsimulator.backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

	@Query("select u from User u where lower(u.email) = lower(:email)")
	Optional<User> findByEmailIgnoreCase(String email);

	@Query("select case when count(u) > 0 then true else false end from User u where lower(u.email) = lower(:email)")
	boolean existsByEmailIgnoreCase(String email);
}

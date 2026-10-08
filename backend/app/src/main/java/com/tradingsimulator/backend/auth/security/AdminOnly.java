package com.tradingsimulator.backend.auth.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize(AdminOnly.HAS_ADMIN_ROLE)
public @interface AdminOnly {

	String HAS_ADMIN_ROLE = "hasRole('ADMIN')";
}

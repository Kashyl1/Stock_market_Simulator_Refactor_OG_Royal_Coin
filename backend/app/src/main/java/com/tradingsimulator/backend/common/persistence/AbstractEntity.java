package com.tradingsimulator.backend.common.persistence;

import java.time.Instant;

import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@CreatedBy
	@Column(name = "created_by", nullable = false, updatable = false)
	private String createdBy;

	@CreatedDate
	@Column(name = "created_when", nullable = false, updatable = false)
	private Instant createdWhen;

	@LastModifiedBy
	@Column(name = "changed_by", nullable = false)
	private String changedBy;

	@LastModifiedDate
	@Column(name = "changed_when", nullable = false)
	private Instant changedWhen;

	@Version
	@Column(nullable = false)
	private Long version;

	@Override
	public final boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		Class<?> thisType = effectiveClass(this);
		Class<?> otherType = effectiveClass(other);
		if (thisType != otherType) {
			return false;
		}
		AbstractEntity that = (AbstractEntity) other;
		return id != null && id.equals(that.id);
	}

	@Override
	public final int hashCode() {
		return effectiveClass(this).hashCode();
	}

	private static Class<?> effectiveClass(Object entity) {
		return entity instanceof HibernateProxy proxy
				? proxy.getHibernateLazyInitializer().getPersistentClass()
				: entity.getClass();
	}
}

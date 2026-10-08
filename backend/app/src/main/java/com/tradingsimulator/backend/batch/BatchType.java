package com.tradingsimulator.backend.batch;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.scheduling.support.CronExpression;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_type")
public class BatchType extends BatchTypeJpa {

	public static BatchType scheduled(BatchTypeCode code, String name, String description, String cron, String zone) {
		BatchType type = new BatchType();
		type.setCode(code);
		type.setName(name);
		type.setDescription(description);
		type.setCron(cron);
		type.setZone(zone);
		type.setEnabled(true);
		return type;
	}

	public boolean runsOnSchedule() {
		return isEnabled() && getCron() != null;
	}

	public Optional<Instant> nextRunAfter(Instant moment) {
		if (!runsOnSchedule()) {
			return Optional.empty();
		}
		ZonedDateTime next = CronExpression.parse(getCron()).next(moment.atZone(ZoneId.of(getZone())));
		return Optional.ofNullable(next).map(ZonedDateTime::toInstant);
	}
}

package com.tradingsimulator.backend.batch.web;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tradingsimulator.backend.auth.security.AdminOnly;
import com.tradingsimulator.backend.batch.BatchItemStatus;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchTypeCode;
import com.tradingsimulator.backend.batch.service.BatchAdminService;
import com.tradingsimulator.backend.batch.service.BatchItemView;
import com.tradingsimulator.backend.batch.service.BatchJobFilter;
import com.tradingsimulator.backend.batch.service.BatchJobView;
import com.tradingsimulator.backend.batch.service.BatchTypeView;
import com.tradingsimulator.backend.batch.web.dto.PlanBatchJobRequest;
import com.tradingsimulator.backend.web.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(BatchAdminPaths.BASE)
@AdminOnly
@RequiredArgsConstructor
public class BatchAdminController {

	private final BatchAdminService batch;

	@GetMapping(BatchAdminPaths.TYPES)
	public List<BatchTypeView> types() {
		return batch.types();
	}

	@GetMapping(BatchAdminPaths.JOBS)
	public PageResponse<BatchJobView> jobs(@RequestParam(required = false) Set<BatchJobStatus> status, @RequestParam(required = false) BatchTypeCode type,
			@RequestParam(required = false) boolean unacknowledged, @RequestParam(defaultValue = PageResponse.FIRST_PAGE) @PositiveOrZero int page,
			@RequestParam(defaultValue = PageResponse.DEFAULT_SIZE) @Positive @Max(PageResponse.MAX_SIZE) int size) {
		return batch.jobs(new BatchJobFilter(status, type, unacknowledged), page, size);
	}

	@GetMapping(BatchAdminPaths.JOB)
	public BatchJobView job(@PathVariable Long id) {
		return batch.job(id);
	}

	@GetMapping(BatchAdminPaths.JOB_ITEMS)
	public PageResponse<BatchItemView> items(@PathVariable Long id, @RequestParam(required = false) BatchItemStatus status,
			@RequestParam(defaultValue = PageResponse.FIRST_PAGE) @PositiveOrZero int page,
			@RequestParam(defaultValue = PageResponse.DEFAULT_SIZE) @Positive @Max(PageResponse.MAX_SIZE) int size) {
		return batch.items(id, status, page, size);
	}

	@PostMapping(BatchAdminPaths.JOBS)
	@ResponseStatus(HttpStatus.CREATED)
	public BatchJobView plan(@Valid @RequestBody PlanBatchJobRequest request) {
		return batch.plan(request.type(), request.scheduledFor());
	}

	@DeleteMapping(BatchAdminPaths.JOB)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long id) {
		batch.remove(id);
	}

	@PostMapping(BatchAdminPaths.JOB_STOP)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void stop(@PathVariable Long id) {
		batch.stop(id);
	}

	@PostMapping(BatchAdminPaths.JOB_ACKNOWLEDGE)
	public BatchJobView acknowledge(@PathVariable Long id) {
		return batch.acknowledge(id);
	}
}

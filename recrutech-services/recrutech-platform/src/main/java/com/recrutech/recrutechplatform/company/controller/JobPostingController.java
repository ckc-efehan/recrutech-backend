package com.recrutech.recrutechplatform.company.controller;

import com.recrutech.recrutechplatform.company.dto.JobPostingCreateRequest;
import com.recrutech.recrutechplatform.company.dto.JobPostingResponse;
import com.recrutech.recrutechplatform.company.dto.JobPostingUpdateRequest;
import com.recrutech.recrutechplatform.company.model.JobPostingStatus;
import com.recrutech.recrutechplatform.company.service.JobPostingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing job postings.
 * Provides endpoints for CRUD operations and status transitions.
 */
@RestController
@RequestMapping("/companies/{companyId}/job-postings")
public class JobPostingController {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;
    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final String SORT_SEPARATOR = ",";
    private static final String SORT_DIRECTION_ASC = "asc";

    private final JobPostingService service;

    public JobPostingController(JobPostingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<JobPostingResponse> create(
            @PathVariable String companyId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody JobPostingCreateRequest request) {
        JobPostingResponse created = service.create(companyId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getById(
            @PathVariable String companyId,
            @PathVariable String id) {
        return ResponseEntity.ok(service.getById(companyId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobPostingResponse> update(
            @PathVariable String companyId,
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody JobPostingUpdateRequest request) {
        return ResponseEntity.ok(service.update(companyId, id, userId, request));
    }

    @GetMapping
    public ResponseEntity<Page<JobPostingResponse>> list(
            @PathVariable String companyId,
            @RequestParam(required = false) JobPostingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {
        Pageable pageable = createPageable(page, size, sort);
        return ResponseEntity.ok(service.list(companyId, status, pageable));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<JobPostingResponse> publish(
            @PathVariable String companyId,
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(service.publish(companyId, id, userId));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<JobPostingResponse> close(
            @PathVariable String companyId,
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(service.close(companyId, id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String companyId,
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        service.softDelete(companyId, id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a Pageable object from pagination parameters.
     * Applies validation to ensure page and size are within acceptable bounds.
     * 
     * @param page the page number (will be clamped to minimum 0)
     * @param size the page size (will be clamped between 1 and 100)
     * @param sort the sort specification in format "field,direction" (e.g., "createdAt,desc")
     * @return a validated Pageable object
     */
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(SORT_SEPARATOR);
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase(SORT_DIRECTION_ASC)) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        int validatedPage = Math.max(page, DEFAULT_PAGE_NUMBER);
        int validatedSize = Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
        
        return PageRequest.of(validatedPage, validatedSize, Sort.by(direction, sortField));
    }
}

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

@RestController
@RequestMapping("/companies/{companyId}/job-postings")
public class JobPostingController {

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
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = toPageable(page, size, sort);
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

    private Pageable toPageable(int page, int size, String sort) {
        // sort format: "field,dir" e.g., "createdAt,desc"
        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, field));
    }
}

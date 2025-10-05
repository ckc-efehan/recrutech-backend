package com.recrutech.recrutechplatform.application.controller;

import com.recrutech.recrutechplatform.application.dto.ApplicationResponse;
import com.recrutech.recrutechplatform.application.dto.ApplicationUpdateStatusRequest;
import com.recrutech.recrutechplatform.application.mapper.ApplicationMapper;
import com.recrutech.recrutechplatform.application.model.Application;
import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import com.recrutech.recrutechplatform.application.service.ApplicationService;
import com.recrutech.recrutechplatform.application.service.MinioStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing job applications.
 * Provides endpoints for application submission, status management, and querying.
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;
    private static final String DEFAULT_SORT = "submittedAt,desc";
    private static final String SORT_SEPARATOR = ",";
    private static final String SORT_DIRECTION_ASC = "asc";

    private final ApplicationService service;
    private final MinioStorageService storageService;

    public ApplicationController(ApplicationService service, MinioStorageService storageService) {
        this.service = service;
        this.storageService = storageService;
    }

    /**
     * Submit a new application to a job posting with PDF documents.
     * POST /applications
     * Accepts multipart/form-data with required PDFs for cover letter and resume,
     * and optional portfolio PDF.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplicationResponse> submit(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("applicantId") String applicantId,
            @RequestParam("jobPostingId") String jobPostingId,
            @RequestParam("coverLetter") MultipartFile coverLetter,
            @RequestParam("resume") MultipartFile resume,
            @RequestParam(value = "portfolio", required = false) MultipartFile portfolio) {
        
        // Store the uploaded files
        String coverLetterPath = storageService.storeFile(coverLetter, "coverLetter", applicantId);
        String resumePath = storageService.storeFile(resume, "resume", applicantId);
        String portfolioPath = (portfolio != null && !portfolio.isEmpty()) 
                ? storageService.storeFile(portfolio, "portfolio", applicantId) 
                : null;

        // Submit the application with file paths
        Application application = service.submit(
                applicantId,
                jobPostingId,
                userId,
                coverLetterPath,
                resumePath,
                portfolioPath
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationMapper.toResponse(application));
    }

    /**
     * Get a specific application by ID.
     * GET /applications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable String id) {
        Application application = service.getById(id);
        return ResponseEntity.ok(ApplicationMapper.toResponse(application));
    }

    /**
     * Get all applications for a specific applicant.
     * GET /applications/applicant/{applicantId}
     */
    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Page<ApplicationResponse>> getByApplicant(
            @PathVariable String applicantId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {
        Pageable pageable = createPageable(page, size, sort);
        Page<Application> applications = service.getByApplicant(applicantId, status, pageable);
        return ResponseEntity.ok(applications.map(ApplicationMapper::toResponse));
    }

    /**
     * Get all applications for a specific job posting.
     * GET /applications/job-posting/{jobPostingId}
     */
    @GetMapping("/job-posting/{jobPostingId}")
    public ResponseEntity<Page<ApplicationResponse>> getByJobPosting(
            @PathVariable String jobPostingId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {
        Pageable pageable = createPageable(page, size, sort);
        Page<Application> applications = service.getByJobPosting(jobPostingId, status, pageable);
        return ResponseEntity.ok(applications.map(ApplicationMapper::toResponse));
    }

    /**
     * Get all applications for a company's job postings.
     * GET /applications/company/{companyId}
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<ApplicationResponse>> getByCompany(
            @PathVariable String companyId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {
        Pageable pageable = createPageable(page, size, sort);
        Page<Application> applications = service.getByCompany(companyId, status, pageable);
        return ResponseEntity.ok(applications.map(ApplicationMapper::toResponse));
    }

    /**
     * Update application status (for HR/recruiter).
     * PUT /applications/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ApplicationUpdateStatusRequest request) {
        Application application = service.updateStatus(
                id,
                request.status(),
                userId,
                request.hrNotes(),
                request.rejectionReason()
        );
        return ResponseEntity.ok(ApplicationMapper.toResponse(application));
    }

    /**
     * Withdraw an application (for applicant).
     * POST /applications/{id}/withdraw
     */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApplicationResponse> withdraw(
            @PathVariable String id,
            @RequestParam String applicantId,
            @RequestHeader("X-User-Id") String userId) {
        Application application = service.withdraw(id, applicantId, userId);
        return ResponseEntity.ok(ApplicationMapper.toResponse(application));
    }

    /**
     * Soft delete an application.
     * DELETE /applications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        service.softDelete(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Download cover letter PDF for an application.
     * GET /applications/{id}/cover-letter
     */
    @GetMapping("/{id}/cover-letter")
    public ResponseEntity<Resource> downloadCoverLetter(
            @PathVariable String id,
            HttpServletRequest request) {
        Application application = service.getById(id);
        return downloadDocument(application.getCoverLetterPath(), request);
    }

    /**
     * Download resume PDF for an application.
     * GET /applications/{id}/resume
     */
    @GetMapping("/{id}/resume")
    public ResponseEntity<Resource> downloadResume(
            @PathVariable String id,
            HttpServletRequest request) {
        Application application = service.getById(id);
        return downloadDocument(application.getResumePath(), request);
    }

    /**
     * Download portfolio PDF for an application.
     * GET /applications/{id}/portfolio
     */
    @GetMapping("/{id}/portfolio")
    public ResponseEntity<Resource> downloadPortfolio(
            @PathVariable String id,
            HttpServletRequest request) {
        Application application = service.getById(id);
        return downloadDocument(application.getPortfolioPath(), request);
    }

    /**
     * Helper method to download a document.
     * Sets appropriate headers for PDF content type and inline display.
     */
    private ResponseEntity<Resource> downloadDocument(String filename, HttpServletRequest request) {
        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = storageService.loadFileAsResource(filename);

        // Set content type
        String contentType = "application/pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Creates a Pageable object from pagination parameters.
     * Applies validation to ensure page and size are within acceptable bounds.
     *
     * @param page the page number (will be clamped to minimum 0)
     * @param size the page size (will be clamped between 1 and 100)
     * @param sort the sort specification in format "field,direction" (e.g., "submittedAt,desc")
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

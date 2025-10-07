package com.recrutech.recrutechplatform.interview.controller;

import com.recrutech.recrutechplatform.interview.dto.InterviewCreateRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewFeedbackRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewResponse;
import com.recrutech.recrutechplatform.interview.dto.InterviewUpdateRequest;
import com.recrutech.recrutechplatform.interview.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing interviews.
 * Provides endpoints for interview scheduling, updates, status transitions, and feedback collection.
 */
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService service;

    public InterviewController(InterviewService service) {
        this.service = service;
    }

    /**
     * Schedules a new interview.
     * POST /api/interviews
     *
     * @param userId  the ID of the user creating the interview (from authentication context)
     * @param request the interview creation request
     * @return the created interview
     */
    @PostMapping
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InterviewCreateRequest request
    ) {
        InterviewResponse response = service.scheduleInterview(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves an interview by ID.
     * GET /api/interviews/{id}
     *
     * @param id the interview ID
     * @return the interview details
     */
    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterview(@PathVariable String id) {
        InterviewResponse response = service.getInterview(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing interview.
     * PUT /api/interviews/{id}
     *
     * @param id      the interview ID
     * @param userId  the ID of the user updating the interview
     * @param request the update request
     * @return the updated interview
     */
    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponse> updateInterview(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InterviewUpdateRequest request
    ) {
        InterviewResponse response = service.updateInterview(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an interview (soft delete).
     * DELETE /api/interviews/{id}
     *
     * @param id     the interview ID
     * @param userId the ID of the user canceling the interview
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelInterview(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        service.cancelInterview(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all interviews for a specific application.
     * GET /api/interviews/application/{applicationId}
     *
     * @param applicationId the application ID
     * @param page          the page number (0-indexed)
     * @param size          the page size
     * @param sort          the sort field and direction (e.g., "scheduledAt,desc")
     * @return a page of interviews
     */
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<Page<InterviewResponse>> getInterviewsByApplication(
            @PathVariable String applicationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scheduledAt,desc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<InterviewResponse> response = service.getInterviewsByApplication(applicationId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all interviews for a specific interviewer.
     * GET /api/interviews/interviewer/{interviewerId}
     *
     * @param interviewerId the interviewer user ID
     * @param page          the page number (0-indexed)
     * @param size          the page size
     * @param sort          the sort field and direction
     * @return a page of interviews
     */
    @GetMapping("/interviewer/{interviewerId}")
    public ResponseEntity<Page<InterviewResponse>> getInterviewsByInterviewer(
            @PathVariable String interviewerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scheduledAt,desc") String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<InterviewResponse> response = service.getInterviewsByInterviewer(interviewerId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks an interview as completed.
     * POST /api/interviews/{id}/complete
     *
     * @param id     the interview ID
     * @param userId the ID of the user marking completion
     * @return the updated interview
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<InterviewResponse> markAsCompleted(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        InterviewResponse response = service.markAsCompleted(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks an interview as no-show.
     * POST /api/interviews/{id}/no-show
     *
     * @param id     the interview ID
     * @param userId the ID of the user marking no-show
     * @return the updated interview
     */
    @PostMapping("/{id}/no-show")
    public ResponseEntity<InterviewResponse> markAsNoShow(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        InterviewResponse response = service.markAsNoShow(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds feedback to a completed interview.
     * POST /api/interviews/{id}/feedback
     *
     * @param id      the interview ID
     * @param userId  the ID of the user adding feedback
     * @param request the feedback request
     * @return the updated interview
     */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<InterviewResponse> addFeedback(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InterviewFeedbackRequest request
    ) {
        InterviewResponse response = service.addFeedback(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves upcoming interviews within a date range.
     * GET /api/interviews/upcoming
     *
     * @param from the start date-time (ISO format)
     * @param to   the end date-time (ISO format)
     * @return list of upcoming interviews
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<InterviewResponse>> getUpcomingInterviews(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<InterviewResponse> response = service.getUpcomingInterviews(from, to);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves today's interviews.
     * GET /api/interviews/today
     *
     * @return list of today's interviews
     */
    @GetMapping("/today")
    public ResponseEntity<List<InterviewResponse>> getTodaysInterviews() {
        List<InterviewResponse> response = service.getTodaysInterviews();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves upcoming interviews for a specific interviewer.
     * GET /api/interviews/interviewer/{interviewerId}/upcoming
     *
     * @param interviewerId the interviewer user ID
     * @return list of upcoming interviews
     */
    @GetMapping("/interviewer/{interviewerId}/upcoming")
    public ResponseEntity<List<InterviewResponse>> getUpcomingInterviewsByInterviewer(
            @PathVariable String interviewerId
    ) {
        List<InterviewResponse> response = service.getUpcomingInterviewsByInterviewer(interviewerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves today's interviews for a specific interviewer.
     * GET /api/interviews/interviewer/{interviewerId}/today
     *
     * @param interviewerId the interviewer user ID
     * @return list of today's interviews
     */
    @GetMapping("/interviewer/{interviewerId}/today")
    public ResponseEntity<List<InterviewResponse>> getTodaysInterviewsByInterviewer(
            @PathVariable String interviewerId
    ) {
        List<InterviewResponse> response = service.getTodaysInterviewsByInterviewer(interviewerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a Pageable object from request parameters.
     *
     * @param page the page number
     * @param size the page size
     * @param sort the sort specification (e.g., "scheduledAt,desc")
     * @return a Pageable object
     */
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}

package com.recrutech.recrutechplatform.interview.controller;

import com.recrutech.recrutechplatform.interview.dto.InterviewCreateRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewFeedbackRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewResponse;
import com.recrutech.recrutechplatform.interview.dto.InterviewUpdateRequest;
import com.recrutech.recrutechplatform.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Interview Management", description = "APIs for managing interviews including scheduling, updates, status transitions, and feedback collection")
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
    @Operation(
            summary = "Schedule a new interview",
            description = "Creates a new interview with the specified details. The interview will be in SCHEDULED status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Interview successfully created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid authentication", content = @Content)
    })
    @PostMapping
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @Parameter(description = "ID of the user creating the interview", required = true)
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
    @Operation(
            summary = "Get interview by ID",
            description = "Retrieves detailed information about a specific interview"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interview found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterview(
            @Parameter(description = "Interview ID", required = true) @PathVariable String id) {
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
    @Operation(
            summary = "Update an interview",
            description = "Updates the details of an existing interview. Only interviews in SCHEDULED status can be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interview successfully updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponse> updateInterview(
            @Parameter(description = "Interview ID", required = true) @PathVariable String id,
            @Parameter(description = "ID of the user updating the interview", required = true)
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
    @Operation(
            summary = "Cancel an interview",
            description = "Cancels an interview by marking it as deleted. This is a soft delete operation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Interview successfully cancelled"),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelInterview(
            @Parameter(description = "Interview ID", required = true) @PathVariable String id,
            @Parameter(description = "ID of the user canceling the interview", required = true)
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
    @Operation(
            summary = "Get interviews by application",
            description = "Retrieves a paginated list of all interviews associated with a specific job application"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved interviews",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    })
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<Page<InterviewResponse>> getInterviewsByApplication(
            @Parameter(description = "Application ID", required = true) @PathVariable String applicationId,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g., 'scheduledAt,desc')", example = "scheduledAt,desc")
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
    @Operation(
            summary = "Get interviews by interviewer",
            description = "Retrieves a paginated list of all interviews assigned to a specific interviewer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved interviews",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/interviewer/{interviewerId}")
    public ResponseEntity<Page<InterviewResponse>> getInterviewsByInterviewer(
            @Parameter(description = "Interviewer user ID", required = true) @PathVariable String interviewerId,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g., 'scheduledAt,desc')", example = "scheduledAt,desc")
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
    @Operation(
            summary = "Mark interview as completed",
            description = "Changes the interview status to COMPLETED. The interview must be in SCHEDULED status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interview marked as completed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition", content = @Content),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<InterviewResponse> markAsCompleted(
            @Parameter(description = "Interview ID", required = true) @PathVariable String id,
            @Parameter(description = "ID of the user marking completion", required = true)
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
    @Operation(
            summary = "Mark interview as no-show",
            description = "Changes the interview status to NO_SHOW when the candidate doesn't attend. The interview must be in SCHEDULED status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interview marked as no-show",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition", content = @Content),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @PostMapping("/{id}/no-show")
    public ResponseEntity<InterviewResponse> markAsNoShow(
            @Parameter(description = "Interview ID", required = true) @PathVariable String id,
            @Parameter(description = "ID of the user marking no-show", required = true)
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
    @Operation(
            summary = "Add feedback to interview",
            description = "Adds interviewer feedback and rating to a completed interview. The interview must be in COMPLETED status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback successfully added",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InterviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or interview not completed", content = @Content),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @PostMapping("/{id}/feedback")
    public ResponseEntity<InterviewResponse> addFeedback(
            @Parameter(description = "Interview ID", required = true) @PathVariable String id,
            @Parameter(description = "ID of the user adding feedback", required = true)
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
    @Operation(
            summary = "Get upcoming interviews",
            description = "Retrieves all interviews scheduled within a specific date-time range"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved interviews",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    })
    @GetMapping("/upcoming")
    public ResponseEntity<List<InterviewResponse>> getUpcomingInterviews(
            @Parameter(description = "Start date-time (ISO 8601 format)", required = true, example = "2025-10-07T09:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End date-time (ISO 8601 format)", required = true, example = "2025-10-14T18:00:00")
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
    @Operation(
            summary = "Get today's interviews",
            description = "Retrieves all interviews scheduled for today (from 00:00 to 23:59)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved today's interviews",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    })
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
    @Operation(
            summary = "Get upcoming interviews by interviewer",
            description = "Retrieves all upcoming interviews (from now onwards) assigned to a specific interviewer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved upcoming interviews",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    })
    @GetMapping("/interviewer/{interviewerId}/upcoming")
    public ResponseEntity<List<InterviewResponse>> getUpcomingInterviewsByInterviewer(
            @Parameter(description = "Interviewer user ID", required = true) @PathVariable String interviewerId
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
    @Operation(
            summary = "Get today's interviews by interviewer",
            description = "Retrieves all interviews scheduled for today (from 00:00 to 23:59) assigned to a specific interviewer"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved today's interviews",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    })
    @GetMapping("/interviewer/{interviewerId}/today")
    public ResponseEntity<List<InterviewResponse>> getTodaysInterviewsByInterviewer(
            @Parameter(description = "Interviewer user ID", required = true) @PathVariable String interviewerId
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

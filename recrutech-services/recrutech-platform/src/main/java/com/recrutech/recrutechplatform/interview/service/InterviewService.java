package com.recrutech.recrutechplatform.interview.service;

import com.recrutech.common.exception.EntityReferenceNotFoundException;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.common.util.UuidValidator;
import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import com.recrutech.recrutechplatform.application.service.ApplicationService;
import com.recrutech.recrutechplatform.interview.dto.InterviewCreateRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewFeedbackRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewResponse;
import com.recrutech.recrutechplatform.interview.dto.InterviewUpdateRequest;
import com.recrutech.recrutechplatform.interview.mapper.InterviewMapper;
import com.recrutech.recrutechplatform.interview.model.Interview;
import com.recrutech.recrutechplatform.interview.model.InterviewStatus;
import com.recrutech.recrutechplatform.interview.repository.InterviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service layer for managing interviews.
 * Handles interview scheduling, updates, status transitions, and feedback collection.
 * Integrates with ApplicationService to automatically update application status.
 */
@Service
@Transactional
public class InterviewService {

    private final InterviewRepository repository;
    private final ApplicationService applicationService;

    public InterviewService(InterviewRepository repository, ApplicationService applicationService) {
        this.repository = repository;
        this.applicationService = applicationService;
    }

    /**
     * Schedules a new interview.
     *
     * @param request the interview creation request
     * @param userId  the ID of the user creating the interview
     * @return the created interview as a response DTO
     */
    public InterviewResponse scheduleInterview(InterviewCreateRequest request, String userId) {
        validateIds(userId);
        validateIds(request.applicationId());

        // Validate application exists
        if (!repository.applicationExists(request.applicationId())) {
            throw new EntityReferenceNotFoundException(
                    "Application not found with id: " + request.applicationId()
            );
        }

        // Validate interviewer exists if provided
        if (request.interviewerAccountId() != null) {
            validateIds(request.interviewerAccountId());
            if (!repository.userExists(request.interviewerAccountId())) {
                throw new EntityReferenceNotFoundException(
                        "User not found with id: " + request.interviewerAccountId()
                );
            }
        }

        // Create interview entity
        Interview interview = new Interview();
        interview.setApplicationId(request.applicationId());
        interview.setScheduledAt(request.scheduledAt());
        interview.setDurationMinutes(request.durationMinutes());
        interview.setInterviewType(request.interviewType());
        interview.setLocation(request.location());
        interview.setMeetingLink(request.meetingLink());
        interview.setInterviewerAccountId(request.interviewerAccountId());
        interview.setDescription(request.description());
        interview.setNotes(request.notes());
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setCreatedByAccountId(userId);

        Interview savedInterview = repository.save(interview);
        
        // Update application status to INTERVIEW_SCHEDULED
        applicationService.updateStatus(
                request.applicationId(),
                ApplicationStatus.INTERVIEW_SCHEDULED,
                userId,
                null,
                null
        );
        
        return InterviewMapper.toResponse(savedInterview);
    }

    /**
     * Updates an existing interview.
     *
     * @param id      the interview ID
     * @param request the update request
     * @param userId  the ID of the user performing the update
     * @return the updated interview as a response DTO
     */
    public InterviewResponse updateInterview(String id, InterviewUpdateRequest request, String userId) {
        validateIds(id, userId);

        Interview interview = findInterviewById(id);

        // Validate status - only SCHEDULED interviews can be updated
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new ValidationException(
                    "Only scheduled interviews can be updated. Current status: " + interview.getStatus()
            );
        }

        // Update fields if provided
        if (request.scheduledAt() != null) {
            interview.setScheduledAt(request.scheduledAt());
        }
        if (request.durationMinutes() != null) {
            interview.setDurationMinutes(request.durationMinutes());
        }
        if (request.interviewType() != null) {
            interview.setInterviewType(request.interviewType());
        }
        if (request.location() != null) {
            interview.setLocation(request.location());
        }
        if (request.meetingLink() != null) {
            interview.setMeetingLink(request.meetingLink());
        }
        if (request.interviewerAccountId() != null) {
            validateIds(request.interviewerAccountId());
            if (!repository.userExists(request.interviewerAccountId())) {
                throw new EntityReferenceNotFoundException(
                        "User not found with id: " + request.interviewerAccountId()
                );
            }
            interview.setInterviewerAccountId(request.interviewerAccountId());
        }
        if (request.description() != null) {
            interview.setDescription(request.description());
        }
        if (request.notes() != null) {
            interview.setNotes(request.notes());
        }

        interview.setUpdatedByAccountId(userId);

        Interview updatedInterview = repository.save(interview);
        return InterviewMapper.toResponse(updatedInterview);
    }

    /**
     * Cancels an interview (soft delete).
     *
     * @param id     the interview ID
     * @param userId the ID of the user canceling the interview
     */
    public void cancelInterview(String id, String userId) {
        validateIds(id, userId);

        Interview interview = findInterviewById(id);

        // Update status to CANCELLED
        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setDeleted(true);
        interview.setDeletedAt(LocalDateTime.now());
        interview.setDeletedByAccountId(userId);

        repository.save(interview);
    }

    /**
     * Retrieves an interview by ID.
     *
     * @param id the interview ID
     * @return the interview as a response DTO
     */
    @Transactional(readOnly = true)
    public InterviewResponse getInterview(String id) {
        validateIds(id);
        Interview interview = findInterviewById(id);
        return InterviewMapper.toResponse(interview);
    }

    /**
     * Retrieves all interviews for a specific application.
     *
     * @param applicationId the application ID
     * @param pageable      pagination parameters
     * @return a page of interviews
     */
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviewsByApplication(String applicationId, Pageable pageable) {
        validateIds(applicationId);
        return repository.findAllByApplicationIdAndIsDeletedFalse(applicationId, pageable)
                .map(InterviewMapper::toResponse);
    }

    /**
     * Retrieves all interviews for a specific interviewer.
     *
     * @param interviewerId the interviewer user ID
     * @param pageable      pagination parameters
     * @return a page of interviews
     */
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviewsByInterviewer(String interviewerId, Pageable pageable) {
        validateIds(interviewerId);
        return repository.findAllByInterviewerAccountIdAndIsDeletedFalse(interviewerId, pageable)
                .map(InterviewMapper::toResponse);
    }

    /**
     * Marks an interview as completed.
     *
     * @param id     the interview ID
     * @param userId the ID of the user marking completion
     * @return the updated interview as a response DTO
     */
    public InterviewResponse markAsCompleted(String id, String userId) {
        validateIds(id, userId);

        Interview interview = findInterviewById(id);

        // Validate status transition
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new ValidationException(
                    "Only scheduled interviews can be marked as completed. Current status: " + interview.getStatus()
            );
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());
        interview.setUpdatedByAccountId(userId);

        Interview updatedInterview = repository.save(interview);
        
        // Update application status to INTERVIEWED
        applicationService.updateStatus(
                interview.getApplicationId(),
                ApplicationStatus.INTERVIEWED,
                userId,
                null,
                null
        );
        
        return InterviewMapper.toResponse(updatedInterview);
    }

    /**
     * Marks an interview as no-show (candidate didn't attend).
     *
     * @param id     the interview ID
     * @param userId the ID of the user marking no-show
     * @return the updated interview as a response DTO
     */
    public InterviewResponse markAsNoShow(String id, String userId) {
        validateIds(id, userId);

        Interview interview = findInterviewById(id);

        // Validate status transition
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new ValidationException(
                    "Only scheduled interviews can be marked as no-show. Current status: " + interview.getStatus()
            );
        }

        interview.setStatus(InterviewStatus.NO_SHOW);
        interview.setCompletedAt(LocalDateTime.now());
        interview.setUpdatedByAccountId(userId);

        Interview updatedInterview = repository.save(interview);
        
        // Update application with NO_SHOW note - keep current status but add HR notes
        try {
            applicationService.updateStatus(
                    interview.getApplicationId(),
                    ApplicationStatus.REJECTED,
                    userId,
                    "Candidate did not attend scheduled interview on " + interview.getScheduledAt(),
                    "No-show for interview"
            );
        } catch (ValidationException e) {
            // If status transition fails (e.g., already finalized), just log and continue
            // The interview NO_SHOW status is still recorded
        }
        
        return InterviewMapper.toResponse(updatedInterview);
    }

    /**
     * Adds feedback to a completed interview.
     *
     * @param id      the interview ID
     * @param request the feedback request
     * @param userId  the ID of the user adding feedback
     * @return the updated interview as a response DTO
     */
    public InterviewResponse addFeedback(String id, InterviewFeedbackRequest request, String userId) {
        validateIds(id, userId);

        Interview interview = findInterviewById(id);

        // Validate status - only COMPLETED interviews can receive feedback
        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new ValidationException(
                    "Feedback can only be added to completed interviews. Current status: " + interview.getStatus()
            );
        }

        interview.setFeedback(request.feedback());
        interview.setRating(request.rating());
        interview.setUpdatedByAccountId(userId);

        Interview updatedInterview = repository.save(interview);
        return InterviewMapper.toResponse(updatedInterview);
    }

    /**
     * Retrieves upcoming interviews within a date range.
     *
     * @param from the start date-time
     * @param to   the end date-time
     * @return list of upcoming interviews
     */
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUpcomingInterviews(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new ValidationException("Start date must be before end date");
        }

        return repository.findByStatusAndScheduledAtBetweenAndIsDeletedFalse(
                        InterviewStatus.SCHEDULED, from, to)
                .stream()
                .map(InterviewMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves today's interviews.
     *
     * @return list of today's interviews
     */
    @Transactional(readOnly = true)
    public List<InterviewResponse> getTodaysInterviews() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        return repository.findByScheduledAtBetweenAndIsDeletedFalse(startOfDay, endOfDay)
                .stream()
                .map(InterviewMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves upcoming interviews for a specific interviewer.
     *
     * @param interviewerId the interviewer user ID
     * @return list of upcoming interviews
     */
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUpcomingInterviewsByInterviewer(String interviewerId) {
        validateIds(interviewerId);

        return repository.findUpcomingInterviewsByInterviewer(interviewerId, LocalDateTime.now())
                .stream()
                .map(InterviewMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves today's interviews for a specific interviewer.
     *
     * @param interviewerId the interviewer user ID
     * @return list of today's interviews
     */
    @Transactional(readOnly = true)
    public List<InterviewResponse> getTodaysInterviewsByInterviewer(String interviewerId) {
        validateIds(interviewerId);

        return repository.findTodaysInterviewsByInterviewer(interviewerId, LocalDateTime.now())
                .stream()
                .map(InterviewMapper::toResponse)
                .toList();
    }

    /**
     * Finds an interview by ID or throws NotFoundException.
     *
     * @param id the interview ID
     * @return the interview entity
     */
    private Interview findInterviewById(String id) {
        return repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Interview not found with id: " + id));
    }

    /**
     * Validates that all provided IDs are valid UUIDs.
     *
     * @param ids the IDs to validate
     * @throws ValidationException if any ID is invalid
     */
    private void validateIds(String... ids) {
        for (String id : ids) {
            if (!UuidValidator.isValidUuid(id)) {
                throw new ValidationException("Invalid UUID format: " + id);
            }
        }
    }
}

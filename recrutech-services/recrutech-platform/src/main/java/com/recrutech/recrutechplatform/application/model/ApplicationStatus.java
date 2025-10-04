package com.recrutech.recrutechplatform.application.model;

/**
 * Enum representing the possible statuses of a job application.
 * Follows the complete workflow from submission to final decision.
 */
public enum ApplicationStatus {
    /**
     * Application has been submitted by the applicant
     */
    SUBMITTED,
    
    /**
     * Application is being reviewed by HR/recruiter
     */
    UNDER_REVIEW,
    
    /**
     * Interview has been scheduled
     */
    INTERVIEW_SCHEDULED,
    
    /**
     * Interview has been completed
     */
    INTERVIEWED,
    
    /**
     * Job offer has been extended to the applicant
     */
    OFFER_EXTENDED,
    
    /**
     * Applicant accepted the offer
     */
    ACCEPTED,
    
    /**
     * Application was rejected by the company
     */
    REJECTED,
    
    /**
     * Application was withdrawn by the applicant
     */
    WITHDRAWN
}

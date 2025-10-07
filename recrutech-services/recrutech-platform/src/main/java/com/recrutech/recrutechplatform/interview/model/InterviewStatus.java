package com.recrutech.recrutechplatform.interview.model;

/**
 * Enum representing the possible statuses of an interview.
 * Tracks the lifecycle of an interview from scheduling to completion.
 */
public enum InterviewStatus {
    /**
     * Interview has been scheduled
     */
    SCHEDULED,
    
    /**
     * Interview has been completed
     */
    COMPLETED,
    
    /**
     * Interview was cancelled
     */
    CANCELLED,
    
    /**
     * Candidate did not show up for the scheduled interview
     */
    NO_SHOW
}

package com.recrutech.common.event;

import lombok.Getter;

/**
 * Event published when an application's status changes.
 * Used by the notification service to notify applicants and HR about status updates.
 */
@Getter
public class ApplicationStatusChangedEvent extends BaseEvent {

    private final String applicationId;
    private final String applicantId;
    private final String jobPostingId;
    private final String companyId;
    private final String previousStatus;
    private final String newStatus;
    private final String updatedByUserId;

    public ApplicationStatusChangedEvent(String applicationId, String applicantId, 
                                          String jobPostingId, String companyId,
                                          String previousStatus, String newStatus, 
                                          String updatedByUserId) {
        super("APPLICATION_STATUS_CHANGED");
        this.applicationId = applicationId;
        this.applicantId = applicantId;
        this.jobPostingId = jobPostingId;
        this.companyId = companyId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.updatedByUserId = updatedByUserId;
    }

    @Override
    public String toString() {
        return "ApplicationStatusChangedEvent{" +
                "applicationId='" + applicationId + '\'' +
                ", applicantId='" + applicantId + '\'' +
                ", jobPostingId='" + jobPostingId + '\'' +
                ", companyId='" + companyId + '\'' +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", updatedByUserId='" + updatedByUserId + '\'' +
                ", " + super.toString() +
                '}';
    }
}

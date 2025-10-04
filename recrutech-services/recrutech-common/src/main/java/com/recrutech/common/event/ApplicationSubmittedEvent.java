package com.recrutech.common.event;

import lombok.Getter;

/**
 * Event published when a new application is submitted.
 * Used by the notification service to notify relevant parties.
 */
@Getter
public class ApplicationSubmittedEvent extends BaseEvent {

    private final String applicationId;
    private final String applicantId;
    private final String jobPostingId;
    private final String companyId;

    public ApplicationSubmittedEvent(String applicationId, String applicantId, 
                                      String jobPostingId, String companyId) {
        super("APPLICATION_SUBMITTED");
        this.applicationId = applicationId;
        this.applicantId = applicantId;
        this.jobPostingId = jobPostingId;
        this.companyId = companyId;
    }

    @Override
    public String toString() {
        return "ApplicationSubmittedEvent{" +
                "applicationId='" + applicationId + '\'' +
                ", applicantId='" + applicantId + '\'' +
                ", jobPostingId='" + jobPostingId + '\'' +
                ", companyId='" + companyId + '\'' +
                ", " + super.toString() +
                '}';
    }
}

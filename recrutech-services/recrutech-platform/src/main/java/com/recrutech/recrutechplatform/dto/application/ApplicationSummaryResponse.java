package com.recrutech.recrutechplatform.dto.application;

/**
 * DTO for application summary information in list views
 */
public record ApplicationSummaryResponse(String id, JobInfo job, UserInfo user, String status) {
}

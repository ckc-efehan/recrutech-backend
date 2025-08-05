package com.recrutech.recrutechplatform.dto.application;

import com.recrutech.common.dto.UserInfo;

/**
 * DTO for application summary information in list views
 */
public record ApplicationSummaryResponse(String id, JobInfo job, UserInfo user, String status) {
}

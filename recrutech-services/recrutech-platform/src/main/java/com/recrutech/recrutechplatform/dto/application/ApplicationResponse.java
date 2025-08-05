package com.recrutech.recrutechplatform.dto.application;

import com.recrutech.common.dto.UserInfo;
import com.recrutech.recrutechplatform.enums.ApplicationStatus;

import java.time.LocalDateTime;

/**
 * DTO for sending application data to clients
 */
public record ApplicationResponse(String id, JobInfo job, UserInfo user, String cvFileId, 
                                 ApplicationStatus status, boolean viewedByHr, LocalDateTime createdAt) {
}

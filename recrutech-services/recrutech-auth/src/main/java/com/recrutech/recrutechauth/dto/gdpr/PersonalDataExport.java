package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Personal data export containing basic user information
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalDataExport(
    String email,
    String firstName,
    String lastName,
    String role,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt,
    Boolean emailVerified
) {}
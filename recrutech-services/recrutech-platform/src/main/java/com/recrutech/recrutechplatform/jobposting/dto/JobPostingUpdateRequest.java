package com.recrutech.recrutechplatform.jobposting.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request payload for updating a job posting.
 */
public record JobPostingUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @Size(max = 200) String location,
        @Size(max = 50) String employmentType,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal salaryMin,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal salaryMax,
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code") String currency,
        LocalDateTime expiresAt
) {
}

package com.recrutech.recrutechnotification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for password reset email events sent via Kafka.
 * Mirrors the payload produced by recrutech-auth EmailEventProducer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordResetEvent {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Reset URL is required")
    private String resetUrl;

    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotNull(message = "Request date is required")
    private LocalDateTime requestDate;

    @NotNull(message = "Expiry date is required")
    @JsonProperty("expiryDate")
    private LocalDateTime expiryDate;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isTokenExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }
}

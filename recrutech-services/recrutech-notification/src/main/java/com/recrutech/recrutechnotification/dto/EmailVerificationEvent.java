package com.recrutech.recrutechnotification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for email verification events sent via Kafka.
 * Contains all necessary information to send a verification email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailVerificationEvent {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Verification token is required")
    private String verificationToken;

    @NotNull(message = "User role is required")
    private String userRole;

    @NotNull(message = "Registration date is required")
    private LocalDateTime registrationDate;

    @NotNull(message = "Token expiry date is required")
    @JsonProperty("tokenExpiryDate")
    private LocalDateTime tokenExpiryDate;

    /**
     * Constructs the full name from first and last name.
     * @return Full name of the user
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Checks if the verification token is expired.
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired() {
        return tokenExpiryDate != null && tokenExpiryDate.isBefore(LocalDateTime.now());
    }

    /**
     * Creates a verification URL with the token.
     * @param baseUrl The base verification URL
     * @return Complete verification URL
     */
    public String createVerificationUrl(String baseUrl) {
        return baseUrl + "?token=" + verificationToken + "&email=" + email;
    }

    @Override
    public String toString() {
        return "EmailVerificationEvent{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userRole='" + userRole + '\'' +
                ", registrationDate=" + registrationDate +
                ", tokenExpiryDate=" + tokenExpiryDate +
                ", tokenExpired=" + isTokenExpired() +
                '}';
    }
}
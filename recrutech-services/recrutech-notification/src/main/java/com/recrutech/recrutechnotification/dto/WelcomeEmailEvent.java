package com.recrutech.recrutechnotification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for welcome email events sent via Kafka.
 * Contains all necessary information to send a welcome email after email verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeEmailEvent {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "User role is required")
    private String userRole;

    @NotNull(message = "Registration date is required")
    private LocalDateTime registrationDate;

    @NotNull(message = "Verification date is required")
    private LocalDateTime verificationDate;

    /**
     * Constructs the full name from first and last name.
     * @return Full name of the user
     */
    @JsonIgnore
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Checks if this is an applicant user.
     * @return true if user role is APPLICANT, false otherwise
     */
    @JsonIgnore
    public boolean isApplicant() {
        return "APPLICANT".equalsIgnoreCase(userRole);
    }

    /**
     * Checks if this is an HR user.
     * @return true if user role is HR, false otherwise
     */
    @JsonIgnore
    public boolean isHR() {
        return "HR".equalsIgnoreCase(userRole);
    }

    /**
     * Checks if this is a company user.
     * @return true if user role is COMPANY, false otherwise
     */
    @JsonIgnore
    public boolean isCompany() {
        return "COMPANY".equalsIgnoreCase(userRole);
    }

    /**
     * Gets a user-friendly role name for display purposes.
     * @return Formatted role name
     */
    @JsonIgnore
    public String getDisplayRoleName() {
        if (userRole == null) return "Unknown";
        
        switch (userRole.toUpperCase()) {
            case "APPLICANT":
                return "Bewerber";
            case "HR":
                return "HR-Manager";
            case "COMPANY":
                return "Unternehmen";
            default:
                return userRole;
        }
    }

    /**
     * Gets the appropriate welcome message based on user role.
     * @return Role-specific welcome message
     */
    @JsonIgnore
    public String getRoleSpecificWelcomeMessage() {
        if (isApplicant()) {
            return "Als Bewerber können Sie jetzt personalisierte Jobempfehlungen erhalten und sich direkt bewerben.";
        } else if (isHR()) {
            return "Als HR-Manager können Sie jetzt Stellenanzeigen erstellen und die besten Talente finden.";
        } else if (isCompany()) {
            return "Als Unternehmen können Sie jetzt Ihr Recruiting-Team verwalten und Stellenanzeigen schalten.";
        }
        return "Willkommen bei RecruTech - Ihrer Karriereplattform!";
    }

    @Override
    public String toString() {
        return "WelcomeEmailEvent{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userRole='" + userRole + '\'' +
                ", registrationDate=" + registrationDate +
                ", verificationDate=" + verificationDate +
                ", displayRole='" + getDisplayRoleName() + '\'' +
                '}';
    }
}
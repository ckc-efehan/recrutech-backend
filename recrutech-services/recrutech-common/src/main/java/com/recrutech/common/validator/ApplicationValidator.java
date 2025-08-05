package com.recrutech.common.validator;

import com.recrutech.common.exception.ValidationException;
import com.recrutech.common.util.UuidValidator;
import org.springframework.util.StringUtils;
import java.lang.reflect.Method;

/**
 * Validator for application-related data.
 * This class provides specialized validation methods for application entities and their properties.
 */
public final class ApplicationValidator {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ApplicationValidator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Validates input parameters for creating an application with simplified request.
     * This method validates jobId and extracts cvFileId from the request object for validation.
     *
     * @param jobId the job ID
     * @param request the application request (must have cvFileId)
     * @throws ValidationException if validation fails
     */
    public static void validateCreateApplicationInput(String jobId, Object request) {
        if (!StringUtils.hasText(jobId)) {
            throw new ValidationException("Job ID cannot be null or empty");
        }
        
        if (request == null) {
            throw new ValidationException("Application request cannot be null");
        }
        
        // Validate job ID is a valid UUID
        UuidValidator.validateUuid(jobId, "Job ID");
        
        // Extract and validate cvFileId from request using reflection
        try {
            Method getCvFileIdMethod = request.getClass().getMethod("cvFileId");
            String cvFileId = (String) getCvFileIdMethod.invoke(request);
            
            if (!StringUtils.hasText(cvFileId)) {
                throw new ValidationException("CV File ID cannot be null or empty");
            }
            
            // Validate CV file ID is a valid UUID
            UuidValidator.validateUuid(cvFileId, "CV File ID");
            
        } catch (Exception e) {
            throw new ValidationException("Invalid application request structure: " + e.getMessage());
        }
    }
    
    /**
     * Validates simplified application request containing only cvFileId.
     * This method is used when user information is extracted from JWT token.
     *
     * @param cvFileId the CV file ID
     * @throws ValidationException if validation fails
     */
    public static void validateSimplifiedApplicationRequest(String cvFileId) {
        if (!StringUtils.hasText(cvFileId)) {
            throw new ValidationException("CV File ID cannot be null or empty");
        }
        
        // Validate CV file ID is a valid UUID
        UuidValidator.validateUuid(cvFileId, "CV File ID");
    }
    
    /**
     * Validates application request fields.
     *
     * @param userId the user ID
     * @param cvFileId the CV file ID
     * @param firstName the first name
     * @param lastName the last name
     * @throws ValidationException if validation fails
     */
    public static void validateApplicationRequestFields(String userId, String cvFileId, String firstName, String lastName) {
        if (!StringUtils.hasText(userId)) {
            throw new ValidationException("User ID cannot be null or empty");
        }
        
        if (!StringUtils.hasText(cvFileId)) {
            throw new ValidationException("CV File ID cannot be null or empty");
        }
        
        if (!StringUtils.hasText(firstName)) {
            throw new ValidationException("First name cannot be null or empty");
        }
        
        if (!StringUtils.hasText(lastName)) {
            throw new ValidationException("Last name cannot be null or empty");
        }
        
        // Validate user ID is a valid UUID
        UuidValidator.validateUuid(userId, "User ID");
        
        // Validate CV file ID is a valid UUID
        UuidValidator.validateUuid(cvFileId, "CV File ID");
    }
}
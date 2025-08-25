package com.recrutech.recrutechauth.dto.common;

import jakarta.validation.groups.Default;

/**
 * Validation groups for different scenarios in authentication DTOs.
 * Provides fine-grained control over validation rules based on context.
 * 
 * @author Senior Java Developer
 * @version 2.0
 * @since 1.0
 */
public interface ValidationGroups {
    
    /**
     * Validation group for create operations.
     * Used when creating new entities with strict validation requirements.
     */
    interface Create extends Default {}
    
    /**
     * Validation group for update operations.
     * Used when updating existing entities with potentially relaxed validation.
     */
    interface Update extends Default {}
    
    /**
     * Validation group for strict validation scenarios.
     * Used when maximum validation rigor is required.
     */
    interface StrictValidation extends Default {}
    
    /**
     * Validation group for basic validation scenarios.
     * Used when minimal validation is sufficient.
     */
    interface BasicValidation extends Default {}
}
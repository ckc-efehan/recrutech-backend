package com.recrutech.common.dto;

/**
 * Common DTO for user information across all services.
 * This DTO can be used by any service that needs to represent user data.
 */
public record UserInfo(String id, String firstName, String lastName) {
    
    /**
     * Creates a UserInfo with "Unknown" values for missing user data.
     * 
     * @param userId the user ID
     * @return UserInfo with unknown first and last name
     */
    public static UserInfo unknown(String userId) {
        return new UserInfo(userId, "Unknown", "Unknown");
    }
    
}
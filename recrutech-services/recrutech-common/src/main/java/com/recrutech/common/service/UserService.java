package com.recrutech.common.service;

/**
 * Service interface for user-related operations.
 * This interface provides methods to retrieve user information
 * that can be implemented by different services.
 */
public interface UserService {

    /**
     * Retrieves the first name of a user by their ID.
     *
     * @param userId the ID of the user
     * @return the first name of the user, or "Unknown" if user not found or userId is null
     */
    String getUserFirstName(String userId);

    /**
     * Retrieves the last name of a user by their ID.
     *
     * @param userId the ID of the user
     * @return the last name of the user, or "Unknown" if user not found or userId is null
     */
    String getUserLastName(String userId);


    /**
     * Checks if a user exists by their ID.
     *
     * @param userId the ID of the user
     * @return true if the user exists, false otherwise
     */
    boolean userExists(String userId);
}
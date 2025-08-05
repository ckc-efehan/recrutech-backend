package com.recrutech.recrutechauth.controller;

import com.recrutech.common.exception.NotFoundException;
import com.recrutech.recrutechauth.dto.UserResponse;
import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for user information endpoints.
 * Provides endpoints for retrieving user data for inter-service communication.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    /**
     * Constructor for UserController.
     *
     * @param userRepository the user repository
     */
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Endpoint for retrieving user information by ID.
     * This endpoint is used by other services to get user details.
     * No authentication required for internal service-to-service communication.
     *
     * @param userId the user ID
     * @return the user information
     * @throws NotFoundException if the user is not found
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }
        
        User user = userOptional.get();
        UserResponse response = new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isEnabled()
        );
        
        return ResponseEntity.ok(response);
    }
}
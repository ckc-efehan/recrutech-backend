package com.recrutech.recrutechauth.service;

import com.recrutech.common.service.UserService;
import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of UserService that provides user information retrieval.
 * This service uses the UserRepository to fetch user data from the database.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /**
     * Constructor for UserServiceImpl.
     *
     * @param userRepository the user repository
     */
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String getUserFirstName(String userId) {
        if (userId == null) {
            return "Unknown";
        }
        
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.map(User::getFirstName).orElse("Unknown");
    }

    @Override
    public String getUserLastName(String userId) {
        if (userId == null) {
            return "Unknown";
        }
        
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.map(User::getLastName).orElse("Unknown");
    }


    @Override
    public boolean userExists(String userId) {
        if (userId == null) {
            return false;
        }
        
        return userRepository.existsById(userId);
    }
}
package com.recrutech.common.service;

import com.recrutech.common.dto.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Service for creating and managing UserInfo DTOs.
 * This service centralizes user information retrieval and provides
 * a consistent way to handle user data across all services.
 * 
 * Future optimizations like caching, batch processing, or REST client
 * communication can be implemented here without affecting other services.
 */
@Service
@Slf4j
public class UserInfoService {
    
    private final UserService userService;
    
    public UserInfoService(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Creates UserInfo DTO with optimized user service calls.
     * This method centralizes user information retrieval to enable future optimizations
     * such as caching or batch processing.
     *
     * @param userId the user ID
     * @return the user info DTO
     */
    public UserInfo createUserInfo(String userId) {
        if (!StringUtils.hasText(userId)) {
            log.warn("createUserInfo called with null or empty userId");
            return UserInfo.unknown(userId);
        }

        try {
            String firstName = userService.getUserFirstName(userId);
            String lastName = userService.getUserLastName(userId);
            return new UserInfo(userId, firstName, lastName);
        } catch (Exception e) {
            log.warn("Failed to retrieve user information for userId: {}, error: {}", userId, e.getMessage());
            return UserInfo.unknown(userId);
        }
    }
    
    /**
     * Creates UserInfo DTOs for multiple user IDs in batch.
     * This method can be optimized in the future for batch processing.
     *
     * @param userIds list of user IDs
     * @return list of UserInfo DTOs
     */
    public List<UserInfo> createUserInfos(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        
        log.debug("Creating UserInfo for {} users", userIds.size());
        
        // TODO: Implement batch processing optimization
        return userIds.stream()
                .map(this::createUserInfo)
                .toList();
    }
}
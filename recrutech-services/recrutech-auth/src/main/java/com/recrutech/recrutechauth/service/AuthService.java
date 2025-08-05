package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.AuthResponse;
import com.recrutech.recrutechauth.dto.HRRegisterRequest;
import com.recrutech.recrutechauth.dto.LoginRequest;
import com.recrutech.recrutechauth.dto.RegisterRequest;
import com.recrutech.recrutechauth.exception.AuthenticationException;
import com.recrutech.recrutechauth.exception.RegistrationException;
import com.recrutech.recrutechauth.exception.TokenException;
import com.recrutech.recrutechauth.model.Role;
import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.repository.RoleRepository;
import com.recrutech.recrutechauth.repository.UserRepository;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for user authentication and registration.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordValidator passwordValidator;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Constructor for AuthService.
     *
     * @param userRepository the user repository
     * @param roleRepository the role repository
     * @param passwordEncoder the password encoder
     * @param authenticationManager the authentication manager
     * @param jwtService the JWT service
     * @param passwordValidator the password validator
     */
    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordValidator passwordValidator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordValidator = passwordValidator;
    }

    /**
     * Registers a new user.
     *
     * @param request the registration request
     * @return the authentication response
     * @throws RegistrationException if registration fails
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        try {
            // Validate password
            List<String> passwordErrors = passwordValidator.validate(request.password());
            if (!passwordErrors.isEmpty()) {
                throw new RegistrationException("Password validation failed", passwordErrors);
            }
            
            // Check if username or email already exists
            if (userRepository.existsByUsername(request.username())) {
                throw new RegistrationException("Username is already taken");
            }

            if (userRepository.existsByEmail(request.email())) {
                throw new RegistrationException("Email is already in use");
            }

            // Create new user
            User user = new User();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());

            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RegistrationException("Default role not found"));
            roles.add(userRole);
            user.setRoles(roles);

            // Save user
            userRepository.save(user);

            // Authenticate the new user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate tokens
            String accessToken = jwtService.generateToken(authentication);
            String refreshToken = jwtService.generateRefreshToken(authentication);

            // Get user roles
            String[] userRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toArray(String[]::new);

            // Build response
            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    jwtExpiration / 1000, // Convert to seconds
                    user.getUsername(),
                    user.getEmail(),
                    userRoles
            );
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a new HR user with automatic HR role assignment.
     *
     * @param request the HR registration request
     * @return the authentication response
     * @throws RegistrationException if registration fails
     */
    @Transactional
    public AuthResponse registerHR(HRRegisterRequest request) {
        try {
            // Validate password
            List<String> passwordErrors = passwordValidator.validate(request.password());
            if (!passwordErrors.isEmpty()) {
                throw new RegistrationException("Password validation failed", passwordErrors);
            }
            
            // Check if username or email already exists
            if (userRepository.existsByUsername(request.username())) {
                throw new RegistrationException("Username is already taken");
            }

            if (userRepository.existsByEmail(request.email())) {
                throw new RegistrationException("Email is already in use");
            }

            // Create new HR user
            User user = new User();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());

            // Automatically assign HR role
            Set<Role> roles = new HashSet<>();
            Role hrRole = roleRepository.findByName("ROLE_HR")
                    .orElseThrow(() -> new RegistrationException("HR role not found"));
            roles.add(hrRole);
            user.setRoles(roles);

            // Save user
            userRepository.save(user);

            // Authenticate the new HR user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate tokens
            String accessToken = jwtService.generateToken(authentication);
            String refreshToken = jwtService.generateRefreshToken(authentication);

            // Get user roles
            String[] userRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toArray(String[]::new);

            // Build response
            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    jwtExpiration / 1000, // Convert to seconds
                    user.getUsername(),
                    user.getEmail(),
                    userRoles
            );
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("HR registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticates a user.
     *
     * @param request the login request
     * @return the authentication response
     * @throws AuthenticationException if authentication fails
     */
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details
            User user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Generate tokens
            String accessToken = jwtService.generateToken(authentication);
            String refreshToken = jwtService.generateRefreshToken(authentication);

            // Get user roles
            String[] userRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toArray(String[]::new);

            // Build response
            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    jwtExpiration / 1000, // Convert to seconds
                    user.getUsername(),
                    user.getEmail(),
                    userRoles
            );
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid username or password", e);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return the authentication response
     * @throws TokenException if the refresh token is invalid
     */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Generate a new access token from the refresh token
            // This will throw a TokenException if the token is invalid, expired, or revoked
            String accessToken = jwtService.generateTokenFromRefreshToken(refreshToken);
            
            // Generate a new refresh token (token rotation)
            String newRefreshToken = jwtService.rotateRefreshToken(refreshToken);
            
            // Extract username from the refresh token
            String username = jwtService.extractUsername(refreshToken);
            
            // Get user details
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("User not found"));
            
            // Get user roles
            String[] userRoles = user.getRoles().stream()
                    .map(Role::getName)
                    .toArray(String[]::new);
            
            // Build response
            return new AuthResponse(
                    accessToken,
                    newRefreshToken, // Return the new refresh token
                    "Bearer",
                    jwtExpiration / 1000, // Convert to seconds
                    user.getUsername(),
                    user.getEmail(),
                    userRoles
            );
        } catch (TokenException e) {
            throw e;
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenException("Error refreshing token: " + e.getMessage(), e);
        }
    }
}
package com.recrutech.recrutechauth.security;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

/**
 * Service for sanitizing user input to prevent XSS and SQL injection attacks.
 * Provides comprehensive input validation and sanitization methods.
 */
@Service
public class InputSanitizationService {

    private static final Logger logger = LoggerFactory.getLogger(InputSanitizationService.class);

    // XSS Prevention Patterns
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern VBSCRIPT_PATTERN = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLOAD_PATTERN = Pattern.compile("onload[^=]*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONERROR_PATTERN = Pattern.compile("onerror[^=]*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONCLICK_PATTERN = Pattern.compile("onclick[^=]*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONMOUSEOVER_PATTERN = Pattern.compile("onmouseover[^=]*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVAL_PATTERN = Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE);
    
    // SQL Injection Prevention Patterns
    private static final Pattern SQL_UNION_PATTERN = Pattern.compile("(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("(--|#|/\\*|\\*/)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_QUOTE_PATTERN = Pattern.compile("('|(\\\\'))", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_SEMICOLON_PATTERN = Pattern.compile(";\\s*(union|select|insert|update|delete|drop|create|alter)", Pattern.CASE_INSENSITIVE);

    // HTML Entity Mappings for XSS Prevention
    private static final Map<String, String> HTML_ENTITIES = new HashMap<>();
    static {
        HTML_ENTITIES.put("<", "&lt;");
        HTML_ENTITIES.put(">", "&gt;");
        HTML_ENTITIES.put("\"", "&quot;");
        HTML_ENTITIES.put("'", "&#x27;");
        HTML_ENTITIES.put("&", "&amp;");
        HTML_ENTITIES.put("/", "&#x2F;");
    }

    /**
     * Sanitizes input string to prevent XSS attacks.
     * 
     * @param input the input string to sanitize
     * @return sanitized string safe from XSS attacks
     */
    public String sanitizeForXSS(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String sanitized = input;
        
        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove javascript: and vbscript: protocols
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = VBSCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove event handlers
        sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONERROR_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONCLICK_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONMOUSEOVER_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove eval and expression functions
        sanitized = EVAL_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EXPRESSION_PATTERN.matcher(sanitized).replaceAll("");
        
        // Encode HTML entities in correct order (& must be first to avoid double-encoding)
        sanitized = sanitized.replace("&", "&amp;");  // Must be first!
        sanitized = sanitized.replace("<", "&lt;");
        sanitized = sanitized.replace(">", "&gt;");
        sanitized = sanitized.replace("\"", "&quot;");
        sanitized = sanitized.replace("'", "&#x27;");
        sanitized = sanitized.replace("/", "&#x2F;");

        // Log if sanitization occurred
        if (!input.equals(sanitized)) {
            logger.warn("XSS attempt detected and sanitized. Original length: {}, Sanitized length: {}", 
                       input.length(), sanitized.length());
        }

        return sanitized;
    }

    /**
     * Validates input for potential SQL injection patterns.
     * 
     * @param input the input string to validate
     * @return true if input is safe, false if potential SQL injection detected
     */
    public boolean isSafeFromSQLInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }

        String lowerInput = input.toLowerCase();
        
        // Check for SQL keywords
        if (SQL_UNION_PATTERN.matcher(lowerInput).find()) {
            logger.warn("Potential SQL injection detected: SQL keywords found in input");
            return false;
        }
        
        // Check for SQL comments
        if (SQL_COMMENT_PATTERN.matcher(input).find()) {
            logger.warn("Potential SQL injection detected: SQL comments found in input");
            return false;
        }
        
        // Check for quote manipulation
        if (SQL_QUOTE_PATTERN.matcher(input).find()) {
            logger.warn("Potential SQL injection detected: Quote manipulation found in input");
            return false;
        }
        
        // Check for semicolon followed by SQL keywords
        if (SQL_SEMICOLON_PATTERN.matcher(lowerInput).find()) {
            logger.warn("Potential SQL injection detected: Semicolon with SQL keywords found in input");
            return false;
        }

        return true;
    }

    /**
     * Sanitizes input for safe database operations.
     * 
     * @param input the input string to sanitize
     * @return sanitized string safe for database operations
     */
    public String sanitizeForDatabase(String input) {
        if (input == null) {
            return null;
        }

        // First check if it's safe
        if (!isSafeFromSQLInjection(input)) {
            logger.warn("Unsafe input detected for database operation, applying strict sanitization");
            // Remove potentially dangerous characters
            return input.replaceAll("[';\"\\-#/*]", "")
                       .replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)", "")
                       .trim();
        }

        return input.trim();
    }

    /**
     * Comprehensive input sanitization that applies both XSS and SQL injection protection.
     * 
     * @param input the input string to sanitize
     * @return fully sanitized string
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Apply XSS sanitization first
        String sanitized = sanitizeForXSS(input);
        
        // Then apply database sanitization
        sanitized = sanitizeForDatabase(sanitized);
        
        return sanitized;
    }

    /**
     * Validates if input contains only allowed characters for specific field types.
     * 
     * @param input the input to validate
     * @param fieldType the type of field (email, name, phone, etc.)
     * @return true if input is valid for the field type
     */
    public boolean isValidForFieldType(String input, String fieldType) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        return switch (fieldType.toLowerCase()) {
            case "email" -> input.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
            case "name" -> input.matches("^[a-zA-ZäöüÄÖÜß\\s'-]{1,100}$");
            case "phone" -> input.matches("^[+]?[0-9\\s()-]{7,20}$");
            case "alphanumeric" -> input.matches("^[a-zA-Z0-9\\s]{1,255}$");
            case "numeric" -> input.matches("^[0-9]+$");
            case "uuid" ->
                    input.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
            default ->
                // For unknown field types, apply general sanitization check
                    isSafeFromSQLInjection(input) && !containsXSSPatterns(input);
        };
    }

    /**
     * Checks if input contains XSS patterns.
     * 
     * @param input the input to check
     * @return true if XSS patterns are found
     */
    private boolean containsXSSPatterns(String input) {
        return SCRIPT_PATTERN.matcher(input).find() ||
               JAVASCRIPT_PATTERN.matcher(input).find() ||
               VBSCRIPT_PATTERN.matcher(input).find() ||
               ONLOAD_PATTERN.matcher(input).find() ||
               ONERROR_PATTERN.matcher(input).find() ||
               ONCLICK_PATTERN.matcher(input).find() ||
               ONMOUSEOVER_PATTERN.matcher(input).find() ||
               EVAL_PATTERN.matcher(input).find() ||
               EXPRESSION_PATTERN.matcher(input).find();
    }

    /**
     * Sanitizes a map of parameters (useful for request parameters).
     * 
     * @param parameters the map of parameters to sanitize
     * @return sanitized map of parameters
     */
    public Map<String, String> sanitizeParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return parameters;
        }

        Map<String, String> sanitizedParams = new HashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String sanitizedKey = sanitizeInput(entry.getKey());
            String sanitizedValue = sanitizeInput(entry.getValue());
            sanitizedParams.put(sanitizedKey, sanitizedValue);
        }

        return sanitizedParams;
    }
}
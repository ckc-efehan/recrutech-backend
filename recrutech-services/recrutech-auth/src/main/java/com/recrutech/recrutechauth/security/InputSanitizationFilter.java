package com.recrutech.recrutechauth.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Servlet filter that sanitizes all incoming HTTP requests to prevent XSS and SQL injection attacks.
 * This filter wraps the HttpServletRequest to sanitize parameters, headers, and request body.
 */
@Component
@Order(1) // Execute early in the filter chain
public class InputSanitizationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(InputSanitizationFilter.class);
    
    private final InputSanitizationService sanitizationService;

    public InputSanitizationFilter(InputSanitizationService sanitizationService) {
        this.sanitizationService = sanitizationService;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("InputSanitizationFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest) {
            SanitizedHttpServletRequestWrapper wrappedRequest = new SanitizedHttpServletRequestWrapper(httpRequest, sanitizationService);
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        logger.info("InputSanitizationFilter destroyed");
    }

    /**
     * Wrapper class that sanitizes HTTP request parameters and headers.
     */
    private static class SanitizedHttpServletRequestWrapper extends HttpServletRequestWrapper {
        
        private final InputSanitizationService sanitizationService;
        private final Map<String, String[]> sanitizedParameters;
        private final Map<String, String> sanitizedHeaders;

        public SanitizedHttpServletRequestWrapper(HttpServletRequest request, InputSanitizationService sanitizationService) {
            super(request);
            this.sanitizationService = sanitizationService;
            this.sanitizedParameters = sanitizeParameters(request.getParameterMap());
            this.sanitizedHeaders = sanitizeHeaders(request);
        }

        @Override
        public String getParameter(String name) {
            String[] values = getParameterValues(name);
            return (values != null && values.length > 0) ? values[0] : null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return sanitizedParameters.get(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.unmodifiableMap(sanitizedParameters);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(sanitizedParameters.keySet());
        }

        @Override
        public String getHeader(String name) {
            // Don't sanitize authorization headers or other security-critical headers
            if (isSecurityHeader(name)) {
                return super.getHeader(name);
            }
            return sanitizedHeaders.get(name.toLowerCase());
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String sanitizedValue = getHeader(name);
            if (sanitizedValue != null) {
                return Collections.enumeration(List.of(sanitizedValue));
            }
            return Collections.enumeration(Collections.emptyList());
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            // Combine all header names - both security headers and sanitized headers
            Set<String> allHeaderNames = new HashSet<>();
            
            // Add all original header names from the wrapped request
            Enumeration<String> originalHeaders = super.getHeaderNames();
            while (originalHeaders.hasMoreElements()) {
                allHeaderNames.add(originalHeaders.nextElement());
            }
            
            // Add sanitized header names
            allHeaderNames.addAll(sanitizedHeaders.keySet());
            
            return Collections.enumeration(allHeaderNames);
        }

        /**
         * Sanitizes request parameters.
         */
        private Map<String, String[]> sanitizeParameters(Map<String, String[]> originalParameters) {
            Map<String, String[]> sanitized = new HashMap<>();
            
            for (Map.Entry<String, String[]> entry : originalParameters.entrySet()) {
                String paramName = sanitizationService.sanitizeInput(entry.getKey());
                String[] paramValues = entry.getValue();
                
                if (paramValues != null) {
                    String[] sanitizedValues = new String[paramValues.length];
                    for (int i = 0; i < paramValues.length; i++) {
                        sanitizedValues[i] = sanitizationService.sanitizeInput(paramValues[i]);
                    }
                    sanitized.put(paramName, sanitizedValues);
                }
            }
            
            return sanitized;
        }

        /**
         * Sanitizes request headers (excluding security-critical headers).
         */
        private Map<String, String> sanitizeHeaders(HttpServletRequest request) {
            Map<String, String> sanitized = new HashMap<>();
            
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                
                // Skip security-critical headers
                if (isSecurityHeader(headerName)) {
                    continue;
                }
                
                String headerValue = request.getHeader(headerName);
                if (headerValue != null) {
                    String sanitizedName = sanitizationService.sanitizeInput(headerName);
                    String sanitizedValue = sanitizationService.sanitizeInput(headerValue);
                    sanitized.put(sanitizedName.toLowerCase(), sanitizedValue);
                }
            }
            
            return sanitized;
        }

        /**
         * Checks if a header is security-critical and should not be sanitized.
         */
        private boolean isSecurityHeader(String headerName) {
            if (headerName == null) {
                return false;
            }
            
            String lowerHeaderName = headerName.toLowerCase();
            return lowerHeaderName.equals("authorization") ||
                   lowerHeaderName.equals("cookie") ||
                   lowerHeaderName.equals("x-csrf-token") ||
                   lowerHeaderName.equals("x-requested-with") ||
                   lowerHeaderName.equals("content-type") ||
                   lowerHeaderName.equals("content-length") ||
                   lowerHeaderName.equals("host") ||
                   lowerHeaderName.equals("origin") ||
                   lowerHeaderName.equals("referer") ||
                   lowerHeaderName.startsWith("sec-") ||
                   lowerHeaderName.startsWith("x-forwarded-");
        }
    }
}
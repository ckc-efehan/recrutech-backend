-- Database initialization script for RecruTech Platform Service
-- Creates MySQL database for platform/domain data only
-- This script runs automatically when MySQL container is first initialized
--
-- NOTE: Auth service now uses PostgreSQL (see docker-compose.yml postgres service)
-- MySQL is used only for platform service domain data

-- Create recrutech database (platform/domain data)
CREATE DATABASE IF NOT EXISTS recrutech CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges to recrutech_user
GRANT ALL PRIVILEGES ON recrutech.* TO 'recrutech_user'@'%';

FLUSH PRIVILEGES;

-- Log completion
SELECT 'Database initialization completed: recrutech (platform) created' AS message;

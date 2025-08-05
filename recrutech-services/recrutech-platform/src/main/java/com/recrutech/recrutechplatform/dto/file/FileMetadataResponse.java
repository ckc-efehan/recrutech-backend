package com.recrutech.recrutechplatform.dto.file;

/**
 * DTO for sending file metadata to clients.
 */
public record FileMetadataResponse(String fileId, String fileName, String contentType, Long size) {
}
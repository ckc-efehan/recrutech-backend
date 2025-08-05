package com.recrutech.recrutechplatform.controller;

import com.recrutech.recrutechplatform.dto.file.FileMetadataResponse;
import com.recrutech.recrutechplatform.model.FileMetadata;
import com.recrutech.recrutechplatform.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for handling file storage operations.
 * This controller provides endpoints for uploading and retrieving files.
 */
@RestController
@RequestMapping("/storage")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    public FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload a file.
     *
     * @param file the file to upload
     * @return the metadata of the uploaded file
     */
    @PostMapping("/files")
    public ResponseEntity<FileMetadataResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        FileMetadata fileMetadata = fileStorageService.storeFile(file);

        FileMetadataResponse response = new FileMetadataResponse(
                fileMetadata.getId(),
                fileMetadata.getFileName(),
                fileMetadata.getContentType(),
                fileMetadata.getSize()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Download a file.
     *
     * @param fileId the ID of the file to download
     * @return the file as a resource
     */
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileId);
        FileMetadata metadata = fileStorageService.getFileMetadata(fileId);

        // Use the content type from metadata
        String contentType = metadata.getContentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                .body(resource);
    }
}

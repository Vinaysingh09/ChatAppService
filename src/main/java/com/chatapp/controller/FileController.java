package com.chatapp.controller;

import com.chatapp.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileService.uploadFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileName) {
        try {
            fileService.deleteFile(fileName);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File deletion failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 
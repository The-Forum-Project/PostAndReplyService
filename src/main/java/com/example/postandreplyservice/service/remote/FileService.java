package com.example.postandreplyservice.service.remote;

import com.example.postandreplyservice.dto.FileUrlResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.multipart.MultipartFile;

@Service
@FeignClient("file-service")
public interface FileService {
    @PostMapping(value = "file-service/files", consumes = "multipart/form-data")
    ResponseEntity<FileUrlResponse> uploadFiles(@RequestPart("files") MultipartFile[] files);
}

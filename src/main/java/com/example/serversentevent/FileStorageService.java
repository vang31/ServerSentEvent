package com.example.serversentevent;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface FileStorageService {
    public void save(MultipartFile file, SseEmitter emitters, String guid);
}

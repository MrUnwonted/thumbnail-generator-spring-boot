package com.techpool.tech;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thumbnail")
public class ThumbnailController {

    @Autowired
    private ThumbnailService thumbnailService;

    @PostMapping("/generate")
    public ResponseEntity<String> generate(@RequestParam String path) {
        File input = new File(path);

        if (!input.exists()) {
            return ResponseEntity.badRequest().body("Path does not exist.");
        }

        thumbnailService.processPath(input);
        return ResponseEntity.ok("Thumbnails generated successfully.");
    }
}
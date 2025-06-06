# Thumbnail Generator Service

A Java/Spring Boot service to generate thumbnails for images, videos, and documents.

## Features
✅ Image thumbnails (JPG, PNG, GIF)  
✅ Video thumbnails (MP4, AVI, MKV)  
✅ Document thumbnails (PDF, DOCX, PPT)  
✅ Fallback for unsupported files  

## Setup
1. Install FFmpeg & LibreOffice.
2. Clone this repo.
3. Run with `mvn spring-boot:run`.

## Usage
```java
thumbnailService.processPath(new File("/path/to/files"));

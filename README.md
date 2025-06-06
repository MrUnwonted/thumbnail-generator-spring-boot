### Thumbnail Generation Service
A Spring Boot service to generate thumbnails for images, videos, and documents with fallback support.

## ✨ Features
Feature	Supported Formats	Details
Images	JPG, PNG, GIF, BMP, WEBP	Resized to 200x200 using Thumbnailator.
Videos	MP4, AVI, MOV, MKV, FLV	Extracts frame at 1-second mark using FFmpeg.
Documents	PDF, DOC/DOCX, PPT/PPTX, XLS/XLSX	Converts to PDF via LibreOffice, then renders first page.
Fallback	Any unsupported file	Generates a default thumbnail with filename and "No Preview Available".
Encrypted PDFs	Password-protected PDFs	Shows "Password Protected" placeholder or extracted text (if possible).
## ⚙️ Prerequisites
Java: JDK 8+

Build Tool: Maven/Gradle

External Tools:

FFmpeg (for videos):

bash
# Linux/macOS
sudo apt install ffmpeg    # Ubuntu/Debian
brew install ffmpeg        # macOS (Homebrew)
Windows: Download from ffmpeg.org and add to PATH.

LibreOffice (for documents):

bash
# Linux/macOS
sudo apt install libreoffice    # Ubuntu/Debian
brew install libreoffice       # macOS
Windows: Install from LibreOffice.org.

## 🚀 How to Use
# 1. Add Dependency (Maven)
xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.27</version>
</dependency>
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.18</version>
</dependency>
# 2. Inject & Run
java
@Autowired
private ThumbnailService thumbnailService;

// Generate thumbnails for a directory
thumbnailService.processPath(new File("/path/to/files"));
# 3. Output
Thumbnails are saved as thumb_<original_name>.jpg in the same directory.

# Example:

text
/path/to/files/
  ├── document.pdf
  └── thumb_document.pdf.jpg  # Generated thumbnail
## 🛠 Troubleshooting
Issue	Solution
FFmpeg/LibreOffice not found	Verify installation and PATH settings.
Unsupported file type	Check logs; falls back to default thumb.
Process fails	Ensure files are not corrupted.
### 📜 License
MIT License. See LICENSE for details.

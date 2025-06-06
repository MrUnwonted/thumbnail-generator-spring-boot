Here's a clean and well-structured version of your documentation formatted for a GitHub `README.md`:

````markdown
# 📸 Thumbnail Generation Service

This guide explains how to integrate the `ThumbnailService` into your GitHub repository, including dependencies, setup, and usage instructions.

---

## ✅ Prerequisites

Before adding this service to your project, ensure the following are installed:

- [x] Java 8+ (JDK)
- [x] Maven
- [x] FFmpeg (for video thumbnails)
- [x] LibreOffice (for document conversion)
- [x] GitHub account

---

## 📦 Adding to GitHub

### Step 1: Create a New Repository

1. Go to GitHub and click **New repository**.
2. Name it (e.g., `thumbnail-generator-service`).
3. Choose Public/Private and optionally initialize with a `README.md`.
4. Click **Create repository**.

### Step 2: Clone the Repository Locally

```bash
git clone https://github.com/your-username/thumbnail-generator-service.git
cd thumbnail-generator-service
````

### Step 3: Add the Code

Create the folder structure:

```
src/main/java/com/techpool/tech/ThumbnailService.java
```

Copy your `ThumbnailService` code into this file.

### Step 4: Add Dependencies (`pom.xml`)

```xml
<dependencies>
    <!-- Spring Boot Starter (if used) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
        <version>2.7.0</version>
    </dependency>

    <!-- Apache Tika (MIME detection) -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>2.4.1</version>
    </dependency>

    <!-- PDFBox (PDF rendering) -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>2.0.27</version>
    </dependency>

    <!-- Thumbnailator (Image resizing) -->
    <dependency>
        <groupId>net.coobird</groupId>
        <artifactId>thumbnailator</artifactId>
        <version>0.4.18</version>
    </dependency>
</dependencies>
```

### Step 5: Commit & Push

```bash
git add .
git commit -m "feat: Add ThumbnailService for generating thumbnails"
git push origin main
```

---

## 🚀 Usage Instructions

### Run as a Spring Boot Component

Inject the service:

```java
@Autowired
private ThumbnailService thumbnailService;
```

Call the method:

```java
thumbnailService.processPath(new File("/path/to/files"));
```

### Run Standalone

```java
public static void main(String[] args) {
    ThumbnailService thumbnailService = new ThumbnailService();
    thumbnailService.processPath(new File("/path/to/files"));
}
```

---

## 🗂 Supported File Types

| Type      | Formats                  | Notes                                     |
| --------- | ------------------------ | ----------------------------------------- |
| Images    | JPG, PNG, GIF, BMP       | Uses Thumbnailator                        |
| Videos    | MP4, AVI, MKV, MOV       | Requires FFmpeg in PATH                   |
| Documents | PDF, DOC, DOCX, PPT, XLS | Converts to PDF first via LibreOffice     |
| Others    | Any unsupported file     | Generates a default placeholder thumbnail |

---

## 🔧 External Dependencies Setup

### FFmpeg (Video Thumbnails)

#### Linux / macOS

```bash
sudo apt install ffmpeg     # Ubuntu/Debian
brew install ffmpeg         # macOS with Homebrew
```

#### Windows

* Download from [ffmpeg.org](https://ffmpeg.org/download.html)
* Add to system PATH.

### LibreOffice (Document Conversion)

#### Linux / macOS

```bash
sudo apt install libreoffice   # Ubuntu/Debian
brew install libreoffice       # macOS with Homebrew
```

#### Windows

* Download from [libreoffice.org](https://www.libreoffice.org/)
* Install and ensure `soffice` is accessible in your PATH.

---

## 🛠 Error Handling & Troubleshooting

| Issue                        | Solution                                    |
| ---------------------------- | ------------------------------------------- |
| FFmpeg not found             | Install FFmpeg and ensure it's in your PATH |
| LibreOffice conversion fails | Check if `soffice --version` works          |
| Password-protected PDFs      | Skipped or fallback to text extraction      |
| Unsupported files            | Default placeholder thumbnail is generated  |

---

## 📄 License

This project is licensed under the **MIT License**. Be sure to include a `LICENSE` file in your repository.

```text
MIT License

Copyright (c) [Year] [Your Name]

Permission is hereby granted, free of charge, to any person obtaining a copy...
```

---

> 📌 Replace `[Year]` and `[Your Name]` in the license section with your details.

```

Let me know if you'd like this embedded directly into a GitHub repo or want additional badges like build status, license, or Java version.
```

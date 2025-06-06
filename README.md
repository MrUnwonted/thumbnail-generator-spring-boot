# 🖼️ Thumbnail Generator – Spring Boot Service

This Spring Boot service takes an input **file** or **folder path** (containing images or videos) and generates **200x200 JPEG thumbnails**.

- 📷 **Images** → Resized using [Thumbnailator](https://github.com/coobird/thumbnailator)
- 🎥 **Videos** → Frame extracted using [FFmpeg](https://ffmpeg.org/)
- 🧠 **File Type Detection** → Done via [Apache Tika](https://tika.apache.org/)

---

## 📌 Features

- Supports both images and video files.
- Automatically detects MIME types.
- Uses external `ffmpeg` for high-quality video thumbnail generation.
- Simple HTTP API to trigger processing.
- Generates thumbnails in the same directory with `thumb_` prefix.

---

## 🛠️ Project Setup

### ✅ 1. Prerequisites

- Java 17+
- Maven
- FFmpeg (installed and added to system PATH)
- Git (optional, for pushing to GitHub)

---

### ✅ 2. Dependencies (add to `pom.xml`)

```xml
<dependencies>
    <!-- Thumbnailator for image resizing -->
    <dependency>
        <groupId>net.coobird</groupId>
        <artifactId>thumbnailator</artifactId>
        <version>0.4.14</version>
    </dependency>

    <!-- Apache Tika for MIME type detection -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>2.9.0</version>
    </dependency>

    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>

###✅ 3. FFmpeg Installation
Download from https://ffmpeg.org/download.html

Extract it and add the ffmpeg/bin folder to your system PATH

Test it:

bash
Copy
Edit
ffmpeg -version
🧩 Code Structure
bash
Copy
Edit
src/
├── main/java/com/techpool/tech/
│   ├── ThumbnailController.java   # REST endpoint
│   └── ThumbnailService.java      # Core logic
└── resources/
    └── application.properties
🚀 Run the Application
Run the application from the terminal:

bash
Copy
Edit
mvn spring-boot:run
Once started, it will be available at:

arduino
Copy
Edit
http://localhost:8080
🧪 API Usage
📤 Endpoint
bash
Copy
Edit
POST /api/thumbnail/generate?path={full_path_to_file_or_folder}
📌 Example Request using cURL:
bash
Copy
Edit
curl -X POST "http://localhost:8080/api/thumbnail/generate?path=C:\Users\YourName\Videos"
✅ Response
200 OK – Thumbnails generated

400 Bad Request – Invalid or missing path

📁 Output Example
For input file:

Copy
Edit
dog.jpg
The service will generate:

Copy
Edit
thumb_dog.jpg.jpg
You can improve this by cleaning up the filename logic if needed.

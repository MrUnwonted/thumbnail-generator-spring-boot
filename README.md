📄 Thumbnail Generator – Documentation Guide
📌 Overview
This Spring Boot service takes an input file or folder path (containing images or videos) and generates 200x200 JPEG thumbnails:

📷 For images: Resized using Thumbnailator

🎥 For videos: Frame extracted using FFmpeg

🧠 File type detection: Done via Apache Tika

🛠️ Project Setup
✅ 1. Prerequisites
Java 17+

Maven

Spring Boot

FFmpeg installed and added to system PATH

Git (optional, for pushing to GitHub)

✅ 2. Dependencies in pom.xml
xml
Copy
Edit
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
✅ 3. FFmpeg Installation
Windows: Download from https://ffmpeg.org/download.html

Add ffmpeg/bin folder to PATH environment variable.

Test with:

sh
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
│   ├── ThumbnailService.java     # Core logic
└── resources/
    └── application.properties
🚀 Run the Application
Use any IDE or run via command line:

sh
Copy
Edit
mvn spring-boot:run
The app starts on http://localhost:8080.

🧪 Usage
Endpoint:
bash
Copy
Edit
POST /api/thumbnail/generate?path={full_path_to_file_or_folder}
Example using cURL:
sh
Copy
Edit
curl -X POST "http://localhost:8080/api/thumbnail/generate?path=C:\Users\YourName\Videos"
Response:
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

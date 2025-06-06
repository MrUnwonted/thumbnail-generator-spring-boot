package com.techpool.tech;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

// import com.techpool.tech.utils.FFmpegUtil;
// import com.techpool.tech.utils.FileTypeUtil;

import net.coobird.thumbnailator.Thumbnails;

import java.awt.image.BufferedImage;

@Service
public class ThumbnailService {

    public void processPath(File file) {
        if (file.isFile()) {
            generateThumbnail(file);
        } else {
            for (File f : file.listFiles()) {
                if (f.isFile()) {
                    generateThumbnail(f);
                }
            }
        }
    }

    private void generateThumbnail(File file) {
        try {
            String type = detectMimeType(file);

            if (type.startsWith("image")) {
                BufferedImage img = ImageIO.read(file);
                BufferedImage thumb = Thumbnails.of(img).size(200, 200).asBufferedImage();
                ImageIO.write(thumb, "jpg", new File(file.getParent(), "thumb_" + file.getName() + ".jpg"));
            } else if (type.startsWith("video")) {
                extractThumbnail(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void extractThumbnail(File videoFile) throws IOException {
        String output = videoFile.getParent() + "/thumb_" + videoFile.getName() + ".jpg";
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", videoFile.getAbsolutePath(),
                "-ss", "00:00:01.000", "-vframes", "1", output);
        pb.inheritIO();
        pb.start();
    }

    public static String detectMimeType(File file) throws IOException {
        return new Tika().detect(file);
    }
}

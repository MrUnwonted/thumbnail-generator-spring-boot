package com.techpool.tech;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ThumbnailService {

    // Constants for thumbnail generation
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;
    private static final String THUMBNAIL_PREFIX = "thumb_";
    private static final String DEFAULT_THUMBNAIL_TEXT = "No Preview\nAvailable";

    public void processPath(File file) {
        if (file.isFile()) {
            generateThumbnail(file);
        } else {
            File[] children = file.listFiles();
            if (children != null) {
                for (File f : children) {
                    processPath(f); // Recursive call
                }
            }
        }
    }

    private void generateThumbnail(File file) {
        try {
            String type = detectMimeType(file);
            System.out.println("Processing: " + file.getName() + " [" + type + "]");

            if (type.startsWith("image")) {
                generateImageThumbnail(file);
            } else if (type.startsWith("video")) {
                generateVideoThumbnail(file);
            } else if (type.equals("application/pdf")) {
                generatePdfThumbnail(file);
            } else if (isSupportedDocument(type)) {
                generateDocumentThumbnail(file, type);
            } else {
                generateDefaultThumbnail(file);
            }
        } catch (Exception e) {
            System.err.println("Failed to generate thumbnail for " + file.getName());
            e.printStackTrace();
            try {
                generateDefaultThumbnail(file);
            } catch (IOException ex) {
                System.err.println("Failed to generate default thumbnail for " + file.getName());
                ex.printStackTrace();
            }
        }
    }

    private boolean isSupportedDocument(String mimeType) {
        return mimeType.equals("application/pdf") ||
                mimeType.equals("application/msword") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                mimeType.equals("application/vnd.ms-excel") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                mimeType.equals("application/vnd.ms-powerpoint") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    private void generateImageThumbnail(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img != null) {
            BufferedImage thumb = Thumbnails.of(img)
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .asBufferedImage();
            saveThumbnail(thumb, file, "jpg");
        } else {
            throw new IOException("Unreadable image: " + file.getName());
        }
    }

    private void generateVideoThumbnail(File videoFile) throws IOException {
        String output = getThumbnailPath(videoFile, "jpg").toString();
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", videoFile.getAbsolutePath(),
                "-ss", "00:00:01.000", "-vframes", "1", output);
        pb.inheritIO();

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("FFmpeg failed with exit code " + exitCode);
            }

            // Verify the thumbnail was created
            if (!Files.exists(Paths.get(output))) {
                throw new IOException("FFmpeg didn't create the thumbnail file");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            throw new IOException("Video thumbnail generation was interrupted", e);
        }
    }

    private void generatePdfThumbnail(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            if (document.isEncrypted()) {
                // Try empty password first
                try {
                    document.setAllSecurityToBeRemoved(true);
                    PDFRenderer renderer = new PDFRenderer(document);
                    BufferedImage image = renderer.renderImageWithDPI(0, 150);
                    saveThumbnail(image, pdfFile, "jpg");
                } catch (Exception e) {
                    System.out.println("Password-protected PDF: " + pdfFile.getName() + " - generating text preview");
                    generateTextPreviewThumbnail(pdfFile, extractTextFromPdf(document));
                }
            } else {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage image = renderer.renderImageWithDPI(0, 150);
                saveThumbnail(image, pdfFile, "jpg");
            }
        } catch (InvalidPasswordException e) {
            System.out.println("Password-protected PDF: " + pdfFile.getName() + " - generating text preview");
            generateTextPreviewThumbnail(pdfFile, "Password Protected\nContent Not Accessible");
        }
    }

    private String extractTextFromPdf(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private void generateDocumentThumbnail(File documentFile, String mimeType)
            throws IOException, InterruptedException {
        // First convert to PDF
        File pdfFile = convertToPdf(documentFile);

        if (pdfFile != null && pdfFile.exists()) {
            // Then generate thumbnail from PDF
            generatePdfThumbnail(pdfFile);

            // Clean up temporary PDF file
            if (!pdfFile.delete()) {
                System.out.println("Warning: Could not delete temporary PDF file: " + pdfFile.getAbsolutePath());
            }
        } else {
            throw new IOException("Failed to convert document to PDF: " + documentFile.getName());
        }
    }

    private File convertToPdf(File inputFile) throws IOException, InterruptedException {
        String sofficeCommand = getLibreOfficeCommand();
        File outputDir = inputFile.getParentFile();
        String outputPath = outputDir.getAbsolutePath();

        ProcessBuilder pb = new ProcessBuilder(
                sofficeCommand,
                "--headless",
                "--convert-to", "pdf",
                "--outdir", outputPath,
                inputFile.getAbsolutePath());

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("LibreOffice conversion failed with exit code " + exitCode);
        }

        // Find the converted PDF file
        String pdfFilename = inputFile.getName().replaceFirst("\\.[^.]+$", "") + ".pdf";
        File pdfFile = new File(outputDir, pdfFilename);

        return pdfFile.exists() ? pdfFile : null;
    }

    private String getLibreOfficeCommand() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Check common Windows installation paths
            String[] possiblePaths = {
                    "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                    "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
            };

            for (String path : possiblePaths) {
                File sofficeFile = new File(path);
                if (sofficeFile.exists()) {
                    return path;
                }
            }
        }

        // Default to just 'soffice' (should be in PATH)
        return "soffice";
    }

    private void generateTextPreviewThumbnail(File originalFile, String text) throws IOException {
        BufferedImage image = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Set background
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        // Set text properties
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial", Font.PLAIN, 12));

        // Draw the text with word wrapping
        drawWrappedText(graphics, text, 10, 10, THUMBNAIL_WIDTH - 20);

        graphics.dispose();
        saveThumbnail(image, originalFile, "jpg");
    }

    private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth) {
        FontMetrics metrics = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine + (currentLine.length() > 0 ? " " : "") + word;
            int testWidth = metrics.stringWidth(testLine);

            if (testWidth > maxWidth && currentLine.length() > 0) {
                g.drawString(currentLine.toString(), x, y);
                y += metrics.getHeight();
                currentLine = new StringBuilder(word);
            } else {
                currentLine.append(currentLine.length() > 0 ? " " : "").append(word);
            }
        }

        if (currentLine.length() > 0) {
            g.drawString(currentLine.toString(), x, y);
        }
    }

    private void generateDefaultThumbnail(File file) throws IOException {
        // Create an image with file icon and name
        BufferedImage image = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Set background
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        // Draw border
        graphics.setColor(Color.GRAY);
        graphics.drawRect(0, 0, THUMBNAIL_WIDTH - 1, THUMBNAIL_HEIGHT - 1);

        // Set text properties
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial", Font.BOLD, 14));

        // Draw file icon (simple rectangle)
        graphics.setColor(new Color(200, 230, 255));
        graphics.fillRect(THUMBNAIL_WIDTH / 4, THUMBNAIL_HEIGHT / 4, THUMBNAIL_WIDTH / 2, THUMBNAIL_HEIGHT / 3);
        graphics.setColor(Color.BLUE);
        graphics.drawRect(THUMBNAIL_WIDTH / 4, THUMBNAIL_HEIGHT / 4, THUMBNAIL_WIDTH / 2, THUMBNAIL_HEIGHT / 3);

        // Draw file name (truncated if needed)
        String name = file.getName();
        FontMetrics metrics = graphics.getFontMetrics();
        if (metrics.stringWidth(name) > THUMBNAIL_WIDTH - 20) {
            while (metrics.stringWidth(name + "...") > THUMBNAIL_WIDTH - 20 && name.length() > 3) {
                name = name.substring(0, name.length() - 1);
            }
            name = name + "...";
        }

        graphics.drawString(name, (THUMBNAIL_WIDTH - metrics.stringWidth(name)) / 2, THUMBNAIL_HEIGHT * 3 / 4);

        // Draw "No Preview" text
        graphics.setFont(new Font("Arial", Font.ITALIC, 12));
        String noPreview = DEFAULT_THUMBNAIL_TEXT;
        int textWidth = metrics.stringWidth(noPreview);
        graphics.drawString(noPreview, (THUMBNAIL_WIDTH - textWidth) / 2, THUMBNAIL_HEIGHT * 4 / 5);

        graphics.dispose();
        saveThumbnail(image, file, "jpg");
    }

    private Path getThumbnailPath(File originalFile, String extension) {
        String thumbName = THUMBNAIL_PREFIX + originalFile.getName() + "." + extension;
        return Paths.get(originalFile.getParent(), thumbName);
    }

    private void saveThumbnail(BufferedImage image, File originalFile, String format) throws IOException {
        Path outputPath = getThumbnailPath(originalFile, format);
        ImageIO.write(image, format, outputPath.toFile());
    }

    private String detectMimeType(File file) throws IOException {
        return new Tika().detect(file);
    }
}
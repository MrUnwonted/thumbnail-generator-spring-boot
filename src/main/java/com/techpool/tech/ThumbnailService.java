package com.techpool.tech;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
            } else if (type.equals("text/csv") || type.equals("application/vnd.ms-excel") ||
                    type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                generateExcelThumbnail(file); // This handles both Excel and CSV
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

    private void generateExcelThumbnail(File file) throws IOException {
        try {
            List<String> previewLines;
            if (file.getName().toLowerCase().endsWith(".csv")) {
                previewLines = readCsvPreview(file, 3);
            } else {
                previewLines = readExcelPreview(file, 3); // You'll need to implement this
            }
            BufferedImage image = createDataPreviewImage(file.getName(), previewLines);
            saveThumbnail(image, file, "jpg");
        } catch (Exception e) {
            generateDefaultThumbnail(file);
        }
    }

    private List<String> readExcelPreview(File excelFile, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(excelFile)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Add header
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                lines.add(getExcelRowAsString(headerRow));
            }

            // Add data rows
            for (int i = 1; i <= maxLines && i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    lines.add(getExcelRowAsString(row));
                }
            }
        }
        return lines;
    }

    private String getExcelRowAsString(Row row) {
        StringBuilder sb = new StringBuilder();
        for (Cell cell : row) {
            switch (cell.getCellType()) {
                case STRING:
                    sb.append(cell.getStringCellValue());
                    break;
                case NUMERIC:
                    sb.append(cell.getNumericCellValue());
                    break;
                case BOOLEAN:
                    sb.append(cell.getBooleanCellValue());
                    break;
                default:
                    sb.append(" ");
            }
            sb.append(" ");
        }
        return cleanCsvLine(sb.toString());
    }

    private String readSpreadsheetPreview(File file) throws IOException {
        StringBuilder preview = new StringBuilder();
        String firstLine;

        // Simple CSV reader (for both CSV and Excel)
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Read header
            firstLine = br.readLine();
            if (firstLine != null) {
                preview.append("Columns: ").append(firstLine.split(",").length).append("\n");
                preview.append(truncateLine(firstLine)).append("\n...\n");
            }

            // Read first 3 data rows
            for (int i = 0; i < 3; i++) {
                String line = br.readLine();
                if (line == null)
                    break;
                preview.append(truncateLine(line)).append("\n");
            }
        }

        return preview.toString();
    }

    private String truncateLine(String line) {
        return line.length() > 50 ? line.substring(0, 47) + "..." : line;
    }

    private void generateCsvThumbnail(File csvFile) throws IOException {
        try {
            List<String> previewLines = readCsvPreview(csvFile, 3); // Read header + 3 lines
            BufferedImage image = createDataPreviewImage(csvFile.getName(), previewLines);
            saveThumbnail(image, csvFile, "jpg");
        } catch (Exception e) {
            generateDefaultThumbnail(csvFile); // Fallback if anything fails
        }
    }

    private List<String> readCsvPreview(File csvFile, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null && lines.size() < maxLines) {
                lines.add(cleanCsvLine(line));
            }
        }
        return lines;
    }

    private String cleanCsvLine(String line) {
        // 1. Trim and limit length
        line = line.trim();
        if (line.length() > 50) {
            line = line.substring(0, 47) + "...";
        }

        // 2. Remove special characters that break rendering
        line = line.replaceAll("[^\\x20-\\x7E]", "");

        // 3. Replace multiple spaces with single space
        return line.replaceAll("\\s+", " ");
    }

    private BufferedImage createDataPreviewImage(String filename, List<String> lines) {
        BufferedImage image = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Set anti-aliasing for better text quality
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        g.setColor(new Color(240, 240, 240)); // Light gray
        g.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        // Draw header
        g.setColor(new Color(0, 82, 165)); // Dark blue
        g.fillRect(0, 0, THUMBNAIL_WIDTH, 25);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(truncateFilename(filename), 5, 18);

        // Draw data rows
        g.setColor(Color.BLACK);
        g.setFont(new Font("Courier New", Font.PLAIN, 10));

        int y = 40;
        for (String line : lines) {
            if (y > THUMBNAIL_HEIGHT - 15)
                break;
            g.drawString(line, 5, y);
            y += 15;
        }

        // Draw footer
        g.setColor(Color.GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString(lines.size() + " rows shown", 5, THUMBNAIL_HEIGHT - 5);

        g.dispose();
        return image;
    }

    private String truncateFilename(String filename) {
        if (filename.length() > 20) {
            return filename.substring(0, 17) + "...";
        }
        return filename;
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
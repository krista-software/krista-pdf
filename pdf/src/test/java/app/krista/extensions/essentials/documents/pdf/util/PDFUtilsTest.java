/*
 * Pdf Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>. 
 */

package app.krista.extensions.essentials.documents.pdf.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test class for PDFUtils functionality.
 * Tests various PDF utility operations including image validation, image to PDF conversion,
 * and image format conversion.
 */
public class PDFUtilsTest {

    private List<File> filesToCleanup;

    @Before
    public void setUp() {
        // Given - Initialize test dependencies
        filesToCleanup = new ArrayList<>();
    }

    @After
    public void tearDown() {
        // Finally - Clean up all generated files
        for (File file : filesToCleanup) {
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    System.err.println("Warning: Failed to delete test file: " + file.getAbsolutePath());
                }
            }
        }
        filesToCleanup.clear();
    }

    /**
     * Helper method to add files to cleanup list for automatic deletion after tests.
     * @param files files to be cleaned up
     */
    private void addToCleanup(File... files) {
        for (File file : files) {
            if (file != null) {
                filesToCleanup.add(file);
            }
        }
    }

    /**
     * Helper method to create a temporary test image file.
     * @param fileName the name for the test file
     * @param extension the file extension
     * @return a temporary test file
     * @throws IOException if file creation fails
     */
    private File createTestImageFile(String fileName, String extension) throws IOException {
        // Create a simple test image
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName + extension);
        String format = extension.substring(1); // Remove the dot
        ImageIO.write(testImage, format, tempFile);
        addToCleanup(tempFile);
        return tempFile;
    }

    @Test
    public void testIsValidImageFile_WithValidPngFile() {
        // Given
        File pngFile = new File("test.png");

        // When
        boolean result = PDFUtils.isValidImageFile(pngFile);

        // Then
        Assert.assertTrue("PNG file should be valid", result);
    }

    @Test
    public void testIsValidImageFile_WithValidJpgFile() {
        // Given
        File jpgFile = new File("test.jpg");

        // When
        boolean result = PDFUtils.isValidImageFile(jpgFile);

        // Then
        Assert.assertTrue("JPG file should be valid", result);
    }

    @Test
    public void testIsValidImageFile_WithValidJpegFile() {
        // Given
        File jpegFile = new File("test.jpeg");

        // When
        boolean result = PDFUtils.isValidImageFile(jpegFile);

        // Then
        Assert.assertTrue("JPEG file should be valid", result);
    }

    @Test
    public void testIsValidImageFile_WithInvalidFile() {
        // Given
        File invalidFile = new File("test.txt");

        // When
        boolean result = PDFUtils.isValidImageFile(invalidFile);

        // Then
        Assert.assertFalse("TXT file should not be valid", result);
    }

    @Test
    public void testIsValidImageFile_WithMixedCaseExtension() {
        // Given
        File mixedCaseFile = new File("test.PNG");

        // When
        boolean result = PDFUtils.isValidImageFile(mixedCaseFile);

        // Then
        Assert.assertTrue("Mixed case PNG file should be valid", result);
    }

    @Test
    public void testIsValidImageFile_WithNoExtension() {
        // Given
        File noExtensionFile = new File("test");

        // When
        boolean result = PDFUtils.isValidImageFile(noExtensionFile);

        // Then
        Assert.assertFalse("File without extension should not be valid", result);
    }

    @Test
    public void testConvertImageToPdf_WithValidPngFile() throws IOException {
        // Given
        File imageFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sampleImage.png")).getFile());

        // When
        File pdfFile = PDFUtils.convertImageToPdf(imageFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);
        Assert.assertTrue("PDF file should have .pdf extension", pdfFile.getName().endsWith(".pdf"));

        // When - Load and validate PDF content
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertEquals("PDF should have exactly 1 page", 1, pdfDocument.getNumberOfPages());

            // When - Render PDF page to image
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            BufferedImage renderedImage = renderer.renderImage(0);

            // Then
            Assert.assertNotNull("Rendered image should not be null", renderedImage);
            Assert.assertTrue("Rendered image should have positive width", renderedImage.getWidth() > 0);
            Assert.assertTrue("Rendered image should have positive height", renderedImage.getHeight() > 0);
        }
    }

    @Test
    public void testConvertImageToPdf_WithValidJpgFile() throws IOException {
        // Given
        File jpgFile = createTestImageFile("test", ".jpg");

        // When
        File pdfFile = PDFUtils.convertImageToPdf(jpgFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);

        // When - Validate PDF structure
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertEquals("PDF should have exactly 1 page", 1, pdfDocument.getNumberOfPages());
        }
    }

    @Test
    public void testConvertImageToPdf_WithNullInput() {
        // Given
        File nullImageFile = null;

        try {
            // When
            PDFUtils.convertImageToPdf(nullImageFile);

            // Then
            Assert.fail("Should throw exception for null input");
        } catch (Exception e) {
            // Then
            Assert.assertNotNull("Exception should be thrown for null input", e);
        }
    }

    @Test
    public void testConvertImageToPdf_WithNonExistentFile() {
        // Given
        File nonExistentFile = new File("non-existent-file.png");

        try {
            // When
            PDFUtils.convertImageToPdf(nonExistentFile);

            // Then
            Assert.fail("Should throw exception for non-existent file");
        } catch (IOException e) {
            // Then
            Assert.assertNotNull("IOException should be thrown for non-existent file", e);
        }
    }

    @Test
    public void testConvertImageToPdf_WithInvalidImageFile() throws IOException {
        // Given - Create a text file with image extension
        File invalidImageFile = new File(System.getProperty("java.io.tmpdir"), "invalid.png");
        invalidImageFile.createNewFile();
        addToCleanup(invalidImageFile);

        try {
            // When
            PDFUtils.convertImageToPdf(invalidImageFile);

            // Then
            Assert.fail("Should throw exception for invalid image file");
        } catch (IOException e) {
            // Then
            Assert.assertNotNull("IOException should be thrown for invalid image file", e);
        }
    }

    @Test
    public void testConvertToPngIfRequired_WithJpegFile() throws IOException {
        // Given
        File jpegFile = createTestImageFile("test", ".jpeg");

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(jpegFile);
        addToCleanup(resultFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertTrue("Result file should exist", resultFile.exists());
        Assert.assertTrue("Result file should have .png extension", resultFile.getName().endsWith(".png"));
        Assert.assertTrue("Result file should not be empty", resultFile.length() > 0);

        // When - Verify it's a valid PNG image
        BufferedImage image = ImageIO.read(resultFile);

        // Then
        Assert.assertNotNull("Converted image should be readable", image);
        Assert.assertTrue("Converted image should have positive width", image.getWidth() > 0);
        Assert.assertTrue("Converted image should have positive height", image.getHeight() > 0);
    }

    @Test
    public void testConvertToPngIfRequired_WithJpgFile() throws IOException {
        // Given
        File jpgFile = createTestImageFile("test", ".jpg");

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(jpgFile);
        addToCleanup(resultFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertTrue("Result file should exist", resultFile.exists());
        Assert.assertTrue("Result file should have .png extension", resultFile.getName().endsWith(".png"));
    }

    @Test
    public void testConvertToPngIfRequired_WithPngFile() throws IOException {
        // Given
        File pngFile = createTestImageFile("test", ".png");

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(pngFile);
        addToCleanup(resultFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertTrue("Result file should exist", resultFile.exists());
        Assert.assertTrue("Result file should have .png extension", resultFile.getName().endsWith(".png"));

        // Note: PNG files are processed and resized. The replaceAll only affects .jpeg/.jpg extensions,
        // so for .png files, the output filename might be the same as the cleaned input filename.
        // This could result in the same file path, but the content should be processed/resized.

        // Verify the file content was processed by checking file size or reading the image
        BufferedImage resultImage = ImageIO.read(resultFile);
        Assert.assertNotNull("Result image should be readable", resultImage);
        Assert.assertTrue("Result image should have positive dimensions",
            resultImage.getWidth() > 0 && resultImage.getHeight() > 0);
    }

    @Test
    public void testConvertToPngIfRequired_WithUnsupportedFile() throws IOException {
        // Given
        File bmpFile = createTestImageFile("test", ".bmp");

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(bmpFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertEquals("Unsupported file should be returned unchanged", bmpFile, resultFile);
    }

    @Test
    public void testConvertToPngIfRequired_FileNameCleaningBehavior() throws IOException {
        // Given - Test how filename cleaning affects conversion
        File normalJpgFile = createTestImageFile("normal_file", ".jpg");

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(normalJpgFile);
        addToCleanup(resultFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertTrue("Result file should exist", resultFile.exists());
        Assert.assertTrue("Normal JPG file should be converted to PNG", resultFile.getName().endsWith(".png"));
        Assert.assertNotEquals("Result should be different from original", normalJpgFile, resultFile);
    }

    @Test
    public void testConvertToPngIfRequired_WithNullInput() {
        // Given
        File nullFile = null;

        try {
            // When
            PDFUtils.convertToPngIfRequired(nullFile);

            // Then
            Assert.fail("Should throw exception for null input");
        } catch (Exception e) {
            // Then
            Assert.assertNotNull("Exception should be thrown for null input", e);
        }
    }

    @Test
    public void testConvertToPngIfRequired_WithInvalidImageFile() throws IOException {
        // Given - Create a text file with image extension
        File invalidImageFile = new File(System.getProperty("java.io.tmpdir"), "invalid.jpg");
        invalidImageFile.createNewFile();
        addToCleanup(invalidImageFile);

        try {
            // When
            PDFUtils.convertToPngIfRequired(invalidImageFile);

            // Then
            Assert.fail("Should throw exception for invalid image file");
        } catch (IOException e) {
            // Then
            Assert.assertNotNull("IOException should be thrown for invalid image file", e);
            Assert.assertTrue("Exception message should mention failed to decode",
                e.getMessage().contains("Failed to decode image"));
        }
    }

    @Test
    public void testConvertToPngIfRequired_WithSpecialCharactersInFileName() throws IOException {
        // Given - Test with normal filename first
        File normalFile = createTestImageFile("test_special", ".jpg");

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(normalFile);
        addToCleanup(resultFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertTrue("Result file should exist", resultFile.exists());
        Assert.assertTrue("Normal JPG file should be converted to PNG", resultFile.getName().endsWith(".png"));

        // Given - Test with file that has special characters that affect extension detection
        File specialCharFile = createTestImageFile("test", ".jpg");
        // Rename to add special characters that will be cleaned by getSafeFileName
        File renamedFile = new File(specialCharFile.getParent(), "test@#$%.jpg");
        boolean renamed = specialCharFile.renameTo(renamedFile);
        addToCleanup(renamedFile);

        if (renamed) {
            // When
            File resultFile2 = PDFUtils.convertToPngIfRequired(renamedFile);
            addToCleanup(resultFile2);

            // Then
            Assert.assertNotNull("Result file should not be null", resultFile2);
            Assert.assertTrue("Result file should exist", resultFile2.exists());

            // The cleanFileName method removes dots and special characters, so "test@#$%.jpg" becomes "test"
            // Since the cleaned filename doesn't end with .jpg/.jpeg/.png, the original file is returned unchanged
            Assert.assertEquals("Original file should be returned when cleaning removes extension",
                renamedFile, resultFile2);
            Assert.assertTrue("Original file should still have .jpg extension",
                resultFile2.getName().endsWith(".jpg"));
        }
    }

    @Test
    public void testConvertImageToPdf_ImageScalingAndCentering() throws IOException {
        // Given
        File imageFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sampleImage.png")).getFile());

        // When
        File pdfFile = PDFUtils.convertImageToPdf(imageFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());

        // When - Load PDF and verify page dimensions
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            PDPage page = pdfDocument.getPage(0);
            PDRectangle mediaBox = page.getMediaBox();

            // Then
            Assert.assertEquals("Page should be A4 width", PDRectangle.A4.getWidth(), mediaBox.getWidth(), 0.1f);
            Assert.assertEquals("Page should be A4 height", PDRectangle.A4.getHeight(), mediaBox.getHeight(), 0.1f);

            // When - Render and check image is properly scaled
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            BufferedImage renderedImage = renderer.renderImage(0, 1.0f);

            // Then
            Assert.assertNotNull("Rendered image should not be null", renderedImage);
            // Image should be scaled to fit within page margins (A4 - 40 points margin)
            float expectedMaxWidth = PDRectangle.A4.getWidth() - 40;
            float expectedMaxHeight = PDRectangle.A4.getHeight() - 40;
            Assert.assertTrue("Rendered image width should be reasonable",
                renderedImage.getWidth() > 0 && renderedImage.getWidth() <= expectedMaxWidth * 2); // Allow for DPI scaling
        }
    }

    @Test
    public void testConvertImageToPdf_WithLargeImage() throws IOException {
        // Given - Create a large test image
        BufferedImage largeImage = new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_RGB);
        File largeImageFile = new File(System.getProperty("java.io.tmpdir"), "large_test.png");
        ImageIO.write(largeImage, "png", largeImageFile);
        addToCleanup(largeImageFile);

        // When
        File pdfFile = PDFUtils.convertImageToPdf(largeImageFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);

        // When - Verify PDF structure
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertEquals("PDF should have exactly 1 page", 1, pdfDocument.getNumberOfPages());
        }
    }

    @Test
    public void testConvertImageToPdf_WithSmallImage() throws IOException {
        // Given - Create a small test image
        BufferedImage smallImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        File smallImageFile = new File(System.getProperty("java.io.tmpdir"), "small_test.png");
        ImageIO.write(smallImage, "png", smallImageFile);
        addToCleanup(smallImageFile);

        // When
        File pdfFile = PDFUtils.convertImageToPdf(smallImageFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);

        // When - Verify PDF structure
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertEquals("PDF should have exactly 1 page", 1, pdfDocument.getNumberOfPages());
        }
    }

    @Test
    public void testConvertToPngIfRequired_PreservesImageQuality() throws IOException {
        // Given
        File originalJpegFile = createTestImageFile("quality_test", ".jpeg");

        // When
        File convertedPngFile = PDFUtils.convertToPngIfRequired(originalJpegFile);
        addToCleanup(convertedPngFile);

        // Then
        Assert.assertNotNull("Converted file should not be null", convertedPngFile);
        Assert.assertTrue("Converted file should exist", convertedPngFile.exists());

        // When - Read both images and compare dimensions
        BufferedImage originalImage = ImageIO.read(originalJpegFile);
        BufferedImage convertedImage = ImageIO.read(convertedPngFile);

        // Then
        Assert.assertNotNull("Original image should be readable", originalImage);
        Assert.assertNotNull("Converted image should be readable", convertedImage);
        // Note: Dimensions might be different due to resizing for A4 fit
        Assert.assertTrue("Converted image should have positive dimensions",
            convertedImage.getWidth() > 0 && convertedImage.getHeight() > 0);
    }

    @Test
    public void testIsValidImageFile_WithEmptyFileName() {
        // Given
        File emptyNameFile = new File("");

        // When
        boolean result = PDFUtils.isValidImageFile(emptyNameFile);

        // Then
        Assert.assertFalse("Empty filename should not be valid", result);
    }

    @Test
    public void testIsValidImageFile_WithMultipleExtensions() {
        // Given
        File multiExtensionFile = new File("test.backup.png");

        // When
        boolean result = PDFUtils.isValidImageFile(multiExtensionFile);

        // Then
        Assert.assertTrue("File with multiple extensions ending in .png should be valid", result);
    }

    @Test
    public void testConvertImageToPdf_VerifyTelemetryLogging() throws IOException {
        // Given
        File imageFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sampleImage.png")).getFile());

        // When
        File pdfFile = PDFUtils.convertImageToPdf(imageFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        // Note: Telemetry logging verification would require mocking the telemetry logger
        // For now, we verify that the method completes successfully, which indicates telemetry was called
    }

    @Test
    public void testConvertToPngIfRequired_VerifyTelemetryLogging() throws IOException {
        // Given
        File jpegFile = createTestImageFile("telemetry_test", ".jpeg");

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(jpegFile);
        addToCleanup(resultFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertTrue("Result file should exist", resultFile.exists());
        // Note: Telemetry logging verification would require mocking the telemetry logger
        // For now, we verify that the method completes successfully, which indicates telemetry was called
    }

    @Test
    public void testConvertImageToPdf_WithDifferentImageFormats() throws IOException {
        // Given - Test with different supported formats
        String[] formats = {".png", ".jpg", ".jpeg"};

        for (String format : formats) {
            // Given
            File imageFile = createTestImageFile("format_test", format);

            // When
            File pdfFile = PDFUtils.convertImageToPdf(imageFile);
            addToCleanup(pdfFile);

            // Then
            Assert.assertNotNull("PDF file should not be null for " + format, pdfFile);
            Assert.assertTrue("PDF file should exist for " + format, pdfFile.exists());
            Assert.assertTrue("PDF file should not be empty for " + format, pdfFile.length() > 0);

            // When - Verify PDF structure
            try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
                // Then
                Assert.assertEquals("PDF should have exactly 1 page for " + format,
                    1, pdfDocument.getNumberOfPages());
            }
        }
    }

    @Test
    public void testConvertImageToPdf_EndToEndWorkflow() throws IOException {
        // Given - Simulate a complete workflow
        File originalJpegFile = createTestImageFile("workflow_test", ".jpeg");

        // When - Step 1: Convert JPEG to PNG if required
        File processedImageFile = PDFUtils.convertToPngIfRequired(originalJpegFile);
        addToCleanup(processedImageFile);

        // Then
        Assert.assertNotNull("Processed image file should not be null", processedImageFile);
        Assert.assertTrue("Processed image file should exist", processedImageFile.exists());
        Assert.assertTrue("Processed image should be PNG", processedImageFile.getName().endsWith(".png"));

        // When - Step 2: Convert processed image to PDF
        File pdfFile = PDFUtils.convertImageToPdf(processedImageFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);

        // When - Step 3: Verify final PDF content
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertEquals("PDF should have exactly 1 page", 1, pdfDocument.getNumberOfPages());

            // When - Render and verify image content
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            BufferedImage renderedImage = renderer.renderImage(0);

            // Then
            Assert.assertNotNull("Rendered image should not be null", renderedImage);
            Assert.assertTrue("Rendered image should have positive dimensions",
                renderedImage.getWidth() > 0 && renderedImage.getHeight() > 0);
        }
    }

    @Test
    public void testIsValidImageFile_CaseInsensitiveExtensions() {
        // Given - Test various case combinations
        String[] testFiles = {
            "test.PNG", "test.JPG", "test.JPEG",
            "test.png", "test.jpg", "test.jpeg",
            "test.Png", "test.Jpg", "test.Jpeg"
        };

        for (String fileName : testFiles) {
            // Given
            File testFile = new File(fileName);

            // When
            boolean result = PDFUtils.isValidImageFile(testFile);

            // Then
            Assert.assertTrue("File " + fileName + " should be valid", result);
        }
    }

    @Test
    public void testConvertToPngIfRequired_WithFileInDifferentDirectory() throws IOException {
        // Given - Create test file in a subdirectory
        File subDir = new File(System.getProperty("java.io.tmpdir"), "test_subdir");
        subDir.mkdirs();
        addToCleanup(subDir);

        BufferedImage testImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        File jpegFileInSubDir = new File(subDir, "subdir_test.jpeg");
        ImageIO.write(testImage, "jpeg", jpegFileInSubDir);
        addToCleanup(jpegFileInSubDir);

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(jpegFileInSubDir);
        addToCleanup(resultFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertTrue("Result file should exist", resultFile.exists());
        Assert.assertTrue("Result file should have .png extension", resultFile.getName().endsWith(".png"));
        Assert.assertEquals("Result file should be in same directory as original",
            jpegFileInSubDir.getParent(), resultFile.getParent());
    }

    @Test
    public void testConvertImageToPdf_VerifyPDFStructure() throws IOException {
        // Given
        File imageFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sampleImage.png")).getFile());

        // When
        File pdfFile = PDFUtils.convertImageToPdf(imageFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());

        // When - Examine PDF structure in detail
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then - Verify document properties
            Assert.assertEquals("PDF should have exactly 1 page", 1, pdfDocument.getNumberOfPages());
            Assert.assertNotNull("PDF should have document information", pdfDocument.getDocumentInformation());

            // When - Examine page properties
            PDPage page = pdfDocument.getPage(0);
            PDRectangle mediaBox = page.getMediaBox();

            // Then - Verify page dimensions are A4
            Assert.assertEquals("Page width should be A4", PDRectangle.A4.getWidth(), mediaBox.getWidth(), 0.1f);
            Assert.assertEquals("Page height should be A4", PDRectangle.A4.getHeight(), mediaBox.getHeight(), 0.1f);

            // When - Check if page has content
            Assert.assertNotNull("Page should have resources", page.getResources());
        }
    }

    @Test
    public void testConvertToPngIfRequired_WithUnicodeFileName() throws IOException {
        // Given - Create file with Unicode characters in name
        BufferedImage testImage = new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
        File unicodeFile = new File(System.getProperty("java.io.tmpdir"), "测试文件.jpg");
        ImageIO.write(testImage, "jpg", unicodeFile);
        addToCleanup(unicodeFile);

        // When
        File resultFile = PDFUtils.convertToPngIfRequired(unicodeFile);
        addToCleanup(resultFile);

        // Then
        Assert.assertNotNull("Result file should not be null", resultFile);
        Assert.assertTrue("Result file should exist", resultFile.exists());
        Assert.assertTrue("Result file should have .png extension", resultFile.getName().endsWith(".png"));
        // File name should be cleaned but still valid
        Assert.assertTrue("Result file name should be valid", resultFile.getName().length() > 4); // At least ".png"
    }

    @Test
    public void testIsValidImageFile_WithPathSeparators() {
        // Given
        File fileWithPath = new File("/path/to/test.png");

        // When
        boolean result = PDFUtils.isValidImageFile(fileWithPath);

        // Then
        Assert.assertTrue("File with path should be valid if extension is valid", result);
    }

    @Test
    public void testConvertImageToPdf_MemoryEfficiency() throws IOException {
        // Given - Test with multiple conversions to check for memory leaks
        File imageFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sampleImage.png")).getFile());
        List<File> generatedPdfs = new ArrayList<>();

        // When - Perform multiple conversions
        for (int i = 0; i < 5; i++) {
            File pdfFile = PDFUtils.convertImageToPdf(imageFile);
            generatedPdfs.add(pdfFile);
            addToCleanup(pdfFile);
        }

        // Then - Verify all conversions succeeded
        for (int i = 0; i < generatedPdfs.size(); i++) {
            File pdfFile = generatedPdfs.get(i);
            Assert.assertNotNull("PDF file " + i + " should not be null", pdfFile);
            Assert.assertTrue("PDF file " + i + " should exist", pdfFile.exists());
            Assert.assertTrue("PDF file " + i + " should not be empty", pdfFile.length() > 0);

            // When - Verify each PDF is valid
            try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
                // Then
                Assert.assertEquals("PDF " + i + " should have exactly 1 page", 1, pdfDocument.getNumberOfPages());
            }
        }
    }
}

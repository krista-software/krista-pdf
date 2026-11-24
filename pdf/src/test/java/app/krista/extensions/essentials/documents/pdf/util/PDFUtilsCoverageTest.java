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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * New JUnit 5 coverage tests for {@link PDFUtils}, targeting methods and branches
 * that are not exercised by the (currently non-executing, JUnit 4) PDFUtilsTest.
 */
class PDFUtilsCoverageTest {

    // ---------------------------------------------------------------
    // isValidImageFile
    // ---------------------------------------------------------------

    @Test
    void isValidImageFile_png_jpg_jpeg_areValid() {
        assertTrue(PDFUtils.isValidImageFile(new File("picture.png")));
        assertTrue(PDFUtils.isValidImageFile(new File("picture.jpg")));
        assertTrue(PDFUtils.isValidImageFile(new File("picture.jpeg")));
    }

    @Test
    void isValidImageFile_caseInsensitive() {
        assertTrue(PDFUtils.isValidImageFile(new File("picture.PNG")));
        assertTrue(PDFUtils.isValidImageFile(new File("picture.JPG")));
    }

    @Test
    void isValidImageFile_unsupportedExtension_isInvalid() {
        assertFalse(PDFUtils.isValidImageFile(new File("document.txt")));
    }

    @Test
    void isValidImageFile_noExtension_isInvalid() {
        assertFalse(PDFUtils.isValidImageFile(new File("noextension")));
    }

    // ---------------------------------------------------------------
    // convertImageToPdf
    // ---------------------------------------------------------------

    @Test
    void convertImageToPdf_withFixtureImage_producesSinglePagePdf() throws IOException {
        File imageFile = new File(Objects.requireNonNull(
                getClass().getClassLoader().getResource("sampleImage.png")).getFile());

        File pdfFile = PDFUtils.convertImageToPdf(imageFile);
        try {
            assertNotNull(pdfFile);
            assertTrue(pdfFile.exists());
            assertTrue(pdfFile.length() > 0);
            assertTrue(pdfFile.getName().endsWith(".pdf"));

            try (PDDocument document = PDDocument.load(pdfFile)) {
                assertEquals(1, document.getNumberOfPages());
                PDPage page = document.getPage(0);
                assertEquals(PDRectangle.A4.getWidth(), page.getMediaBox().getWidth(), 0.1f);
                assertEquals(PDRectangle.A4.getHeight(), page.getMediaBox().getHeight(), 0.1f);
            }
        } finally {
            Files.deleteIfExists(pdfFile.toPath());
        }
    }

    @Test
    void convertImageToPdf_withJpgImage_producesSinglePagePdf(@TempDir Path tempDir) throws IOException {
        File jpgFile = tempDir.resolve("coverage_test.jpg").toFile();
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "jpg", jpgFile);

        File pdfFile = PDFUtils.convertImageToPdf(jpgFile);
        try {
            assertNotNull(pdfFile);
            assertTrue(pdfFile.exists());
            try (PDDocument document = PDDocument.load(pdfFile)) {
                assertEquals(1, document.getNumberOfPages());
            }
        } finally {
            Files.deleteIfExists(pdfFile.toPath());
        }
    }

    @Test
    void convertImageToPdf_nullInput_throwsException() {
        assertThrows(Exception.class, () -> PDFUtils.convertImageToPdf(null));
    }

    @Test
    void convertImageToPdf_nonExistentFile_throwsIOException(@TempDir Path tempDir) {
        File missing = tempDir.resolve("does_not_exist.png").toFile();
        assertThrows(IOException.class, () -> PDFUtils.convertImageToPdf(missing));
    }

    @Test
    void convertImageToPdf_invalidImageContent_throwsIOExceptionWithMessage(@TempDir Path tempDir) throws IOException {
        File fakeImage = tempDir.resolve("not_really_an_image.png").toFile();
        Files.writeString(fakeImage.toPath(), "this is not image data");

        IOException ex = assertThrows(IOException.class, () -> PDFUtils.convertImageToPdf(fakeImage));
        assertTrue(ex.getMessage().contains("Failed to decode image"));
    }

    // ---------------------------------------------------------------
    // convertToPngIfRequired
    // ---------------------------------------------------------------

    @Test
    void convertToPngIfRequired_jpegFile_convertsToPng(@TempDir Path tempDir) throws IOException {
        File jpegFile = tempDir.resolve("coverage_test.jpeg").toFile();
        BufferedImage image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "jpeg", jpegFile);

        File result = PDFUtils.convertToPngIfRequired(jpegFile);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".png"));
        BufferedImage resultImage = ImageIO.read(result);
        assertNotNull(resultImage);
    }

    @Test
    void convertToPngIfRequired_pngFile_isProcessedAndStaysPng(@TempDir Path tempDir) throws IOException {
        File pngFile = tempDir.resolve("coverage_test.png").toFile();
        BufferedImage image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", pngFile);

        File result = PDFUtils.convertToPngIfRequired(pngFile);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".png"));
    }

    @Test
    void convertToPngIfRequired_unsupportedExtension_returnsOriginalFileUnchanged(@TempDir Path tempDir) throws IOException {
        File gifFile = tempDir.resolve("coverage_test.gif").toFile();
        Files.write(gifFile.toPath(), new byte[]{1, 2, 3, 4});

        File result = PDFUtils.convertToPngIfRequired(gifFile);

        assertSame(gifFile, result);
    }

    @Test
    void convertToPngIfRequired_nullInput_throwsException() {
        assertThrows(Exception.class, () -> PDFUtils.convertToPngIfRequired(null));
    }

    @Test
    void convertToPngIfRequired_invalidImageContent_throwsIOExceptionWithMessage(@TempDir Path tempDir) throws IOException {
        File fakeImage = tempDir.resolve("not_really_an_image.jpg").toFile();
        Files.writeString(fakeImage.toPath(), "this is not image data");

        IOException ex = assertThrows(IOException.class, () -> PDFUtils.convertToPngIfRequired(fakeImage));
        assertTrue(ex.getMessage().contains("Failed to decode image"));
    }

    @Test
    void convertToPngIfRequired_cleanedNameLosesExtension_returnsOriginalFileUnchanged(@TempDir Path tempDir) throws IOException {
        // "weird@@@.jpg" is cleaned by cleanFileName (strips '@' and '.') to "weirdjpg",
        // which no longer ends with .jpg/.jpeg/.png, so the original file must be returned unchanged.
        File weirdFile = tempDir.resolve("weird@@@.jpg").toFile();
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "jpg", weirdFile);

        File result = PDFUtils.convertToPngIfRequired(weirdFile);

        assertSame(weirdFile, result);
        assertTrue(result.getName().endsWith(".jpg"));
    }

    @Test
    void convertToPngIfRequired_resultLandsInSameDirectoryAsOriginal(@TempDir Path tempDir) throws IOException {
        File subDir = tempDir.resolve("sub").toFile();
        assertTrue(subDir.mkdirs());
        File jpegFile = new File(subDir, "nested.jpeg");
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "jpeg", jpegFile);

        File result = PDFUtils.convertToPngIfRequired(jpegFile);

        assertEquals(jpegFile.getParent(), result.getParent());
    }
}

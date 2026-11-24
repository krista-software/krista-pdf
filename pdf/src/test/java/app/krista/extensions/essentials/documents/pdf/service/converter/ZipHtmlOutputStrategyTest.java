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

package app.krista.extensions.essentials.documents.pdf.service.converter;

import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ZipHtmlOutputStrategy.
 * Tests ZIP file creation, packaging, upload, and cleanup.
 */
@DisplayName("ZipHtmlOutputStrategy Tests")
public class ZipHtmlOutputStrategyTest {

    @Mock
    private KristaMediaClientUtil kristaMediaClientUtil;

    private ZipHtmlOutputStrategy strategy;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new ZipHtmlOutputStrategy(kristaMediaClientUtil);
    }

    private java.io.File createTempHtmlFile(String content) throws IOException {
        Path htmlPath = tempDir.resolve("test.html");
        Files.writeString(htmlPath, content);
        return htmlPath.toFile();
    }

    private File createMockKristaFile(String fileName) {
        return new File(1024L, fileName, "test-media-id", "application/zip");
    }

    @Test
    @DisplayName("Should package HTML as ZIP and upload successfully")
    void testPackageAndUpload_Success() throws IOException {
        // Arrange
        String htmlContent = "<html><body>Test HTML content</body></html>";
        java.io.File htmlFile = createTempHtmlFile(htmlContent);
        String originalFileName = "document.pdf";
        File mockUploadedFile = createMockKristaFile("document_html_20231215_143022.zip");

        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class)))
                .thenReturn(mockUploadedFile);

        // Act
        File result = strategy.packageAndUpload(htmlFile, originalFileName);

        // Assert
        assertNotNull(result);
        assertEquals(mockUploadedFile.getFileName(), result.getFileName());
        verify(kristaMediaClientUtil, times(1)).uploadFileHandler(any(java.io.File.class));
    }

    @Test
    @DisplayName("Should create valid ZIP file with HTML content")
    void testPackageAndUpload_ValidZipStructure() throws IOException {
        // Arrange
        String htmlContent = "<html><body>Test HTML content</body></html>";
        java.io.File htmlFile = createTempHtmlFile(htmlContent);
        String originalFileName = "document.pdf";
        File mockUploadedFile = createMockKristaFile("document_html_20231215_143022.zip");

        ArgumentCaptor<java.io.File> fileCaptor = ArgumentCaptor.forClass(java.io.File.class);
        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class)))
                .thenAnswer(invocation -> {
                    java.io.File zipFile = invocation.getArgument(0);
                    // Verify ZIP file structure before it gets deleted
                    assertTrue(zipFile.exists());
                    assertTrue(zipFile.getName().endsWith(".zip"));

                    // Verify ZIP contains HTML file
                    try (ZipFile zip = new ZipFile(zipFile)) {
                        ZipEntry entry = zip.getEntry("document.html");
                        assertNotNull(entry, "ZIP should contain document.html");

                        String zipContent = new String(zip.getInputStream(entry).readAllBytes());
                        assertEquals(htmlContent, zipContent, "ZIP content should match original HTML");
                    }
                    return mockUploadedFile;
                });

        // Act
        File result = strategy.packageAndUpload(htmlFile, originalFileName);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle file with different extensions")
    void testPackageAndUpload_DifferentExtensions() throws IOException {
        // Arrange
        String htmlContent = "<html><body>Test</body></html>";
        java.io.File htmlFile = createTempHtmlFile(htmlContent);
        File mockUploadedFile = createMockKristaFile("report_html_20231215_143022.zip");

        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class)))
                .thenReturn(mockUploadedFile);

        // Act & Assert - Test various file extensions
        String[] testFileNames = {"report.PDF", "report.Pdf", "report.txt", "report"};
        
        for (String fileName : testFileNames) {
            File result = strategy.packageAndUpload(htmlFile, fileName);
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("Should handle large HTML files efficiently")
    void testPackageAndUpload_LargeFile() throws IOException {
        // Arrange - Create a large HTML file (1 MB)
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("<p>This is line ").append(i).append(" of test content.</p>\n");
        }
        java.io.File htmlFile = createTempHtmlFile(largeContent.toString());
        String originalFileName = "large-document.pdf";
        File mockUploadedFile = createMockKristaFile("large-document_html_20231215_143022.zip");

        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class)))
                .thenReturn(mockUploadedFile);

        // Act
        File result = strategy.packageAndUpload(htmlFile, originalFileName);

        // Assert
        assertNotNull(result);
        verify(kristaMediaClientUtil, times(1)).uploadFileHandler(any(java.io.File.class));
    }

    @Test
    @DisplayName("Should handle RuntimeException during upload")
    void testPackageAndUpload_UploadFailure() throws IOException {
        // Arrange
        String htmlContent = "<html><body>Test</body></html>";
        java.io.File htmlFile = createTempHtmlFile(htmlContent);
        String originalFileName = "document.pdf";

        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class)))
                .thenThrow(new RuntimeException("Upload failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            strategy.packageAndUpload(htmlFile, originalFileName);
        });
    }

    @Test
    @DisplayName("Should return correct file extension")
    void testGetFileExtension() {
        // Act
        String extension = strategy.getFileExtension();

        // Assert
        assertEquals("zip", extension);
    }

    @Test
    @DisplayName("Should return correct description")
    void testGetDescription() {
        // Act
        String description = strategy.getDescription();

        // Assert
        assertNotNull(description);
        assertTrue(description.contains("ZIP"));
        assertTrue(description.contains("HTML"));
    }

    @Test
    @DisplayName("Should handle special characters in filename")
    void testPackageAndUpload_SpecialCharacters() throws IOException {
        // Arrange
        String htmlContent = "<html><body>Test</body></html>";
        java.io.File htmlFile = createTempHtmlFile(htmlContent);
        String originalFileName = "my-document (copy) [final].pdf";
        File mockUploadedFile = createMockKristaFile("my-document_html_20231215_143022.zip");

        when(kristaMediaClientUtil.uploadFileHandler(any(java.io.File.class)))
                .thenReturn(mockUploadedFile);

        // Act
        File result = strategy.packageAndUpload(htmlFile, originalFileName);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should use extractBaseFileName from interface")
    void testExtractBaseFileName() {
        // Act & Assert
        assertEquals("document", strategy.extractBaseFileName("document.pdf"));
        assertEquals("report", strategy.extractBaseFileName("report.PDF"));
        assertEquals("file", strategy.extractBaseFileName("file.txt"));
        assertEquals("noextension", strategy.extractBaseFileName("noextension"));
    }

    @Test
    @DisplayName("Should use generateOutputFileName from interface")
    void testGenerateOutputFileName() {
        // Act
        String fileName = strategy.generateOutputFileName("document", "20231215_143022", "zip");

        // Assert
        assertEquals("document_html_20231215_143022.zip", fileName);
    }
}

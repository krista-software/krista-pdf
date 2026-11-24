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

package app.krista.extensions.essentials.documents.pdf.client;

import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for KristaMediaClient.
 * Tests file download, upload, and unique file generation functionality.
 */
@DisplayName("KristaMediaClient Unit Tests")
public class KristaMediaClientTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileHandle fileHandle;

    @Mock
    private File kristaFile;

    private KristaMediaClient kristaMediaClient;
    private List<java.io.File> filesToCleanup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kristaMediaClient = new KristaMediaClient();

        // Use reflection to inject the mock FileRepository
        try {
            java.lang.reflect.Field field = KristaMediaClient.class.getDeclaredField("fileRepository");
            field.setAccessible(true);
            field.set(kristaMediaClient, fileRepository);
        } catch (Exception e) {
            fail("Failed to inject mock FileRepository: " + e.getMessage());
        }

        filesToCleanup = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        // Clean up created files
        for (java.io.File file : filesToCleanup) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
        filesToCleanup.clear();
    }

    // ==================== toJavaFile() Tests ====================

    @Test
    @DisplayName("Should download file with unique filename to prevent collisions")
    void testToJavaFile_GeneratesUniqueFilename() throws IOException {
        // Arrange
        String fileName = "test.pdf";
        byte[] fileContent = "Test PDF content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);

        when(kristaFile.getFileName()).thenReturn(fileName);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(inputStream);

        // Act
        java.io.File result = kristaMediaClient.toJavaFile(kristaFile);
        filesToCleanup.add(result);

        // Assert
        assertNotNull(result, "Result file should not be null");
        assertTrue(result.exists(), "Downloaded file should exist");
        assertTrue(result.getName().startsWith("test_"), "Filename should start with base name");
        assertTrue(result.getName().endsWith(".pdf"), "Filename should end with .pdf extension");
        assertNotEquals(fileName, result.getName(), "Filename should be unique, not the original name");

        // Verify file content
        byte[] actualContent = Files.readAllBytes(result.toPath());
        assertArrayEquals(fileContent, actualContent, "File content should match");

        verify(fileRepository).getFile(kristaFile);
        verify(fileHandle).getContent();
        verify(fileHandle).close();
    }

    @Test
    @DisplayName("Should generate different unique filenames for files with same name")
    void testToJavaFile_MultipleFilesWithSameName_GeneratesDifferentFilenames() throws IOException {
        // Arrange
        String fileName = "document.pdf";
        byte[] content1 = "Content 1".getBytes();
        byte[] content2 = "Content 2".getBytes();

        File kristaFile1 = mock(File.class);
        File kristaFile2 = mock(File.class);

        FileHandle fileHandle1 = mock(FileHandle.class);
        FileHandle fileHandle2 = mock(FileHandle.class);

        when(kristaFile1.getFileName()).thenReturn(fileName);
        when(kristaFile2.getFileName()).thenReturn(fileName);

        when(fileRepository.getFile(kristaFile1)).thenReturn(fileHandle1);
        when(fileRepository.getFile(kristaFile2)).thenReturn(fileHandle2);

        when(fileHandle1.getContent()).thenReturn(new ByteArrayInputStream(content1));
        when(fileHandle2.getContent()).thenReturn(new ByteArrayInputStream(content2));

        // Act
        java.io.File result1 = kristaMediaClient.toJavaFile(kristaFile1);
        java.io.File result2 = kristaMediaClient.toJavaFile(kristaFile2);
        filesToCleanup.add(result1);
        filesToCleanup.add(result2);

        // Assert
        assertNotNull(result1, "First result file should not be null");
        assertNotNull(result2, "Second result file should not be null");
        assertTrue(result1.exists(), "First file should exist");
        assertTrue(result2.exists(), "Second file should exist");
        assertNotEquals(result1.getName(), result2.getName(), "Files should have different unique names");
        assertNotEquals(result1.getAbsolutePath(), result2.getAbsolutePath(), "Files should have different paths");

        // Verify content is different
        byte[] actualContent1 = Files.readAllBytes(result1.toPath());
        byte[] actualContent2 = Files.readAllBytes(result2.toPath());
        assertArrayEquals(content1, actualContent1, "First file content should match");
        assertArrayEquals(content2, actualContent2, "Second file content should match");
        assertFalse(java.util.Arrays.equals(actualContent1, actualContent2), "File contents should be different");
    }

    @Test
    @DisplayName("Should handle files with multiple dots in filename")
    void testToJavaFile_FileWithMultipleDots() throws IOException {
        // Arrange
        String fileName = "my.document.test.pdf";
        byte[] fileContent = "Test content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);

        when(kristaFile.getFileName()).thenReturn(fileName);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(inputStream);

        // Act
        java.io.File result = kristaMediaClient.toJavaFile(kristaFile);
        filesToCleanup.add(result);

        // Assert
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().startsWith("my.document.test_"), "Should preserve dots in base name");
        assertTrue(result.getName().endsWith(".pdf"), "Should have correct extension");
    }

    @Test
    @DisplayName("Should handle files with no extension")
    void testToJavaFile_FileWithoutExtension_ThrowsException() throws IOException {
        // Arrange
        String fileName = "documentwithoutextension";
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());

        when(kristaFile.getFileName()).thenReturn(fileName);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(inputStream);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            kristaMediaClient.toJavaFile(kristaFile);
        }, "Should throw IllegalArgumentException for files without extension");

        verify(fileRepository).getFile(kristaFile);
    }

    @Test
    @DisplayName("Should handle large file downloads")
    void testToJavaFile_LargeFile() throws IOException {
        // Arrange
        String fileName = "large.pdf";
        byte[] largeContent = new byte[1024 * 1024]; // 1 MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        InputStream inputStream = new ByteArrayInputStream(largeContent);

        when(kristaFile.getFileName()).thenReturn(fileName);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(inputStream);

        // Act
        java.io.File result = kristaMediaClient.toJavaFile(kristaFile);
        filesToCleanup.add(result);

        // Assert
        assertNotNull(result);
        assertTrue(result.exists());
        assertEquals(largeContent.length, result.length(), "File size should match");

        // Verify content
        byte[] actualContent = Files.readAllBytes(result.toPath());
        assertArrayEquals(largeContent, actualContent, "Large file content should match");
    }

    @Test
    @DisplayName("Should handle IOException during file download")
    void testToJavaFile_IOExceptionDuringDownload() throws IOException {
        // Arrange
        when(kristaFile.getFileName()).thenReturn("test.pdf");
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenThrow(new IOException("Network error"));

        // Act & Assert
        assertThrows(IOException.class, () -> {
            kristaMediaClient.toJavaFile(kristaFile);
        }, "Should propagate IOException");

        verify(fileRepository).getFile(kristaFile);
    }

    @Test
    @DisplayName("Should create files in /tmp/ directory")
    void testToJavaFile_CreatesFilesInTmpDirectory() throws IOException {
        // Arrange
        String fileName = "test.pdf";
        byte[] fileContent = "Test content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);

        when(kristaFile.getFileName()).thenReturn(fileName);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(inputStream);

        // Act
        java.io.File result = kristaMediaClient.toJavaFile(kristaFile);
        filesToCleanup.add(result);

        // Assert
        assertNotNull(result);
        assertTrue(result.getAbsolutePath().startsWith("/tmp/"), "File should be created in /tmp/ directory");
    }

    @Test
    @DisplayName("Should generate unique filenames for concurrent downloads")
    void testToJavaFile_ConcurrentDownloads_GeneratesUniqueFilenames() throws IOException {
        // Arrange
        String fileName = "concurrent.pdf";
        int numberOfFiles = 10;
        Set<String> generatedFilenames = new HashSet<>();

        for (int i = 0; i < numberOfFiles; i++) {
            File mockFile = mock(File.class);
            FileHandle mockHandle = mock(FileHandle.class);
            byte[] content = ("Content " + i).getBytes();

            when(mockFile.getFileName()).thenReturn(fileName);
            when(fileRepository.getFile(mockFile)).thenReturn(mockHandle);
            when(mockHandle.getContent()).thenReturn(new ByteArrayInputStream(content));

            // Act
            java.io.File result = kristaMediaClient.toJavaFile(mockFile);
            filesToCleanup.add(result);

            // Assert
            assertNotNull(result);
            assertTrue(result.exists());
            generatedFilenames.add(result.getName());
        }

        // Assert all filenames are unique
        assertEquals(numberOfFiles, generatedFilenames.size(),
            "All generated filenames should be unique");
    }

    @Test
    @DisplayName("Should handle different file extensions correctly")
    void testToJavaFile_DifferentExtensions() throws IOException {
        // Arrange
        String[] fileNames = {"test.pdf", "document.docx", "image.png", "data.xlsx"};

        for (String fileName : fileNames) {
            File mockFile = mock(File.class);
            FileHandle mockHandle = mock(FileHandle.class);
            byte[] content = "Test content".getBytes();

            when(mockFile.getFileName()).thenReturn(fileName);
            when(fileRepository.getFile(mockFile)).thenReturn(mockHandle);
            when(mockHandle.getContent()).thenReturn(new ByteArrayInputStream(content));

            // Act
            java.io.File result = kristaMediaClient.toJavaFile(mockFile);
            filesToCleanup.add(result);

            // Assert
            assertNotNull(result);
            assertTrue(result.exists());
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            assertTrue(result.getName().endsWith(extension),
                "File should preserve extension: " + extension);
        }
    }

    // ==================== toKristaFile() Tests ====================

    @Test
    @DisplayName("Should upload PDF file to Krista media server")
    void testToKristaFile_PdfFile() throws IOException {
        // Arrange
        java.io.File testFile = createTempFile("test.pdf", "PDF content".getBytes());
        filesToCleanup.add(testFile);

        File mockKristaFile = mock(File.class);
        when(fileRepository.createNewFileByName(testFile.getName())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockKristaFile);

        // Act
        File result = kristaMediaClient.toKristaFile(testFile);

        // Assert
        assertNotNull(result);
        assertEquals(mockKristaFile, result);
        verify(fileRepository).createNewFileByName(testFile.getName());
        verify(fileHandle).setContent(any(FileInputStream.class));
        verify(fileHandle).getFile();
        verify(fileHandle).close();
    }

    @Test
    @DisplayName("Should compress and upload unsupported file formats")
    void testToKristaFile_UnsupportedFormat_CompressesToZip() throws IOException {
        // Arrange
        java.io.File htmlFile = createTempFile("test.html", "<html>Test</html>".getBytes());
        filesToCleanup.add(htmlFile);

        File mockKristaFile = mock(File.class);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockKristaFile);

        // Act
        File result = kristaMediaClient.toKristaFile(htmlFile);

        // Assert
        assertNotNull(result);
        verify(fileRepository).createNewFileByName(contains(".zip"));
        verify(fileHandle).setContent(any(FileInputStream.class));
    }

    @Test
    @DisplayName("Should handle supported file formats without compression")
    void testToKristaFile_SupportedFormats() throws IOException {
        // Arrange
        String[] supportedFiles = {"test.pdf", "document.docx", "image.png", "data.xlsx"};

        // Set up mock to return fileHandle for any filename
        File mockKristaFile = mock(File.class);
        when(fileRepository.createNewFileByName(anyString())).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(mockKristaFile);

        for (String fileName : supportedFiles) {
            java.io.File testFile = createTempFile(fileName, "Test content".getBytes());
            filesToCleanup.add(testFile);

            // Act
            File result = kristaMediaClient.toKristaFile(testFile);

            // Assert
            assertNotNull(result);
        }

        // Verify that createNewFileByName was called for each file
        verify(fileRepository, times(supportedFiles.length)).createNewFileByName(anyString());
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a temporary file for testing.
     */
    private java.io.File createTempFile(String fileName, byte[] content) throws IOException {
        Path tempPath = Files.createTempFile("test_", "_" + fileName);
        java.io.File tempFile = tempPath.toFile();
        Files.write(tempPath, content);
        return tempFile;
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle empty file download")
    void testToJavaFile_EmptyFile() throws IOException {
        // Arrange
        String fileName = "empty.pdf";
        byte[] emptyContent = new byte[0];
        InputStream inputStream = new ByteArrayInputStream(emptyContent);

        when(kristaFile.getFileName()).thenReturn(fileName);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(inputStream);

        // Act
        java.io.File result = kristaMediaClient.toJavaFile(kristaFile);
        filesToCleanup.add(result);

        // Assert
        assertNotNull(result);
        assertTrue(result.exists());
        assertEquals(0, result.length(), "Empty file should have zero length");
    }

    @Test
    @DisplayName("Should handle filename with special characters")
    void testToJavaFile_SpecialCharactersInFilename() throws IOException {
        // Arrange
        String fileName = "test-file_v1.0.pdf";
        byte[] fileContent = "Test content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);

        when(kristaFile.getFileName()).thenReturn(fileName);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(inputStream);

        // Act
        java.io.File result = kristaMediaClient.toJavaFile(kristaFile);
        filesToCleanup.add(result);

        // Assert
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().contains("test-file_v1.0_"), "Should preserve special characters in base name");
    }

    @Test
    @DisplayName("Should verify UUID is included in generated filename")
    void testToJavaFile_ContainsUUID() throws IOException {
        // Arrange
        String fileName = "test.pdf";
        byte[] fileContent = "Test content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);

        when(kristaFile.getFileName()).thenReturn(fileName);
        when(fileRepository.getFile(kristaFile)).thenReturn(fileHandle);
        when(fileHandle.getContent()).thenReturn(inputStream);

        // Act
        java.io.File result = kristaMediaClient.toJavaFile(kristaFile);
        filesToCleanup.add(result);

        // Assert
        assertNotNull(result);
        String generatedName = result.getName();

        // UUID format: test_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.pdf
        assertTrue(generatedName.matches("test_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf"),
            "Filename should contain UUID pattern");
    }
}


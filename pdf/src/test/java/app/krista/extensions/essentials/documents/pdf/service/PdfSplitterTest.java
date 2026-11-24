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

package app.krista.extensions.essentials.documents.pdf.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PdfSplitter service class.
 * Tests PDF splitting functionality including validation, edge cases, and error handling.
 */
@DisplayName("PdfSplitter Unit Tests")
public class PdfSplitterTest {

    private PdfSplitter pdfSplitter;
    private List<File> filesToCleanup;

    @BeforeEach
    void setUp() {
        pdfSplitter = new PdfSplitter();
        filesToCleanup = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        // Clean up test files
        for (File file : filesToCleanup) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    // ==================== Successful Split Tests ====================

    @Test
    @DisplayName("Should split 10-page PDF at page 6 into two files")
    void testSplitAtPageNumber_ValidSplit() throws IOException {
        // Arrange
        File testPdf = createTestPdf("test_10_pages.pdf", 10);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 6);
        filesToCleanup.addAll(result);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size(), "Should produce exactly 2 files");

        // Verify first part (pages 1-5)
        File firstPart = result.get(0);
        assertTrue(firstPart.exists(), "First part should exist");
        assertTrue(firstPart.getName().contains("pages_1_to_5"), "First part should contain pages 1-5");
        try (PDDocument doc = PDDocument.load(firstPart)) {
            assertEquals(5, doc.getNumberOfPages(), "First part should have 5 pages");
        }

        // Verify second part (pages 6-10)
        File secondPart = result.get(1);
        assertTrue(secondPart.exists(), "Second part should exist");
        assertTrue(secondPart.getName().contains("pages_6_to_10"), "Second part should contain pages 6-10");
        try (PDDocument doc = PDDocument.load(secondPart)) {
            assertEquals(5, doc.getNumberOfPages(), "Second part should have 5 pages");
        }
    }

    @Test
    @DisplayName("Should split PDF at page 2 (minimum valid split)")
    void testSplitAtPageNumber_MinimumValidSplit() throws IOException {
        // Arrange
        File testPdf = createTestPdf("test_5_pages.pdf", 5);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 2);
        filesToCleanup.addAll(result);

        // Assert
        assertEquals(2, result.size());
        
        try (PDDocument doc = PDDocument.load(result.get(0))) {
            assertEquals(1, doc.getNumberOfPages(), "First part should have 1 page");
        }
        
        try (PDDocument doc = PDDocument.load(result.get(1))) {
            assertEquals(4, doc.getNumberOfPages(), "Second part should have 4 pages");
        }
    }

    @Test
    @DisplayName("Should split PDF at last page")
    void testSplitAtPageNumber_SplitAtLastPage() throws IOException {
        // Arrange
        File testPdf = createTestPdf("test_8_pages.pdf", 8);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 8);
        filesToCleanup.addAll(result);

        // Assert
        assertEquals(2, result.size());
        
        try (PDDocument doc = PDDocument.load(result.get(0))) {
            assertEquals(7, doc.getNumberOfPages(), "First part should have 7 pages");
        }
        
        try (PDDocument doc = PDDocument.load(result.get(1))) {
            assertEquals(1, doc.getNumberOfPages(), "Second part should have 1 page");
        }
    }

    @Test
    @DisplayName("Should generate unique filenames with timestamp")
    void testSplitAtPageNumber_UniqueFilenames() throws IOException, InterruptedException {
        // Arrange
        File testPdf = createTestPdf("document.pdf", 6);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result1 = pdfSplitter.splitAtPageNumber(testPdf, 3);
        filesToCleanup.addAll(result1);
        
        // Wait a bit to ensure different timestamp
        Thread.sleep(10);
        
        List<File> result2 = pdfSplitter.splitAtPageNumber(testPdf, 4);
        filesToCleanup.addAll(result2);

        // Assert
        assertNotEquals(result1.get(0).getName(), result2.get(0).getName(), 
            "Files from different splits should have different names");
    }

    @Test
    @DisplayName("Should preserve base filename in output")
    void testSplitAtPageNumber_PreservesBaseName() throws IOException {
        // Arrange
        File testPdf = createTestPdf("my_important_document.pdf", 4);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 3);
        filesToCleanup.addAll(result);

        // Assert
        assertTrue(result.get(0).getName().startsWith("my_important_document_"), 
            "Output filename should preserve base name");
        assertTrue(result.get(1).getName().startsWith("my_important_document_"), 
            "Output filename should preserve base name");
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("Should throw exception when split page is less than 2")
    void testSplitAtPageNumber_InvalidSplitPageTooLow() throws IOException {
        // Arrange
        File testPdf = createTestPdf("test.pdf", 10);
        filesToCleanup.add(testPdf);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> pdfSplitter.splitAtPageNumber(testPdf, 1));
        
        assertTrue(exception.getMessage().contains("must be at least 2"), 
            "Error message should indicate minimum requirement");
    }

    @Test
    @DisplayName("Should throw exception when split page is 0")
    void testSplitAtPageNumber_SplitPageZero() throws IOException {
        // Arrange
        File testPdf = createTestPdf("test.pdf", 10);
        filesToCleanup.add(testPdf);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> pdfSplitter.splitAtPageNumber(testPdf, 0));
        
        assertTrue(exception.getMessage().contains("must be at least 2"));
    }

    @Test
    @DisplayName("Should throw exception when split page is negative")
    void testSplitAtPageNumber_NegativeSplitPage() throws IOException {
        // Arrange
        File testPdf = createTestPdf("test.pdf", 10);
        filesToCleanup.add(testPdf);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> pdfSplitter.splitAtPageNumber(testPdf, -5));
        
        assertTrue(exception.getMessage().contains("must be at least 2"));
    }

    @Test
    @DisplayName("Should throw exception when split page exceeds total pages")
    void testSplitAtPageNumber_SplitPageExceedsTotalPages() throws IOException {
        // Arrange
        File testPdf = createTestPdf("test.pdf", 5);
        filesToCleanup.add(testPdf);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> pdfSplitter.splitAtPageNumber(testPdf, 10));
        
        assertTrue(exception.getMessage().contains("cannot be greater than total pages"), 
            "Error message should indicate page count exceeded");
        assertTrue(exception.getMessage().contains("10"));
        assertTrue(exception.getMessage().contains("5"));
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle PDF with special characters in filename")
    void testSplitAtPageNumber_SpecialCharactersInFilename() throws IOException {
        // Arrange
        File testPdf = createTestPdf("test-file_v1.0.pdf", 4);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 3);
        filesToCleanup.addAll(result);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.get(0).getName().contains("test-file_v1.0_"), 
            "Should preserve special characters in base name");
    }

    @Test
    @DisplayName("Should handle PDF with multiple dots in filename")
    void testSplitAtPageNumber_MultipleDotsInFilename() throws IOException {
        // Arrange
        File testPdf = createTestPdf("my.document.v2.0.pdf", 3);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 2);
        filesToCleanup.addAll(result);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.get(0).getName().contains("my.document.v2.0_"), 
            "Should correctly extract base name with multiple dots");
    }

    @Test
    @DisplayName("Should handle 2-page PDF split at page 2")
    void testSplitAtPageNumber_TwoPagePdf() throws IOException {
        // Arrange
        File testPdf = createTestPdf("two_pages.pdf", 2);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 2);
        filesToCleanup.addAll(result);

        // Assert
        assertEquals(2, result.size());
        
        try (PDDocument doc = PDDocument.load(result.get(0))) {
            assertEquals(1, doc.getNumberOfPages());
        }
        
        try (PDDocument doc = PDDocument.load(result.get(1))) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    // ==================== Output Validation Tests (reload-and-remove) ====================

    @Test
    @DisplayName("Split output files should be valid, loadable PDFs")
    void testSplitAtPageNumber_OutputsAreValidPdfs() throws IOException {
        // Arrange
        File testPdf = createTestPdf("valid_output_test.pdf", 6);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 4);
        filesToCleanup.addAll(result);

        // Assert - each output should be independently loadable and valid
        for (File file : result) {
            assertDoesNotThrow(() -> {
                try (PDDocument doc = PDDocument.load(file)) {
                    assertTrue(doc.getNumberOfPages() > 0,
                            "Loaded PDF should have at least 1 page");
                }
            }, "Split file should be a valid, loadable PDF: " + file.getName());
        }
    }

    @Test
    @DisplayName("Total pages across splits should equal original page count")
    void testSplitAtPageNumber_TotalPagesPreserved() throws IOException {
        // Arrange
        int originalPageCount = 8;
        File testPdf = createTestPdf("total_pages_test.pdf", originalPageCount);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 5);
        filesToCleanup.addAll(result);

        // Assert
        int totalSplitPages = 0;
        for (File file : result) {
            try (PDDocument doc = PDDocument.load(file)) {
                totalSplitPages += doc.getNumberOfPages();
            }
        }
        assertEquals(originalPageCount, totalSplitPages,
                "Total pages across all splits should equal original page count");
    }

    @Test
    @DisplayName("Split files should not exceed original file size")
    void testSplitAtPageNumber_OutputSizeNotExceedOriginal() throws IOException {
        // Arrange
        File testPdf = createTestPdf("size_test.pdf", 10);
        filesToCleanup.add(testPdf);
        long originalSize = testPdf.length();

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 6);
        filesToCleanup.addAll(result);

        // Assert
        for (File file : result) {
            assertTrue(file.length() <= originalSize,
                    String.format("Split %s (%d bytes) should not exceed original (%d bytes)",
                            file.getName(), file.length(), originalSize));
        }
    }

    @Test
    @DisplayName("Split output should be re-loadable after save (round-trip)")
    void testSplitAtPageNumber_RoundTrip() throws IOException {
        // Arrange
        File testPdf = createTestPdf("round_trip_test.pdf", 6);
        filesToCleanup.add(testPdf);

        // Act
        List<File> result = pdfSplitter.splitAtPageNumber(testPdf, 3);
        filesToCleanup.addAll(result);

        // Assert - save and reload each split to verify integrity
        for (File split : result) {
            File resaved = new File("resaved_" + split.getName());
            filesToCleanup.add(resaved);

            try (PDDocument loaded = PDDocument.load(split)) {
                loaded.save(resaved);
            }

            try (PDDocument reloaded = PDDocument.load(resaved)) {
                assertTrue(reloaded.getNumberOfPages() > 0,
                        "Re-saved PDF should load correctly with pages intact");
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a test PDF file with the specified number of pages.
     */
    private File createTestPdf(String filename, int numberOfPages) throws IOException {
        File pdfFile = new File(filename);
        
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < numberOfPages; i++) {
                document.addPage(new PDPage());
            }
            document.save(pdfFile);
        }
        
        return pdfFile;
    }
}


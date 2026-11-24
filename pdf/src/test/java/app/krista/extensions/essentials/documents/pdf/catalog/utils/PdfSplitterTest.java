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

package app.krista.extensions.essentials.documents.pdf.catalog.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PdfSplitter (catalog) Unit Tests")
class PdfSplitterTest {

    private File testPdfFile;
    private final List<File> filesToCleanup = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        // Create a test PDF file with multiple pages
        testPdfFile = createTestPdfWithSections();
        filesToCleanup.add(testPdfFile);
    }

    @AfterEach
    void tearDown() {
        // Clean up generated split files
        File currentDir = new File(".");
        File[] pdfParts = currentDir.listFiles((dir, name) -> name.startsWith("pdf_part_") && name.endsWith(".pdf"));
        if (pdfParts != null) {
            for (File part : pdfParts) {
                part.delete();
            }
        }
        for (File f : filesToCleanup) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }

    @Test
    @DisplayName("Should split PDF by criteria with Total units pattern")
    void testSplitPdfByCriteria_WithTotalUnitsPattern() throws IOException {
        // Act
        List<File> result = PdfSplitter.splitPdfByCriteria(testPdfFile);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() > 0);

        // Verify all generated files exist
        for (File file : result) {
            assertTrue(file.exists());
            assertTrue(file.length() > 0);
        }
    }

    @Test
    @DisplayName("Should handle PDF with single page")
    void testSplitPdfByCriteria_SinglePage() throws IOException {
        // Arrange
        File singlePagePdf = createSinglePagePdf();

        try {
            // Act
            List<File> result = PdfSplitter.splitPdfByCriteria(singlePagePdf);

            // Assert
            assertNotNull(result);
            assertFalse(result.isEmpty());
        } finally {
            if (singlePagePdf.exists()) {
                singlePagePdf.delete();
            }
        }
    }

    @Test
    @DisplayName("Should throw IOException for invalid file")
    void testSplitPdfByCriteria_InvalidFile() {
        // Arrange
        File invalidFile = new File("nonexistent.pdf");

        // Act & Assert
        assertThrows(IOException.class, () -> PdfSplitter.splitPdfByCriteria(invalidFile));
    }

    @Test
    @DisplayName("Should handle PDF with End of report marker")
    void testSplitPdfByCriteria_WithEndOfReportMarker() throws IOException {
        // Arrange
        File pdfWithEndMarker = createPdfWithEndOfReportMarker();

        try {
            // Act
            List<File> result = PdfSplitter.splitPdfByCriteria(pdfWithEndMarker);

            // Assert
            assertNotNull(result);
            assertFalse(result.isEmpty());
        } finally {
            if (pdfWithEndMarker.exists()) {
                pdfWithEndMarker.delete();
            }
        }
    }

    @Test
    @DisplayName("Should produce correct number of sections for multi-section PDF")
    void testSplitPdfByCriteria_CorrectSectionCount() throws IOException {
        // The default test PDF has 3 pages: page1 (content), page2 (Total units = 100), page3 (content)
        // Expected: section [0,1] (pages 1-2 ending at "Total units"), section [2,2] (page 3, open section)
        List<File> result = PdfSplitter.splitPdfByCriteria(testPdfFile);

        assertNotNull(result);
        assertEquals(2, result.size(), "Should produce exactly 2 sections");
    }

    @Test
    @DisplayName("Should produce valid PDFs with correct page counts per section")
    void testSplitPdfByCriteria_CorrectPageCountsPerSection() throws IOException {
        List<File> result = PdfSplitter.splitPdfByCriteria(testPdfFile);

        // First section: pages 0-1 (2 pages)
        try (PDDocument doc = PDDocument.load(result.get(0))) {
            assertEquals(2, doc.getNumberOfPages(),
                    "First section should contain 2 pages (up to 'Total units' boundary)");
        }

        // Second section: page 2 (1 page, the trailing open section)
        try (PDDocument doc = PDDocument.load(result.get(1))) {
            assertEquals(1, doc.getNumberOfPages(),
                    "Second section should contain 1 page (trailing content)");
        }
    }

    @Test
    @DisplayName("Should account for all original pages across split files")
    void testSplitPdfByCriteria_AllPagesAccountedFor() throws IOException {
        List<File> result = PdfSplitter.splitPdfByCriteria(testPdfFile);

        int totalSplitPages = 0;
        for (File file : result) {
            try (PDDocument doc = PDDocument.load(file)) {
                totalSplitPages += doc.getNumberOfPages();
            }
        }

        try (PDDocument original = PDDocument.load(testPdfFile)) {
            assertEquals(original.getNumberOfPages(), totalSplitPages,
                    "Sum of pages across all splits should equal original page count");
        }
    }

    @Test
    @DisplayName("Should handle PDF with multiple section boundaries")
    void testSplitPdfByCriteria_MultipleSectionBoundaries() throws IOException {
        File multiSectionPdf = createPdfWithMultipleSections();
        filesToCleanup.add(multiSectionPdf);

        List<File> result = PdfSplitter.splitPdfByCriteria(multiSectionPdf);

        assertNotNull(result);
        assertEquals(3, result.size(), "Should produce 3 sections for 3 section boundaries");

        // Verify each split file is a valid, loadable PDF
        for (File file : result) {
            assertTrue(file.exists(), "Split file should exist");
            assertTrue(file.length() > 0, "Split file should not be empty");
            try (PDDocument doc = PDDocument.load(file)) {
                assertTrue(doc.getNumberOfPages() > 0, "Split file should have at least 1 page");
            }
        }
    }

    @Test
    @DisplayName("Should handle PDF with no section boundaries as single section")
    void testSplitPdfByCriteria_NoSectionBoundaries() throws IOException {
        File noSectionPdf = createPdfWithNoSectionBoundaries();
        filesToCleanup.add(noSectionPdf);

        List<File> result = PdfSplitter.splitPdfByCriteria(noSectionPdf);

        assertNotNull(result);
        assertEquals(1, result.size(), "Should produce 1 section when no boundaries found");

        try (PDDocument doc = PDDocument.load(result.get(0))) {
            assertEquals(3, doc.getNumberOfPages(),
                    "Single section should contain all original pages");
        }
    }

    @Test
    @DisplayName("Split output files should not exceed original file size")
    void testSplitPdfByCriteria_OutputSizeNotExceedOriginal() throws IOException {
        long originalSize = testPdfFile.length();
        List<File> result = PdfSplitter.splitPdfByCriteria(testPdfFile);

        for (File file : result) {
            assertTrue(file.length() <= originalSize,
                    String.format("Split file %s (%d bytes) should not exceed original (%d bytes)",
                            file.getName(), file.length(), originalSize));
        }
    }

    // Helper methods to create test PDFs

    private File createPdfWithMultipleSections() throws IOException {
        File tempFile = File.createTempFile("multi_section_", ".pdf");
        try (PDDocument document = new PDDocument()) {
            // Section 1: page 1 ends with Total units
            addPageWithText(document, "Section 1 data\nTotal units = 50");
            // Section 2: pages 2-3, page 3 ends with Total units
            addPageWithText(document, "Section 2 page 1\nMore data here");
            addPageWithText(document, "Section 2 page 2\nTotal units = 200");
            // Section 3: pages 4-5, page 5 ends with End of report
            addPageWithText(document, "Section 3 page 1\nSome content");
            addPageWithText(document, "Final section data\nEnd of report");
            document.save(tempFile);
        }
        return tempFile;
    }

    private File createPdfWithNoSectionBoundaries() throws IOException {
        File tempFile = File.createTempFile("no_section_", ".pdf");
        try (PDDocument document = new PDDocument()) {
            addPageWithText(document, "Page 1 regular content");
            addPageWithText(document, "Page 2 regular content");
            addPageWithText(document, "Page 3 regular content");
            document.save(tempFile);
        }
        return tempFile;
    }

    private File createTestPdfWithSections() throws IOException {
        File tempFile = File.createTempFile("test_pdf_", ".pdf");
        try (PDDocument document = new PDDocument()) {
            // Page 1: Regular content
            addPageWithText(document, "Page 1 content\nSome data here");

            // Page 2: Ends with Total units pattern
            addPageWithText(document, "Page 2 content\nTotal units = 100");

            // Page 3: Regular content
            addPageWithText(document, "Page 3 content\nMore data");

            document.save(tempFile);
        }
        return tempFile;
    }

    private File createSinglePagePdf() throws IOException {
        File tempFile = File.createTempFile("single_page_", ".pdf");
        try (PDDocument document = new PDDocument()) {
            addPageWithText(document, "Single page content\nTotal units = 50");
            document.save(tempFile);
        }
        return tempFile;
    }

    private File createPdfWithEndOfReportMarker() throws IOException {
        File tempFile = File.createTempFile("end_marker_", ".pdf");
        try (PDDocument document = new PDDocument()) {
            addPageWithText(document, "Report content\nEnd of report");
            document.save(tempFile);
        }
        return tempFile;
    }

    private void addPageWithText(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(50, 700);

            String[] lines = text.split("\n");
            for (String line : lines) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15);
            }

            contentStream.endText();
        }
    }
}


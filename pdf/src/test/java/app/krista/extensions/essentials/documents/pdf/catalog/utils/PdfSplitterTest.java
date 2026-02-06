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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfSplitterTest {

    private File testPdfFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test PDF file with multiple pages
        testPdfFile = createTestPdfWithSections();
    }

    @AfterEach
    void tearDown() {
        // Clean up test files
        if (testPdfFile != null && testPdfFile.exists()) {
            testPdfFile.delete();
        }
        // Clean up generated split files
        File currentDir = new File(".");
        File[] pdfParts = currentDir.listFiles((dir, name) -> name.startsWith("pdf_part_") && name.endsWith(".pdf"));
        if (pdfParts != null) {
            for (File part : pdfParts) {
                part.delete();
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

    // Helper methods to create test PDFs

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


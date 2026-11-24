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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PDFManager.splitPDFAtPageNumber() method.
 */
class SplitPdfAtPageNumberTest {

    private PDFManager pdfManager;
    private PdfSplitter pdfSplitter;
    private File testPdfFile;

    @BeforeEach
    void setUp() throws IOException {
        pdfSplitter = new PdfSplitter();
        pdfManager = new PDFManager(pdfSplitter);

        // Create a test PDF with 10 pages
        testPdfFile = File.createTempFile("test_split_", ".pdf");
        PDDocument document = new PDDocument();
        for (int i = 0; i < 10; i++) {
            document.addPage(new PDPage());
        }
        document.save(testPdfFile);
        document.close();
    }

    @AfterEach
    void tearDown() {
        if (testPdfFile != null && testPdfFile.exists()) {
            testPdfFile.delete();
        }
    }

    @Test
    @DisplayName("Should split 10-page PDF at page 6 into two files (pages 1-5 and 6-10)")
    void testSplitPdfAtPageNumber_Success() throws IOException {
        // Act
        List<File> result = pdfManager.splitPDFAtPageNumber(testPdfFile, 6);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return exactly 2 files");

        // Verify first file has pages 1-5
        PDDocument firstDoc = PDDocument.load(result.get(0));
        assertEquals(5, firstDoc.getNumberOfPages(), "First file should have 5 pages");
        firstDoc.close();

        // Verify second file has pages 6-10
        PDDocument secondDoc = PDDocument.load(result.get(1));
        assertEquals(5, secondDoc.getNumberOfPages(), "Second file should have 5 pages");
        secondDoc.close();

        // Clean up
        result.forEach(File::delete);
    }

    @Test
    @DisplayName("Should split at page 2 (1 page in first file, 9 pages in second)")
    void testSplitPdfAtPageNumber_SplitAtPage2() throws IOException {
        // Act
        List<File> result = pdfManager.splitPDFAtPageNumber(testPdfFile, 2);

        // Assert
        assertEquals(2, result.size(), "Should return exactly 2 files");

        PDDocument firstDoc = PDDocument.load(result.get(0));
        assertEquals(1, firstDoc.getNumberOfPages(), "First file should have 1 page");
        firstDoc.close();

        PDDocument secondDoc = PDDocument.load(result.get(1));
        assertEquals(9, secondDoc.getNumberOfPages(), "Second file should have 9 pages");
        secondDoc.close();

        // Clean up
        result.forEach(File::delete);
    }

    @Test
    @DisplayName("Should split at last page (9 pages in first file, 1 page in second)")
    void testSplitPdfAtPageNumber_SplitAtLastPage() throws IOException {
        List<File> result = pdfManager.splitPDFAtPageNumber(testPdfFile, 10);
        assertEquals(2, result.size(), "Should return exactly 2 files");
        PDDocument firstDoc = PDDocument.load(result.get(0));
        assertEquals(9, firstDoc.getNumberOfPages(), "First file should have 9 pages");
        firstDoc.close();
        PDDocument secondDoc = PDDocument.load(result.get(1));
        assertEquals(1, secondDoc.getNumberOfPages(), "Second file should have 1 page");
        secondDoc.close();
        result.forEach(File::delete);
    }

    @Test
    @DisplayName("Should throw exception when split page is less than 2")
    void testSplitPdfAtPageNumber_InvalidPageTooLow() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pdfManager.splitPDFAtPageNumber(testPdfFile, 1),
                "Should throw IllegalArgumentException for page number < 2"
        );

        assertTrue(exception.getMessage().contains("must be at least 2"),
                "Error message should mention minimum page number");
    }

    @Test
    @DisplayName("Should throw exception when split page exceeds total pages")
    void testSplitPdfAtPageNumber_InvalidPageTooHigh() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pdfManager.splitPDFAtPageNumber(testPdfFile, 11),
                "Should throw IllegalArgumentException for page number > total pages"
        );

        assertTrue(exception.getMessage().contains("cannot be greater than total pages"),
                "Error message should mention page number exceeds total");
    }

    @Test
    @DisplayName("Should throw exception when PDF file is null")
    void testSplitPdfAtPageNumber_NullFile() {
        // Act & Assert
        assertThrows(
                NullPointerException.class,
                () -> pdfManager.splitPDFAtPageNumber(null, 5),
                "Should throw NullPointerException for null file"
        );
    }
}


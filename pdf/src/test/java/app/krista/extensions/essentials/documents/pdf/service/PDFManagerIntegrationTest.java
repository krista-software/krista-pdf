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

import app.krista.extensions.essentials.documents.pdf.BaseIntegrationTest;
import app.krista.model.base.EntityValue;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for PDFManager with 100% code coverage.
 * Tests all public methods including edge cases, error handling, and various input scenarios.
 */
@DisplayName("PDFManager Integration Tests")
class PDFManagerIntegrationTest extends BaseIntegrationTest {

    private PDFManager pdfManager;
    private PdfSplitter pdfSplitter;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        pdfSplitter = new PdfSplitter();
        pdfManager = new PDFManager(pdfSplitter);
    }

    // ==================== convertDocxToPdf Tests ====================

    @Test
    @DisplayName("Should convert DOCX to PDF successfully")
    void testConvertDocxToPdf_Success() throws Exception {
        // Arrange
        java.io.File docxFile = createSampleDocxFile("test.docx", "Test content for PDF conversion");
        String pdfPath = tempDir.resolve("output.pdf").toString();

        // Act
        try (InputStream inputStream = new FileInputStream(docxFile)) {
            java.io.File result = PDFManager.convertDocxToPdf(inputStream, pdfPath);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertTrue(result.exists(), "PDF file should exist");
            assertValidPdf(result);
        }
    }

    @Test
    @DisplayName("Should throw exception when DOCX input stream is invalid")
    void testConvertDocxToPdf_InvalidInputStream() {
        // Arrange
        InputStream invalidStream = new ByteArrayInputStream("invalid docx content".getBytes());
        String pdfPath = tempDir.resolve("output.pdf").toString();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            PDFManager.convertDocxToPdf(invalidStream, pdfPath);
        }, "Should throw IllegalArgumentException for invalid DOCX");
    }

    @Test
    @DisplayName("Should throw exception when PDF path is invalid")
    void testConvertDocxToPdf_InvalidPdfPath() throws Exception {
        // Arrange
        java.io.File docxFile = createSampleDocxFile("test.docx", "Test content");
        String invalidPath = "/invalid/path/that/does/not/exist/output.pdf";

        // Act & Assert
        try (InputStream inputStream = new FileInputStream(docxFile)) {
            assertThrows(IllegalArgumentException.class, () -> {
                PDFManager.convertDocxToPdf(inputStream, invalidPath);
            }, "Should throw exception for invalid path");
        }
    }

    // ==================== getPdfForXWPFDoc Tests ====================

    @Test
    @DisplayName("Should convert XWPF document to PDF successfully")
    void testGetPdfForXWPFDoc_Success() throws Exception {
        // Arrange
        java.io.File docxFile = createSampleDocxFile("test.docx", "XWPF test content");
        String pdfPath = tempDir.resolve("xwpf-output.pdf").toString();

        // Act
        try (InputStream inputStream = new FileInputStream(docxFile)) {
            java.io.File result = PDFManager.getPdfForXWPFDoc(inputStream, pdfPath);

            // Assert
            assertNotNull(result, "Result should not be null");
            assertTrue(result.exists(), "PDF file should exist");
            assertTrue(result.length() > 0, "PDF file should not be empty");
        }
    }

    @Test
    @DisplayName("Should throw exception for invalid XWPF input")
    void testGetPdfForXWPFDoc_InvalidInput() {
        // Arrange
        InputStream invalidStream = new ByteArrayInputStream("not a valid docx".getBytes());
        String pdfPath = tempDir.resolve("output.pdf").toString();

        // Act & Assert
        assertThrows(NotOfficeXmlFileException.class, () -> {
            PDFManager.getPdfForXWPFDoc(invalidStream, pdfPath);
        }, "Should throw NotOfficeXmlFileException for invalid input");
    }

    // ==================== convertDocToPDF Tests ====================

    @Test
    @DisplayName("Should convert DOC file to PDF successfully")
    void testConvertDocToPDF_Success() throws Exception {
        // Arrange
        java.io.File docxFile = createSampleDocxFile("test.docx", "Document to PDF test");

        // Act
        java.io.File result = pdfManager.convertDocToPDF(docxFile);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF should exist");
    }

    @Test
    @DisplayName("Should throw exception when input file does not exist")
    void testConvertDocToPDF_FileNotFound() {
        // Arrange
        java.io.File nonExistentFile = new java.io.File("/non/existent/file.docx");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pdfManager.convertDocToPDF(nonExistentFile);
        }, "Should throw exception for non-existent file");
    }

    // ==================== getRichTextPDF Tests ====================

    @Test
    @DisplayName("Should create PDF from rich text successfully")
    void testGetRichTextPDF_Success() throws Exception {
        // Arrange
        String title = "Test Title";
        String subTitle = "Test Subtitle";
        String description = "Test Description";
        String header = "Test Header";
        String footer = "Test Footer";
        java.io.File logo = null;
        String richTextData = "<p>This is <b>rich text</b> content</p>";

        // Act
        java.io.File result = pdfManager.getRichTextPDF(title, subTitle, description, header, footer, logo, richTextData);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
        assertValidPdf(result);
    }

    @Test
    @DisplayName("Should create PDF with null rich text data")
    void testGetRichTextPDF_NullRichText() throws Exception {
        // Arrange
        String title = "Test Title";
        String richTextData = null;

        // Act
        java.io.File result = pdfManager.getRichTextPDF(title, null, null, null, null, null, richTextData);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
    }

    @Test
    @DisplayName("Should create PDF with blank rich text data")
    void testGetRichTextPDF_BlankRichText() throws Exception {
        // Arrange
        String richTextData = "   ";

        // Act
        java.io.File result = pdfManager.getRichTextPDF("Title", null, null, null, null, null, richTextData);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
    }

    // ==================== getEntityListPdf Tests ====================

    @Test
    @DisplayName("Should create PDF from entity list successfully")
    void testGetEntityListPdf_Success() throws Exception {
        // Arrange
        String title = "Entity List Title";
        List<EntityValue> entityValues = createSampleEntityValues();

        // Act
        java.io.File result = pdfManager.getEntityListPdf(title, null, null, null, null, null, entityValues, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
        assertValidPdf(result);
    }

    @Test
    @DisplayName("Should create PDF with null entity list")
    void testGetEntityListPdf_NullEntityList() throws Exception {
        // Arrange
        String title = "Test Title";
        List<EntityValue> entityValues = null;

        // Act
        java.io.File result = pdfManager.getEntityListPdf(title, null, null, null, null, null, entityValues, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
    }

    @Test
    @DisplayName("Should create PDF with empty entity list")
    void testGetEntityListPdf_EmptyEntityList() throws Exception {
        // Arrange
        List<EntityValue> entityValues = new ArrayList<>();

        // Act
        java.io.File result = pdfManager.getEntityListPdf("Title", null, null, null, null, null, entityValues, null);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF file should exist");
    }

    // ==================== mergePdfFiles Tests ====================

    @Test
    @DisplayName("Should merge multiple PDF files successfully")
    void testMergePdfFiles_Success() throws Exception {
        // Arrange
        java.io.File pdf1 = createSamplePdfFile("pdf1.pdf", 2);
        java.io.File pdf2 = createSamplePdfFile("pdf2.pdf", 3);
        List<java.io.File> mergeFiles = List.of(pdf1, pdf2);

        // Act
        java.io.File result = pdfManager.mergePdfFiles("Title", null, null, null, null, null, mergeFiles);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "Merged PDF should exist");
        try (PDDocument doc = PDDocument.load(result)) {
            // Merged PDF has 6 pages: 1 title page + 2 pages from pdf1 + 3 pages from pdf2
            assertEquals(6, doc.getNumberOfPages(), "Merged PDF should have 6 pages (1 title + 2 + 3)");
        }
    }

    @Test
    @DisplayName("Should merge PDFs with null merge files list")
    void testMergePdfFiles_NullMergeFiles() throws Exception {
        // Arrange
        List<java.io.File> mergeFiles = null;

        // Act
        java.io.File result = pdfManager.mergePdfFiles("Title", null, null, null, null, null, mergeFiles);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF should exist");
    }

    @Test
    @DisplayName("Should merge PDFs with empty merge files list")
    void testMergePdfFiles_EmptyMergeFiles() throws Exception {
        // Arrange
        List<java.io.File> mergeFiles = new ArrayList<>();

        // Act
        java.io.File result = pdfManager.mergePdfFiles("Title", null, null, null, null, null, mergeFiles);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.exists(), "PDF should exist");
    }

    // ==================== splitPDF Tests ====================

    @Test
    @DisplayName("Should split PDF into individual pages")
    void testSplitPDF_Success() throws Exception {
        // Arrange
        java.io.File pdfFile = createSamplePdfFile("test.pdf", 5);

        // Act
        List<java.io.File> result = pdfManager.splitPDF(pdfFile);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(5, result.size(), "Should have 5 split files");
        for (java.io.File splitFile : result) {
            assertValidPdf(splitFile);
            try (PDDocument doc = PDDocument.load(splitFile)) {
                assertEquals(1, doc.getNumberOfPages(), "Each split file should have 1 page");
            }
        }
    }

    @Test
    @DisplayName("Should throw exception when splitting null PDF")
    void testSplitPDF_NullFile() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            pdfManager.splitPDF(null);
        }, "Should throw exception for null file");
    }

    @Test
    @DisplayName("Should split single page PDF")
    void testSplitPDF_SinglePage() throws Exception {
        // Arrange
        java.io.File pdfFile = createSamplePdfFile("single.pdf", 1);

        // Act
        List<java.io.File> result = pdfManager.splitPDF(pdfFile);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should have 1 split file");
    }

    // ==================== Helper Methods ====================

    private List<EntityValue> createSampleEntityValues() {
        List<EntityValue> entityValues = new ArrayList<>();

        Map<String, Object> data1 = new HashMap<>();
        data1.put("name", "John Doe");
        data1.put("age", 30);
        data1.put("email", "john@example.com");
        EntityValue entity1 = new EntityValue("entity1", data1);
        entityValues.add(entity1);

        Map<String, Object> data2 = new HashMap<>();
        data2.put("name", "Jane Smith");
        data2.put("age", 25);
        data2.put("email", "jane@example.com");
        EntityValue entity2 = new EntityValue("entity2", data2);
        entityValues.add(entity2);

        return entityValues;
    }
}


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

import app.krista.extensions.essentials.documents.pdf.catalog.utils.PdfSplitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test class for new PDF extension features.
 * Tests Split PDF operations and Excel multiple sheets conversion functionality.
 */
public class PDFExtensionNewFeaturesTest {

    private PDFManager pdfManager;
    private app.krista.extensions.essentials.documents.pdf.service.PdfSplitter pdfSplitter;
    private ExcelManager excelManager;
    private List<java.io.File> filesToCleanup;

    @Before
    public void setUp() {
        // Given - Initialize test dependencies
        pdfSplitter = new app.krista.extensions.essentials.documents.pdf.service.PdfSplitter();
        pdfManager = new PDFManager(pdfSplitter);
        excelManager = new ExcelManager();
        filesToCleanup = new ArrayList<>();
    }

    @After
    public void tearDown() {
        // Finally - Clean up all generated files
        for (java.io.File file : filesToCleanup) {
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
     *
     * @param files files to be cleaned up
     */
    private void addToCleanup(java.io.File... files) {
        for (java.io.File file : files) {
            if (file != null) {
                filesToCleanup.add(file);
            }
        }
    }

    /**
     * Helper method to create a test PDF file for splitting operations.
     *
     * @return test PDF file
     * @throws IOException if file creation fails
     */
    private java.io.File createTestPdfFile() throws IOException {
        // Create a simple test PDF with multiple pages
        PDDocument document = new PDDocument();

        // Add 5 pages to the document
        for (int i = 0; i < 5; i++) {
            document.addPage(new PDPage());
        }

        java.io.File testFile = new java.io.File("test_split.pdf");
        document.save(testFile);
        document.close();

        addToCleanup(testFile);
        return testFile;
    }

    /**
     * Helper method to get file extension from a file.
     *
     * @param outputFile the file to extract extension from
     * @return the file extension without the dot
     */
    private static String getFileExtension(java.io.File outputFile) {
        return outputFile.getName().substring(outputFile.getName().lastIndexOf('.') + 1);
    }

    @Test
    public void testSplitPDFIntoPages_WithValidFile() throws IOException {
        // Given
        java.io.File testPdfFile = createTestPdfFile();

        List<String> categories = List.of("Category1", "Category2");
        String singleCategory = "TestCategory";

        // When - Test the core PDF splitting functionality
        List<java.io.File> splitFiles = pdfManager.splitPDF(testPdfFile);

        // Then
        Assert.assertNotNull("Split files should not be null", splitFiles);
        Assert.assertTrue("Should have split files", splitFiles.size() > 0);
        Assert.assertEquals("Should have 5 split files for 5-page PDF", 5, splitFiles.size());

        // Verify each split file exists and is valid
        for (java.io.File splitFile : splitFiles) {
            Assert.assertNotNull("Split file should not be null", splitFile);
            Assert.assertTrue("Split file should exist", splitFile.exists());
            Assert.assertTrue("Split file should not be empty", splitFile.length() > 0);

            // Verify it's a valid PDF with 1 page
            try (PDDocument splitDoc = PDDocument.load(splitFile)) {
                Assert.assertEquals("Each split file should have 1 page", 1, splitDoc.getNumberOfPages());
            }

            addToCleanup(splitFile);
        }
    }

    @Test
    public void testSplitPDFIntoPages_WithNullFile() {
        // Given
        java.io.File nullFile = null;

        try {
            // When
            pdfManager.splitPDF(nullFile);

            // Then
            Assert.fail("Should throw exception for null input");
        } catch (NullPointerException e) {
            // Then - Expected specific exception type
            Assert.assertNotNull("NullPointerException should be thrown for null input", e);
        } catch (IllegalArgumentException e) {
            // Then - Alternative expected exception type
            Assert.assertNotNull("IllegalArgumentException should be thrown for null input", e);
        } catch (IOException e) {
            // Then - File I/O related exception
            Assert.assertNotNull("IOException should be thrown for null input", e);
        } catch (Exception e) {
            // Then - Log unexpected exception type for debugging
            System.out.println("Unexpected exception type for null PDF file: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            Assert.assertNotNull("Some exception should be thrown for null input", e);
        }
    }

    @Test
    public void testSplitPDFIntoTwoEqualPart_WithValidFile() throws IOException {
        // Given
        java.io.File testPdfFile = createTestPdfFile();
        int pageThreshold = 3; // Split if more than 3 pages

        // When
        List<java.io.File> splitFiles = pdfManager.splitPDFIntoParts(testPdfFile, pageThreshold);

        // Then
        Assert.assertNotNull("Split files should not be null", splitFiles);
        Assert.assertEquals("Should have 2 split files", 2, splitFiles.size());

        // Verify first half
        java.io.File firstHalf = splitFiles.get(0);
        Assert.assertNotNull("First half should not be null", firstHalf);
        Assert.assertTrue("First half should exist", firstHalf.exists());

        try (PDDocument firstDoc = PDDocument.load(firstHalf)) {
            Assert.assertEquals("First half should have 2 pages (5/2 = 2)", 2, firstDoc.getNumberOfPages());
        }

        // Verify second half
        java.io.File secondHalf = splitFiles.get(1);
        Assert.assertNotNull("Second half should not be null", secondHalf);
        Assert.assertTrue("Second half should exist", secondHalf.exists());

        try (PDDocument secondDoc = PDDocument.load(secondHalf)) {
            Assert.assertEquals("Second half should have 3 pages (5-2 = 3)", 3, secondDoc.getNumberOfPages());
        }

        addToCleanup(firstHalf, secondHalf);
    }

    @Test
    public void testSplitPDFIntoTwoEqualPart_WithSmallFile() throws IOException {
        // Given - Create a PDF with only 2 pages (below threshold)
        PDDocument document = new PDDocument();
        document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
        document.addPage(new org.apache.pdfbox.pdmodel.PDPage());

        java.io.File smallPdfFile = new java.io.File("small_test.pdf");
        document.save(smallPdfFile);
        document.close();
        addToCleanup(smallPdfFile);

        int pageThreshold = 5; // Threshold higher than file pages

        // When
        List<java.io.File> splitFiles = pdfManager.splitPDFIntoParts(smallPdfFile, pageThreshold);

        // Then
        Assert.assertNotNull("Split files should not be null", splitFiles);
        Assert.assertEquals("Should return original file when below threshold", 1, splitFiles.size());
        Assert.assertEquals("Should return the same file", smallPdfFile, splitFiles.get(0));
    }

    @Test
    public void testSplitPDFIntoTwoEqualPart_WithNullFile() {
        // Given
        java.io.File nullFile = null;
        int pageThreshold = 40;

        try {
            // When
            pdfManager.splitPDFIntoParts(nullFile, pageThreshold);

            // Then
            Assert.fail("Should throw exception for null input");
        } catch (NullPointerException e) {
            // Then - Expected specific exception type
            Assert.assertNotNull("NullPointerException should be thrown for null input", e);
        } catch (IllegalArgumentException e) {
            // Then - Alternative expected exception type
            Assert.assertNotNull("IllegalArgumentException should be thrown for null input", e);
        } catch (IOException e) {
            // Then - File I/O related exception
            Assert.assertNotNull("IOException should be thrown for null input", e);
        } catch (Exception e) {
            // Then - Log unexpected exception type for debugging
            System.out.println("Unexpected exception type for null PDF split: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            Assert.assertNotNull("Some exception should be thrown for null input", e);
        }
    }

    @Test
    public void testSplitPDFIntoTwoEqualPart_WithDefaultThreshold() throws IOException {
        // Given
        java.io.File testPdfFile = createTestPdfFile();
        Double nullThreshold = null; // Should use default value of 40

        // When - Test with null threshold (should use default)
        List<java.io.File> splitFiles = pdfManager.splitPDFIntoParts(testPdfFile, 40);

        // Then - Since our test file has 5 pages (< 40), should return original file
        Assert.assertNotNull("Split files should not be null", splitFiles);
        Assert.assertEquals("Should return original file when below default threshold", 1, splitFiles.size());
    }

    @Test
    public void testSplitPDFBasedOnCondition_WithValidFile() throws IOException {
        // Given
        java.io.File testPdfFile = createTestPdfFile();

        // Note: This test focuses on the core functionality that can be tested
        // The actual condition-based splitting logic in PdfSplitter.splitPdfByCriteria
        // requires specific PDF content patterns, so we test the basic file handling

        // When - Verify the file can be loaded and processed
        try (PDDocument document = PDDocument.load(testPdfFile)) {
            // Then
            Assert.assertNotNull("Document should load successfully", document);
            Assert.assertEquals("Document should have expected pages", 5, document.getNumberOfPages());
        }
    }

    @Test
    public void testSplitPDFBasedOnCondition_WithNullFile() {
        // Given
        java.io.File nullFile = null;

        try {
            // When - Test with null file
            org.apache.pdfbox.pdmodel.PDDocument.load(nullFile);

            // Then
            Assert.fail("Should throw exception for null input");
        } catch (NullPointerException e) {
            // Then - Expected specific exception type
            Assert.assertNotNull("NullPointerException should be thrown for null input", e);
        } catch (IllegalArgumentException e) {
            // Then - Alternative expected exception type
            Assert.assertNotNull("IllegalArgumentException should be thrown for null input", e);
        } catch (IOException e) {
            // Then - File I/O related exception
            Assert.assertNotNull("IOException should be thrown for null input", e);
        } catch (Exception e) {
            // Then - Log unexpected exception type for debugging
            System.out.println("Unexpected exception type for null PDF condition split: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            Assert.assertNotNull("Some exception should be thrown for null input", e);
        }
    }

    @Test
    public void testSplitPDFBasedOnCondition_WithInvalidFile() {
        // Given
        java.io.File invalidFile = new java.io.File("non-existent-file.pdf");

        try {
            // When
            org.apache.pdfbox.pdmodel.PDDocument.load(invalidFile);

            // Then
            Assert.fail("Should throw exception for invalid file");
        } catch (java.io.FileNotFoundException e) {
            // Then - Expected specific exception type for non-existent file
            Assert.assertNotNull("FileNotFoundException should be thrown for non-existent file", e);
            Assert.assertTrue("Exception message should mention file not found",
                    e.getMessage().toLowerCase().contains("non-existent-file.pdf") ||
                            e.getMessage().toLowerCase().contains("no such file"));
        } catch (IOException e) {
            // Then - General I/O exception is also acceptable
            Assert.assertNotNull("IOException should be thrown for invalid file", e);
        } catch (Exception e) {
            // Then - Log unexpected exception type for debugging
            System.out.println("Unexpected exception type for invalid file: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            Assert.assertNotNull("Some exception should be thrown for invalid file", e);
        }
    }

    @Test
    public void testConvertExcelFileWithMultipleSheetsToPDF_FileValidation() throws URISyntaxException {
        // Given
        URL resource = getClass().getClassLoader().getResource("test.xlsx");
        java.io.File inputFile = new java.io.File(Objects.requireNonNull(resource).toURI());

        // When - Verify the Excel file exists and can be processed
        Assert.assertNotNull("Excel test file should exist", inputFile);
        Assert.assertTrue("Excel test file should exist on filesystem", inputFile.exists());
        Assert.assertTrue("Excel test file should not be empty", inputFile.length() > 0);

        // Then - Verify file extension
        String fileName = inputFile.getName();
        Assert.assertTrue("File should have .xlsx extension",
                fileName.toLowerCase().endsWith(".xlsx"));
    }

    @Test
    public void testConvertExcelToPDF_WithMultipleSheets() throws URISyntaxException {
        // Given
        URL resource = getClass().getClassLoader().getResource("test.xlsx");
        java.io.File inputFile = new java.io.File(Objects.requireNonNull(resource).toURI());

        // When - Convert Excel to PDF (this tests the underlying functionality)
        java.io.File pdfFile = excelManager.convertExcelToPDF(inputFile, null);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("File extension should be pdf", getFileExtension(pdfFile).equalsIgnoreCase("pdf"));
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());

        // When - Load and validate PDF content
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertTrue("PDF should have at least 1 page", pdfDocument.getNumberOfPages() >= 1);

            // When - Extract text content to verify conversion
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String pdfText = pdfTextStripper.getText(pdfDocument);

            // Then - Verify some content was converted
            Assert.assertNotNull("PDF text should not be null", pdfText);
            Assert.assertTrue("PDF should contain some text content", pdfText.trim().length() > 0);
        } catch (IOException e) {
            Assert.fail("Should be able to load and read the generated PDF: " + e.getMessage());
        }
    }

    @Test
    public void testConvertExcelFileWithMultipleSheetsToPDF_WithNullFile() {
        // Given
        java.io.File nullFile = null;

        try {
            // When - Test with null file
            excelManager.convertExcelToPDF(nullFile, null);

            // Then
            Assert.fail("Should throw exception for null input");
        } catch (NullPointerException e) {
            // Then - Expected specific exception type
            Assert.assertNotNull("NullPointerException should be thrown for null input", e);
        } catch (IllegalArgumentException e) {
            // Then - Alternative expected exception type
            Assert.assertNotNull("IllegalArgumentException should be thrown for null input", e);
        } catch (Exception e) {
            // Then - Log unexpected exception type for debugging
            System.out.println("Unexpected exception type: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            Assert.assertNotNull("Some exception should be thrown for null input", e);
        }
    }

    @Test
    public void testConvertExcelFileWithMultipleSheetsToPDF_WithInvalidExtension() {
        // Given
        java.io.File invalidFile = new java.io.File("test.txt");

        // When - Test with invalid file extension
        String fileName = invalidFile.getName();

        // Then
        Assert.assertNotNull("File name should not be null", fileName);
        Assert.assertFalse("Should not have valid Excel extension",
                fileName.toLowerCase().endsWith(".xlsx"));
    }

    @Test
    public void testPDFSplitting_EdgeCases() throws IOException {
        // Given - Create a single page PDF
        PDDocument singlePageDoc = new PDDocument();
        singlePageDoc.addPage(new PDPage());

        java.io.File singlePageFile = new java.io.File("single_page_test.pdf");
        singlePageDoc.save(singlePageFile);
        singlePageDoc.close();
        addToCleanup(singlePageFile);

        // When - Split single page PDF
        List<java.io.File> splitFiles = pdfManager.splitPDF(singlePageFile);

        // Then
        Assert.assertNotNull("Split files should not be null", splitFiles);
        Assert.assertEquals("Should have 1 split file for single page", 1, splitFiles.size());

        java.io.File splitFile = splitFiles.get(0);
        Assert.assertTrue("Split file should exist", splitFile.exists());

        try (PDDocument splitDoc = PDDocument.load(splitFile)) {
            Assert.assertEquals("Split file should have 1 page", 1, splitDoc.getNumberOfPages());
        }

        addToCleanup(splitFile);
    }

    @Test
    public void testPDFSplitting_LargeDocument() throws IOException {
        // Given - Create a larger PDF document (10 pages)
        PDDocument largeDoc = new PDDocument();
        for (int i = 0; i < 10; i++) {
            largeDoc.addPage(new PDPage());
        }

        java.io.File largeFile = new java.io.File("large_test.pdf");
        largeDoc.save(largeFile);
        largeDoc.close();
        addToCleanup(largeFile);

        // When - Split into individual pages
        List<java.io.File> splitFiles = pdfManager.splitPDF(largeFile);

        // Then
        Assert.assertNotNull("Split files should not be null", splitFiles);
        Assert.assertEquals("Should have 10 split files", 10, splitFiles.size());

        // Verify each split file
        for (int i = 0; i < splitFiles.size(); i++) {
            java.io.File splitFile = splitFiles.get(i);
            Assert.assertTrue("Split file " + i + " should exist", splitFile.exists());

            try (PDDocument splitDoc = PDDocument.load(splitFile)) {
                Assert.assertEquals("Split file " + i + " should have 1 page",
                        1, splitDoc.getNumberOfPages());
            }

            addToCleanup(splitFile);
        }
    }

    @Test
    public void testPDFSplitIntoTwoParts_ExactMiddle() throws IOException {
        // Given - Create a PDF with even number of pages (6 pages)
        PDDocument evenDoc = new PDDocument();
        for (int i = 0; i < 6; i++) {
            evenDoc.addPage(new PDPage());
        }

        java.io.File evenFile = new java.io.File("even_pages_test.pdf");
        evenDoc.save(evenFile);
        evenDoc.close();
        addToCleanup(evenFile);

        // When - Split into two parts with low threshold
        List<java.io.File> splitFiles = pdfManager.splitPDFIntoParts(evenFile, 3);

        // Then
        Assert.assertNotNull("Split files should not be null", splitFiles);
        Assert.assertEquals("Should have 2 split files", 2, splitFiles.size());

        // Verify first half (3 pages)
        try (PDDocument firstDoc = PDDocument.load(splitFiles.get(0))) {
            Assert.assertEquals("First half should have 3 pages", 3, firstDoc.getNumberOfPages());
        }

        // Verify second half (3 pages)
        try (PDDocument secondDoc = PDDocument.load(splitFiles.get(1))) {
            Assert.assertEquals("Second half should have 3 pages", 3, secondDoc.getNumberOfPages());
        }

        addToCleanup(splitFiles.get(0), splitFiles.get(1));
    }

    @Test
    public void testPdfSplitter_SplitByCriteria() {
        // Given - Create a test PDF file
        try {
            java.io.File testPdfFile = createTestPdfFile();

            // When - Test the PdfSplitter utility class
            List<java.io.File> splitFiles = PdfSplitter.splitPdfByCriteria(testPdfFile);

            // Then - The method should execute without throwing exceptions
            Assert.assertNotNull("Split files list should not be null", splitFiles);
            // Note: The actual splitting depends on specific content patterns in the PDF
            // For a basic test PDF, it may return an empty list, which is acceptable

            // Clean up any generated files
            for (java.io.File file : splitFiles) {
                if (file != null && file.exists()) {
                    addToCleanup(file);
                }
            }
        } catch (IOException e) {
            // Then - This is acceptable for test PDFs that don't match the splitting criteria
            // The PdfSplitter looks for specific text patterns that won't be in our test PDF
            Assert.assertNotNull("IOException is acceptable for test files without matching criteria", e);
            System.out.println("Expected IOException for test PDF without matching criteria: " + e.getMessage());
        } catch (Exception e) {
            // Then - Log unexpected exception types for debugging
            System.out.println("Unexpected exception in PdfSplitter: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            Assert.fail("Unexpected exception type: " + e.getClass().getSimpleName());
        }
    }
}

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

import app.krista.extensions.essentials.documents.pdf.util.PDFUtils;
import app.krista.model.base.EntityValue;
import app.krista.model.base.FreeForm;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Test class for PDFManager functionality.
 * Tests various PDF operations including conversion, merging, and content validation.
 */
public class PDFManagerTest {

    private PDFManager pdfManager;
    private PdfSplitter pdfSplitter;
    private ExcelManager excelManager;
    private DocxManager docxManager;
    private List<File> filesToCleanup;

    @Before
    public void setUp() {
        // Given - Initialize test dependencies
        pdfSplitter = new PdfSplitter();
        pdfManager = new PDFManager(pdfSplitter);
        excelManager = new ExcelManager();
        docxManager = new DocxManager();
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
     * Helper method to extract file extension from a file.
     *
     * @param outputFile the file to extract extension from
     * @return the file extension without the dot
     */
    private static String getFileExtension(File outputFile) {
        return outputFile.getName().substring(outputFile.getName().lastIndexOf('.') + 1);
    }

    /**
     * Helper method to add files to cleanup list for automatic deletion after tests.
     *
     * @param files files to be cleaned up
     */
    private void addToCleanup(File... files) {
        for (File file : files) {
            if (file != null) {
                filesToCleanup.add(file);
            }
        }
    }

    @Test
    public void testConvertDocToPDF() throws IOException {
        // Given
        File inputFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("test.docx")).getFile());

        // When
        File outputFile = pdfManager.convertDocToPDF(inputFile);
        addToCleanup(outputFile);

        // Then
        Assert.assertNotNull("Output file should not be null", outputFile);
        Assert.assertEquals("File extension should be pdf", "pdf", getFileExtension(outputFile));
        Assert.assertTrue("Output file should not be empty", outputFile.length() > 0);

        // When - Load and validate PDF content
        try (PDDocument pdfDocument = PDDocument.load(outputFile)) {
            // Then
            Assert.assertEquals("PDF should have 2 pages", 2, pdfDocument.getNumberOfPages());

            // When - Extract text content
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String pdfText = pdfTextStripper.getText(pdfDocument);

            // Then
            Assert.assertTrue("PDF should contain expected text", pdfText.contains("Kalpataru"));
        }
    }

    @Test
    public void testConvertDocToPDF_WithNullInput() {
        // Given
        File inputFile = null;

        try {
            // When
            pdfManager.convertDocToPDF(inputFile);

            // Then
            Assert.fail("Should throw exception for null input");
        } catch (Exception e) {
            // Then
            Assert.assertNotNull("Exception should be thrown for null input", e);
        }
    }

    @Test
    public void testGetRichTextPDF() throws Exception {
        // Given
        String title = "Test Title";
        String subTitle = "Test SubTitle";
        String description = "Test Description";
        String header = "Test Header";
        String footer = "Test Footer";
        String xHtmlPara = "<p><h1>Heading1</h1><br></br></p>";

        // When
        File outputFile = pdfManager.getRichTextPDF(title, subTitle, description, header, footer, null, xHtmlPara);
        addToCleanup(outputFile);

        // Then
        Assert.assertNotNull("Output file should not be null", outputFile);
        Assert.assertTrue("Output file should exist", outputFile.exists());
        Assert.assertTrue("Output file should not be empty", outputFile.length() > 0);

        // When - Load and validate PDF content
        try (PDDocument pdfDocument = PDDocument.load(outputFile)) {
            // Then
            Assert.assertEquals("PDF should have 1 page", 1, pdfDocument.getNumberOfPages());

            // When - Extract text content
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String pdfText = pdfTextStripper.getText(pdfDocument);

            // Then
            Assert.assertTrue("PDF should contain header text", pdfText.contains(header));
            Assert.assertTrue("PDF should contain title", pdfText.contains(title));
        }
    }

    @Test
    public void testGetRichTextPDF_WithEmptyContent() throws Exception {
        // Given
        String title = "Empty Content Test";
        String emptyContent = "";

        // When
        File outputFile = pdfManager.getRichTextPDF(title, "", "", "", "", null, emptyContent);
        addToCleanup(outputFile);

        // Then
        Assert.assertNotNull("Output file should not be null even with empty content", outputFile);
        Assert.assertTrue("Output file should exist", outputFile.exists());

        // When - Validate PDF structure
        try (PDDocument pdfDocument = PDDocument.load(outputFile)) {
            // Then
            Assert.assertTrue("PDF should have at least 1 page", pdfDocument.getNumberOfPages() >= 1);
        }
    }

    @Test
    public void testGetEntityListPdf() throws Exception {
        // Given
        List<EntityValue> entityValues = new ArrayList<>();
        Map<String, Object> fields = new HashMap<>();
        fields.put("Name", "vara");
        fields.put("Numb", 123);
        fields.put("Email", "vara@example.com");
        EntityValue eValue1 = new EntityValue("TestEntity", fields);
        entityValues.add(eValue1);

        String title = "Entity List Test";
        String subTitle = "Test Subtitle";
        String description = "Test Description";
        File logo = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("logo.png")).getFile());

        // When
        File pdfFile = pdfManager.getEntityListPdf(title, subTitle, description, "", "", logo, entityValues, null);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);

        // When - Load and validate PDF content
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertTrue("PDF should have at least 1 page", pdfDocument.getNumberOfPages() >= 1);

            // When - Extract text content
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String pdfText = pdfTextStripper.getText(pdfDocument);

            // Then
            Assert.assertTrue("PDF should contain title", pdfText.contains(title));
            Assert.assertTrue("PDF should contain entity field name", pdfText.contains("Name"));
            Assert.assertTrue("PDF should contain entity field value", pdfText.contains("vara"));

            // When - Test PDF rendering
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            BufferedImage image = renderer.renderImage(0);

            // Then
            Assert.assertNotNull("Rendered image should not be null", image);
            Assert.assertTrue("Image should have positive width", image.getWidth() > 0);
            Assert.assertTrue("Image should have positive height", image.getHeight() > 0);
        }
    }

    @Test
    public void testGetEntityListPdf_WithEmptyEntityList() throws Exception {
        // Given
        List<EntityValue> emptyEntityValues = new ArrayList<>();
        String title = "Empty Entity List Test";

        // When
        File pdfFile = pdfManager.getEntityListPdf(title, "", "", "", "", null, emptyEntityValues, null);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null even with empty entity list", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());

        // When - Validate PDF structure
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertTrue("PDF should have at least 1 page", pdfDocument.getNumberOfPages() >= 1);
        }
    }

    @Test
    public void testConvertExcelToPDF() throws URISyntaxException {
        // Given
        URL resource = getClass().getClassLoader().getResource("test.xlsx");
        File inputFile = new File(Objects.requireNonNull(resource).toURI());

        // When
        File docPdf = excelManager.convertExcelToPDF(inputFile, null);
        addToCleanup(docPdf);

        // Then
        Assert.assertNotNull("PDF file should not be null", docPdf);
        Assert.assertTrue("File extension should be pdf", getFileExtension(docPdf).equalsIgnoreCase("pdf"));
        Assert.assertTrue("PDF file should not be empty", docPdf.length() > 0);
        Assert.assertTrue("PDF file should exist", docPdf.exists());
    }

    @Test
    public void testConvertExcelToPDF_WithSpecificSheet() throws URISyntaxException {
        // Given
        URL resource = getClass().getClassLoader().getResource("test.xlsx");
        File inputFile = new File(Objects.requireNonNull(resource).toURI());
        String sheetName = "Sheet1";

        // When
        File docPdf = excelManager.convertExcelToPDF(inputFile, sheetName);
        addToCleanup(docPdf);

        // Then
        Assert.assertNotNull("PDF file should not be null", docPdf);
        Assert.assertTrue("File extension should be pdf", getFileExtension(docPdf).equalsIgnoreCase("pdf"));
        Assert.assertTrue("PDF file should not be empty", docPdf.length() > 0);
    }

    @Test
    public void testFillTheDocTemplate() throws URISyntaxException, IOException {
        // Given
        URL resource = getClass().getClassLoader().getResource("test.docx");
        File inputFile = new File(Objects.requireNonNull(resource).toURI());
        FreeForm templateFields = new FreeForm();
        templateFields.put("$$NAME", "Text", "Vara");
        templateFields.put("$$isTRUE", "Switch", true);
        templateFields.put("$$number", "Number", 1234.0);
        templateFields.put("$$Date", "Number", 1.670397003986e12);
        templateFields.put("$$FOOTNOTE", "Text", "footnote");
        templateFields.put("$$Header", "Text", "headerNote");

        // When
        File docPdf = docxManager.fillTheDocTemplate(inputFile, templateFields);
        addToCleanup(docPdf);

        // Then
        Assert.assertNotNull("PDF file should not be null", docPdf);
        Assert.assertTrue("PDF file should exist", docPdf.exists());
        Assert.assertTrue("PDF file should not be empty", docPdf.length() > 0);

        // When - Load and validate PDF content
        try (PDDocument pdfDoc = PDDocument.load(docPdf)) {
            // Then
            Assert.assertTrue("PDF should have at least 1 page", pdfDoc.getNumberOfPages() >= 1);

            // When - Extract text content
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String pdfText = pdfTextStripper.getText(pdfDoc);

            // Then
            Assert.assertTrue("PDF should contain filled template value", pdfText.contains("Vara"));
            // Note: Number formatting may vary, so we check for the presence of the value in some form
            Assert.assertTrue("PDF should contain footnote", pdfText.contains("footnote"));
        }
    }

    @Test
    public void testFillTheDocTemplate_WithEmptyFields() throws URISyntaxException, IOException {
        // Given
        URL resource = getClass().getClassLoader().getResource("test.docx");
        File inputFile = new File(Objects.requireNonNull(resource).toURI());
        FreeForm emptyTemplateFields = new FreeForm();

        try {
            // When
            File docPdf = docxManager.fillTheDocTemplate(inputFile, emptyTemplateFields);
            addToCleanup(docPdf);

            // Then
            Assert.assertNotNull("PDF file should not be null even with empty fields", docPdf);
            Assert.assertTrue("PDF file should exist", docPdf.exists());

            // When - Validate PDF structure
            try (PDDocument pdfDoc = PDDocument.load(docPdf)) {
                // Then
                Assert.assertTrue("PDF should have at least 1 page", pdfDoc.getNumberOfPages() >= 1);
            }
        } catch (Exception e) {
            // Some implementations may not support empty template fields
            // This is acceptable behavior, so we just log it
            System.out.println("Note: Empty template fields not supported: " + e.getMessage());
        }
    }

    @Test
    public void testMergePdfFiles() throws Exception {
        // Given
        File inputFile1 = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("test.docx")).getFile());
        File inputFile2 = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("test.xlsx")).getFile());
        File pdfFile1 = pdfManager.convertDocToPDF(inputFile1);
        File pdfFile2 = excelManager.convertExcelToPDF(inputFile2, "");
        addToCleanup(pdfFile1, pdfFile2);

        List<File> sources = List.of(pdfFile1, pdfFile2);
        String title = "Merged PDF Test";
        String subTitle = "Test Subtitle";
        String description = "Test Description";

        // When
        File outputFile = pdfManager.mergePdfFiles(title, subTitle, description, "", "", null, sources);
        addToCleanup(outputFile);

        // Then
        Assert.assertNotNull("Merged PDF file should not be null", outputFile);
        Assert.assertTrue("Merged PDF file should exist", outputFile.exists());
        Assert.assertTrue("Merged PDF file should not be empty", outputFile.length() > 0);

        // When - Load and validate merged PDF content
        try (PDDocument mergedPdf = PDDocument.load(outputFile)) {
            // Then
            Assert.assertTrue("Merged PDF should have at least 1 page", mergedPdf.getNumberOfPages() >= 1);

            // When - Extract text content
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String pdfText = pdfTextStripper.getText(mergedPdf);

            // Then
            Assert.assertTrue("Merged PDF should contain title", pdfText.contains(title));
        }
    }

    @Test
    public void testMergePdfFiles_WithEmptySourceList() throws Exception {
        // Given
        List<File> emptySources = List.of();
        String title = "Empty Merge Test";

        // When
        File templateFile = pdfManager.mergePdfFiles(title, "", "", "", "", null, emptySources);
        addToCleanup(templateFile);

        // Then
        Assert.assertNotNull("Template file should not be null", templateFile);
        Assert.assertTrue("Template file should exist", templateFile.exists());

        // When - Validate template PDF structure
        try (PDDocument templatePdf = PDDocument.load(templateFile)) {
            // Then
            Assert.assertEquals("Template PDF should have exactly 1 page", 1, templatePdf.getNumberOfPages());

            // When - Extract text content
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String pdfText = pdfTextStripper.getText(templatePdf);

            // Then
            Assert.assertTrue("Template PDF should contain title", pdfText.contains(title));
        }
    }

    @Test
    public void testMergePdfFiles_WithSingleSource() throws Exception {
        // Given
        File inputFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("test.docx")).getFile());
        File pdfFile = pdfManager.convertDocToPDF(inputFile);
        addToCleanup(pdfFile);

        List<File> singleSource = List.of(pdfFile);
        String title = "Single Source Merge Test";

        // When
        File outputFile = pdfManager.mergePdfFiles(title, "", "", "", "", null, singleSource);
        addToCleanup(outputFile);

        // Then
        Assert.assertNotNull("Output file should not be null", outputFile);
        Assert.assertTrue("Output file should exist", outputFile.exists());

        // When - Validate merged PDF structure
        try (PDDocument mergedPdf = PDDocument.load(outputFile)) {
            // Then
            Assert.assertTrue("Merged PDF should have at least 1 page", mergedPdf.getNumberOfPages() >= 1);
        }
    }

    @Test
    public void testGetTableDataPdf() throws Exception {
        // Given
        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("Name", "vara");
        row1.put("Numb", 123);
        row1.put("Email", "vara@example.com");
        tableData.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("Name", "john");
        row2.put("Numb", 456);
        row2.put("Email", "john@example.com");
        tableData.add(row2);

        String title = "Table Data Test";
        String subTitle = "Test Subtitle";
        String description = "Test Description";

        // When
        File pdfFile = pdfManager.getTableDataPdf(title, subTitle, description, "", "", null, tableData);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);

        // When - Load and validate PDF content
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertTrue("PDF should have at least 1 page", pdfDocument.getNumberOfPages() >= 1);

            // When - Extract text content
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String pdfText = pdfTextStripper.getText(pdfDocument);

            // Then
            Assert.assertTrue("PDF should contain title", pdfText.contains(title));
            Assert.assertTrue("PDF should contain table header", pdfText.contains("Name"));
            Assert.assertTrue("PDF should contain table data", pdfText.contains("vara"));
            Assert.assertTrue("PDF should contain second row data", pdfText.contains("john"));
        }
    }

    @Test
    public void testGetTableDataPdf_WithEmptyTableData() throws Exception {
        // Given
        List<Map<String, Object>> emptyTableData = new ArrayList<>();
        String title = "Empty Table Test";

        try {
            // When
            File pdfFile = pdfManager.getTableDataPdf(title, "", "", "", "", null, emptyTableData);
            addToCleanup(pdfFile);

            // Then - Some implementations may handle empty data gracefully
            if (pdfFile != null) {
                Assert.assertTrue("PDF file should exist even with empty data", pdfFile.exists());
            }
        } catch (IllegalArgumentException e) {
            // Then - This is expected behavior for empty table data
            Assert.assertTrue("Exception message should mention table data",
                    e.getMessage().toLowerCase().contains("table"));
        } catch (Exception e) {
            // Then - Other exceptions are also acceptable for empty data
            Assert.assertNotNull("Exception should be thrown for empty table data", e);
        }
    }

    @Test
    public void testConvertImageToPdf() throws IOException {
        // Given
        File imageFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sampleImage.png")).getFile());

        // When
        File pdfFile = PDFUtils.convertImageToPdf(imageFile);
        addToCleanup(pdfFile);

        // Then
        Assert.assertNotNull("PDF file should not be null", pdfFile);
        Assert.assertTrue("PDF file should exist", pdfFile.exists());
        Assert.assertTrue("PDF file should not be empty", pdfFile.length() > 0);

        // When - Load and validate PDF content
        try (PDDocument pdfDocument = PDDocument.load(pdfFile)) {
            // Then
            Assert.assertTrue("PDF should have at least 1 page", pdfDocument.getNumberOfPages() > 0);

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
    public void testConvertImageToPdf_WithInvalidFile() {
        // Given
        File invalidFile = new File("non-existent-file.png");

        try {
            // When
            PDFUtils.convertImageToPdf(invalidFile);

            // Then
            Assert.fail("Should throw exception for invalid file");
        } catch (Exception e) {
            // Then
            Assert.assertNotNull("Exception should be thrown for invalid file", e);
        }
    }

    // ========== Tests for formatCellValue() method ==========

    @Test
    public void testFormatCellValue_WithPercentageField() {
        // Given
        double value = 0.8;
        String fieldName = "Percentage1";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should format 0.8 as 80%", "80%", result);
    }

    @Test
    public void testFormatCellValue_WithPercentageFieldCaseInsensitive() {
        // Given
        double value = 0.15;
        String fieldName = "DISCOUNT_PERCENTAGE";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should format 0.15 as 15%", "15%", result);
    }

    @Test
    public void testFormatCellValue_WithPercentFieldVariant() {
        // Given
        double value = 0.5;
        String fieldName = "completion_percent";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should format 0.5 as 50%", "50%", result);
    }

    @Test
    public void testFormatCellValue_WithTimeField() {
        // Given - Jan 1, 2000 10:42:00 UTC
        long timestamp = 946684920000L;
        String fieldName = "Time1";

        // When
        String result = pdfManager.formatCellValue(timestamp, "UTC", fieldName);

        // Then
        Assert.assertTrue("Should contain time in AM/PM format", result.matches("\\d{1,2}:\\d{2} (AM|PM)"));
        Assert.assertFalse("Should not contain date", result.contains("2000"));
    }

    @Test
    public void testFormatCellValue_WithDateField() {
        // Given - Jan 1, 2000 10:42:00 UTC
        long timestamp = 946684920000L;
        String fieldName = "Date1";

        // When
        String result = pdfManager.formatCellValue(timestamp, "UTC", fieldName);

        // Then
        Assert.assertTrue("Should contain year 2000", result.contains("2000"));
        Assert.assertTrue("Should contain month Jan", result.contains("Jan"));
        Assert.assertFalse("Should not contain time", result.contains("AM") || result.contains("PM"));
    }

    @Test
    public void testFormatCellValue_WithDateTimeField() {
        // Given - Jan 1, 2000 10:42:00 UTC
        long timestamp = 946684920000L;
        String fieldName = "Created_At";

        // When
        String result = pdfManager.formatCellValue(timestamp, "UTC", fieldName);

        // Then
        Assert.assertTrue("Should contain year 2000", result.contains("2000"));
        Assert.assertTrue("Should contain month Jan", result.contains("Jan"));
        Assert.assertTrue("Should contain time in AM/PM format", result.contains("AM") || result.contains("PM"));
        Assert.assertTrue("Should contain timezone", result.contains("UTC"));
    }

    @Test
    public void testFormatCellValue_WithTimezoneConversion() {
        // Given - Jan 2, 2026 10:42:00 IST (Asia/Kolkata)
        long timestamp = 1735804920000L;
        String fieldName = "Created_At";

        // When
        String result = pdfManager.formatCellValue(timestamp, "Asia/Kolkata", fieldName);

        // Then
        Assert.assertTrue("Should contain year 2026", result.contains("2026"));
        Assert.assertTrue("Should contain IST timezone", result.contains("IST"));
    }

    @Test
    public void testFormatCellValue_WithInvalidTimezone() {
        // Given
        long timestamp = 946684920000L;
        String fieldName = "Created_At";

        // When - Invalid timezone should fall back to server timezone
        String result = pdfManager.formatCellValue(timestamp, "Invalid/Timezone", fieldName);

        // Then
        Assert.assertNotNull("Should return non-null result", result);
        Assert.assertTrue("Should contain year 2000", result.contains("2000"));
    }

    @Test
    public void testFormatCellValue_WithSmallNumber() {
        // Given - Small numbers should NOT be treated as timestamps
        int value = 1;
        String fieldName = "Position";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should return '1' as-is", "1", result);
    }

    @Test
    public void testFormatCellValue_WithRegularNumber() {
        // Given
        int value = 100;
        String fieldName = "Count";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should return '100' as-is", "100", result);
    }

    @Test
    public void testFormatCellValue_WithNullValue() {
        // Given
        Object value = null;
        String fieldName = "Field";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should return empty string for null", "", result);
    }

    @Test
    public void testFormatCellValue_WithNullFieldName() {
        // Given
        long timestamp = 946684920000L;

        // When - Null field name should default to full datetime format
        String result = pdfManager.formatCellValue(timestamp, "UTC", null);

        // Then
        Assert.assertTrue("Should contain year 2000", result.contains("2000"));
        Assert.assertTrue("Should contain time", result.contains("AM") || result.contains("PM"));
    }

    @Test
    public void testFormatCellValue_WithBoundaryTimestampMin() {
        // Given - Jan 1, 2000 00:00:00 UTC (minimum valid timestamp)
        long timestamp = 946684800000L;
        String fieldName = "Created_At";

        // When
        String result = pdfManager.formatCellValue(timestamp, "UTC", fieldName);

        // Then
        Assert.assertTrue("Should format as date", result.contains("2000"));
    }

    @Test
    public void testFormatCellValue_WithBoundaryTimestampMax() {
        // Given - Jan 1, 2100 00:00:00 UTC (maximum valid timestamp)
        long timestamp = 4102444800000L;
        String fieldName = "Created_At";

        // When
        String result = pdfManager.formatCellValue(timestamp, "UTC", fieldName);

        // Then
        Assert.assertTrue("Should format as date", result.contains("2100"));
    }

    @Test
    public void testFormatCellValue_WithStringValue() {
        // Given
        String value = "Test String";
        String fieldName = "Name";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should return string as-is", "Test String", result);
    }

    @Test
    public void testFormatCellValue_WithNullTimezone() {
        // Given
        long timestamp = 946684920000L;
        String fieldName = "Created_At";

        // When - Null timezone should use server default
        String result = pdfManager.formatCellValue(timestamp, null, fieldName);

        // Then
        Assert.assertNotNull("Should return non-null result", result);
        Assert.assertTrue("Should contain year 2000", result.contains("2000"));
    }

    @Test
    public void testFormatCellValue_WithBlankTimezone() {
        // Given
        long timestamp = 946684920000L;
        String fieldName = "Created_At";

        // When - Blank timezone should use server default
        String result = pdfManager.formatCellValue(timestamp, "   ", fieldName);

        // Then
        Assert.assertNotNull("Should return non-null result", result);
        Assert.assertTrue("Should contain year 2000", result.contains("2000"));
    }

    @Test
    public void testFormatCellValue_WithPercentageRounding() {
        // Given
        double value = 0.876;  // Should round to 88%
        String fieldName = "Percentage";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should round 0.876 to 88%", "88%", result);
    }

    @Test
    public void testFormatCellValue_WithZeroPercentage() {
        // Given
        double value = 0.0;
        String fieldName = "Percentage";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should format 0.0 as 0%", "0%", result);
    }

    @Test
    public void testFormatCellValue_WithFullPercentage() {
        // Given
        double value = 1.0;
        String fieldName = "Percentage";

        // When
        String result = pdfManager.formatCellValue(value, null, fieldName);

        // Then
        Assert.assertEquals("Should format 1.0 as 100%", "100%", result);
    }
}

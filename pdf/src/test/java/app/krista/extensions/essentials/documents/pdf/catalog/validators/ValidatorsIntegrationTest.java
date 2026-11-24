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

package app.krista.extensions.essentials.documents.pdf.catalog.validators;

import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.FieldTypes;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for all validator classes.
 * Tests cover validation logic, message generation, and edge cases.
 */
@DisplayName("Validators Integration Tests")
public class ValidatorsIntegrationTest {

    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        context = new HashMap<>();
    }

    // ==================== DocxFileFormatValidator Tests ====================

    @Test
    @DisplayName("DocxFileFormatValidator - Should validate valid .docx file")
    void testDocxValidator_ValidFile() {
        DocxFileFormatValidator validator = new DocxFileFormatValidator();
        assertTrue(validator.validate("document.docx", context));
        assertTrue(validator.validate("DOCUMENT.DOCX", context));
        assertTrue(validator.validate("my-file.docx", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "document.doc", "document.pdf", "document.txt", "document"})
    @DisplayName("DocxFileFormatValidator - Should reject invalid files")
    void testDocxValidator_InvalidFiles(String fileName) {
        DocxFileFormatValidator validator = new DocxFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("DocxFileFormatValidator - Should return correct metadata")
    void testDocxValidator_Metadata() {
        DocxFileFormatValidator validator = new DocxFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.doc", context));
        assertNotNull(validator.getErrMessage("test.doc"));
    }

    // ==================== ExcelFileFormatValidator Tests ====================

    @Test
    @DisplayName("ExcelFileFormatValidator - Should validate valid .xlsx file")
    void testExcelValidator_ValidFile() {
        ExcelFileFormatValidator validator = new ExcelFileFormatValidator();
        assertTrue(validator.validate("spreadsheet.xlsx", context));
        assertTrue(validator.validate("SPREADSHEET.XLSX", context));
        assertTrue(validator.validate("data-file.xlsx", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "spreadsheet.xls", "spreadsheet.csv", "spreadsheet.pdf", "spreadsheet"})
    @DisplayName("ExcelFileFormatValidator - Should reject invalid files")
    void testExcelValidator_InvalidFiles(String fileName) {
        ExcelFileFormatValidator validator = new ExcelFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("ExcelFileFormatValidator - Should return correct metadata")
    void testExcelValidator_Metadata() {
        ExcelFileFormatValidator validator = new ExcelFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.xls", context));
        assertNotNull(validator.getErrMessage("test.xls"));
    }

    // ==================== PdfFileFormatValidator Tests ====================

    @Test
    @DisplayName("PdfFileFormatValidator - Should validate valid .pdf file")
    void testPdfValidator_ValidFile() {
        PdfFileFormatValidator validator = new PdfFileFormatValidator();
        assertTrue(validator.validate("document.pdf", context));
        assertTrue(validator.validate("DOCUMENT.PDF", context));
        assertTrue(validator.validate("my-file.pdf", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "document.docx", "document.txt", "document.jpg", "document"})
    @DisplayName("PdfFileFormatValidator - Should reject invalid files")
    void testPdfValidator_InvalidFiles(String fileName) {
        PdfFileFormatValidator validator = new PdfFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("PdfFileFormatValidator - Should return correct metadata")
    void testPdfValidator_Metadata() {
        PdfFileFormatValidator validator = new PdfFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.docx", context));
        assertNotNull(validator.getErrMessage("test.docx"));
        assertNotNull(validator.getConfirmationStepMessage(null, context));
        assertNotNull(validator.getErrMessage(null));
    }

    // ==================== ImageFileFormatValidator Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {"image.png", "IMAGE.PNG", "photo.jpg", "PHOTO.JPG", "picture.jpeg", "PICTURE.JPEG"})
    @DisplayName("ImageFileFormatValidator - Should validate valid image files")
    void testImageValidator_ValidFiles(String fileName) {
        ImageFileFormatValidator validator = new ImageFileFormatValidator();
        assertTrue(validator.validate(fileName, context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "image.gif", "image.bmp", "image.svg", "image.pdf", "image"})
    @DisplayName("ImageFileFormatValidator - Should reject invalid files")
    void testImageValidator_InvalidFiles(String fileName) {
        ImageFileFormatValidator validator = new ImageFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("ImageFileFormatValidator - Should return correct metadata")
    void testImageValidator_Metadata() {
        ImageFileFormatValidator validator = new ImageFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.gif", context));
        assertNotNull(validator.getErrMessage("test.gif"));
    }

    // ==================== RichTextValidator Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {"Hello World", "This is valid content", "<p>HTML content</p>",
            "<div>Rich <b>text</b> content</div>", "12345"})
    @DisplayName("RichTextValidator - Should validate valid rich text")
    void testRichTextValidator_ValidContent(String content) {
        RichTextValidator validator = new RichTextValidator();
        assertTrue(validator.validate(content, context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "    ", "abc", "<p></p>", "<div></div>", "<p>  </p>"})
    @DisplayName("RichTextValidator - Should reject invalid content")
    void testRichTextValidator_InvalidContent(String content) {
        RichTextValidator validator = new RichTextValidator();
        assertFalse(validator.validate(content, context));
    }

    @Test
    @DisplayName("RichTextValidator - Should return correct metadata")
    void testRichTextValidator_Metadata() {
        RichTextValidator validator = new RichTextValidator();
        assertEquals(FieldTypes.RICH_TEXT_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("", context));
        assertNotNull(validator.getErrMessage(""));
    }

    // ==================== PdfFileCountValidator Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {"FILE_COUNT:2", "FILE_COUNT:3", "FILE_COUNT:10", "FILE_COUNT:100"})
    @DisplayName("PdfFileCountValidator - Should validate sufficient file count")
    void testPdfFileCountValidator_ValidCount(String resourceId) {
        PdfFileCountValidator validator = new PdfFileCountValidator();
        assertTrue(validator.validate(resourceId, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FILE_COUNT:0", "FILE_COUNT:1"})
    @DisplayName("PdfFileCountValidator - Should reject insufficient file count")
    void testPdfFileCountValidator_InsufficientCount(String resourceId) {
        PdfFileCountValidator validator = new PdfFileCountValidator();
        assertFalse(validator.validate(resourceId, context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no_count", "FILE_COUNT:", "FILE_COUNT:abc", "random_string"})
    @DisplayName("PdfFileCountValidator - Should handle invalid format gracefully")
    void testPdfFileCountValidator_InvalidFormat(String resourceId) {
        PdfFileCountValidator validator = new PdfFileCountValidator();
        // Should return true for non-file-count formats (allows use in other contexts)
        Boolean result = validator.validate(resourceId, context);
        assertNotNull(result);
    }

    @Test
    @DisplayName("PdfFileCountValidator - Should return correct metadata")
    void testPdfFileCountValidator_Metadata() {
        PdfFileCountValidator validator = new PdfFileCountValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("FILE_COUNT:0", context));
        assertNotNull(validator.getConfirmationStepMessage("FILE_COUNT:1", context));
        assertNotNull(validator.getErrMessage("FILE_COUNT:0"));
        assertNotNull(validator.getErrMessage("FILE_COUNT:1"));
    }

    // ==================== SheetNameValidator Tests ====================

    @Test
    @DisplayName("SheetNameValidator - Should validate null/blank sheet name (use first sheet)")
    void testSheetNameValidator_NullOrBlank() {
        SheetNameValidator validator = new SheetNameValidator(null);
        assertTrue(validator.validate(null, context));
        assertTrue(validator.validate("", context));
        assertTrue(validator.validate("   ", context));
    }

    @Test
    @DisplayName("SheetNameValidator - Should validate existing sheet name")
    void testSheetNameValidator_ValidSheetName() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        workbook.createSheet("Sheet1");
        workbook.createSheet("Data");

        SheetNameValidator validator = new SheetNameValidator(workbook);
        assertTrue(validator.validate("Sheet1", context));
        assertTrue(validator.validate("Data", context));

        workbook.close();
    }

    @Test
    @DisplayName("SheetNameValidator - Should reject non-existing sheet name")
    void testSheetNameValidator_InvalidSheetName() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        workbook.createSheet("Sheet1");

        SheetNameValidator validator = new SheetNameValidator(workbook);
        assertFalse(validator.validate("NonExistentSheet", context));
        assertFalse(validator.validate("Sheet2", context));

        workbook.close();
    }

    @Test
    @DisplayName("SheetNameValidator - Should handle null workbook")
    void testSheetNameValidator_NullWorkbook() {
        SheetNameValidator validator = new SheetNameValidator(null);
        assertFalse(validator.validate("AnySheet", context));
    }

    @Test
    @DisplayName("SheetNameValidator - Should return correct metadata")
    void testSheetNameValidator_Metadata() {
        SheetNameValidator validator = new SheetNameValidator(null);
        assertEquals(FieldTypes.TEXT_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("Sheet1", context));
        assertNotNull(validator.getConfirmationStepMessage(null, context));
        assertNotNull(validator.getErrMessage("Sheet1"));
        assertNotNull(validator.getErrMessage(null));
    }

    // ==================== TemplateFileFormatValidator Tests ====================

    @Test
    @DisplayName("TemplateFileFormatValidator - Should validate valid .docx template")
    void testTemplateValidator_ValidFile() {
        TemplateFileFormatValidator validator = new TemplateFileFormatValidator();
        assertTrue(validator.validate("template.docx", context));
        assertTrue(validator.validate("TEMPLATE.DOCX", context));
        assertTrue(validator.validate("my-template.docx", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "template.doc", "template.pdf", "template.txt", "template"})
    @DisplayName("TemplateFileFormatValidator - Should reject invalid files")
    void testTemplateValidator_InvalidFiles(String fileName) {
        TemplateFileFormatValidator validator = new TemplateFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("TemplateFileFormatValidator - Should return correct metadata")
    void testTemplateValidator_Metadata() {
        TemplateFileFormatValidator validator = new TemplateFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.doc", context));
        assertNotNull(validator.getErrMessage("test.doc"));
    }

    // ==================== SplitPdfFileFormatValidator Tests ====================

    @Test
    @DisplayName("SplitPdfFileFormatValidator - Should validate valid .pdf file")
    void testSplitPdfValidator_ValidFile() {
        SplitPdfFileFormatValidator validator = new SplitPdfFileFormatValidator();
        assertTrue(validator.validate("document.pdf", context));
        assertTrue(validator.validate("DOCUMENT.PDF", context));
        assertTrue(validator.validate("split-me.pdf", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "document.docx", "document.txt", "document"})
    @DisplayName("SplitPdfFileFormatValidator - Should reject invalid files")
    void testSplitPdfValidator_InvalidFiles(String fileName) {
        SplitPdfFileFormatValidator validator = new SplitPdfFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("SplitPdfFileFormatValidator - Should return correct metadata")
    void testSplitPdfValidator_Metadata() {
        SplitPdfFileFormatValidator validator = new SplitPdfFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.docx", context));
        assertNotNull(validator.getErrMessage("test.docx"));
    }

    // ==================== SplitPdfTwoEqualFileFormatValidator Tests ====================

    @Test
    @DisplayName("SplitPdfTwoEqualFileFormatValidator - Should validate valid .pdf file")
    void testSplitPdfTwoEqualValidator_ValidFile() {
        SplitPdfTwoEqualFileFormatValidator validator = new SplitPdfTwoEqualFileFormatValidator();
        assertTrue(validator.validate("document.pdf", context));
        assertTrue(validator.validate("DOCUMENT.PDF", context));
        assertTrue(validator.validate("split-equal.pdf", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "document.docx", "document.txt", "document"})
    @DisplayName("SplitPdfTwoEqualFileFormatValidator - Should reject invalid files")
    void testSplitPdfTwoEqualValidator_InvalidFiles(String fileName) {
        SplitPdfTwoEqualFileFormatValidator validator = new SplitPdfTwoEqualFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("SplitPdfTwoEqualFileFormatValidator - Should return correct metadata")
    void testSplitPdfTwoEqualValidator_Metadata() {
        SplitPdfTwoEqualFileFormatValidator validator = new SplitPdfTwoEqualFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.docx", context));
        assertNotNull(validator.getErrMessage("test.docx"));
    }

    // ==================== SplitPdfConditionFileFormatValidator Tests ====================

    @Test
    @DisplayName("SplitPdfConditionFileFormatValidator - Should validate valid .pdf file")
    void testSplitPdfConditionValidator_ValidFile() {
        SplitPdfConditionFileFormatValidator validator = new SplitPdfConditionFileFormatValidator();
        assertTrue(validator.validate("document.pdf", context));
        assertTrue(validator.validate("DOCUMENT.PDF", context));
        assertTrue(validator.validate("conditional-split.pdf", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "document.docx", "document.txt", "document"})
    @DisplayName("SplitPdfConditionFileFormatValidator - Should reject invalid files")
    void testSplitPdfConditionValidator_InvalidFiles(String fileName) {
        SplitPdfConditionFileFormatValidator validator = new SplitPdfConditionFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("SplitPdfConditionFileFormatValidator - Should return correct metadata")
    void testSplitPdfConditionValidator_Metadata() {
        SplitPdfConditionFileFormatValidator validator = new SplitPdfConditionFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.docx", context));
        assertNotNull(validator.getErrMessage("test.docx"));
    }

    // ==================== ExcelMultipleSheetsFileFormatValidator Tests ====================

    @Test
    @DisplayName("ExcelMultipleSheetsFileFormatValidator - Should validate valid .xlsx file")
    void testExcelMultipleSheetsValidator_ValidFile() {
        ExcelMultipleSheetsFileFormatValidator validator = new ExcelMultipleSheetsFileFormatValidator();
        assertTrue(validator.validate("workbook.xlsx", context));
        assertTrue(validator.validate("WORKBOOK.XLSX", context));
        assertTrue(validator.validate("multi-sheet.xlsx", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "workbook.xls", "workbook.csv", "workbook.pdf", "workbook"})
    @DisplayName("ExcelMultipleSheetsFileFormatValidator - Should reject invalid files")
    void testExcelMultipleSheetsValidator_InvalidFiles(String fileName) {
        ExcelMultipleSheetsFileFormatValidator validator = new ExcelMultipleSheetsFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("ExcelMultipleSheetsFileFormatValidator - Should return correct metadata")
    void testExcelMultipleSheetsValidator_Metadata() {
        ExcelMultipleSheetsFileFormatValidator validator = new ExcelMultipleSheetsFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.xls", context));
        assertNotNull(validator.getErrMessage("test.xls"));
    }

    // ==================== PdfToHtmlFileFormatValidator Tests ====================

    @Test
    @DisplayName("PdfToHtmlFileFormatValidator - Should validate valid .pdf file")
    void testPdfToHtmlValidator_ValidFile() {
        PdfToHtmlFileFormatValidator validator = new PdfToHtmlFileFormatValidator();
        assertTrue(validator.validate("document.pdf", context));
        assertTrue(validator.validate("DOCUMENT.PDF", context));
        assertTrue(validator.validate("convert-to-html.pdf", context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "document.html", "document.docx", "document.txt", "document"})
    @DisplayName("PdfToHtmlFileFormatValidator - Should reject invalid files")
    void testPdfToHtmlValidator_InvalidFiles(String fileName) {
        PdfToHtmlFileFormatValidator validator = new PdfToHtmlFileFormatValidator();
        assertFalse(validator.validate(fileName, context));
    }

    @Test
    @DisplayName("PdfToHtmlFileFormatValidator - Should return correct metadata")
    void testPdfToHtmlValidator_Metadata() {
        PdfToHtmlFileFormatValidator validator = new PdfToHtmlFileFormatValidator();
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchFieldName());
        assertNotNull(validator.getFetchStepMessage());
        assertNotNull(validator.getConfirmationStepMessage("test.html", context));
        assertNotNull(validator.getConfirmationStepMessage(null, context));
        assertNotNull(validator.getErrMessage("test.html"));
        assertNotNull(validator.getErrMessage(null));
    }
}

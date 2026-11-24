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
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
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
 * Comprehensive integration tests for PdfToHtmlFileFormatValidator.
 * Tests all validation scenarios, edge cases, and metadata methods.
 * <p>
 * Related to Jira Ticket: KE-2783
 */
@DisplayName("PdfToHtmlFileFormatValidator Integration Tests")
public class PdfToHtmlFileFormatValidatorIntegrationTest {

    private PdfToHtmlFileFormatValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new PdfToHtmlFileFormatValidator();
        context = new HashMap<>();
    }

    // ==================== Valid PDF File Tests ====================

    @Test
    @DisplayName("Should validate valid PDF file with lowercase extension")
    void testValidate_ValidPdfLowercase() {
        // Arrange
        String fileName = "document.pdf";

        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertTrue(result, "Should accept valid PDF file with lowercase extension");
    }

    @Test
    @DisplayName("Should validate valid PDF file with uppercase extension")
    void testValidate_ValidPdfUppercase() {
        // Arrange
        String fileName = "DOCUMENT.PDF";

        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertTrue(result, "Should accept valid PDF file with uppercase extension");
    }

    @Test
    @DisplayName("Should validate valid PDF file with mixed case extension")
    void testValidate_ValidPdfMixedCase() {
        // Arrange
        String fileName = "Document.PdF";

        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertTrue(result, "Should accept valid PDF file with mixed case extension");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "report.pdf",
            "my-document.pdf",
            "file_name.pdf",
            "document with spaces.pdf",
            "document.v2.pdf",
            "2024-report.pdf",
            "file123.pdf",
            "very_long_file_name_with_many_characters_in_it.pdf",
            "file.name.with.dots.pdf"
    })
    @DisplayName("Should validate various valid PDF file names")
    void testValidate_VariousValidPdfNames(String fileName) {
        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertTrue(result, "Should accept PDF file: " + fileName);
    }

    // ==================== Invalid File Format Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "document.html",
            "document.htm",
            "document.docx",
            "document.doc",
            "document.txt",
            "document.xlsx",
            "document.pptx",
            "document.jpg",
            "document.png",
            "document.gif",
            "document.xml",
            "document.json",
            "document.csv"
    })
    @DisplayName("Should reject files with invalid extensions")
    void testValidate_InvalidExtensions(String fileName) {
        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertFalse(result, "Should reject file with invalid extension: " + fileName);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "document",
            "file.PDF.txt",
            "pdf.document",
            "document.pdf.bak",
            "file.pdf.old",
            "pdf"
    })
    @DisplayName("Should reject files without proper PDF extension")
    void testValidate_NoPdfExtension(String fileName) {
        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertFalse(result, "Should reject file without proper PDF extension: " + fileName);
    }

    // ==================== Null and Empty Input Tests ====================

    @Test
    @DisplayName("Should reject null file name")
    void testValidate_NullFileName() {
        // Act
        Boolean result = validator.validate(null, context);

        // Assert
        assertFalse(result, "Should reject null file name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "  \t\n  "})
    @DisplayName("Should reject null, empty, or blank file names")
    void testValidate_NullEmptyBlank(String fileName) {
        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertFalse(result, "Should reject null/empty/blank file name");
    }

    // ==================== Metadata Method Tests ====================

    @Test
    @DisplayName("Should return correct field name")
    void testGetFetchFieldName() {
        // Act
        String fieldName = validator.getFetchFieldName();

        // Assert
        assertEquals(SubCatalogConstants.PDF_TO_HTML_FILE, fieldName,
                "Should return PDF_TO_HTML_FILE constant");
        assertNotNull(fieldName, "Field name should not be null");
        assertFalse(fieldName.isBlank(), "Field name should not be blank");
    }

    @Test
    @DisplayName("Should return correct field type")
    void testGetFieldType() {
        // Act
        String fieldType = validator.getFieldType();

        // Assert
        assertEquals(FieldTypes.FILE_FIELD, fieldType,
                "Should return FILE_FIELD type");
        assertNotNull(fieldType, "Field type should not be null");
        assertFalse(fieldType.isBlank(), "Field type should not be blank");
    }

    @Test
    @DisplayName("Should return meaningful fetch step message")
    void testGetFetchStepMessage() {
        // Act
        String message = validator.getFetchStepMessage();

        // Assert
        assertNotNull(message, "Fetch step message should not be null");
        assertFalse(message.isBlank(), "Fetch step message should not be blank");
        assertTrue(message.toLowerCase().contains("pdf"),
                "Message should mention PDF");
        assertTrue(message.toLowerCase().contains("html"),
                "Message should mention HTML");
        assertEquals("Please provide a valid PDF file to convert to HTML.", message,
                "Should return expected fetch step message");
    }

    // ==================== Confirmation Step Message Tests ====================

    @Test
    @DisplayName("Should return confirmation message for valid PDF file")
    void testGetConfirmationStepMessage_ValidPdf() {
        // Arrange
        String fileName = "document.pdf";

        // Act
        String message = validator.getConfirmationStepMessage(fileName, context);

        // Assert
        assertNotNull(message, "Confirmation message should not be null");
        assertFalse(message.isBlank(), "Confirmation message should not be blank");
        assertTrue(message.contains(fileName),
                "Message should contain the file name");
    }

    @Test
    @DisplayName("Should return confirmation message for invalid file")
    void testGetConfirmationStepMessage_InvalidFile() {
        // Arrange
        String fileName = "document.html";

        // Act
        String message = validator.getConfirmationStepMessage(fileName, context);

        // Assert
        assertNotNull(message, "Confirmation message should not be null");
        assertTrue(message.contains(fileName),
                "Message should contain the file name");
        assertTrue(message.toLowerCase().contains("not") && message.toLowerCase().contains("pdf"),
                "Message should indicate file is not PDF");
        assertEquals("The file '" + fileName + "' is not in PDF format. Please provide a valid PDF file.",
                message, "Should return expected confirmation message");
    }

    @Test
    @DisplayName("Should return special confirmation message for null file name")
    void testGetConfirmationStepMessage_NullFileName() {
        // Act
        String message = validator.getConfirmationStepMessage(null, context);

        // Assert
        assertNotNull(message, "Confirmation message should not be null");
        assertFalse(message.isBlank(), "Confirmation message should not be blank");
        assertTrue(message.toLowerCase().contains("no file"),
                "Message should indicate no file was provided");
        assertEquals("No file was provided. Please upload a valid PDF file.",
                message, "Should return expected message for null file");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("Should return special confirmation message for blank file name")
    void testGetConfirmationStepMessage_BlankFileName(String fileName) {
        // Act
        String message = validator.getConfirmationStepMessage(fileName, context);

        // Assert
        assertNotNull(message, "Confirmation message should not be null");
        assertTrue(message.toLowerCase().contains("no file"),
                "Message should indicate no file was provided");
    }

    @Test
    @DisplayName("Should return confirmation message with null context")
    void testGetConfirmationStepMessage_NullContext() {
        // Arrange
        String fileName = "document.pdf";

        // Act
        String message = validator.getConfirmationStepMessage(fileName, null);

        // Assert
        assertNotNull(message, "Confirmation message should not be null");
        assertTrue(message.contains(fileName),
                "Message should contain the file name");
    }

    // ==================== Error Message Tests ====================

    @Test
    @DisplayName("Should return error message for invalid file")
    void testGetErrMessage_InvalidFile() {
        // Arrange
        String fileName = "document.docx";

        // Act
        String message = validator.getErrMessage(fileName);

        // Assert
        assertNotNull(message, "Error message should not be null");
        assertFalse(message.isBlank(), "Error message should not be blank");
        assertTrue(message.contains(fileName),
                "Message should contain the file name");
        assertTrue(message.toLowerCase().contains("pdf"),
                "Message should mention PDF");
        assertTrue(message.toLowerCase().contains("html"),
                "Message should mention HTML");
        assertEquals("The file '" + fileName + "' is not in PDF format. Only PDF files can be converted to HTML.",
                message, "Should return expected error message");
    }

    @Test
    @DisplayName("Should return error message for null file name")
    void testGetErrMessage_NullFileName() {
        // Act
        String message = validator.getErrMessage(null);

        // Assert
        assertNotNull(message, "Error message should not be null");
        assertFalse(message.isBlank(), "Error message should not be blank");
        assertTrue(message.toLowerCase().contains("no file"),
                "Message should indicate no file was provided");
        assertEquals("No file was provided. Please upload a valid PDF file.",
                message, "Should return expected error message for null file");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Should return error message for blank file name")
    void testGetErrMessage_BlankFileName(String fileName) {
        // Act
        String message = validator.getErrMessage(fileName);

        // Assert
        assertNotNull(message, "Error message should not be null");
        assertTrue(message.toLowerCase().contains("no file"),
                "Message should indicate no file was provided");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "report.pdf",
            "invoice.pdf",
            "contract.pdf",
            "presentation.pdf"
    })
    @DisplayName("Should return specific error message for each invalid file")
    void testGetErrMessage_SpecificFiles(String fileName) {
        // Act
        String message = validator.getErrMessage(fileName);

        // Assert
        assertTrue(message.contains(fileName),
                "Error message should contain the specific file name: " + fileName);
    }

    // ==================== Context Handling Tests ====================

    @Test
    @DisplayName("Should validate with empty context map")
    void testValidate_EmptyContext() {
        // Arrange
        String fileName = "document.pdf";
        Map<Validator.ValidationResource, String> emptyContext = new HashMap<>();

        // Act
        Boolean result = validator.validate(fileName, emptyContext);

        // Assert
        assertTrue(result, "Should validate successfully with empty context");
    }

    @Test
    @DisplayName("Should validate with null context")
    void testValidate_NullContext() {
        // Arrange
        String fileName = "document.pdf";

        // Act
        Boolean result = validator.validate(fileName, null);

        // Assert
        assertTrue(result, "Should validate successfully with null context");
    }

    @Test
    @DisplayName("Should validate with populated context")
    void testValidate_PopulatedContext() {
        // Arrange
        String fileName = "document.pdf";
        context.put(Validator.ValidationResource.PDF_TO_HTML_FILE_FORMAT, "document.pdf");
        context.put(Validator.ValidationResource.RICH_TEXT_CONTENT, "Some content");

        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertTrue(result, "Should validate successfully with populated context");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle file name with only extension")
    void testValidate_OnlyExtension() {
        // Arrange
        String fileName = ".pdf";

        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertTrue(result, "Should accept file name with only .pdf extension");
    }

    @Test
    @DisplayName("Should handle very long file name")
    void testValidate_VeryLongFileName() {
        // Arrange
        String fileName = "a".repeat(500) + ".pdf";

        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertTrue(result, "Should accept very long file name with .pdf extension");
    }

    @Test
    @DisplayName("Should handle file name with special characters")
    void testValidate_SpecialCharacters() {
        // Arrange
        String[] fileNames = {
                "file@name.pdf",
                "file#name.pdf",
                "file$name.pdf",
                "file%name.pdf",
                "file&name.pdf",
                "file(name).pdf",
                "file[name].pdf",
                "file{name}.pdf"
        };

        // Act & Assert
        for (String fileName : fileNames) {
            Boolean result = validator.validate(fileName, context);
            assertTrue(result, "Should accept file name with special characters: " + fileName);
        }
    }

    @Test
    @DisplayName("Should handle file name with Unicode characters")
    void testValidate_UnicodeCharacters() {
        // Arrange
        String[] fileNames = {
                "文档.pdf",
                "документ.pdf",
                "مستند.pdf",
                "दस्तावेज़.pdf"
        };

        // Act & Assert
        for (String fileName : fileNames) {
            Boolean result = validator.validate(fileName, context);
            assertTrue(result, "Should accept file name with Unicode characters: " + fileName);
        }
    }

    @Test
    @DisplayName("Should reject file with PDF in name but wrong extension")
    void testValidate_PdfInNameWrongExtension() {
        // Arrange
        String fileName = "my-pdf-file.docx";

        // Act
        Boolean result = validator.validate(fileName, context);

        // Assert
        assertFalse(result, "Should reject file with 'pdf' in name but wrong extension");
    }

    @Test
    @DisplayName("Should be case-insensitive for extension")
    void testValidate_CaseInsensitiveExtension() {
        // Arrange
        String[] fileNames = {
                "file.PDF",
                "file.Pdf",
                "file.pDf",
                "file.pdF",
                "file.PdF",
                "file.pDF"
        };

        // Act & Assert
        for (String fileName : fileNames) {
            Boolean result = validator.validate(fileName, context);
            assertTrue(result, "Should accept PDF file regardless of extension case: " + fileName);
        }
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should validate and return consistent results")
    void testValidate_ConsistentResults() {
        // Arrange
        String validFile = "document.pdf";
        String invalidFile = "document.html";

        // Act
        Boolean result1 = validator.validate(validFile, context);
        Boolean result2 = validator.validate(validFile, context);
        Boolean result3 = validator.validate(invalidFile, context);
        Boolean result4 = validator.validate(invalidFile, context);

        // Assert
        assertTrue(result1, "First validation of valid file should pass");
        assertTrue(result2, "Second validation of valid file should pass");
        assertFalse(result3, "First validation of invalid file should fail");
        assertFalse(result4, "Second validation of invalid file should fail");
        assertEquals(result1, result2, "Multiple validations of same valid file should return same result");
        assertEquals(result3, result4, "Multiple validations of same invalid file should return same result");
    }

    @Test
    @DisplayName("Should work correctly with all interface methods")
    void testAllInterfaceMethods() {
        // Arrange
        String validFile = "report.pdf";
        String invalidFile = "report.html";

        // Act & Assert - Test all methods work together
        assertEquals(SubCatalogConstants.PDF_TO_HTML_FILE, validator.getFetchFieldName());
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
        assertNotNull(validator.getFetchStepMessage());

        assertTrue(validator.validate(validFile, context));
        assertFalse(validator.validate(invalidFile, context));

        assertNotNull(validator.getConfirmationStepMessage(validFile, context));
        assertNotNull(validator.getConfirmationStepMessage(invalidFile, context));
        assertNotNull(validator.getConfirmationStepMessage(null, context));

        assertNotNull(validator.getErrMessage(validFile));
        assertNotNull(validator.getErrMessage(invalidFile));
        assertNotNull(validator.getErrMessage(null));
    }

    @Test
    @DisplayName("Should handle rapid successive validations")
    void testValidate_RapidSuccessiveValidations() {
        // Arrange
        String fileName = "document.pdf";

        // Act & Assert - Validate 100 times rapidly
        for (int i = 0; i < 100; i++) {
            Boolean result = validator.validate(fileName, context);
            assertTrue(result, "Validation #" + i + " should pass");
        }
    }
}




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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PdfFileFormatValidatorTest {

    private PdfFileFormatValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new PdfFileFormatValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should validate valid .pdf file")
    void testValidate_ValidPdfFile() {
        // Act
        Boolean result = validator.validate("document.pdf", context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should validate .pdf file with uppercase extension")
    void testValidate_UppercaseExtension() {
        // Act
        Boolean result = validator.validate("DOCUMENT.PDF", context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should validate .pdf file with mixed case")
    void testValidate_MixedCaseExtension() {
        // Act
        Boolean result = validator.validate("MyDocument.Pdf", context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should reject null file name")
    void testValidate_NullFileName() {
        // Act
        Boolean result = validator.validate(null, context);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject blank file name")
    void testValidate_BlankFileName() {
        // Act
        Boolean result = validator.validate("   ", context);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject empty file name")
    void testValidate_EmptyFileName() {
        // Act
        Boolean result = validator.validate("", context);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject .docx file")
    void testValidate_DocxFile() {
        // Act
        Boolean result = validator.validate("document.docx", context);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject .txt file")
    void testValidate_TxtFile() {
        // Act
        Boolean result = validator.validate("document.txt", context);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject file without extension")
    void testValidate_NoExtension() {
        // Act
        Boolean result = validator.validate("document", context);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return correct field name")
    void testGetFetchFieldName() {
        // Act
        String fieldName = validator.getFetchFieldName();

        // Assert
        assertEquals(SubCatalogConstants.PDF_FILES, fieldName);
    }

    @Test
    @DisplayName("Should return correct field type")
    void testGetFieldType() {
        // Act
        String fieldType = validator.getFieldType();

        // Assert
        assertEquals(FieldTypes.FILE_FIELD, fieldType);
    }

    @Test
    @DisplayName("Should return fetch step message")
    void testGetFetchStepMessage() {
        // Act
        String message = validator.getFetchStepMessage();

        // Assert
        assertNotNull(message);
        assertTrue(message.contains("PDF"));
    }

    @Test
    @DisplayName("Should return confirmation step message for null file")
    void testGetConfirmationStepMessage_NullFile() {
        // Act
        String message = validator.getConfirmationStepMessage(null, context);

        // Assert
        assertNotNull(message);
        assertTrue(message.contains("No file"));
    }

    @Test
    @DisplayName("Should return confirmation step message for invalid file")
    void testGetConfirmationStepMessage_InvalidFile() {
        // Act
        String message = validator.getConfirmationStepMessage("test.docx", context);

        // Assert
        assertNotNull(message);
        assertTrue(message.contains("test.docx"));
    }

    @Test
    @DisplayName("Should return error message for null file")
    void testGetErrMessage_NullFile() {
        // Act
        String message = validator.getErrMessage(null);

        // Assert
        assertNotNull(message);
        assertTrue(message.contains("No file"));
    }

    @Test
    @DisplayName("Should return error message for invalid file")
    void testGetErrMessage_InvalidFile() {
        // Act
        String message = validator.getErrMessage("test.docx");

        // Assert
        assertNotNull(message);
        assertTrue(message.contains("test.docx"));
    }
}


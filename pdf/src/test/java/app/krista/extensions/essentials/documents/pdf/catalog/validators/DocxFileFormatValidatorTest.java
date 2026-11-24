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

class DocxFileFormatValidatorTest {

    private DocxFileFormatValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new DocxFileFormatValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should validate valid .docx file")
    void testValidate_ValidDocxFile() {
        // Act
        Boolean result = validator.validate("document.docx", context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should validate .docx file with uppercase extension")
    void testValidate_UppercaseExtension() {
        // Act
        Boolean result = validator.validate("DOCUMENT.DOCX", context);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should validate .docx file with mixed case")
    void testValidate_MixedCaseExtension() {
        // Act
        Boolean result = validator.validate("MyDocument.DocX", context);

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
    @DisplayName("Should reject .doc file")
    void testValidate_DocFile() {
        // Act
        Boolean result = validator.validate("document.doc", context);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject .pdf file")
    void testValidate_PdfFile() {
        // Act
        Boolean result = validator.validate("document.pdf", context);

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
        assertEquals(SubCatalogConstants.DOCUMENT_FILE, fieldName);
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
        assertTrue(message.contains(".docx"));
    }

    @Test
    @DisplayName("Should return confirmation step message")
    void testGetConfirmationStepMessage() {
        // Act
        String message = validator.getConfirmationStepMessage("test.pdf", context);

        // Assert
        assertNotNull(message);
        assertTrue(message.contains("test.pdf"));
    }

    @Test
    @DisplayName("Should return error message")
    void testGetErrMessage() {
        // Act
        String message = validator.getErrMessage("test.pdf");

        // Assert
        assertNotNull(message);
        assertTrue(message.contains(".docx"));
    }
}


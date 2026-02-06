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

class PdfToHtmlFileFormatValidatorTest {

    private PdfToHtmlFileFormatValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new PdfToHtmlFileFormatValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should validate valid .pdf file")
    void testValidate_ValidPdfFile() {
        assertTrue(validator.validate("document.pdf", context));
    }

    @Test
    @DisplayName("Should validate .pdf file with uppercase extension")
    void testValidate_UppercaseExtension() {
        assertTrue(validator.validate("DOCUMENT.PDF", context));
    }

    @Test
    @DisplayName("Should validate .pdf file with mixed case")
    void testValidate_MixedCaseExtension() {
        assertTrue(validator.validate("MyDocument.Pdf", context));
    }

    @Test
    @DisplayName("Should reject null file name")
    void testValidate_NullFileName() {
        assertFalse(validator.validate(null, context));
    }

    @Test
    @DisplayName("Should reject blank file name")
    void testValidate_BlankFileName() {
        assertFalse(validator.validate("   ", context));
    }

    @Test
    @DisplayName("Should reject empty file name")
    void testValidate_EmptyFileName() {
        assertFalse(validator.validate("", context));
    }

    @Test
    @DisplayName("Should reject .docx file")
    void testValidate_DocxFile() {
        assertFalse(validator.validate("document.docx", context));
    }

    @Test
    @DisplayName("Should reject .html file")
    void testValidate_HtmlFile() {
        assertFalse(validator.validate("document.html", context));
    }

    @Test
    @DisplayName("Should reject file without extension")
    void testValidate_NoExtension() {
        assertFalse(validator.validate("document", context));
    }

    @Test
    @DisplayName("Should return correct field name")
    void testGetFetchFieldName() {
        assertEquals(SubCatalogConstants.PDF_TO_HTML_FILE, validator.getFetchFieldName());
    }

    @Test
    @DisplayName("Should return correct field type")
    void testGetFieldType() {
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
    }

    @Test
    @DisplayName("Should return fetch step message")
    void testGetFetchStepMessage() {
        String message = validator.getFetchStepMessage();
        assertNotNull(message);
        assertTrue(message.contains("PDF") && message.contains("HTML"));
    }

    @Test
    @DisplayName("Should return confirmation message for null file")
    void testGetConfirmationStepMessage_NullFile() {
        String message = validator.getConfirmationStepMessage(null, context);
        assertNotNull(message);
        assertTrue(message.contains("No file"));
    }

    @Test
    @DisplayName("Should return confirmation message for invalid file")
    void testGetConfirmationStepMessage_InvalidFile() {
        String message = validator.getConfirmationStepMessage("test.docx", context);
        assertNotNull(message);
        assertTrue(message.contains("test.docx"));
    }

    @Test
    @DisplayName("Should return error message for null file")
    void testGetErrMessage_NullFile() {
        String message = validator.getErrMessage(null);
        assertNotNull(message);
        assertTrue(message.contains("No file"));
    }

    @Test
    @DisplayName("Should return error message for invalid file")
    void testGetErrMessage_InvalidFile() {
        String message = validator.getErrMessage("test.docx");
        assertNotNull(message);
        assertTrue(message.contains("test.docx"));
    }
}


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

/**
 * Unit tests for SplitPdfAtPageNumberFileFormatValidator.
 * Covers valid/invalid PDF file name formats and the static metadata methods.
 */
class SplitPdfAtPageNumberFileFormatValidatorTest {

    private SplitPdfAtPageNumberFileFormatValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new SplitPdfAtPageNumberFileFormatValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should validate a lowercase .pdf file")
    void testValidate_ValidPdfFile() {
        assertTrue(validator.validate("document.pdf", context));
    }

    @Test
    @DisplayName("Should validate a .pdf file with uppercase extension")
    void testValidate_UppercaseExtension() {
        assertTrue(validator.validate("DOCUMENT.PDF", context));
    }

    @Test
    @DisplayName("Should validate a .pdf file with mixed case extension")
    void testValidate_MixedCaseExtension() {
        assertTrue(validator.validate("MyDocument.Pdf", context));
    }

    @Test
    @DisplayName("Should validate a filename that is just the extension")
    void testValidate_FilenameIsJustExtension() {
        assertTrue(validator.validate(".pdf", context));
    }

    @Test
    @DisplayName("Should reject a null file name")
    void testValidate_NullFileName() {
        assertFalse(validator.validate(null, context));
    }

    @Test
    @DisplayName("Should reject a blank (whitespace-only) file name")
    void testValidate_BlankFileName() {
        assertFalse(validator.validate("   ", context));
    }

    @Test
    @DisplayName("Should reject an empty file name")
    void testValidate_EmptyFileName() {
        assertFalse(validator.validate("", context));
    }

    @Test
    @DisplayName("Should reject a .docx file")
    void testValidate_DocxFile() {
        assertFalse(validator.validate("document.docx", context));
    }

    @Test
    @DisplayName("Should reject a .txt file")
    void testValidate_TxtFile() {
        assertFalse(validator.validate("document.txt", context));
    }

    @Test
    @DisplayName("Should reject a .png file")
    void testValidate_PngFile() {
        assertFalse(validator.validate("image.png", context));
    }

    @Test
    @DisplayName("Should reject a file name without any extension")
    void testValidate_NoExtension() {
        assertFalse(validator.validate("document", context));
    }

    @Test
    @DisplayName("Should return the correct fetch field name")
    void testGetFetchFieldName() {
        assertEquals(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE, validator.getFetchFieldName());
    }

    @Test
    @DisplayName("Should return the correct field type")
    void testGetFieldType() {
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
    }

    @Test
    @DisplayName("Should return a non-null fetch step message mentioning PDF")
    void testGetFetchStepMessage() {
        String message = validator.getFetchStepMessage();
        assertNotNull(message);
        assertTrue(message.contains("PDF"));
    }

    @Test
    @DisplayName("Should return a confirmation step message containing the resource id")
    void testGetConfirmationStepMessage() {
        String message = validator.getConfirmationStepMessage("test.docx", context);
        assertNotNull(message);
        assertTrue(message.contains("test.docx"));
    }

    @Test
    @DisplayName("Should return a non-null error message mentioning PDF")
    void testGetErrMessage() {
        String message = validator.getErrMessage("test.docx");
        assertNotNull(message);
        assertTrue(message.contains("PDF"));
    }
}

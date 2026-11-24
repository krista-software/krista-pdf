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

class PdfFileCountValidatorTest {

    private PdfFileCountValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new PdfFileCountValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should validate when file count is 2")
    void testValidate_TwoFiles() {
        assertTrue(validator.validate("FILE_COUNT:2", context));
    }

    @Test
    @DisplayName("Should validate when file count is greater than 2")
    void testValidate_MoreThanTwoFiles() {
        assertTrue(validator.validate("FILE_COUNT:5", context));
        assertTrue(validator.validate("FILE_COUNT:10", context));
    }

    @Test
    @DisplayName("Should reject when file count is 0")
    void testValidate_ZeroFiles() {
        assertFalse(validator.validate("FILE_COUNT:0", context));
    }

    @Test
    @DisplayName("Should reject when file count is 1")
    void testValidate_OneFile() {
        assertFalse(validator.validate("FILE_COUNT:1", context));
    }

    @Test
    @DisplayName("Should validate when no file count information provided")
    void testValidate_NoFileCountInfo() {
        assertTrue(validator.validate("some-file.pdf", context));
        assertTrue(validator.validate(null, context));
    }

    @Test
    @DisplayName("Should reject invalid file count format")
    void testValidate_InvalidFormat() {
        assertFalse(validator.validate("FILE_COUNT:abc", context));
        assertFalse(validator.validate("FILE_COUNT:", context));
    }

    @Test
    @DisplayName("Should return correct field name")
    void testGetFetchFieldName() {
        assertEquals(SubCatalogConstants.PDF_FILES, validator.getFetchFieldName());
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
        assertTrue(message.contains("two"));
    }

    @Test
    @DisplayName("Should return confirmation message for zero files")
    void testGetConfirmationStepMessage_ZeroFiles() {
        String message = validator.getConfirmationStepMessage("FILE_COUNT:0", context);
        assertNotNull(message);
        assertTrue(message.contains("No PDF files"));
    }

    @Test
    @DisplayName("Should return confirmation message for one file")
    void testGetConfirmationStepMessage_OneFile() {
        String message = validator.getConfirmationStepMessage("FILE_COUNT:1", context);
        assertNotNull(message);
        assertTrue(message.contains("Only one"));
    }

    @Test
    @DisplayName("Should return confirmation message for generic case")
    void testGetConfirmationStepMessage_Generic() {
        String message = validator.getConfirmationStepMessage("FILE_COUNT:5", context);
        assertNotNull(message);
    }

    @Test
    @DisplayName("Should return error message for zero files")
    void testGetErrMessage_ZeroFiles() {
        String message = validator.getErrMessage("FILE_COUNT:0");
        assertNotNull(message);
        assertTrue(message.contains("No PDF files"));
    }

    @Test
    @DisplayName("Should return error message for one file")
    void testGetErrMessage_OneFile() {
        String message = validator.getErrMessage("FILE_COUNT:1");
        assertNotNull(message);
        assertTrue(message.contains("Only one"));
    }

    @Test
    @DisplayName("Should return error message for generic case")
    void testGetErrMessage_Generic() {
        String message = validator.getErrMessage("FILE_COUNT:5");
        assertNotNull(message);
    }
}


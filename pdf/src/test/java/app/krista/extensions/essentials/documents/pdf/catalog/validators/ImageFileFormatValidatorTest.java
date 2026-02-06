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

class ImageFileFormatValidatorTest {

    private ImageFileFormatValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new ImageFileFormatValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should validate valid .png file")
    void testValidate_ValidPngFile() {
        assertTrue(validator.validate("image.png", context));
    }

    @Test
    @DisplayName("Should validate valid .jpg file")
    void testValidate_ValidJpgFile() {
        assertTrue(validator.validate("image.jpg", context));
    }

    @Test
    @DisplayName("Should validate valid .jpeg file")
    void testValidate_ValidJpegFile() {
        assertTrue(validator.validate("image.jpeg", context));
    }

    @Test
    @DisplayName("Should validate image with uppercase extension")
    void testValidate_UppercaseExtension() {
        assertTrue(validator.validate("IMAGE.PNG", context));
        assertTrue(validator.validate("IMAGE.JPG", context));
        assertTrue(validator.validate("IMAGE.JPEG", context));
    }

    @Test
    @DisplayName("Should validate image with mixed case extension")
    void testValidate_MixedCaseExtension() {
        assertTrue(validator.validate("MyImage.Png", context));
        assertTrue(validator.validate("MyImage.JpG", context));
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
    @DisplayName("Should reject .pdf file")
    void testValidate_PdfFile() {
        assertFalse(validator.validate("document.pdf", context));
    }

    @Test
    @DisplayName("Should reject .docx file")
    void testValidate_DocxFile() {
        assertFalse(validator.validate("document.docx", context));
    }

    @Test
    @DisplayName("Should reject .gif file")
    void testValidate_GifFile() {
        assertFalse(validator.validate("image.gif", context));
    }

    @Test
    @DisplayName("Should reject .bmp file")
    void testValidate_BmpFile() {
        assertFalse(validator.validate("image.bmp", context));
    }

    @Test
    @DisplayName("Should reject file without extension")
    void testValidate_NoExtension() {
        assertFalse(validator.validate("image", context));
    }

    @Test
    @DisplayName("Should return correct field name")
    void testGetFetchFieldName() {
        assertEquals(SubCatalogConstants.DOCUMENT_FILE, validator.getFetchFieldName());
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
        assertTrue(message.contains("PNG") || message.contains("JPG") || message.contains("JPEG"));
    }

    @Test
    @DisplayName("Should return confirmation step message")
    void testGetConfirmationStepMessage() {
        String message = validator.getConfirmationStepMessage("test.pdf", context);
        assertNotNull(message);
        assertTrue(message.contains("test.pdf"));
    }

    @Test
    @DisplayName("Should return error message")
    void testGetErrMessage() {
        String message = validator.getErrMessage("test.pdf");
        assertNotNull(message);
        assertTrue(message.contains("PNG") || message.contains("JPG") || message.contains("JPEG"));
    }
}


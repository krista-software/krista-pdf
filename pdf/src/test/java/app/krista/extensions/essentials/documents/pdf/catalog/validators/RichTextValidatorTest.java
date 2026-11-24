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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RichTextValidatorTest {

    private RichTextValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new RichTextValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should validate valid rich text content")
    void testValidate_ValidRichText() {
        assertTrue(validator.validate("This is valid content", context));
    }

    @Test
    @DisplayName("Should validate rich text with HTML tags")
    void testValidate_RichTextWithHtml() {
        assertTrue(validator.validate("<p>This is valid content</p>", context));
    }

    @Test
    @DisplayName("Should validate long rich text content")
    void testValidate_LongContent() {
        assertTrue(validator.validate("This is a very long piece of rich text content that should definitely pass validation", context));
    }

    @Test
    @DisplayName("Should reject null content")
    void testValidate_NullContent() {
        assertFalse(validator.validate(null, context));
    }

    @Test
    @DisplayName("Should reject blank content")
    void testValidate_BlankContent() {
        assertFalse(validator.validate("   ", context));
    }

    @Test
    @DisplayName("Should reject empty content")
    void testValidate_EmptyContent() {
        assertFalse(validator.validate("", context));
    }

    @Test
    @DisplayName("Should reject content that is too short")
    void testValidate_TooShort() {
        assertFalse(validator.validate("Hi", context));
    }

    @Test
    @DisplayName("Should reject HTML with insufficient text content")
    void testValidate_HtmlWithShortContent() {
        assertFalse(validator.validate("<p>Hi</p>", context));
    }

    @Test
    @DisplayName("Should validate content with exactly minimum length")
    void testValidate_MinimumLength() {
        assertTrue(validator.validate("Hello", context));
    }

    @Test
    @DisplayName("Should reject only HTML tags without content")
    void testValidate_OnlyHtmlTags() {
        assertFalse(validator.validate("<p></p><div></div>", context));
    }

    @Test
    @DisplayName("Should return correct field name")
    void testGetFetchFieldName() {
        String fieldName = validator.getFetchFieldName();
        assertNotNull(fieldName);
        assertTrue(fieldName.contains("Rich Text"));
    }

    @Test
    @DisplayName("Should return correct field type")
    void testGetFieldType() {
        assertEquals(FieldTypes.RICH_TEXT_FIELD, validator.getFieldType());
    }

    @Test
    @DisplayName("Should return fetch step message")
    void testGetFetchStepMessage() {
        String message = validator.getFetchStepMessage();
        assertNotNull(message);
        assertTrue(message.contains("5"));
    }

    @Test
    @DisplayName("Should return confirmation step message")
    void testGetConfirmationStepMessage() {
        String message = validator.getConfirmationStepMessage("", context);
        assertNotNull(message);
        assertTrue(message.contains("5"));
    }

    @Test
    @DisplayName("Should return error message")
    void testGetErrMessage() {
        String message = validator.getErrMessage("");
        assertNotNull(message);
        assertTrue(message.contains("5"));
    }
}


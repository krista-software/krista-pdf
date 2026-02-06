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

class BaseValidatorTest {

    private TestValidator validator;
    private Map<Validator.ValidationResource, String> context;

    // Concrete implementation for testing
    private static class TestValidator extends BaseValidator {
        public TestValidator(String fieldName, String fieldType) {
            super(fieldName, fieldType);
        }

        public TestValidator(String fieldName) {
            super(fieldName);
        }

        @Override
        public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
            return resourceId != null && !resourceId.isBlank();
        }

        // Expose protected method for testing
        public boolean testLogExceptionAndReturnFalse(Exception e) {
            return logExceptionAndReturnFalse(e);
        }
    }

    @BeforeEach
    void setUp() {
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should create validator with field name and type")
    void testConstructor_WithFieldNameAndType() {
        validator = new TestValidator("testField", FieldTypes.FILE_FIELD);

        assertEquals("testField", validator.getFetchFieldName());
        assertEquals(FieldTypes.FILE_FIELD, validator.getFieldType());
    }

    @Test
    @DisplayName("Should create validator with field name only (defaults to TEXT_FIELD)")
    void testConstructor_WithFieldNameOnly() {
        validator = new TestValidator("testField");

        assertEquals("testField", validator.getFetchFieldName());
        assertEquals(FieldTypes.TEXT_FIELD, validator.getFieldType());
    }

    @Test
    @DisplayName("Should return correct fetch field name")
    void testGetFetchFieldName() {
        validator = new TestValidator("myField", FieldTypes.RICH_TEXT_FIELD);

        assertEquals("myField", validator.getFetchFieldName());
    }

    @Test
    @DisplayName("Should return correct field type")
    void testGetFieldType() {
        validator = new TestValidator("myField", FieldTypes.NUMBER_FIELD);

        assertEquals(FieldTypes.NUMBER_FIELD, validator.getFieldType());
    }

    @Test
    @DisplayName("Should return fetch step message with field name")
    void testGetFetchStepMessage() {
        validator = new TestValidator("document file");

        String message = validator.getFetchStepMessage();

        assertNotNull(message);
        assertTrue(message.contains("document file"));
        assertTrue(message.contains("valid"));
    }

    @Test
    @DisplayName("Should return confirmation step message with resource ID")
    void testGetConfirmationStepMessage() {
        validator = new TestValidator("file name");

        String message = validator.getConfirmationStepMessage("test.pdf", context);

        assertNotNull(message);
        assertTrue(message.contains("file name"));
        assertTrue(message.contains("test.pdf"));
        assertTrue(message.contains("invalid"));
    }

    @Test
    @DisplayName("Should return error message with resource ID")
    void testGetErrMessage() {
        validator = new TestValidator("document");

        String message = validator.getErrMessage("invalid.txt");

        assertNotNull(message);
        assertTrue(message.contains("Invalid"));
        assertTrue(message.contains("document"));
        assertTrue(message.contains("invalid.txt"));
    }

    @Test
    @DisplayName("Should log exception and return false")
    void testLogExceptionAndReturnFalse() {
        validator = new TestValidator("testField");
        Exception testException = new RuntimeException("Test exception");

        boolean result = validator.testLogExceptionAndReturnFalse(testException);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should validate non-null and non-blank resource ID")
    void testValidate_ValidResourceId() {
        validator = new TestValidator("testField");

        assertTrue(validator.validate("valid-resource", context));
    }

    @Test
    @DisplayName("Should reject null resource ID")
    void testValidate_NullResourceId() {
        validator = new TestValidator("testField");

        assertFalse(validator.validate(null, context));
    }

    @Test
    @DisplayName("Should reject blank resource ID")
    void testValidate_BlankResourceId() {
        validator = new TestValidator("testField");

        assertFalse(validator.validate("   ", context));
    }
}


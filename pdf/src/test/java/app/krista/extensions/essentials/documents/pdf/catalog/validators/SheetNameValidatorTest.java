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
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SheetNameValidatorTest {

    @Mock
    private Workbook workbook;

    private SheetNameValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new SheetNameValidator(workbook);
        context = new HashMap<>();
    }

    @Test
    @DisplayName("Should validate null sheet name (use first sheet)")
    void testValidate_NullSheetName() {
        assertTrue(validator.validate(null, context));
    }

    @Test
    @DisplayName("Should validate blank sheet name (use first sheet)")
    void testValidate_BlankSheetName() {
        assertTrue(validator.validate("   ", context));
    }

    @Test
    @DisplayName("Should validate empty sheet name (use first sheet)")
    void testValidate_EmptySheetName() {
        assertTrue(validator.validate("", context));
    }

    @Test
    @DisplayName("Should validate existing sheet name")
    void testValidate_ExistingSheet() {
        when(workbook.getSheetIndex("Sheet1")).thenReturn(0);

        assertTrue(validator.validate("Sheet1", context));
        verify(workbook).getSheetIndex("Sheet1");
    }

    @Test
    @DisplayName("Should reject non-existing sheet name")
    void testValidate_NonExistingSheet() {
        when(workbook.getSheetIndex("NonExistent")).thenReturn(-1);

        assertFalse(validator.validate("NonExistent", context));
        verify(workbook).getSheetIndex("NonExistent");
    }

    @Test
    @DisplayName("Should reject sheet name when workbook is null")
    void testValidate_NullWorkbook() {
        SheetNameValidator validatorWithNullWorkbook = new SheetNameValidator(null);

        assertFalse(validatorWithNullWorkbook.validate("Sheet1", context));
    }

    @Test
    @DisplayName("Should validate when workbook is null but sheet name is blank")
    void testValidate_NullWorkbookBlankSheet() {
        SheetNameValidator validatorWithNullWorkbook = new SheetNameValidator(null);

        assertTrue(validatorWithNullWorkbook.validate("", context));
    }

    @Test
    @DisplayName("Should return correct field name")
    void testGetFetchFieldName() {
        assertEquals(SubCatalogConstants.SHEET_NAME, validator.getFetchFieldName());
    }

    @Test
    @DisplayName("Should return correct field type")
    void testGetFieldType() {
        assertEquals(FieldTypes.TEXT_FIELD, validator.getFieldType());
    }

    @Test
    @DisplayName("Should return fetch step message")
    void testGetFetchStepMessage() {
        String message = validator.getFetchStepMessage();
        assertNotNull(message);
        assertTrue(message.contains("sheet"));
    }

    @Test
    @DisplayName("Should return confirmation message for null sheet name")
    void testGetConfirmationStepMessage_NullSheet() {
        String message = validator.getConfirmationStepMessage(null, context);
        assertNotNull(message);
        assertTrue(message.contains("first sheet"));
    }

    @Test
    @DisplayName("Should return confirmation message for invalid sheet name")
    void testGetConfirmationStepMessage_InvalidSheet() {
        String message = validator.getConfirmationStepMessage("InvalidSheet", context);
        assertNotNull(message);
        assertTrue(message.contains("InvalidSheet"));
    }

    @Test
    @DisplayName("Should return error message for null sheet name")
    void testGetErrMessage_NullSheet() {
        String message = validator.getErrMessage(null);
        assertNotNull(message);
        assertTrue(message.contains("first sheet"));
    }

    @Test
    @DisplayName("Should return error message for invalid sheet name")
    void testGetErrMessage_InvalidSheet() {
        String message = validator.getErrMessage("InvalidSheet");
        assertNotNull(message);
        assertTrue(message.contains("InvalidSheet"));
    }
}


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

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary branch-coverage tests for {@link SheetNameValidator}.
 *
 * <p>{@code SheetNameValidatorTest} already drives every branch in
 * {@code isSheetNamePresent} (null/blank -&gt; true, null workbook -&gt; false,
 * unknown sheet -&gt; false, known sheet -&gt; true) using a mocked {@link Workbook}.
 * This class re-confirms the same branches against a real, in-memory
 * {@link XSSFWorkbook} instance (no mocking) to guard against the validator
 * depending on any Mockito-specific stubbing behavior, and adds a whitespace-only
 * sheet-name case.</p>
 */
class SheetNameValidatorBranchCoverageTest {

    private final Map<Validator.ValidationResource, String> context = new HashMap<>();

    @Test
    @DisplayName("validate: tab/newline-only sheet name is treated as blank and passes (uses first sheet)")
    void testValidate_WhitespaceOnlySheetName() {
        SheetNameValidator validator = new SheetNameValidator(null);
        assertTrue(validator.validate("\t\n", context));
    }

    @Test
    @DisplayName("validate: real workbook with a matching sheet name passes")
    void testValidate_RealWorkbook_ExistingSheet() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Data");
            SheetNameValidator validator = new SheetNameValidator(workbook);

            assertTrue(validator.validate("Data", context));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("validate: real workbook without a matching sheet name fails")
    void testValidate_RealWorkbook_NonExistingSheet() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Data");
            SheetNameValidator validator = new SheetNameValidator(workbook);

            assertFalse(validator.validate("DoesNotExist", context));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("getConfirmationStepMessage: whitespace-only sheet name is treated as blank")
    void testGetConfirmationStepMessage_WhitespaceOnly() {
        SheetNameValidator validator = new SheetNameValidator(null);
        String message = validator.getConfirmationStepMessage(" \t ", context);
        assertTrue(message.contains("first sheet"));
    }

    @Test
    @DisplayName("getErrMessage: whitespace-only sheet name is treated as blank")
    void testGetErrMessage_WhitespaceOnly() {
        SheetNameValidator validator = new SheetNameValidator(null);
        String message = validator.getErrMessage(" \t ");
        assertTrue(message.contains("first sheet"));
    }
}

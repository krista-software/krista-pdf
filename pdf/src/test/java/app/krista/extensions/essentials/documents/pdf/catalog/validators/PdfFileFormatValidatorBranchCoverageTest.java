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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary branch-coverage tests for {@link PdfFileFormatValidator}.
 *
 * <p>{@code PdfFileFormatValidatorTest} already drives both decision points in
 * {@code validate()} (the {@code resourceId == null || resourceId.isBlank()} guard,
 * and the {@code !isPdfFile} check) through every combination. This class adds a few
 * independent combinations of the same two guards to confirm both operands of the
 * compound null/blank check are each independently exercised (null vs. non-null-but-
 * blank), rounding out the 2-branch total for this validator.</p>
 */
class PdfFileFormatValidatorBranchCoverageTest {

    private PdfFileFormatValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new PdfFileFormatValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("validate: tab/newline-only resourceId is blank and rejected (second operand of the OR)")
    void testValidate_WhitespaceOnlyResourceId() {
        assertFalse(validator.validate("\t\n  ", context));
    }

    @Test
    @DisplayName("validate: null resourceId short-circuits the OR on its first operand and is rejected")
    void testValidate_NullResourceId() {
        assertFalse(validator.validate(null, context));
    }

    @Test
    @DisplayName("validate: a valid .pdf path with directories still passes the endsWith check")
    void testValidate_PathWithDirectories() {
        assertTrue(validator.validate("/some/nested/path/report.pdf", context));
    }

    @Test
    @DisplayName("getConfirmationStepMessage: whitespace-only resourceId is treated as blank")
    void testGetConfirmationStepMessage_WhitespaceOnly() {
        String message = validator.getConfirmationStepMessage("   \t", context);
        assertTrue(message.contains("No file"));
    }

    @Test
    @DisplayName("getErrMessage: whitespace-only resourceId is treated as blank")
    void testGetErrMessage_WhitespaceOnly() {
        String message = validator.getErrMessage("   \t");
        assertTrue(message.contains("No file"));
    }
}

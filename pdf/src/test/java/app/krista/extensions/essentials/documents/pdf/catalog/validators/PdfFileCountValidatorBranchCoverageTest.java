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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Supplementary branch-coverage tests for {@link PdfFileCountValidator}.
 *
 * <p>{@code PdfFileCountValidatorTest} already covers {@code validate()} thoroughly
 * (valid counts, 0/1 files, no-prefix, invalid number format). This class targets the
 * gaps left in the private {@code extractFileCount(String)} helper, which is only
 * reachable through {@code getConfirmationStepMessage} / {@code getErrMessage} and has
 * its own try/catch (separate bytecode from {@code validate()}'s try/catch), plus the
 * outer "fileCount>=0 && fileCount<2" guard's false outcome via those two methods:</p>
 * <ul>
 *   <li>resourceId == null (startsWith short-circuits to false -&gt; fileCount=-1)</li>
 *   <li>resourceId without the FILE_COUNT: prefix (-&gt; fileCount=-1)</li>
 *   <li>resourceId with an unparsable number after the prefix (NumberFormatException
 *       caught inside extractFileCount -&gt; fileCount=-1)</li>
 *   <li>fileCount that is valid but &gt;=2 (outer guard false via the "normal" path)</li>
 * </ul>
 */
class PdfFileCountValidatorBranchCoverageTest {

    private PdfFileCountValidator validator;
    private Map<Validator.ValidationResource, String> context;

    @BeforeEach
    void setUp() {
        validator = new PdfFileCountValidator();
        context = new HashMap<>();
    }

    @Test
    @DisplayName("getConfirmationStepMessage: null resourceId falls back to the generic message")
    void testGetConfirmationStepMessage_NullResourceId() {
        String message = validator.getConfirmationStepMessage(null, context);
        assertEquals("Please upload at least two PDF files to merge.", message);
    }

    @Test
    @DisplayName("getConfirmationStepMessage: resourceId without FILE_COUNT prefix falls back to the generic message")
    void testGetConfirmationStepMessage_NoPrefix() {
        String message = validator.getConfirmationStepMessage("some-file.pdf", context);
        assertEquals("Please upload at least two PDF files to merge.", message);
    }

    @Test
    @DisplayName("getConfirmationStepMessage: non-numeric FILE_COUNT triggers extractFileCount's own caught NumberFormatException")
    void testGetConfirmationStepMessage_InvalidNumberFormat() {
        String message = validator.getConfirmationStepMessage("FILE_COUNT:notanumber", context);
        assertEquals("Please upload at least two PDF files to merge.", message);
    }

    @Test
    @DisplayName("getConfirmationStepMessage: count exactly at the minimum threshold falls back to the generic message")
    void testGetConfirmationStepMessage_ExactlyTwoFiles() {
        String message = validator.getConfirmationStepMessage("FILE_COUNT:2", context);
        assertEquals("Please upload at least two PDF files to merge.", message);
    }

    @Test
    @DisplayName("getErrMessage: null resourceId falls back to the generic message")
    void testGetErrMessage_NullResourceId() {
        String message = validator.getErrMessage(null);
        assertEquals("Insufficient PDF files provided. At least two PDF files are required for merging.", message);
    }

    @Test
    @DisplayName("getErrMessage: resourceId without FILE_COUNT prefix falls back to the generic message")
    void testGetErrMessage_NoPrefix() {
        String message = validator.getErrMessage("random.pdf");
        assertEquals("Insufficient PDF files provided. At least two PDF files are required for merging.", message);
    }

    @Test
    @DisplayName("getErrMessage: non-numeric FILE_COUNT triggers extractFileCount's own caught NumberFormatException")
    void testGetErrMessage_InvalidNumberFormat() {
        String message = validator.getErrMessage("FILE_COUNT:oops");
        assertEquals("Insufficient PDF files provided. At least two PDF files are required for merging.", message);
    }

    @Test
    @DisplayName("getErrMessage: count exactly at the minimum threshold falls back to the generic message")
    void testGetErrMessage_ExactlyTwoFiles() {
        String message = validator.getErrMessage("FILE_COUNT:2");
        assertEquals("Insufficient PDF files provided. At least two PDF files are required for merging.", message);
    }

    @Test
    @DisplayName("validate: negative count parses successfully and is rejected as below the minimum")
    void testValidate_NegativeCountStillParsesAndFails() {
        assertFalse(validator.validate("FILE_COUNT:-1", context));
    }
}

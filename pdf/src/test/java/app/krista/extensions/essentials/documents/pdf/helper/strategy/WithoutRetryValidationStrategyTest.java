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

package app.krista.extensions.essentials.documents.pdf.helper.strategy;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for WithoutRetryValidationStrategy.
 * Covers both the validate() branches (missing vs. present split page number) and the
 * direct-construction FAILURE ExtensionResponse built by handleValidationFailure().
 */
@DisplayName("WithoutRetryValidationStrategy Unit Tests")
class WithoutRetryValidationStrategyTest {

    private WithoutRetryValidationStrategy strategy;
    private File inputFile;

    @BeforeEach
    void setUp() {
        strategy = new WithoutRetryValidationStrategy();
        inputFile = mock(File.class);
    }

    // ==================== validate() tests ====================

    @Test
    @DisplayName("Should fail validation when split page number is null")
    void testValidate_NullSplitPageNumber() {
        ValidationStrategy.ValidationResult result = strategy.validate(inputFile, null);

        assertFalse(result.isValid());
        assertEquals("Split page number is required", result.getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getExceptionType());
        assertNull(result.getAdditionalData());
    }

    @Test
    @DisplayName("Should succeed validation when split page number is provided")
    void testValidate_ValidSplitPageNumber() {
        ValidationStrategy.ValidationResult result = strategy.validate(inputFile, 3.0);

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        assertNull(result.getExceptionType());
    }

    @Test
    @DisplayName("Should succeed validation even when input file is null, as long as page number is present")
    void testValidate_NullInputFile_ValidPageNumber() {
        ValidationStrategy.ValidationResult result = strategy.validate(null, 2.0);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should fail validation when both input file and split page number are null")
    void testValidate_NullInputFileAndNullPageNumber() {
        ValidationStrategy.ValidationResult result = strategy.validate(null, null);

        assertFalse(result.isValid());
        assertEquals("Split page number is required", result.getErrorMessage());
    }

    // ==================== handleValidationFailure() tests ====================

    @Test
    @DisplayName("Should return a direct FAILURE ExtensionResponse without remediation actions")
    void testHandleValidationFailure_ReturnsDirectFailureResponse() {
        ValidationStrategy.ValidationResult failureResult = ValidationStrategy.ValidationResult.failure(
                "Split page number is required", ExtensionResponse.Error.ExceptionType.INPUT_ERROR);

        ExtensionResponse response = strategy.handleValidationFailure(failureResult, inputFile, null);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals("Split page number is required", response.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertNull(response.getRemediationActions());
        assertNull(response.getState());
        assertNull(response.getResponseValue());
    }

    @Test
    @DisplayName("Should propagate a different exception type and message through to the response")
    void testHandleValidationFailure_PropagatesExceptionTypeAndMessage() {
        ValidationStrategy.ValidationResult failureResult = ValidationStrategy.ValidationResult.failure(
                "custom error", ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR);

        ExtensionResponse response = strategy.handleValidationFailure(failureResult, null, null);

        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());
        assertEquals("custom error", response.getError().getErrorMessage());
    }

    @Test
    @DisplayName("Should derive the FAILURE response directly from the real validate() output")
    void testHandleValidationFailure_UsingRealValidateOutput() {
        ValidationStrategy.ValidationResult validationResult = strategy.validate(inputFile, null);
        assertFalse(validationResult.isValid());

        ExtensionResponse response = strategy.handleValidationFailure(validationResult, inputFile, null);

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals("Split page number is required", response.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
    }
}

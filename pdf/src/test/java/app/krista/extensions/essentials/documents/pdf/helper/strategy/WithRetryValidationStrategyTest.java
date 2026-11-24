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
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WithRetryValidationStrategy.
 * Covers every validate() branch (missing file, invalid file format via the orchestrator,
 * missing/too-small/valid split page number) and both handleValidationFailure() branches
 * (orchestrator-provided validation results vs. the synthesized single-result fallback),
 * including the conditional inclusion of the input file / split page number in the state map.
 */
@DisplayName("WithRetryValidationStrategy Unit Tests")
class WithRetryValidationStrategyTest {

    @Mock
    private ValidationOrchestrator validationOrchestrator;

    @Mock
    private ExtensionResponseGenerator responseGenerator;

    @Mock
    private File inputFile;

    private WithRetryValidationStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new WithRetryValidationStrategy(validationOrchestrator, responseGenerator);
    }

    // ==================== validate() tests ====================

    @Test
    @DisplayName("Should fail validation when input file is null, without consulting the orchestrator")
    void testValidate_NullInputFile() {
        WithRetryValidationStrategy.ValidationResult result = strategy.validate(null, 3.0);

        assertFalse(result.isValid());
        assertEquals("No PDF file provided", result.getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getExceptionType());
        assertNull(result.getAdditionalData());
        verifyNoInteractions(validationOrchestrator);
    }

    @Test
    @DisplayName("Should fail validation and expose orchestrator results when file format is invalid")
    void testValidate_InvalidFileFormat() {
        when(inputFile.getFileName()).thenReturn("document.docx");
        ValidationOrchestrator.ValidationResult orchestratorResult = new ValidationOrchestrator.ValidationResult(
                "confirm", "field", "fetch", "err", "File");
        List<ValidationOrchestrator.ValidationResult> results = List.of(orchestratorResult);
        when(validationOrchestrator.validate(anyMap())).thenReturn(results);

        WithRetryValidationStrategy.ValidationResult result = strategy.validate(inputFile, 3.0);

        assertFalse(result.isValid());
        assertEquals("PDF format validation failed", result.getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getExceptionType());
        assertSame(results, result.getAdditionalData());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Validator.ValidationResource, String>> contextCaptor = ArgumentCaptor.forClass(Map.class);
        verify(validationOrchestrator).validate(contextCaptor.capture());
        assertEquals("document.docx",
                contextCaptor.getValue().get(Validator.ValidationResource.SPLIT_PDF_AT_PAGE_NUMBER_FILE_FORMAT));
    }

    @Test
    @DisplayName("Should fail validation when split page number is null but file format is valid")
    void testValidate_NullSplitPageNumber() {
        when(inputFile.getFileName()).thenReturn("document.pdf");
        when(validationOrchestrator.validate(anyMap())).thenReturn(new ArrayList<>());

        WithRetryValidationStrategy.ValidationResult result = strategy.validate(inputFile, null);

        assertFalse(result.isValid());
        assertEquals("Split page number is required", result.getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getExceptionType());
    }

    @Test
    @DisplayName("Should fail validation when split page number is less than 2")
    void testValidate_SplitPageNumberLessThanTwo() {
        when(inputFile.getFileName()).thenReturn("document.pdf");
        when(validationOrchestrator.validate(anyMap())).thenReturn(new ArrayList<>());

        WithRetryValidationStrategy.ValidationResult result = strategy.validate(inputFile, 1.0);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("at least 2"));
        assertTrue(result.getErrorMessage().contains("1"));
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, result.getExceptionType());
    }

    @Test
    @DisplayName("Should succeed validation at the page-number-2 boundary")
    void testValidate_SplitPageNumberExactlyTwo_Succeeds() {
        when(inputFile.getFileName()).thenReturn("document.pdf");
        when(validationOrchestrator.validate(anyMap())).thenReturn(new ArrayList<>());

        WithRetryValidationStrategy.ValidationResult result = strategy.validate(inputFile, 2.0);

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should succeed validation with a valid file and a page number greater than 2")
    void testValidate_ValidInputs_Succeeds() {
        when(inputFile.getFileName()).thenReturn("document.pdf");
        when(validationOrchestrator.validate(anyMap())).thenReturn(new ArrayList<>());

        WithRetryValidationStrategy.ValidationResult result = strategy.validate(inputFile, 5.0);

        assertTrue(result.isValid());
    }

    // ==================== handleValidationFailure() tests ====================

    @Test
    @DisplayName("Should use the orchestrator's validation results when additionalData is a List")
    void testHandleValidationFailure_WithListAdditionalData() {
        when(inputFile.getFileName()).thenReturn("document.docx");
        ValidationOrchestrator.ValidationResult orchestratorResult = new ValidationOrchestrator.ValidationResult(
                "confirm", "field", "fetch", "err", "File");
        List<ValidationOrchestrator.ValidationResult> results = List.of(orchestratorResult);
        when(validationOrchestrator.validate(anyMap())).thenReturn(results);

        WithRetryValidationStrategy.ValidationResult validationResult = strategy.validate(inputFile, 5.0);
        assertFalse(validationResult.isValid());

        ExtensionResponse mockResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), anyList(), anyString(), anyMap()))
                .thenReturn(mockResponse);

        ExtensionResponse response = strategy.handleValidationFailure(validationResult, inputFile, 5.0);

        assertSame(mockResponse, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ValidationOrchestrator.ValidationResult>> listCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(responseGenerator).generateConfirmationResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                listCaptor.capture(),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_AT_PAGE_NUMBER_VALIDATION),
                stateCaptor.capture());

        assertSame(results, listCaptor.getValue());

        Map<String, Object> state = stateCaptor.getValue();
        assertTrue(state.containsKey(SubCatalogConstants.STATE_ID));
        assertEquals(List.of(inputFile), state.get(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE));
        assertEquals(5.0, state.get(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_SPLIT_PAGE));
    }

    @Test
    @DisplayName("Should synthesize a single validation result when additionalData is not a List")
    void testHandleValidationFailure_WithoutListAdditionalData() {
        WithRetryValidationStrategy.ValidationResult validationResult = strategy.validate(null, 5.0);
        assertFalse(validationResult.isValid());
        assertNull(validationResult.getAdditionalData());

        ExtensionResponse mockResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), anyList(), anyString(), anyMap()))
                .thenReturn(mockResponse);

        ExtensionResponse response = strategy.handleValidationFailure(validationResult, null, null);

        assertSame(mockResponse, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ValidationOrchestrator.ValidationResult>> listCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(responseGenerator).generateConfirmationResponse(
                any(), listCaptor.capture(), anyString(), stateCaptor.capture());

        List<ValidationOrchestrator.ValidationResult> capturedList = listCaptor.getValue();
        assertEquals(1, capturedList.size());
        assertEquals("No PDF file provided", capturedList.get(0).getErrMessage());
        assertEquals(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE, capturedList.get(0).getFetchFieldName());

        Map<String, Object> state = stateCaptor.getValue();
        assertTrue(state.containsKey(SubCatalogConstants.STATE_ID));
        assertFalse(state.containsKey(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE));
        assertFalse(state.containsKey(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_SPLIT_PAGE));
    }

    @Test
    @DisplayName("Should include only the input file in state when the split page number is null")
    void testHandleValidationFailure_InputFilePresent_PageNumberNull() {
        ExtensionResponse mockResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), anyList(), anyString(), anyMap()))
                .thenReturn(mockResponse);

        WithRetryValidationStrategy.ValidationResult failureResult = WithRetryValidationStrategy.ValidationResult.failure(
                "Split page number is required", ExtensionResponse.Error.ExceptionType.INPUT_ERROR);

        strategy.handleValidationFailure(failureResult, inputFile, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(responseGenerator).generateConfirmationResponse(any(), anyList(), anyString(), stateCaptor.capture());

        Map<String, Object> state = stateCaptor.getValue();
        assertTrue(state.containsKey(SubCatalogConstants.STATE_ID));
        assertTrue(state.containsKey(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE));
        assertFalse(state.containsKey(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_SPLIT_PAGE));
    }

    @Test
    @DisplayName("Should include only the split page number in state when the input file is null")
    void testHandleValidationFailure_PageNumberPresent_InputFileNull() {
        ExtensionResponse mockResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), anyList(), anyString(), anyMap()))
                .thenReturn(mockResponse);

        WithRetryValidationStrategy.ValidationResult failureResult = WithRetryValidationStrategy.ValidationResult.failure(
                "No PDF file provided", ExtensionResponse.Error.ExceptionType.INPUT_ERROR);

        strategy.handleValidationFailure(failureResult, null, 7.0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(responseGenerator).generateConfirmationResponse(any(), anyList(), anyString(), stateCaptor.capture());

        Map<String, Object> state = stateCaptor.getValue();
        assertTrue(state.containsKey(SubCatalogConstants.STATE_ID));
        assertFalse(state.containsKey(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE));
        assertEquals(7.0, state.get(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_SPLIT_PAGE));
    }
}

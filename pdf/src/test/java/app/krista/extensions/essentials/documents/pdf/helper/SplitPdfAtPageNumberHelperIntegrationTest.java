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

package app.krista.extensions.essentials.documents.pdf.helper;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extensions.essentials.documents.pdf.helper.errorhandler.PdfSplitErrorHandler;
import app.krista.extensions.essentials.documents.pdf.helper.factory.ValidationStrategyFactory;
import app.krista.extensions.essentials.documents.pdf.helper.processor.PdfSplitProcessor;
import app.krista.extensions.essentials.documents.pdf.helper.strategy.ValidationStrategy;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for SplitPdfAtPageNumberHelper.
 * Tests PDF splitting at specific page number with validation, error handling, and retry logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SplitPdfAtPageNumberHelper Integration Tests")
public class SplitPdfAtPageNumberHelperIntegrationTest {

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private ValidationStrategyFactory validationStrategyFactory;

    @Mock
    private PdfSplitProcessor processor;

    @Mock
    private PdfSplitErrorHandler errorHandler;

    @Mock
    private ValidationStrategy validationStrategy;

    private SplitPdfAtPageNumberHelper helper;

    @BeforeEach
    void setUp() {
        helper = new SplitPdfAtPageNumberHelper(
                telemetryHelper,
                validationStrategyFactory,
                processor,
                errorHandler
        );
    }

    // ==================== Successful Split Tests ====================

    @Test
    @DisplayName("Should successfully split PDF at page number with allowRetry=false")
    void testSplitPDFAtPageNumber_Success_NoRetry() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
        Double splitPageNumber = 6.0;

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenReturn(ValidationStrategy.ValidationResult.success());

        File file1 = mock(File.class);
        File file2 = mock(File.class);
        List<File> splitFiles = Arrays.asList(file1, file2);

        PdfSplitProcessor.ProcessResult processResult =
                PdfSplitProcessor.ProcessResult.success(splitFiles);
        when(processor.execute(inputFile, 6)).thenReturn(processResult);

        ExtensionResponse expectedResponse = mock(ExtensionResponse.class);
        when(processor.buildSuccessResponse(splitFiles)).thenReturn(expectedResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(expectedResponse, response, "Response should match expected response");

        // Verify workflow
        verify(validationStrategyFactory).createStrategy(false);
        verify(validationStrategy).validate(inputFile, splitPageNumber);
        verify(processor).execute(inputFile, 6);
        verify(processor).buildSuccessResponse(splitFiles);

        // Verify telemetry
        verify(telemetryHelper).incrementCount("pdf.splitPDFAtPageNumber");
        verify(telemetryHelper).recordSuccess(eq("pdf.splitPDFAtPageNumber"), anyLong(), any());
        verify(telemetryHelper, never()).recordError(anyString(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should successfully split PDF at page number with allowRetry=true")
    void testSplitPDFAtPageNumber_Success_WithRetry() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("document.pdf");
        Double splitPageNumber = 10.0;

        when(validationStrategyFactory.createStrategy(true)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenReturn(ValidationStrategy.ValidationResult.success());

        File file1 = mock(File.class);
        File file2 = mock(File.class);
        List<File> splitFiles = Arrays.asList(file1, file2);

        PdfSplitProcessor.ProcessResult processResult =
                PdfSplitProcessor.ProcessResult.success(splitFiles);
        when(processor.execute(inputFile, 10)).thenReturn(processResult);

        ExtensionResponse expectedResponse = mock(ExtensionResponse.class);
        when(processor.buildSuccessResponse(splitFiles)).thenReturn(expectedResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, true);

        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(validationStrategyFactory).createStrategy(true);
        verify(telemetryHelper).recordSuccess(eq("pdf.splitPDFAtPageNumber"), anyLong(), any());
    }

    @Test
    @DisplayName("Should handle empty split result")
    void testSplitPDFAtPageNumber_EmptyResult() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
        Double splitPageNumber = 2.0;

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenReturn(ValidationStrategy.ValidationResult.success());

        List<File> emptyList = new ArrayList<>();
        PdfSplitProcessor.ProcessResult processResult =
                PdfSplitProcessor.ProcessResult.success(emptyList);
        when(processor.execute(inputFile, 2)).thenReturn(processResult);

        ExtensionResponse expectedResponse = mock(ExtensionResponse.class);
        when(processor.buildSuccessResponse(emptyList)).thenReturn(expectedResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);
        verify(processor).buildSuccessResponse(emptyList);
    }

    // ==================== Validation Failure Tests ====================

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testSplitPDFAtPageNumber_ValidationFailure_NoRetry() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
        Double splitPageNumber = 1.0; // Invalid page number

        ValidationStrategy.ValidationResult validationResult =
                ValidationStrategy.ValidationResult.failure(
                        "Split page number must be at least 2. Provided: 1",
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR
                );

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber)).thenReturn(validationResult);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(validationStrategy.handleValidationFailure(validationResult, inputFile, splitPageNumber))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);
        assertEquals(errorResponse, response);
        verify(validationStrategy).handleValidationFailure(validationResult, inputFile, splitPageNumber);
        verify(processor, never()).execute(any(), anyInt());
        verify(telemetryHelper).incrementCount("pdf.splitPDFAtPageNumber");
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testSplitPDFAtPageNumber_ValidationFailure_WithRetry() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("invalid.txt");
        Double splitPageNumber = 5.0;

        ValidationStrategy.ValidationResult validationResult =
                ValidationStrategy.ValidationResult.failure(
                        "Invalid file format",
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR
                );

        when(validationStrategyFactory.createStrategy(true)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber)).thenReturn(validationResult);

        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(validationStrategy.handleValidationFailure(validationResult, inputFile, splitPageNumber))
                .thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, true);

        // Assert
        assertNotNull(response);
        assertEquals(confirmationResponse, response);
        verify(validationStrategy).handleValidationFailure(validationResult, inputFile, splitPageNumber);
        verify(processor, never()).execute(any(), anyInt());
    }

    @Test
    @DisplayName("Should handle null input file")
    void testSplitPDFAtPageNumber_NullInputFile() {
        // Arrange
        File inputFile = null;
        Double splitPageNumber = 5.0;

        ValidationStrategy.ValidationResult validationResult =
                ValidationStrategy.ValidationResult.failure(
                        "No PDF file provided",
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR
                );

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber)).thenReturn(validationResult);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(validationStrategy.handleValidationFailure(validationResult, inputFile, splitPageNumber))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);
        assertEquals(errorResponse, response);
        verify(processor, never()).execute(any(), anyInt());
    }

    @Test
    @DisplayName("Should handle null split page number")
    void testSplitPDFAtPageNumber_NullPageNumber() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
        Double splitPageNumber = null;

        ValidationStrategy.ValidationResult validationResult =
                ValidationStrategy.ValidationResult.failure(
                        "Split page number is required",
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR
                );

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber)).thenReturn(validationResult);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(validationStrategy.handleValidationFailure(validationResult, inputFile, splitPageNumber))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);
        assertEquals(errorResponse, response);
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle IOException during processing")
    void testSplitPDFAtPageNumber_IOException() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
        Double splitPageNumber = 5.0;

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenReturn(ValidationStrategy.ValidationResult.success());

        IOException ioException = new IOException("Failed to read PDF file");
        PdfSplitProcessor.ProcessResult processResult =
                PdfSplitProcessor.ProcessResult.failure(ioException);
        when(processor.execute(inputFile, 5)).thenReturn(processResult);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(errorHandler.handleException(ioException, inputFile, splitPageNumber, false))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);
        assertEquals(errorResponse, response);
        verify(errorHandler).handleException(ioException, inputFile, splitPageNumber, false);
        verify(telemetryHelper).incrementCount("pdf.splitPDFAtPageNumber");
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Should handle IOException with retry enabled")
    void testSplitPDFAtPageNumber_IOException_WithRetry() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("corrupted.pdf");
        Double splitPageNumber = 3.0;

        when(validationStrategyFactory.createStrategy(true)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenReturn(ValidationStrategy.ValidationResult.success());

        IOException ioException = new IOException("PDF is corrupted");
        PdfSplitProcessor.ProcessResult processResult =
                PdfSplitProcessor.ProcessResult.failure(ioException);
        when(processor.execute(inputFile, 3)).thenReturn(processResult);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(errorHandler.handleException(ioException, inputFile, splitPageNumber, true))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, true);

        // Assert
        assertNotNull(response);
        assertEquals(errorResponse, response);
        verify(errorHandler).handleException(ioException, inputFile, splitPageNumber, true);
    }

    @Test
    @DisplayName("Should handle general exception during processing")
    void testSplitPDFAtPageNumber_GeneralException() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
        Double splitPageNumber = 4.0;

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenReturn(ValidationStrategy.ValidationResult.success());

        RuntimeException runtimeException = new RuntimeException("Unexpected error");
        PdfSplitProcessor.ProcessResult processResult =
                PdfSplitProcessor.ProcessResult.failure(runtimeException);
        when(processor.execute(inputFile, 4)).thenReturn(processResult);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(errorHandler.handleException(runtimeException, inputFile, splitPageNumber, false))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);
        assertEquals(errorResponse, response);
        verify(errorHandler).handleException(runtimeException, inputFile, splitPageNumber, false);
    }

    @Test
    @DisplayName("Should handle unexpected exception in workflow")
    void testSplitPDFAtPageNumber_UnexpectedException() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
        Double splitPageNumber = 5.0;

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);

        // Simulate unexpected exception during validation
        RuntimeException unexpectedException = new RuntimeException("Unexpected workflow error");
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenThrow(unexpectedException);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(errorHandler.handleException(unexpectedException, inputFile, splitPageNumber, false))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);
        assertEquals(errorResponse, response);
        verify(errorHandler).handleException(unexpectedException, inputFile, splitPageNumber, false);
        verify(telemetryHelper).incrementCount("pdf.splitPDFAtPageNumber");
        verify(telemetryHelper).recordError(eq("pdf.splitPDFAtPageNumber"), anyLong(), eq(unexpectedException), any());
    }

    // ==================== Telemetry Tests ====================

    @Test
    @DisplayName("Should record telemetry for successful operation")
    void testSplitPDFAtPageNumber_TelemetrySuccess() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("report.pdf");
        Double splitPageNumber = 8.0;

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenReturn(ValidationStrategy.ValidationResult.success());

        File file1 = mock(File.class);
        File file2 = mock(File.class);
        List<File> splitFiles = Arrays.asList(file1, file2);

        PdfSplitProcessor.ProcessResult processResult =
                PdfSplitProcessor.ProcessResult.success(splitFiles);
        when(processor.execute(inputFile, 8)).thenReturn(processResult);

        ExtensionResponse expectedResponse = mock(ExtensionResponse.class);
        when(processor.buildSuccessResponse(splitFiles)).thenReturn(expectedResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);

        // Verify telemetry calls
        verify(telemetryHelper).incrementCount("pdf.splitPDFAtPageNumber");
        verify(telemetryHelper).recordSuccess(
                eq("pdf.splitPDFAtPageNumber"),
                anyLong(),
                argThat(tags ->
                        tags.containsKey("file_name") &&
                        tags.containsKey("split_count") &&
                        tags.containsKey("split_page")
                )
        );
        verify(telemetryHelper, never()).recordError(anyString(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should record telemetry for error operation")
    void testSplitPDFAtPageNumber_TelemetryError() {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");
        Double splitPageNumber = 5.0;

        when(validationStrategyFactory.createStrategy(false)).thenReturn(validationStrategy);

        RuntimeException exception = new RuntimeException("Test error");
        when(validationStrategy.validate(inputFile, splitPageNumber))
                .thenThrow(exception);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(errorHandler.handleException(exception, inputFile, splitPageNumber, false))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);

        // Verify telemetry calls
        verify(telemetryHelper).incrementCount("pdf.splitPDFAtPageNumber");
        verify(telemetryHelper).recordError(
                eq("pdf.splitPDFAtPageNumber"),
                anyLong(),
                eq(exception),
                argThat(tags ->
                        tags.containsKey("file_name") &&
                        tags.containsKey("split_page")
                )
        );
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }
}


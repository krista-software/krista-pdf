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
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.utils.PdfSplitter;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for SplitPdfConditionHelper.
 * Tests PDF splitting based on condition with validation, error handling, and retry logic.
 */
@DisplayName("SplitPdfConditionHelper Integration Tests")
public class SplitPdfConditionHelperIntegrationTest {

    @Mock
    private KristaMediaClientUtil kristaMediaClientUtil;

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private ValidationOrchestrator validationOrchestrator;

    @Mock
    private ErrorHandlingStateManager errorHandlingStateManager;

    @Mock
    private ExtensionResponseGenerator responseGenerator;

    private SplitPdfConditionHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new SplitPdfConditionHelper(
                kristaMediaClientUtil,
                telemetryHelper,
                validationOrchestrator,
                errorHandlingStateManager,
                responseGenerator
        );
    }

    // ==================== Successful Split Tests ====================

    @Test
    @DisplayName("Should successfully split PDF based on condition")
    void testSplitPDFBasedOnCondition_Success() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitFile1 = mock(java.io.File.class);
        java.io.File splitFile2 = mock(java.io.File.class);
        List<java.io.File> splitFiles = Arrays.asList(splitFile1, splitFile2);

        File outputFile1 = mock(File.class);
        File outputFile2 = mock(File.class);

        // Mock validation - no errors
        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());

        // Mock file operations
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile1)).thenReturn(outputFile1);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile2)).thenReturn(outputFile2);

        // Mock PdfSplitter
        try (MockedStatic<PdfSplitter> pdfSplitterMock = mockStatic(PdfSplitter.class)) {
            pdfSplitterMock.when(() -> PdfSplitter.splitPdfByCriteria(downloadedFile))
                    .thenReturn(splitFiles);

            // Act
            ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

            // Assert
            assertNotNull(response, "Response should not be null");

            // Verify all expected operations were performed
            verify(validationOrchestrator).validate(any());
            verify(kristaMediaClientUtil).downloadFileHandler(inputFile);
            verify(kristaMediaClientUtil).uploadFileHandler(splitFile1);
            verify(kristaMediaClientUtil).uploadFileHandler(splitFile2);

            // Verify telemetry
            verify(telemetryHelper).incrementCount("pdf.splitPDFBasedOnCondition");
            verify(telemetryHelper).recordSuccess(eq("pdf.splitPDFBasedOnCondition"), anyLong(), any());
        }
    }

    @Test
    @DisplayName("Should split PDF with single result file")
    void testSplitPDFBasedOnCondition_SingleFile() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitFile = mock(java.io.File.class);
        List<java.io.File> splitFiles = Collections.singletonList(splitFile);

        File outputFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile)).thenReturn(outputFile);

        try (MockedStatic<PdfSplitter> pdfSplitterMock = mockStatic(PdfSplitter.class)) {
            pdfSplitterMock.when(() -> PdfSplitter.splitPdfByCriteria(downloadedFile))
                    .thenReturn(splitFiles);

            // Act
            ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

            // Assert
            assertNotNull(response);
            verify(kristaMediaClientUtil).uploadFileHandler(splitFile);
        }
    }

    @Test
    @DisplayName("Should return empty list when split returns no files")
    void testSplitPDFBasedOnCondition_NoSplitFiles() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);

        try (MockedStatic<PdfSplitter> pdfSplitterMock = mockStatic(PdfSplitter.class)) {
            pdfSplitterMock.when(() -> PdfSplitter.splitPdfByCriteria(downloadedFile))
                    .thenReturn(new ArrayList<>());

            // Act
            ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

            // Assert
            assertNotNull(response);
            verify(kristaMediaClientUtil).downloadFileHandler(inputFile);
            verify(kristaMediaClientUtil, never()).uploadFileHandler(any());
        }
    }

    // ==================== Validation Failure Tests ====================

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testSplitPDFBasedOnCondition_ValidationFailure_NoRetry() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Please confirm the file format",
                        "File",
                        "Please provide a valid PDF file",
                        "The provided file 'test.txt' is not a valid PDF format. Only .pdf files are supported.",
                        "File"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults =
                Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(validationResults))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
        verify(responseGenerator, never()).generateConfirmationResponse(any(), any(), any(), any());
        verify(kristaMediaClientUtil, never()).downloadFileHandler(any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testSplitPDFBasedOnCondition_ValidationFailure_WithRetry() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Please confirm the file format",
                        "File",
                        "Please provide a valid PDF file",
                        "The provided file 'test.txt' is not a valid PDF format. Only .pdf files are supported.",
                        "File"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults =
                Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);

        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_CONDITION_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, true);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateConfirmationResponse(
                eq(ExtensionResponse.Error.ExceptionType.INPUT_ERROR),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_CONDITION_VALIDATION),
                any()
        );
        verify(errorHandlingStateManager).put(anyString(), any());
        verify(kristaMediaClientUtil, never()).downloadFileHandler(any());
    }

    // ==================== Null Input Tests ====================

    @Test
    @DisplayName("Should handle null input file")
    void testSplitPDFBasedOnCondition_NullInput() {
        // Act
        ExtensionResponse response = helper.splitPDFBasedOnCondition(null, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil, never()).downloadFileHandler(any());
    }

    // ==================== IOException Tests ====================

    @Test
    @DisplayName("Should handle IOException during file download")
    void testSplitPDFBasedOnCondition_DownloadIOException() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile))
                .thenThrow(new RuntimeException(new IOException("Download failed")));

        // Act
        ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil).downloadFileHandler(inputFile);
    }

    @Test
    @DisplayName("Should handle IOException during PDF splitting")
    void testSplitPDFBasedOnCondition_SplitIOException() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);

        try (MockedStatic<PdfSplitter> pdfSplitterMock = mockStatic(PdfSplitter.class)) {
            pdfSplitterMock.when(() -> PdfSplitter.splitPdfByCriteria(downloadedFile))
                    .thenThrow(new IOException("Split failed"));

            // Act
            ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

            // Assert
            assertNotNull(response);
            verify(telemetryHelper).recordError(eq("pdf.splitPDFBasedOnCondition"), anyLong(),
                    any(IOException.class), any());
        }
    }

    @Test
    @DisplayName("Should handle corrupted PDF file")
    void testSplitPDFBasedOnCondition_CorruptedPDF() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("corrupted.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);

        try (MockedStatic<PdfSplitter> pdfSplitterMock = mockStatic(PdfSplitter.class)) {
            pdfSplitterMock.when(() -> PdfSplitter.splitPdfByCriteria(downloadedFile))
                    .thenThrow(new IOException("Missing root object specification in trailer"));

            // Act
            ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

            // Assert
            assertNotNull(response);
            verify(telemetryHelper).recordError(eq("pdf.splitPDFBasedOnCondition"), anyLong(),
                    any(IOException.class), any());
        }
    }

    // ==================== General Exception Tests ====================

    @Test
    @DisplayName("Should handle general exception")
    void testSplitPDFBasedOnCondition_GeneralException() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil).downloadFileHandler(inputFile);
    }

    // ==================== Telemetry Tests ====================

    @Test
    @DisplayName("Should record telemetry for successful operation")
    void testSplitPDFBasedOnCondition_TelemetrySuccess() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitFile = mock(java.io.File.class);
        List<java.io.File> splitFiles = Collections.singletonList(splitFile);

        File outputFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile)).thenReturn(outputFile);

        try (MockedStatic<PdfSplitter> pdfSplitterMock = mockStatic(PdfSplitter.class)) {
            pdfSplitterMock.when(() -> PdfSplitter.splitPdfByCriteria(downloadedFile))
                    .thenReturn(splitFiles);

            // Act
            helper.splitPDFBasedOnCondition(inputFile, false);

            // Assert
            verify(telemetryHelper).incrementCount("pdf.splitPDFBasedOnCondition");
            verify(telemetryHelper).recordSuccess(eq("pdf.splitPDFBasedOnCondition"), anyLong(), any());
        }
    }

    @Test
    @DisplayName("Should record telemetry for failed operation")
    void testSplitPDFBasedOnCondition_TelemetryFailure() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile))
                .thenThrow(new RuntimeException(new IOException("Download failed")));

        // Act
        helper.splitPDFBasedOnCondition(inputFile, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.splitPDFBasedOnCondition");
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }
}



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
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
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
 * Comprehensive integration tests for SplitPdfHelper.
 * Tests PDF splitting into pages with validation, error handling, and retry logic.
 */
@DisplayName("SplitPdfHelper Integration Tests")
public class SplitPdfHelperIntegrationTest {

    @Mock
    private PDFManager pdfManager;

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

    private SplitPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new SplitPdfHelper(
                pdfManager,
                kristaMediaClientUtil,
                telemetryHelper,
                validationOrchestrator,
                errorHandlingStateManager,
                responseGenerator
        );
    }

    // ==================== Successful Split Tests ====================

    @Test
    @DisplayName("Should successfully split PDF into pages using standard logic")
    void testSplitPDFIntoPages_StandardLogic_Success() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitFile1 = mock(java.io.File.class);
        java.io.File splitFile2 = mock(java.io.File.class);
        List<java.io.File> splitFiles = Arrays.asList(splitFile1, splitFile2);

        File outputFile1 = mock(File.class);
        File outputFile2 = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDF(downloadedFile)).thenReturn(splitFiles);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile1)).thenReturn(outputFile1);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile2)).thenReturn(outputFile2);

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        assertNotNull(response);
        verify(validationOrchestrator).validate(any());
        verify(pdfManager).splitPDF(downloadedFile);
        verify(kristaMediaClientUtil).uploadFileHandler(splitFile1);
        verify(kristaMediaClientUtil).uploadFileHandler(splitFile2);
        verify(telemetryHelper).incrementCount("pdf.splitPDFIntoPages");
        verify(telemetryHelper).recordSuccess(eq("pdf.splitPDFIntoPages"), anyLong(), any());
    }

    @Test
    @DisplayName("Should successfully split PDF using custom NET&SYN logic")
    void testSplitPDFIntoPages_CustomLogic_Success() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("talent_report.pdf");

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
            ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, "NET&SYN Talent Report", false);

            // Assert
            assertNotNull(response);
            verify(kristaMediaClientUtil).uploadFileHandler(splitFile);
            verify(pdfManager, never()).splitPDF(any());
        }
    }

    @Test
    @DisplayName("Should filter first file when category contains singleCategory")
    void testSplitPDFIntoPages_CategoryFiltering() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitFile1 = mock(java.io.File.class);
        java.io.File splitFile2 = mock(java.io.File.class);
        java.io.File splitFile3 = mock(java.io.File.class);
        List<java.io.File> splitFiles = Arrays.asList(splitFile1, splitFile2, splitFile3);

        File outputFile1 = mock(File.class);
        File outputFile2 = mock(File.class);
        File outputFile3 = mock(File.class);

        List<String> category = Arrays.asList("Category1", "Category2");
        String singleCategory = "Category1";

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDF(downloadedFile)).thenReturn(splitFiles);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile1)).thenReturn(outputFile1);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile2)).thenReturn(outputFile2);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile3)).thenReturn(outputFile3);

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, category, singleCategory, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil).uploadFileHandler(splitFile1);
        verify(kristaMediaClientUtil).uploadFileHandler(splitFile2);
        verify(kristaMediaClientUtil).uploadFileHandler(splitFile3);
    }

    @Test
    @DisplayName("Should return empty list when split returns no files")
    void testSplitPDFIntoPages_EmptyResult() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDF(downloadedFile)).thenReturn(new ArrayList<>());

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).splitPDF(downloadedFile);
        verify(kristaMediaClientUtil, never()).uploadFileHandler(any());
    }

    // ==================== Validation Failure Tests ====================

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testSplitPDFIntoPages_ValidationFailure_NoRetry() throws Exception {
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
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
        verify(kristaMediaClientUtil, never()).downloadFileHandler(any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testSplitPDFIntoPages_ValidationFailure_WithRetry() throws Exception {
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
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);

        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, true);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_VALIDATION),
                any()
        );
        verify(errorHandlingStateManager).put(anyString(), any());
        verify(kristaMediaClientUtil, never()).downloadFileHandler(any());
    }

    // ==================== Null Input Tests ====================

    @Test
    @DisplayName("Should handle null input file")
    void testSplitPDFIntoPages_NullInput() {
        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(null, null, null, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil, never()).downloadFileHandler(any());
    }

    // ==================== IOException Tests ====================

    @Test
    @DisplayName("Should handle IOException during file download")
    void testSplitPDFIntoPages_DownloadIOException() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile))
                .thenThrow(new RuntimeException(new IOException("Download failed")));

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil).downloadFileHandler(inputFile);
    }

    @Test
    @DisplayName("Should handle IOException during PDF splitting")
    void testSplitPDFIntoPages_SplitIOException() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDF(downloadedFile))
                .thenThrow(new IOException("Split failed"));

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).splitPDF(downloadedFile);
    }

    @Test
    @DisplayName("Should handle corrupted PDF file")
    void testSplitPDFIntoPages_CorruptedPDF() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("corrupted.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDF(downloadedFile))
                .thenThrow(new IOException("Missing root object specification in trailer"));

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).splitPDF(downloadedFile);
    }

    // ==================== General Exception Tests ====================

    @Test
    @DisplayName("Should handle general exception")
    void testSplitPDFIntoPages_GeneralException() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClientUtil).downloadFileHandler(inputFile);
    }

    // ==================== Telemetry Tests ====================

    @Test
    @DisplayName("Should record telemetry for successful operation")
    void testSplitPDFIntoPages_TelemetrySuccess() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitFile = mock(java.io.File.class);
        List<java.io.File> splitFiles = Collections.singletonList(splitFile);

        File outputFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDF(downloadedFile)).thenReturn(splitFiles);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile)).thenReturn(outputFile);

        // Act
        helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.splitPDFIntoPages");
        verify(telemetryHelper).recordSuccess(eq("pdf.splitPDFIntoPages"), anyLong(), any());
    }

    @Test
    @DisplayName("Should record telemetry for failed operation")
    void testSplitPDFIntoPages_TelemetryFailure() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile))
                .thenThrow(new RuntimeException(new IOException("Download failed")));

        // Act
        helper.splitPDFIntoPages(inputFile, null, null, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.splitPDFIntoPages");
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle validation with all parameters")
    void testSplitPDFIntoPages_AllParameters() throws Exception {
        // Arrange
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitFile = mock(java.io.File.class);
        List<java.io.File> splitFiles = Collections.singletonList(splitFile);

        File outputFile = mock(File.class);

        List<String> category = Arrays.asList("Cat1", "Cat2");
        String singleCategory = "Cat3";

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDF(downloadedFile)).thenReturn(splitFiles);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile)).thenReturn(outputFile);

        // Act
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, category, singleCategory, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).splitPDF(downloadedFile);
    }
}



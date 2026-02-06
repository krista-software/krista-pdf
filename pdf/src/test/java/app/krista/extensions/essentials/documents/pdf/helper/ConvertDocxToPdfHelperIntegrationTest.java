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
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for ConvertDocxToPdfHelper.
 */
@DisplayName("ConvertDocxToPdfHelper Integration Tests")
public class ConvertDocxToPdfHelperIntegrationTest {

    @Mock
    private ValidationOrchestrator validationOrchestrator;
    @Mock
    private ErrorHandlingStateManager errorHandlingStateManager;
    @Mock
    private PDFManager pdfManager;
    @Mock
    private KristaMediaClientUtil kristaMediaClientUtil;
    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private ExtensionResponseGenerator responseGenerator;

    private ConvertDocxToPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new ConvertDocxToPdfHelper(
                validationOrchestrator,
                errorHandlingStateManager,
                pdfManager,
                kristaMediaClientUtil,
                telemetryHelper,
                responseGenerator
        );
    }

    @Test
    @DisplayName("Should successfully convert DOCX to PDF")
    void testConvertDocxToPDF_Success() throws Exception {
        // Arrange
        File docxFile = mock(File.class);
        when(docxFile.getFileName()).thenReturn("document.docx");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File convertedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(docxFile)).thenReturn(downloadedFile);
        when(pdfManager.convertDocToPDF(downloadedFile)).thenReturn(convertedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(convertedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.convertDocxToPDF(docxFile, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).convertDocToPDF(downloadedFile);
        verify(telemetryHelper).incrementCount("pdf.convertDocxToPdf");
        verify(telemetryHelper).recordSuccess(eq("pdf.convertDocxToPdf"), anyLong(), any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testConvertDocxToPDF_ValidationFailure_NoRetry() {
        // Arrange
        File docxFile = mock(File.class);
        when(docxFile.getFileName()).thenReturn("document.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: not a DOCX file", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.convertDocxToPDF(docxFile, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
        verify(pdfManager, never()).convertDocToPDF(any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testConvertDocxToPDF_ValidationFailure_WithRetry() {
        // Arrange
        File docxFile = mock(File.class);
        when(docxFile.getFileName()).thenReturn("document.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: not a DOCX file", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_DOCX_FORMAT_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.convertDocxToPDF(docxFile, true);

        // Assert
        assertNotNull(response);
        verify(errorHandlingStateManager).put(anyString(), any());
    }

    @Test
    @DisplayName("Should handle null input file")
    void testConvertDocxToPDF_NullInput() {
        // Act
        ExtensionResponse response = helper.convertDocxToPDF(null, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager, never()).convertDocToPDF(any());
    }

    @Test
    @DisplayName("Should handle exception during conversion")
    void testConvertDocxToPDF_ConversionException() throws Exception {
        // Arrange
        File docxFile = mock(File.class);
        when(docxFile.getFileName()).thenReturn("document.docx");
        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(docxFile)).thenReturn(downloadedFile);
        when(pdfManager.convertDocToPDF(downloadedFile))
                .thenThrow(new RuntimeException("Conversion failed"));

        // Act
        ExtensionResponse response = helper.convertDocxToPDF(docxFile, false);

        // Assert
        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertDocxToPdf"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should record telemetry for successful operation")
    void testConvertDocxToPDF_TelemetrySuccess() throws Exception {
        // Arrange
        File docxFile = mock(File.class);
        when(docxFile.getFileName()).thenReturn("document.docx");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File convertedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(docxFile)).thenReturn(downloadedFile);
        when(pdfManager.convertDocToPDF(downloadedFile)).thenReturn(convertedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(convertedPdf)).thenReturn(uploadedFile);

        // Act
        helper.convertDocxToPDF(docxFile, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.convertDocxToPdf");
        verify(telemetryHelper).recordSuccess(eq("pdf.convertDocxToPdf"), anyLong(), any());
    }

    @Test
    @DisplayName("Should record telemetry for failed operation")
    void testConvertDocxToPDF_TelemetryFailure() throws Exception {
        // Arrange
        File docxFile = mock(File.class);
        when(docxFile.getFileName()).thenReturn("document.docx");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(docxFile))
                .thenThrow(new RuntimeException("Failed"));

        // Act
        helper.convertDocxToPDF(docxFile, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.convertDocxToPdf");
        verify(telemetryHelper).recordError(eq("pdf.convertDocxToPdf"), anyLong(), any(), any());
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }
}



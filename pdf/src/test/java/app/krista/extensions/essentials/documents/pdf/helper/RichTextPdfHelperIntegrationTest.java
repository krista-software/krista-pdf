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
 * Comprehensive integration tests for RichTextPdfHelper.
 */
@DisplayName("RichTextPdfHelper Integration Tests")
public class RichTextPdfHelperIntegrationTest {

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

    private RichTextPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new RichTextPdfHelper(
                validationOrchestrator,
                errorHandlingStateManager,
                pdfManager,
                kristaMediaClientUtil,
                telemetryHelper,
                responseGenerator
        );
    }

    @Test
    @DisplayName("Should successfully convert rich text to PDF with all parameters")
    void testGetRichTextPDF_AllParameters_Success() throws Exception {
        // Arrange
        String richText = "<p>This is <b>rich</b> text content</p>";
        File logoFile = mock(File.class);
        java.io.File downloadedLogo = mock(java.io.File.class);
        java.io.File generatedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(logoFile)).thenReturn(downloadedLogo);
        when(pdfManager.getRichTextPDF(anyString(), anyString(), anyString(), anyString(), anyString(),
                eq(downloadedLogo), eq(richText))).thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.getRichTextPDF("Title", "Subtitle", "Description",
                "Header", "Footer", logoFile, richText, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).getRichTextPDF(eq("Title"), eq("Subtitle"), eq("Description"),
                eq("Header"), eq("Footer"), eq(downloadedLogo), eq(richText));
        verify(telemetryHelper).incrementCount("pdf.getRichTextPDF");
        verify(telemetryHelper).recordSuccess(eq("pdf.getRichTextPDF"), anyLong(), any());
    }

    @Test
    @DisplayName("Should successfully convert rich text with minimal parameters")
    void testGetRichTextPDF_MinimalParameters_Success() throws Exception {
        // Arrange
        String richText = "Simple text";
        java.io.File generatedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(pdfManager.getRichTextPDF(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(richText))).thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.getRichTextPDF(null, null, null, null, null, null, richText, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager).getRichTextPDF(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(richText));
    }

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testGetRichTextPDF_ValidationFailure_NoRetry() throws Exception {
        // Arrange
        String richText = "abc";
        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: too short", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.getRichTextPDF("Title", null, null, null, null, null, richText, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
        verify(pdfManager, never()).getRichTextPDF(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testGetRichTextPDF_ValidationFailure_WithRetry() {
        // Arrange
        String richText = "abc";
        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: too short", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_RICH_TEXT_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.getRichTextPDF("Title", null, null, null, null, null, richText, true);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_RICH_TEXT_VALIDATION),
                any()
        );
        verify(errorHandlingStateManager).put(anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception when rich text is null")
    void testGetRichTextPDF_NullRichText() throws Exception {
        // Act
        ExtensionResponse response = helper.getRichTextPDF("Title", null, null, null, null, null, null, false);

        // Assert
        assertNotNull(response);
        verify(pdfManager, never()).getRichTextPDF(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle exception during PDF generation")
    void testGetRichTextPDF_GenerationException() throws Exception {
        // Arrange
        String richText = "Valid text content";
        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(pdfManager.getRichTextPDF(any(), any(), any(), any(), any(), any(), eq(richText)))
                .thenThrow(new RuntimeException("PDF generation failed"));

        // Act
        ExtensionResponse response = helper.getRichTextPDF("Title", null, null, null, null, null, richText, false);

        // Assert
        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.getRichTextPDF"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should handle exception during file upload")
    void testGetRichTextPDF_UploadException() throws Exception {
        // Arrange
        String richText = "Valid text";
        java.io.File generatedPdf = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(pdfManager.getRichTextPDF(any(), any(), any(), any(), any(), any(), eq(richText)))
                .thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf))
                .thenThrow(new RuntimeException("Upload failed"));

        // Act
        ExtensionResponse response = helper.getRichTextPDF("Title", null, null, null, null, null, richText, false);

        // Assert
        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.getRichTextPDF"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should record telemetry for successful operation")
    void testGetRichTextPDF_TelemetrySuccess() throws Exception {
        // Arrange
        String richText = "Test content";
        java.io.File generatedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(pdfManager.getRichTextPDF(any(), any(), any(), any(), any(), any(), eq(richText)))
                .thenReturn(generatedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(generatedPdf)).thenReturn(uploadedFile);

        // Act
        helper.getRichTextPDF("Title", null, null, null, null, null, richText, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.getRichTextPDF");
        verify(telemetryHelper).recordSuccess(eq("pdf.getRichTextPDF"), anyLong(), any());
    }

    @Test
    @DisplayName("Should record telemetry for failed operation")
    void testGetRichTextPDF_TelemetryFailure() throws Exception {
        // Arrange
        String richText = "Test";
        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(pdfManager.getRichTextPDF(any(), any(), any(), any(), any(), any(), eq(richText)))
                .thenThrow(new RuntimeException("Failed"));

        // Act
        helper.getRichTextPDF("Title", null, null, null, null, null, richText, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.getRichTextPDF");
        verify(telemetryHelper).recordError(eq("pdf.getRichTextPDF"), anyLong(), any(), any());
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }
}



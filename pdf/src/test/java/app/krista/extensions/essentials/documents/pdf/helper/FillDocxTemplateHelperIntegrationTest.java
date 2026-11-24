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
import app.krista.extensions.essentials.documents.pdf.service.DocxManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import app.krista.model.base.FreeForm;
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
 * Comprehensive integration tests for FillDocxTemplateHelper.
 */
@DisplayName("FillDocxTemplateHelper Integration Tests")
public class FillDocxTemplateHelperIntegrationTest {

    @Mock
    private ValidationOrchestrator validationOrchestrator;
    @Mock
    private ErrorHandlingStateManager internalStateManager;
    @Mock
    private DocxManager docxManager;
    @Mock
    private KristaMediaClientUtil kristaMediaClientUtil;
    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private ExtensionResponseGenerator responseGenerator;

    private FillDocxTemplateHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new FillDocxTemplateHelper(
                validationOrchestrator,
                internalStateManager,
                docxManager,
                kristaMediaClientUtil,
                telemetryHelper,
                responseGenerator
        );
    }

    @Test
    @DisplayName("Should successfully fill DOCX template")
    void testFillTheDocxTemplate_Success() throws Exception {
        // Arrange
        File templateFile = mock(File.class);
        when(templateFile.getFileName()).thenReturn("template.docx");
        FreeForm templateFields = mock(FreeForm.class);

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File filledPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(templateFile)).thenReturn(downloadedFile);
        when(docxManager.fillTheDocTemplate(eq(downloadedFile), any())).thenReturn(filledPdf);
        when(kristaMediaClientUtil.uploadFileHandler(filledPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.fillTheDocxTemplate(templateFile, templateFields, false);

        // Assert
        assertNotNull(response);
        verify(docxManager).fillTheDocTemplate(eq(downloadedFile), any());
        verify(telemetryHelper).incrementCount("pdf.fillTheDocxTemplate");
        verify(telemetryHelper).recordSuccess(eq("pdf.fillTheDocxTemplate"), anyLong(), any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testFillTheDocxTemplate_ValidationFailure_NoRetry() throws Exception {
        // Arrange
        File templateFile = mock(File.class);
        when(templateFile.getFileName()).thenReturn("template.txt");
        FreeForm templateFields = mock(FreeForm.class);

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: not a DOCX file", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.fillTheDocxTemplate(templateFile, templateFields, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
        verify(docxManager, never()).fillTheDocTemplate(any(), any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testFillTheDocxTemplate_ValidationFailure_WithRetry() throws Exception {
        // Arrange
        File templateFile = mock(File.class);
        when(templateFile.getFileName()).thenReturn("template.txt");
        FreeForm templateFields = mock(FreeForm.class);

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
                eq(SubCatalogConstants.CONFIRM_TEMPLATE_FORMAT_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.fillTheDocxTemplate(templateFile, templateFields, true);

        // Assert
        assertNotNull(response);
        verify(internalStateManager).put(anyString(), any());
    }

    @Test
    @DisplayName("Should handle null template file")
    void testFillTheDocxTemplate_NullTemplate() throws Exception {
        // Arrange
        FreeForm templateFields = mock(FreeForm.class);

        // Act
        ExtensionResponse response = helper.fillTheDocxTemplate(null, templateFields, false);

        // Assert
        assertNotNull(response);
        verify(docxManager, never()).fillTheDocTemplate(any(), any());
    }

    @Test
    @DisplayName("Should handle exception during template filling")
    void testFillTheDocxTemplate_FillingException() throws Exception {
        // Arrange
        File templateFile = mock(File.class);
        when(templateFile.getFileName()).thenReturn("template.docx");
        FreeForm templateFields = mock(FreeForm.class);
        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(templateFile)).thenReturn(downloadedFile);
        when(docxManager.fillTheDocTemplate(eq(downloadedFile), any()))
                .thenThrow(new RuntimeException("Filling failed"));

        // Act
        ExtensionResponse response = helper.fillTheDocxTemplate(templateFile, templateFields, false);

        // Assert
        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.fillTheDocxTemplate"), anyLong(), any(), any());
    }
}


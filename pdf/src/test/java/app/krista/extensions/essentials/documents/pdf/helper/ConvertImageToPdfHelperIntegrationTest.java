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
import app.krista.extensions.essentials.documents.pdf.client.KristaMediaClient;
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
 * Comprehensive integration tests for ConvertImageToPdfHelper.
 */
@DisplayName("ConvertImageToPdfHelper Integration Tests")
public class ConvertImageToPdfHelperIntegrationTest {

    @Mock
    private KristaMediaClient kristaMediaClient;
    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private ValidationOrchestrator validationOrchestrator;
    @Mock
    private ErrorHandlingStateManager internalStateManager;
    @Mock
    private ExtensionResponseGenerator responseGenerator;

    private ConvertImageToPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new ConvertImageToPdfHelper(
                kristaMediaClient,
                telemetryHelper,
                validationOrchestrator,
                internalStateManager,
                responseGenerator
        );
    }

    @Test
    @DisplayName("Should attempt to convert image to PDF")
    void testConvertImageToPdf_Attempt() throws Exception {
        // Arrange
        File imageFile = mock(File.class);
        when(imageFile.getFileName()).thenReturn("image.jpg");

        java.io.File sourceFile = mock(java.io.File.class);
        when(sourceFile.exists()).thenReturn(true);
        when(sourceFile.getAbsolutePath()).thenReturn("/tmp/image.jpg");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClient.toJavaFile(imageFile)).thenReturn(sourceFile);

        // Act
        ExtensionResponse response = helper.convertImageToPdf(imageFile, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClient).toJavaFile(imageFile);
        verify(telemetryHelper).incrementCount("pdf.convertImageToPdf");
        // Note: Actual conversion may fail, but telemetry is recorded
    }

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testConvertImageToPdf_ValidationFailure_NoRetry() throws Exception {
        // Arrange
        File imageFile = mock(File.class);
        when(imageFile.getFileName()).thenReturn("document.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: not an image file", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.convertImageToPdf(imageFile, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
        verify(kristaMediaClient, never()).toJavaFile(any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testConvertImageToPdf_ValidationFailure_WithRetry() throws Exception {
        // Arrange
        File imageFile = mock(File.class);
        when(imageFile.getFileName()).thenReturn("document.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: not an image file", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_IMAGE_FORMAT_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.convertImageToPdf(imageFile, true);

        // Assert
        assertNotNull(response);
        verify(internalStateManager).put(anyString(), any());
    }

    @Test
    @DisplayName("Should handle null input file")
    void testConvertImageToPdf_NullInput() throws Exception {
        // Act
        ExtensionResponse response = helper.convertImageToPdf(null, false);

        // Assert
        assertNotNull(response);
        verify(kristaMediaClient, never()).toJavaFile(any());
    }

    @Test
    @DisplayName("Should handle exception during conversion")
    void testConvertImageToPdf_ConversionException() throws Exception {
        // Arrange
        File imageFile = mock(File.class);
        when(imageFile.getFileName()).thenReturn("image.png");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClient.toJavaFile(imageFile))
                .thenThrow(new RuntimeException("Conversion failed"));

        // Act
        ExtensionResponse response = helper.convertImageToPdf(imageFile, false);

        // Assert
        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertImageToPdf"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should record telemetry increment for operation")
    void testConvertImageToPdf_TelemetryIncrement() throws Exception {
        // Arrange
        File imageFile = mock(File.class);
        when(imageFile.getFileName()).thenReturn("image.jpg");

        java.io.File sourceFile = mock(java.io.File.class);
        when(sourceFile.exists()).thenReturn(true);
        when(sourceFile.getAbsolutePath()).thenReturn("/tmp/image.jpg");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClient.toJavaFile(imageFile)).thenReturn(sourceFile);

        // Act
        helper.convertImageToPdf(imageFile, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.convertImageToPdf");
    }

    @Test
    @DisplayName("Should record telemetry for failed operation")
    void testConvertImageToPdf_TelemetryFailure() throws Exception {
        // Arrange
        File imageFile = mock(File.class);
        when(imageFile.getFileName()).thenReturn("image.png");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClient.toJavaFile(imageFile))
                .thenThrow(new RuntimeException("Failed"));

        // Act
        helper.convertImageToPdf(imageFile, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.convertImageToPdf");
        verify(telemetryHelper).recordError(eq("pdf.convertImageToPdf"), anyLong(), any(), any());
        verify(telemetryHelper, never()).recordSuccess(anyString(), anyLong(), any());
    }
}



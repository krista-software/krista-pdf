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
import app.krista.extensions.essentials.documents.pdf.service.ExcelConverterService;
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
 * Comprehensive integration tests for ConvertExcelMultipleSheetsHelper.
 */
@DisplayName("ConvertExcelMultipleSheetsHelper Integration Tests")
public class ConvertExcelMultipleSheetsHelperIntegrationTest {

    @Mock
    private ExcelConverterService excelConverterService;
    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private ValidationOrchestrator validationOrchestrator;
    @Mock
    private ErrorHandlingStateManager internalStateManager;
    @Mock
    private ExtensionResponseGenerator responseGenerator;

    private ConvertExcelMultipleSheetsHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new ConvertExcelMultipleSheetsHelper(
                excelConverterService,
                telemetryHelper,
                validationOrchestrator,
                internalStateManager,
                responseGenerator
        );
    }

    @Test
    @DisplayName("Should successfully convert Excel with multiple sheets to PDF")
    void testConvertExcelFileWithMultipleSheetsToPDF_Success() throws Exception {
        // Arrange
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("workbook.xlsx");

        File convertedPdf = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(excelConverterService.convertExcelFileWithMultipleSheetsToPDF(excelFile)).thenReturn(convertedPdf);

        // Act
        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, false);

        // Assert
        assertNotNull(response);
        verify(excelConverterService).convertExcelFileWithMultipleSheetsToPDF(excelFile);
        verify(telemetryHelper).incrementCount("pdf.convertExcelFileWithMultipleSheetsToPDF");
        verify(telemetryHelper).recordSuccess(eq("pdf.convertExcelFileWithMultipleSheetsToPDF"), anyLong(), any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testConvertExcelFileWithMultipleSheetsToPDF_ValidationFailure_NoRetry() throws Exception {
        // Arrange
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("document.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: not an Excel file", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateValidationErrorResponse(validationResults)).thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
        verify(excelConverterService, never()).convertExcelFileWithMultipleSheetsToPDF(any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testConvertExcelFileWithMultipleSheetsToPDF_ValidationFailure_WithRetry() throws Exception {
        // Arrange
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("document.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: not an Excel file", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_EXCEL_MULTIPLE_SHEETS_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, true);

        // Assert
        assertNotNull(response);
        verify(internalStateManager).put(anyString(), any());
    }

    @Test
    @DisplayName("Should handle null Excel file")
    void testConvertExcelFileWithMultipleSheetsToPDF_NullFile() throws Exception {
        // Act
        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(null, false);

        // Assert
        assertNotNull(response);
        verify(excelConverterService, never()).convertExcelFileWithMultipleSheetsToPDF(any());
        verify(telemetryHelper).recordError(eq("pdf.convertExcelFileWithMultipleSheetsToPDF"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should handle exception during conversion")
    void testConvertExcelFileWithMultipleSheetsToPDF_ConversionException() throws Exception {
        // Arrange
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("workbook.xlsx");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(excelConverterService.convertExcelFileWithMultipleSheetsToPDF(excelFile))
                .thenThrow(new RuntimeException("Invalid Excel file"));

        // Act
        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, false);

        // Assert
        assertNotNull(response);
        verify(telemetryHelper).incrementCount("pdf.convertExcelFileWithMultipleSheetsToPDF");
        // Note: Exception is caught and handled by ExtensionResponseWrapper
    }

    @Test
    @DisplayName("Should record telemetry for successful operation")
    void testConvertExcelFileWithMultipleSheetsToPDF_TelemetrySuccess() throws Exception {
        // Arrange
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("workbook.xlsx");
        File convertedPdf = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(excelConverterService.convertExcelFileWithMultipleSheetsToPDF(excelFile)).thenReturn(convertedPdf);

        // Act
        helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, false);

        // Assert
        verify(telemetryHelper).incrementCount("pdf.convertExcelFileWithMultipleSheetsToPDF");
        verify(telemetryHelper).recordSuccess(eq("pdf.convertExcelFileWithMultipleSheetsToPDF"), anyLong(), any());
    }
}


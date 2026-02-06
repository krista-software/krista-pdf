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
import app.krista.extensions.essentials.documents.pdf.service.ExcelManager;
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
 * Comprehensive integration tests for ConvertExcelToPDFHelper.
 */
@DisplayName("ConvertExcelToPDFHelper Integration Tests")
public class ConvertExcelToPDFHelperIntegrationTest {

    @Mock
    private ValidationOrchestrator validationOrchestrator;
    @Mock
    private ErrorHandlingStateManager internalstateManager;
    @Mock
    private ExcelManager excelManager;
    @Mock
    private KristaMediaClient kristaMediaClient;
    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private KristaMediaClientUtil kristaMediaClientUtil;
    @Mock
    private ExtensionResponseGenerator responseGenerator;

    private ConvertExcelToPDFHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new ConvertExcelToPDFHelper(
                validationOrchestrator,
                internalstateManager,
                excelManager,
                kristaMediaClient,
                telemetryHelper,
                kristaMediaClientUtil,
                responseGenerator
        );
    }

    @Test
    @DisplayName("Should successfully convert Excel to PDF without sheet name")
    void testConvertExcelToPDF_NoSheetName_Success() throws Exception {
        // Arrange
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("spreadsheet.xlsx");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File convertedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(excelFile)).thenReturn(downloadedFile);
        when(excelManager.convertExcelToPDF(downloadedFile, null)).thenReturn(convertedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(convertedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.convertExcelToPDF(excelFile, null, false);

        // Assert
        assertNotNull(response);
        verify(excelManager).convertExcelToPDF(downloadedFile, null);
        verify(telemetryHelper).incrementCount("pdf.convertExcelToPDF");
        verify(telemetryHelper).recordSuccess(eq("pdf.convertExcelToPdf"), anyLong(), any());
    }

    @Test
    @DisplayName("Should successfully convert Excel to PDF with sheet name")
    void testConvertExcelToPDF_WithSheetName_Success() throws Exception {
        // Arrange
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("spreadsheet.xlsx");
        String sheetName = "Sheet1";

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File convertedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);
        org.apache.poi.ss.usermodel.Workbook workbook = mock(org.apache.poi.ss.usermodel.Workbook.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(excelFile)).thenReturn(downloadedFile);
        when(excelManager.getWorkbook(downloadedFile)).thenReturn(workbook);
        when(excelManager.convertExcelToPDF(downloadedFile, sheetName)).thenReturn(convertedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(convertedPdf)).thenReturn(uploadedFile);

        // Act
        ExtensionResponse response = helper.convertExcelToPDF(excelFile, sheetName, false);

        // Assert
        assertNotNull(response);
        verify(excelManager).convertExcelToPDF(downloadedFile, sheetName);
    }

    @Test
    @DisplayName("Should handle validation failure with retry disabled")
    void testConvertExcelToPDF_ValidationFailure_NoRetry() throws Exception {
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
        ExtensionResponse response = helper.convertExcelToPDF(excelFile, null, false);

        // Assert
        assertNotNull(response);
        verify(responseGenerator).generateValidationErrorResponse(validationResults);
        verify(excelManager, never()).convertExcelToPDF(any(), any());
    }

    @Test
    @DisplayName("Should handle validation failure with retry enabled")
    void testConvertExcelToPDF_ValidationFailure_WithRetry() throws Exception {
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
                eq(SubCatalogConstants.CONFIRM_SHEET_NAME_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // Act
        ExtensionResponse response = helper.convertExcelToPDF(excelFile, null, true);

        // Assert
        assertNotNull(response);
        verify(internalstateManager).put(anyString(), any());
    }
}


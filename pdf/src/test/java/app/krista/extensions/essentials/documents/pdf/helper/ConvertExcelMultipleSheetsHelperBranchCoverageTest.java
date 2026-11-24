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
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.service.ExcelConverterService;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage focused unit tests for {@link ConvertExcelMultipleSheetsHelper}.
 *
 * <p>Key insight driving these tests: {@code ExtensionResponseWrapper.wrapResponse()}
 * swallows ANY exception thrown by the excelConverterService call internally and turns it
 * into a normal failure {@link ExtensionResponse} - it never rethrows. That means the
 * helper's own outer {@code catch (Exception cause)} block can only ever be reached when
 * something throws BEFORE the wrapResponse() call - i.e. from
 * {@code validationOrchestrator.validate(...)} itself (or the explicit null-file check).
 * These tests deliberately make {@code validate()} throw to drive that catch block with a
 * non-null Excel file, which is the only way to exercise its message-matching branches.</p>
 */
@DisplayName("ConvertExcelMultipleSheetsHelper Branch Coverage Tests")
class ConvertExcelMultipleSheetsHelperBranchCoverageTest {

    private ExcelConverterService excelConverterService;
    private TelemetryHelper telemetryHelper;
    private ValidationOrchestrator validationOrchestrator;
    private ErrorHandlingStateManager internalStateManager;
    private ExtensionResponseGenerator responseGenerator;
    private ConvertExcelMultipleSheetsHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        excelConverterService = mock(ExcelConverterService.class);
        telemetryHelper = mock(TelemetryHelper.class);
        validationOrchestrator = mock(ValidationOrchestrator.class);
        internalStateManager = mock(ErrorHandlingStateManager.class);
        responseGenerator = mock(ExtensionResponseGenerator.class);
        helper = new ConvertExcelMultipleSheetsHelper(
                excelConverterService, telemetryHelper, validationOrchestrator, internalStateManager, responseGenerator);
    }

    @Test
    @DisplayName("validate() throwing with a message containing 'Invalid Excel file' hits the INPUT_ERROR branch, non-null file")
    void testConvertExcel_ValidateThrows_InvalidExcelFileMessage() {
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("workbook.xlsx");
        when(validationOrchestrator.validate(any())).thenThrow(new RuntimeException("Invalid Excel file - corrupted"));

        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertExcelFileWithMultipleSheetsToPDF"), anyLong(), any(), any());
        verifyNoInteractions(excelConverterService);
    }

    @Test
    @DisplayName("validate() throwing with a message containing 'Unsupported Excel format' hits the second OR branch, non-null file")
    void testConvertExcel_ValidateThrows_UnsupportedExcelFormatMessage() {
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("workbook.xlsx");
        when(validationOrchestrator.validate(any()))
                .thenThrow(new RuntimeException("Unsupported Excel format detected"));

        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertExcelFileWithMultipleSheetsToPDF"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("validate() throwing with a message containing 'Cannot read Excel file' hits the third OR branch, non-null file")
    void testConvertExcel_ValidateThrows_CannotReadExcelFileMessage() {
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("workbook.xlsx");
        when(validationOrchestrator.validate(any()))
                .thenThrow(new RuntimeException("Cannot read Excel file properly"));

        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertExcelFileWithMultipleSheetsToPDF"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("validate() throwing with an unrelated message hits the SYSTEM_ERROR else branch, non-null file")
    void testConvertExcel_ValidateThrows_UnrelatedMessage_SystemError() {
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("workbook.xlsx");
        when(validationOrchestrator.validate(any()))
                .thenThrow(new RuntimeException("Something totally unrelated went wrong"));

        ExtensionResponse response = helper.convertExcelFileWithMultipleSheetsToPDF(excelFile, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertExcelFileWithMultipleSheetsToPDF"), anyLong(), any(), any());
    }

    @Test
    @DisplayName("handleValidationFailure(): excelFile == null is unreachable via the public method, exercised via reflection")
    void testHandleValidationFailure_NullExcelFile_ViaReflection() throws Exception {
        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult("Confirm", "Field", "Fetch", "Error", "Type");
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), any(), any(), any()))
                .thenReturn(confirmationResponse);

        Method handleValidationFailure = ConvertExcelMultipleSheetsHelper.class.getDeclaredMethod(
                "handleValidationFailure", List.class, File.class, Boolean.class);
        handleValidationFailure.setAccessible(true);

        assertDoesNotThrow(() -> handleValidationFailure.invoke(helper, validationResults, null, true));

        verify(internalStateManager).put(anyString(), any());
    }
}

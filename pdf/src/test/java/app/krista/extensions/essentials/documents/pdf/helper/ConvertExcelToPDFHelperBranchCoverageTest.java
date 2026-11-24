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
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.extensions.essentials.documents.pdf.client.KristaMediaClient;
import app.krista.extensions.essentials.documents.pdf.service.ExcelManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage focused unit tests for {@link ConvertExcelToPDFHelper}.
 *
 * <p>Covers:
 * <ul>
 *   <li>excelFile == null (never exercised by the existing integration test suite, which
 *       has no null-file case for this class) - also exercises the method's
 *       {@code assert excelFile != null;} statement inside the catch block. Gradle's
 *       {@code Test} task runs with JVM assertions enabled by default, so this is expected
 *       to surface as an {@link AssertionError} rather than a normal response.</li>
 *   <li>{@code sheetName != null && !sheetName.isEmpty()} with an empty (non-null) sheet
 *       name, which the existing tests never pass (they use either null or a populated
 *       name).</li>
 *   <li>{@code !allValidationResults.isEmpty()} being true specifically because the SHEET
 *       NAME validation failed while the file FORMAT validation passed - distinguished via
 *       argument matchers on the validation context map, since the existing tests stub
 *       validate(any()) with one canned answer regardless of context.</li>
 * </ul>
 */
@DisplayName("ConvertExcelToPDFHelper Branch Coverage Tests")
class ConvertExcelToPDFHelperBranchCoverageTest {

    private ValidationOrchestrator validationOrchestrator;
    private ErrorHandlingStateManager internalstateManager;
    private ExcelManager excelManager;
    private KristaMediaClient kristaMediaClient;
    private TelemetryHelper telemetryHelper;
    private KristaMediaClientUtil kristaMediaClientUtil;
    private ExtensionResponseGenerator responseGenerator;
    private ConvertExcelToPDFHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationOrchestrator = mock(ValidationOrchestrator.class);
        internalstateManager = mock(ErrorHandlingStateManager.class);
        excelManager = mock(ExcelManager.class);
        kristaMediaClient = mock(KristaMediaClient.class);
        telemetryHelper = mock(TelemetryHelper.class);
        kristaMediaClientUtil = mock(KristaMediaClientUtil.class);
        responseGenerator = mock(ExtensionResponseGenerator.class);
        helper = new ConvertExcelToPDFHelper(
                validationOrchestrator, internalstateManager, excelManager, kristaMediaClient,
                telemetryHelper, kristaMediaClientUtil, responseGenerator);
    }

    @Test
    @DisplayName("Null excelFile flips the null-check branches; the method's own `assert excelFile != null` " +
            "then fails under Gradle's assertions-enabled test JVM instead of returning a normal response")
    void testConvertExcelToPDF_NullExcelFile() {
        assertThrows(AssertionError.class, () -> helper.convertExcelToPDF(null, "Sheet1", false));
    }

    @Test
    @DisplayName("Empty (non-null) sheet name with valid file format flips the sheetName.isEmpty()==true branch")
    void testConvertExcelToPDF_EmptySheetName_FileFormatValid() throws Exception {
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("spreadsheet.xlsx");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File convertedPdf = mock(java.io.File.class);
        File uploadedFile = mock(File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(excelFile)).thenReturn(downloadedFile);
        when(excelManager.convertExcelToPDF(downloadedFile, "")).thenReturn(convertedPdf);
        when(kristaMediaClientUtil.uploadFileHandler(convertedPdf)).thenReturn(uploadedFile);

        ExtensionResponse response = helper.convertExcelToPDF(excelFile, "", false);

        assertNotNull(response);
        // Sheet name validation must be skipped entirely (sheetName.isEmpty() == true),
        // so excelManager.getWorkbook() should never be invoked.
        verify(excelManager, never()).getWorkbook(any());
        verify(excelManager).convertExcelToPDF(downloadedFile, "");
    }

    @Test
    @DisplayName("File format passes but sheet-name validation fails: !allValidationResults.isEmpty() becomes " +
            "true even though fileFormatResults was empty")
    void testConvertExcelToPDF_FileFormatValid_SheetNameValidationFails_WithRetry() throws Exception {
        File excelFile = mock(File.class);
        when(excelFile.getFileName()).thenReturn("spreadsheet.xlsx");
        String sheetName = "BadSheet";

        java.io.File downloadedFile = mock(java.io.File.class);
        org.apache.poi.ss.usermodel.Workbook workbook = mock(org.apache.poi.ss.usermodel.Workbook.class);

        ValidationOrchestrator.ValidationResult sheetNameError = new ValidationOrchestrator.ValidationResult(
                "Confirm", "Field", "Fetch", "Error: sheet not found", "Type");
        List<ValidationOrchestrator.ValidationResult> sheetNameResults = Collections.singletonList(sheetNameError);

        // File-format context -> empty (valid); sheet-name context -> non-empty (invalid).
        when(validationOrchestrator.validate(argThat(ctx ->
                ctx != null && ctx.containsKey(Validator.ValidationResource.EXCEL_FILE_FORMAT))))
                .thenReturn(new ArrayList<>());
        when(validationOrchestrator.validate(argThat(ctx ->
                ctx != null && ctx.containsKey(Validator.ValidationResource.SHEET_NAME))))
                .thenReturn(sheetNameResults);

        when(kristaMediaClientUtil.downloadFileHandler(excelFile)).thenReturn(downloadedFile);
        when(excelManager.getWorkbook(downloadedFile)).thenReturn(workbook);

        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), any(), any(), any()))
                .thenReturn(confirmationResponse);

        ExtensionResponse response = helper.convertExcelToPDF(excelFile, sheetName, true);

        assertNotNull(response);
        verify(excelManager, never()).convertExcelToPDF(any(), any());
        verify(internalstateManager).put(anyString(), any());
        verify(responseGenerator).generateConfirmationResponse(any(), any(), any(), any());
    }
}

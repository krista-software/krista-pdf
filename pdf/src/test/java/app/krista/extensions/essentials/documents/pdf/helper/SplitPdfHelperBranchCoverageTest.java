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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage tests for {@link SplitPdfHelper}.
 *
 * <p>These tests deliberately target branches that {@code SplitPdfHelperIntegrationTest}
 * does not exercise:</p>
 * <ul>
 *   <li>{@code javaFiles == null} short-circuit in {@code splitPDFIntoPages} (the existing
 *       tests only ever return an empty list, never a hard {@code null}).</li>
 *   <li>The {@code category != null && singleCategory != null && ...} filter when
 *       {@code category} is present but {@code singleCategory} is {@code null} (the
 *       {@code singleCategory != null} sub-condition's false branch).</li>
 *   <li>The corrupted-PDF message check matching on exactly
 *       {@code "Header doesn't contain versioninfo"} (second clause of the OR true).</li>
 *   <li>{@code handleValidationFailure}'s retry branch storing {@code category} /
 *       {@code singleCategory} into the confirmation state map, exercised independently.</li>
 * </ul>
 *
 * <p>Branches intentionally NOT targeted here (verified unreachable given the guard clauses
 * in the production code, not merely hard to mock):</p>
 * <ul>
 *   <li>The {@code file != null ? ... : "null"/"N/A"} ternaries inside the
 *       {@code catch (IOException e)} block -- {@code file} is always non-null by the time
 *       that catch block can be entered, because a null file throws
 *       {@code IllegalArgumentException} earlier, which is caught by the generic
 *       {@code catch (Exception cause)} block instead.</li>
 *   <li>The third OR clause of the corrupted-PDF check,
 *       {@code contains("Error: Header doesn't contain versioninfo")} -- that literal is a
 *       superset of the second clause's literal, so any message satisfying clause 3 already
 *       satisfies clause 2 and short-circuits before clause 3 is evaluated.</li>
 *   <li>{@code handleValidationFailure}'s {@code file != null} false branch -- the method is
 *       only ever invoked from a call site already guarded by {@code if (file != null)}.</li>
 * </ul>
 */
@DisplayName("SplitPdfHelper Branch Coverage Tests")
public class SplitPdfHelperBranchCoverageTest {

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

    // ==================== javaFiles == null branch (line 109) ====================

    @Test
    @DisplayName("Should return empty success when standard split returns a hard null (not just an empty list)")
    void testSplitPDFIntoPages_StandardSplitReturnsNull_ReturnsEmptySuccess() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        // Returning null (not an empty list) exercises the short-circuit true branch of
        // "javaFiles == null || javaFiles.isEmpty()" without ever calling isEmpty().
        when(pdfManager.splitPDF(downloadedFile)).thenReturn(null);

        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(new ArrayList<>(), response.getResponseValue().get("Split Files"));
        verify(kristaMediaClientUtil, never()).uploadFileHandler(any());
    }

    // ==================== category present, singleCategory null branch (line 128) ====================

    @Test
    @DisplayName("Should not filter the first file when category is present but singleCategory is null")
    void testSplitPDFIntoPages_CategoryPresent_SingleCategoryNull_NoFiltering() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);
        java.io.File splitFile1 = mock(java.io.File.class);
        java.io.File splitFile2 = mock(java.io.File.class);
        List<java.io.File> splitFiles = Arrays.asList(splitFile1, splitFile2);

        File outputFile1 = mock(File.class);
        File outputFile2 = mock(File.class);

        List<String> category = Collections.singletonList("Category1");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDF(downloadedFile)).thenReturn(splitFiles);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile1)).thenReturn(outputFile1);
        when(kristaMediaClientUtil.uploadFileHandler(splitFile2)).thenReturn(outputFile2);

        // category is present but singleCategory is null -> the "singleCategory != null"
        // sub-condition of the AND-chain is false, so no filtering should occur.
        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, category, null, false);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        @SuppressWarnings("unchecked")
        List<File> resultFiles = (List<File>) response.getResponseValue().get("Split Files");
        assertEquals(2, resultFiles.size(), "No file should have been filtered out");
        assertTrue(resultFiles.contains(outputFile1));
        assertTrue(resultFiles.contains(outputFile2));
    }

    // ==================== corrupted-PDF "Header doesn't contain versioninfo" branch (line 153) ====================

    @Test
    @DisplayName("Should return INPUT_ERROR when IOException message is exactly the header-corruption phrase")
    void testSplitPDFIntoPages_IOException_HeaderDoesNotContainVersionInfo_ReturnsInputError() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        // This message satisfies clause 2 ("Header doesn't contain versioninfo") directly,
        // WITHOUT containing clause 1's phrase ("Missing root object specification"),
        // forcing evaluation to reach and short-circuit on clause 2 as true.
        when(pdfManager.splitPDF(downloadedFile))
                .thenThrow(new IOException("Header doesn't contain versioninfo"));

        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, null, false);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNotNull(response.getError());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("corrupted or invalid"));
    }

    // ==================== handleValidationFailure retry state branches (lines 208, 211) ====================

    @Test
    @DisplayName("Should store Category in retry state when category is present and singleCategory is null")
    void testHandleValidationFailureRetry_CategoryPresent_SingleCategoryNull_StoresCategory() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Please confirm the file format",
                        "File",
                        "Please provide a valid PDF file",
                        "The provided file 'test.txt' is not a valid PDF format.",
                        "File"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);
        when(validationOrchestrator.validate(any())).thenReturn(validationResults);

        List<String> category = Arrays.asList("Cat1", "Cat2");

        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, category, null, true);

        assertNotNull(response);
        assertEquals(confirmationResponse, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(responseGenerator).generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_VALIDATION),
                stateCaptor.capture()
        );
        Map<String, Object> capturedState = stateCaptor.getValue();
        assertEquals(category, capturedState.get("Category"));
        assertFalse(capturedState.containsKey("Single Category"));
    }

    @Test
    @DisplayName("Should store Single Category in retry state when singleCategory is present and category is null")
    void testHandleValidationFailureRetry_SingleCategoryPresent_CategoryNull_StoresSingleCategory() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Please confirm the file format",
                        "File",
                        "Please provide a valid PDF file",
                        "The provided file 'test.txt' is not a valid PDF format.",
                        "File"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);
        when(validationOrchestrator.validate(any())).thenReturn(validationResults);

        String singleCategory = "NET&SYN Talent Report";

        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        ExtensionResponse response = helper.splitPDFIntoPages(inputFile, null, singleCategory, true);

        assertNotNull(response);
        assertEquals(confirmationResponse, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(responseGenerator).generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_VALIDATION),
                stateCaptor.capture()
        );
        Map<String, Object> capturedState = stateCaptor.getValue();
        assertEquals(singleCategory, capturedState.get("Single Category"));
        assertFalse(capturedState.containsKey("Category"));
    }
}

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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage tests for {@link SplitPdfTwoEqualHelper}.
 *
 * <p>Targets branches not exercised by {@code SplitPdfTwoEqualHelperIntegrationTest}:</p>
 * <ul>
 *   <li>{@code javaFiles == null} short-circuit (existing tests only return an empty list).</li>
 *   <li>The {@code pageThreshHold != null ? ... : 40} ternary used when building the telemetry
 *       tag map inside the {@code catch (IOException e)} block, with a non-null threshold
 *       (existing IOException tests only ever pass a null threshold).</li>
 *   <li>The corrupted-PDF message check matching exactly
 *       {@code "Header doesn't contain versioninfo"}.</li>
 *   <li>{@code handleValidationFailure}'s {@code pageThreshHold != null} FALSE branch: the
 *       existing {@code SplitPdfTwoEqualHelperIntegrationTest} retry test always passes a
 *       non-null threshold (50.0), so the only uncovered side was retry-with-a-null-threshold
 *       (the "do not store Page Thresh Hold" path).</li>
 * </ul>
 *
 * <p>Branches intentionally NOT targeted (verified unreachable given the guard clauses):</p>
 * <ul>
 *   <li>The {@code inputFile != null ? ... : "N/A"/"null"} ternaries inside
 *       {@code catch (IOException e)} -- {@code inputFile} is always non-null there, since a
 *       null input file throws {@code IllegalArgumentException} earlier and is caught by the
 *       generic {@code catch (Exception cause)} block instead.</li>
 *   <li>The third OR clause of the corrupted-PDF check
 *       ({@code "Error: Header doesn't contain versioninfo"}) -- subsumed by clause 2's
 *       shorter literal, so clause 3's true branch can never be reached.</li>
 *   <li>{@code handleValidationFailure}'s {@code inputFile != null} false branch -- the method
 *       is only ever invoked from a call site already guarded by {@code if (inputFile != null)}.</li>
 * </ul>
 */
@DisplayName("SplitPdfTwoEqualHelper Branch Coverage Tests")
public class SplitPdfTwoEqualHelperBranchCoverageTest {

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

    private SplitPdfTwoEqualHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new SplitPdfTwoEqualHelper(
                pdfManager,
                kristaMediaClientUtil,
                telemetryHelper,
                validationOrchestrator,
                errorHandlingStateManager,
                responseGenerator
        );
    }

    // ==================== javaFiles == null branch ====================

    @Test
    @DisplayName("Should return empty success when split returns a hard null (not just an empty list)")
    void testSplitPDFIntoTwoEqualPart_SplitReturnsNull_ReturnsEmptySuccess() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDFIntoParts(downloadedFile, 40)).thenReturn(null);

        ExtensionResponse response = helper.splitPDFIntoTwoEqualPart(inputFile, null, false);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        assertEquals(new ArrayList<>(), response.getResponseValue().get("Two Equal Files"));
        verify(kristaMediaClientUtil, never()).uploadFileHandler(any());
    }

    // ==================== pageThreshHold ternary true branch inside IOException catch ====================

    @Test
    @DisplayName("Should use the custom page threshold in telemetry tags when IOException occurs with a non-null threshold")
    void testSplitPDFIntoTwoEqualPart_IOException_WithCustomThreshold_UsesThresholdInTelemetry() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDFIntoParts(downloadedFile, 75))
                .thenThrow(new IOException("Split failed"));

        ExtensionResponse response = helper.splitPDFIntoTwoEqualPart(inputFile, 75.0, false);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());

        // Confirms the non-null branch of "pageThreshHold != null ? ... : 40" was exercised
        // when building the recordError telemetry tags.
        verify(telemetryHelper).recordError(eq("pdf.splitPDFIntoTwoEqualPart"), anyLong(), any(),
                argThat(tags -> "75".equals(tags.get("page_threshold"))));
    }

    // ==================== corrupted-PDF "Header doesn't contain versioninfo" branch ====================

    @Test
    @DisplayName("Should return INPUT_ERROR when IOException message is exactly the header-corruption phrase")
    void testSplitPDFIntoTwoEqualPart_IOException_HeaderDoesNotContainVersionInfo_ReturnsInputError() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);
        when(pdfManager.splitPDFIntoParts(downloadedFile, 40))
                .thenThrow(new IOException("Header doesn't contain versioninfo"));

        ExtensionResponse response = helper.splitPDFIntoTwoEqualPart(inputFile, null, false);

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertTrue(response.getError().getErrorMessage().contains("corrupted or invalid"));
    }

    // ==================== handleValidationFailure retry state: pageThreshHold == null branch ====================

    @Test
    @DisplayName("Should NOT store Page Thresh Hold in retry state when threshold is null")
    void testHandleValidationFailureRetry_PageThresholdNull_DoesNotStoreThreshold() throws Exception {
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

        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_TWO_EQUAL_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // pageThreshHold is null AND allowRetry is true -- the only combination that reaches
        // the "if (pageThreshHold != null)" check inside handleValidationFailure's retry
        // branch with a null value (the sibling IntegrationTest's "NoRetry" null-threshold
        // test never reaches this check because allowRetry is false there).
        ExtensionResponse response = helper.splitPDFIntoTwoEqualPart(inputFile, null, true);

        assertNotNull(response);
        assertEquals(confirmationResponse, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> stateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(responseGenerator).generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_SPLIT_PDF_TWO_EQUAL_VALIDATION),
                stateCaptor.capture()
        );
        Map<String, Object> capturedState = stateCaptor.getValue();
        assertFalse(capturedState.containsKey("Page Thresh Hold"));
    }
}

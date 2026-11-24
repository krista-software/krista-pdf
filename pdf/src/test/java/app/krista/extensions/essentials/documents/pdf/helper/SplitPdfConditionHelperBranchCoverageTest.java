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
import app.krista.extensions.essentials.documents.pdf.catalog.utils.PdfSplitter;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage tests for {@link SplitPdfConditionHelper}.
 *
 * <p>Targets branches not exercised by {@code SplitPdfConditionHelperIntegrationTest}:</p>
 * <ul>
 *   <li>{@code javaFiles == null} short-circuit in {@code splitPDFBasedOnCondition} (existing
 *       tests only ever return an empty list, never a hard {@code null}).</li>
 *   <li>The corrupted-PDF message check matching exactly on clause 2,
 *       {@code "Header doesn't contain versioninfo"}, without also matching clause 1.</li>
 * </ul>
 *
 * <p>Branches intentionally NOT targeted (verified unreachable given the guard clauses in the
 * production code):</p>
 * <ul>
 *   <li>The {@code inputFile != null ? ... : "N/A"/"null"} ternaries inside
 *       {@code catch (IOException e)} -- {@code inputFile} is guaranteed non-null there,
 *       because a null input file throws {@code IllegalArgumentException} earlier, which is
 *       caught by the generic {@code catch (Exception cause)} block instead, never by
 *       {@code catch (IOException e)}.</li>
 *   <li>The third OR clause of the corrupted-PDF check
 *       ({@code "Error: Header doesn't contain versioninfo"}) -- its literal is a superset of
 *       clause 2's literal, so clause 3's true branch can never actually be reached (clause 2
 *       always short-circuits first).</li>
 *   <li>{@code handleValidationFailure}'s {@code inputFile != null} false branch -- the method
 *       is only ever invoked from a call site already guarded by
 *       {@code if (inputFile != null)}.</li>
 * </ul>
 */
@DisplayName("SplitPdfConditionHelper Branch Coverage Tests")
public class SplitPdfConditionHelperBranchCoverageTest {

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

    private SplitPdfConditionHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        helper = new SplitPdfConditionHelper(
                kristaMediaClientUtil,
                telemetryHelper,
                validationOrchestrator,
                errorHandlingStateManager,
                responseGenerator
        );
    }

    // ==================== javaFiles == null branch ====================

    @Test
    @DisplayName("Should return empty success when condition-based split returns a hard null (not just an empty list)")
    void testSplitPDFBasedOnCondition_SplitReturnsNull_ReturnsEmptySuccess() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);

        try (MockedStatic<PdfSplitter> pdfSplitterMock = mockStatic(PdfSplitter.class)) {
            pdfSplitterMock.when(() -> PdfSplitter.splitPdfByCriteria(downloadedFile)).thenReturn(null);

            ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            assertEquals(new ArrayList<>(), response.getResponseValue().get("Output Files"));
            verify(kristaMediaClientUtil, never()).uploadFileHandler(any());
        }
    }

    // ==================== corrupted-PDF "Header doesn't contain versioninfo" branch ====================

    @Test
    @DisplayName("Should return INPUT_ERROR when IOException message is exactly the header-corruption phrase")
    void testSplitPDFBasedOnCondition_IOException_HeaderDoesNotContainVersionInfo_ReturnsInputError() throws Exception {
        File inputFile = mock(File.class);
        when(inputFile.getFileName()).thenReturn("test.pdf");

        java.io.File downloadedFile = mock(java.io.File.class);

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(inputFile)).thenReturn(downloadedFile);

        try (MockedStatic<PdfSplitter> pdfSplitterMock = mockStatic(PdfSplitter.class)) {
            pdfSplitterMock.when(() -> PdfSplitter.splitPdfByCriteria(downloadedFile))
                    .thenThrow(new IOException("Header doesn't contain versioninfo"));

            ExtensionResponse response = helper.splitPDFBasedOnCondition(inputFile, false);

            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
            assertTrue(response.getError().getErrorMessage().contains("corrupted or invalid"));
        }
    }
}

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
import app.krista.extensions.essentials.documents.pdf.helper.errorhandler.PdfSplitErrorHandler;
import app.krista.extensions.essentials.documents.pdf.helper.factory.ValidationStrategyFactory;
import app.krista.extensions.essentials.documents.pdf.helper.processor.PdfSplitProcessor;
import app.krista.extensions.essentials.documents.pdf.helper.strategy.ValidationStrategy;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage tests for {@link SplitPdfAtPageNumberHelper}.
 *
 * <p>The existing {@code SplitPdfAtPageNumberHelperIntegrationTest} already exercises the
 * {@code catch (Exception e)} block of {@code splitPDFAtPageNumber} with a non-null
 * {@code inputFile} and a non-null {@code splitPageNumber} (see its
 * {@code testSplitPDFAtPageNumber_UnexpectedException} /
 * {@code testSplitPDFAtPageNumber_TelemetryError} tests), which covers the "true" side of the
 * two ternaries built for the telemetry tag map inside that catch block:
 * {@code inputFile != null ? ... : "N/A"} and
 * {@code splitPageNumber != null ? ... : "N/A"}.
 *
 * <p>What was never exercised is the "false" side of <em>both</em> ternaries -- i.e. reaching
 * {@code catch (Exception e)} while {@code inputFile} and {@code splitPageNumber} are
 * themselves {@code null}. Unlike the sibling helpers (SplitPdfHelper, SplitPdfTwoEqualHelper,
 * SplitPdfConditionHelper), this class has no early
 * {@code if (inputFile == null) throw new IllegalArgumentException(...)} guard -- null
 * handling is delegated entirely to {@code ValidationStrategy.validate(...)}. That means if
 * strategy creation/validation itself blows up (e.g. the factory throws) while the caller
 * passed null arguments, the catch block genuinely runs with {@code inputFile == null} and
 * {@code splitPageNumber == null}, making both false branches reachable and real.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SplitPdfAtPageNumberHelper Branch Coverage Tests")
public class SplitPdfAtPageNumberHelperBranchCoverageTest {

    @Mock
    private TelemetryHelper telemetryHelper;

    @Mock
    private ValidationStrategyFactory validationStrategyFactory;

    @Mock
    private PdfSplitProcessor processor;

    @Mock
    private PdfSplitErrorHandler errorHandler;

    private SplitPdfAtPageNumberHelper helper;

    @BeforeEach
    void setUp() {
        helper = new SplitPdfAtPageNumberHelper(
                telemetryHelper,
                validationStrategyFactory,
                processor,
                errorHandler
        );
    }

    @Test
    @DisplayName("Should handle null inputFile and null splitPageNumber together when an unexpected error occurs before validation")
    void testSplitPDFAtPageNumber_NullFileAndNullPageNumber_UnexpectedErrorBeforeValidation_UsesNAFallbacks() {
        // Arrange: both arguments are null, and strategy creation itself fails, so control
        // jumps straight into catch (Exception e) with inputFile == null and
        // splitPageNumber == null -- exercising the false branch of BOTH telemetry ternaries
        // in a single, realistic scenario.
        File inputFile = null;
        Double splitPageNumber = null;

        RuntimeException factoryFailure = new RuntimeException("Strategy factory blew up");
        when(validationStrategyFactory.createStrategy(false)).thenThrow(factoryFailure);

        ExtensionResponse errorResponse = mock(ExtensionResponse.class);
        when(errorHandler.handleException(factoryFailure, null, null, false))
                .thenReturn(errorResponse);

        // Act
        ExtensionResponse response = helper.splitPDFAtPageNumber(inputFile, splitPageNumber, false);

        // Assert
        assertNotNull(response);
        assertEquals(errorResponse, response);
        verify(errorHandler).handleException(factoryFailure, null, null, false);
        verify(processor, never()).execute(any(), anyInt());

        // The telemetry call must still have been made with the "N/A" fallbacks for both
        // the missing file name and the missing split page -- proving both ternaries took
        // their false branch instead of throwing an NPE themselves.
        verify(telemetryHelper).incrementCount("pdf.splitPDFAtPageNumber");
        verify(telemetryHelper).recordError(
                eq("pdf.splitPDFAtPageNumber"),
                anyLong(),
                eq(factoryFailure),
                argThat(tags -> "N/A".equals(tags.get("file_name")) && "N/A".equals(tags.get("split_page")))
        );
    }
}

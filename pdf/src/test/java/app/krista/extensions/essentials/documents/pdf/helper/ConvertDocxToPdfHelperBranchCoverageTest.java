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
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
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
 * Branch-coverage focused unit tests for {@link ConvertDocxToPdfHelper}.
 *
 * <p>Two remaining branches are structurally dead code reachable only via the public
 * method's null-check, which always routes a null document file into the
 * {@code catch (IllegalArgumentException)} block before a non-null-aware catch(Exception)
 * or handleValidationFailure() call could ever see a null file:
 * <ul>
 *   <li>{@code catch (Exception cause)}'s "documentFile == null" ternary branch can never
 *       execute, because a null file always throws before reaching that generic catch -
 *       it is explicitly NOT exercised here (see note on the skipped case below).</li>
 *   <li>{@code handleValidationFailure}'s "documentFile == null" branch is only reachable
 *       by calling that private method directly - exercised here via reflection.</li>
 * </ul>
 */
@DisplayName("ConvertDocxToPdfHelper Branch Coverage Tests")
class ConvertDocxToPdfHelperBranchCoverageTest {

    private ValidationOrchestrator validationOrchestrator;
    private ErrorHandlingStateManager errorHandlingStateManager;
    private PDFManager pdfManager;
    private KristaMediaClientUtil kristaMediaClientUtil;
    private TelemetryHelper telemetryHelper;
    private ExtensionResponseGenerator responseGenerator;
    private ConvertDocxToPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationOrchestrator = mock(ValidationOrchestrator.class);
        errorHandlingStateManager = mock(ErrorHandlingStateManager.class);
        pdfManager = mock(PDFManager.class);
        kristaMediaClientUtil = mock(KristaMediaClientUtil.class);
        telemetryHelper = mock(TelemetryHelper.class);
        responseGenerator = mock(ExtensionResponseGenerator.class);
        helper = new ConvertDocxToPdfHelper(
                validationOrchestrator, errorHandlingStateManager, pdfManager, kristaMediaClientUtil,
                telemetryHelper, responseGenerator);
    }

    @Test
    @DisplayName("Non-null document whose download raises IllegalArgumentException hits the input-error branch with a non-null file")
    void testConvertDocxToPDF_DownloadThrowsIllegalArgumentException_NonNullFile() {
        File docxFile = mock(File.class);
        when(docxFile.getFileName()).thenReturn("broken.docx");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClientUtil.downloadFileHandler(docxFile))
                .thenThrow(new IllegalArgumentException("Corrupt DOCX"));

        ExtensionResponse response = helper.convertDocxToPDF(docxFile, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertDocxToPdf"), anyLong(), any(IllegalArgumentException.class), any());
        verify(pdfManager, never()).convertDocToPDF(any());
    }

    @Test
    @DisplayName("handleValidationFailure(): documentFile == null is unreachable via the public method, exercised via reflection")
    void testHandleValidationFailure_NullDocumentFile_ViaReflection() throws Exception {
        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult("Confirm", "Field", "Fetch", "Error", "Type");
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), any(), any(), any()))
                .thenReturn(confirmationResponse);

        Method handleValidationFailure = ConvertDocxToPdfHelper.class.getDeclaredMethod(
                "handleValidationFailure", List.class, File.class, Boolean.class);
        handleValidationFailure.setAccessible(true);

        assertDoesNotThrow(() -> handleValidationFailure.invoke(helper, validationResults, null, true));

        verify(errorHandlingStateManager).put(anyString(), any());
    }

    // NOTE on the one branch intentionally left uncovered:
    // ConvertDocxToPdfHelper's generic `catch (Exception cause)` block contains a ternary
    // `documentFile != null ? documentFile.getFileName() : "N/A"`. Its "documentFile == null"
    // side is unreachable through the public convertDocxToPDF(File, Boolean) method: when
    // documentFile is null, the method takes the explicit `throw new IllegalArgumentException(...)`
    // path, which is always caught by the earlier, more specific
    // `catch (IllegalArgumentException cause)` block - the generic catch(Exception) can only
    // ever run with a non-null documentFile (every other statement that can throw - validate(),
    // downloadFileHandler(), convertDocToPDF(), uploadFileHandler() - executes only after the
    // null-check has already passed). This is true dead code under the current implementation,
    // not merely hard-to-mock, so no reflection trick was used to force it.
}

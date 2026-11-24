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
import app.krista.extensions.essentials.documents.pdf.client.KristaMediaClient;
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
 * Branch-coverage focused unit tests for {@link ConvertImageToPdfHelper}.
 * Mirrors the pattern used for ConvertDocxToPdfHelper: drives the
 * catch(IllegalArgumentException) block with a non-null file, and reflects into
 * handleValidationFailure() to cover its documentFile == null branch, which the
 * public method can never reach with a null file (null always short-circuits into
 * the explicit IllegalArgumentException throw).
 */
@DisplayName("ConvertImageToPdfHelper Branch Coverage Tests")
class ConvertImageToPdfHelperBranchCoverageTest {

    private KristaMediaClient kristaMediaClient;
    private TelemetryHelper telemetryHelper;
    private ValidationOrchestrator validationOrchestrator;
    private ErrorHandlingStateManager internalStateManager;
    private ExtensionResponseGenerator responseGenerator;
    private ConvertImageToPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kristaMediaClient = mock(KristaMediaClient.class);
        telemetryHelper = mock(TelemetryHelper.class);
        validationOrchestrator = mock(ValidationOrchestrator.class);
        internalStateManager = mock(ErrorHandlingStateManager.class);
        responseGenerator = mock(ExtensionResponseGenerator.class);
        helper = new ConvertImageToPdfHelper(
                kristaMediaClient, telemetryHelper, validationOrchestrator, internalStateManager, responseGenerator);
    }

    @Test
    @DisplayName("Non-null image whose toJavaFile raises IllegalArgumentException hits the input-error branch with a non-null file")
    void testConvertImageToPdf_ToJavaFileThrowsIllegalArgumentException_NonNullFile() throws Exception {
        File imageFile = mock(File.class);
        when(imageFile.getFileName()).thenReturn("broken.jpg");

        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(kristaMediaClient.toJavaFile(imageFile))
                .thenThrow(new IllegalArgumentException("Corrupt image"));

        ExtensionResponse response = helper.convertImageToPdf(imageFile, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.convertImageToPdf"), anyLong(), any(IllegalArgumentException.class), any());
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

        Method handleValidationFailure = ConvertImageToPdfHelper.class.getDeclaredMethod(
                "handleValidationFailure", List.class, File.class, Boolean.class);
        handleValidationFailure.setAccessible(true);

        assertDoesNotThrow(() -> handleValidationFailure.invoke(helper, validationResults, null, true));

        verify(internalStateManager).put(anyString(), any());
    }

    // NOTE: the generic catch(Exception)'s "documentFile == null" ternary branch is true dead
    // code via the public API for the same reason as in ConvertDocxToPdfHelper - a null
    // documentFile always throws IllegalArgumentException first, which is caught by the more
    // specific catch(IllegalArgumentException) block. Intentionally left uncovered.
}

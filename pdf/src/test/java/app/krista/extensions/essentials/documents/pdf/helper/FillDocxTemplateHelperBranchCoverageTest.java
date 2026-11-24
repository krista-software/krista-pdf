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
import app.krista.extensions.essentials.documents.pdf.service.DocxManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage focused unit tests for {@link FillDocxTemplateHelper}.
 *
 * <p>Targets the two remaining branches in {@code handleValidationFailure}:
 * <ul>
 *   <li>{@code templateFields != null} - reachable via the public API by passing a null
 *       {@code FreeForm} together with a validation failure and retry enabled.</li>
 *   <li>{@code templateFile != null} - always true through the public API (the outer
 *       null-check on templateFile already guards the only call site), so its false side
 *       is exercised via reflection.</li>
 * </ul>
 */
@DisplayName("FillDocxTemplateHelper Branch Coverage Tests")
class FillDocxTemplateHelperBranchCoverageTest {

    private ValidationOrchestrator validationOrchestrator;
    private ErrorHandlingStateManager internalStateManager;
    private DocxManager docxManager;
    private KristaMediaClientUtil kristaMediaClientUtil;
    private TelemetryHelper telemetryHelper;
    private ExtensionResponseGenerator responseGenerator;
    private FillDocxTemplateHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationOrchestrator = mock(ValidationOrchestrator.class);
        internalStateManager = mock(ErrorHandlingStateManager.class);
        docxManager = mock(DocxManager.class);
        kristaMediaClientUtil = mock(KristaMediaClientUtil.class);
        telemetryHelper = mock(TelemetryHelper.class);
        responseGenerator = mock(ExtensionResponseGenerator.class);
        helper = new FillDocxTemplateHelper(
                validationOrchestrator, internalStateManager, docxManager, kristaMediaClientUtil,
                telemetryHelper, responseGenerator);
    }

    @Test
    @DisplayName("Validation failure with retry enabled and null templateFields flips the templateFields != null branch to false")
    void testFillTheDocxTemplate_ValidationFailure_WithRetry_NullTemplateFields() {
        File templateFile = mock(File.class);
        when(templateFile.getFileName()).thenReturn("template.txt");

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult("Confirm", "Field", "Fetch", "Error", "Type");
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_TEMPLATE_FORMAT_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        ExtensionResponse response = helper.fillTheDocxTemplate(templateFile, null, true);

        assertNotNull(response);
        verify(internalStateManager).put(anyString(), any());
        verify(responseGenerator).generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_TEMPLATE_FORMAT_VALIDATION),
                any());
    }

    @Test
    @DisplayName("handleValidationFailure(): templateFile == null is unreachable via the public method, exercised via reflection")
    void testHandleValidationFailure_NullTemplateFile_ViaReflection() throws Exception {
        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult("Confirm", "Field", "Fetch", "Error", "Type");
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), any(), any(), any()))
                .thenReturn(confirmationResponse);

        Method handleValidationFailure = FillDocxTemplateHelper.class.getDeclaredMethod(
                "handleValidationFailure", List.class, File.class,
                app.krista.model.base.FreeForm.class, Boolean.class);
        handleValidationFailure.setAccessible(true);

        assertDoesNotThrow(() -> handleValidationFailure.invoke(helper, validationResults, null, null, true));

        verify(internalStateManager).put(anyString(), any());
    }
}

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
 * Branch-coverage focused unit tests for {@link RichTextPdfHelper}, targeting the
 * handful of branches left uncovered by {@code RichTextPdfHelperIntegrationTest}:
 * the "title == null" catch-block branches, and the optional-field null-checks inside
 * {@code handleValidationFailure}. One branch there (richTextData == null) can never be
 * reached through the public method (the outer null-check on richTextData always blocks
 * it first), so it is exercised directly via reflection.
 */
@DisplayName("RichTextPdfHelper Branch Coverage Tests")
class RichTextPdfHelperBranchCoverageTest {

    private ValidationOrchestrator validationOrchestrator;
    private ErrorHandlingStateManager errorHandlingStateManager;
    private PDFManager pdfManager;
    private KristaMediaClientUtil kristaMediaClientUtil;
    private TelemetryHelper telemetryHelper;
    private ExtensionResponseGenerator responseGenerator;
    private RichTextPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationOrchestrator = mock(ValidationOrchestrator.class);
        errorHandlingStateManager = mock(ErrorHandlingStateManager.class);
        pdfManager = mock(PDFManager.class);
        kristaMediaClientUtil = mock(KristaMediaClientUtil.class);
        telemetryHelper = mock(TelemetryHelper.class);
        responseGenerator = mock(ExtensionResponseGenerator.class);
        helper = new RichTextPdfHelper(
                validationOrchestrator,
                errorHandlingStateManager,
                pdfManager,
                kristaMediaClientUtil,
                telemetryHelper,
                responseGenerator
        );
    }

    @Test
    @DisplayName("Null richTextData with null title hits the N/A ternary branch in the IllegalArgumentException catch block")
    void testGetRichTextPDF_NullRichTextAndNullTitle_CatchesIllegalArgumentWithNullTitle() throws Exception {
        ExtensionResponse response = helper.getRichTextPDF(null, null, null, null, null, null, null, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.getRichTextPDF"), anyLong(), any(IllegalArgumentException.class), any());
        verify(pdfManager, never()).getRichTextPDF(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Generic exception with null title hits the N/A ternary branch in the generic catch block")
    void testGetRichTextPDF_GenericExceptionWithNullTitle() throws Exception {
        String richText = "content";
        when(validationOrchestrator.validate(any())).thenReturn(new ArrayList<>());
        when(pdfManager.getRichTextPDF(any(), any(), any(), any(), any(), any(), eq(richText)))
                .thenThrow(new RuntimeException("generation failed"));

        ExtensionResponse response = helper.getRichTextPDF(null, null, null, null, null, null, richText, false);

        assertNotNull(response);
        verify(telemetryHelper).recordError(eq("pdf.getRichTextPDF"), anyLong(), any(RuntimeException.class), any());
    }

    @Test
    @DisplayName("Validation failure with retry=true and all optional fields populated flips the opposite null-check branches")
    void testGetRichTextPDF_ValidationFailure_WithRetry_AllOptionalFieldsPresent_TitleNull() {
        String richText = "xyz";
        File logo = mock(File.class);

        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error: too short", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);

        when(validationOrchestrator.validate(any())).thenReturn(validationResults);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_RICH_TEXT_VALIDATION),
                any()
        )).thenReturn(confirmationResponse);

        // title == null (flips branch 177 to false), all other optional fields non-null
        // (flips branches 178-182 to true) - the existing integration test exercises the
        // exact opposite combination.
        ExtensionResponse response = helper.getRichTextPDF(
                null, "Sub", "Desc", "Head", "Foot", logo, richText, true);

        assertNotNull(response);
        verify(errorHandlingStateManager).put(anyString(), any());
        verify(responseGenerator).generateConfirmationResponse(
                any(ExtensionResponse.Error.ExceptionType.class),
                eq(validationResults),
                eq(SubCatalogConstants.CONFIRM_RICH_TEXT_VALIDATION),
                any()
        );
    }

    @Test
    @DisplayName("handleValidationFailure(): richTextData == null is unreachable via the public method, exercised via reflection")
    void testHandleValidationFailure_NullRichTextData_ViaReflection() throws Exception {
        ValidationOrchestrator.ValidationResult validationError =
                new ValidationOrchestrator.ValidationResult(
                        "Confirm", "Field", "Fetch", "Error", "Type"
                );
        List<ValidationOrchestrator.ValidationResult> validationResults = Collections.singletonList(validationError);
        ExtensionResponse confirmationResponse = mock(ExtensionResponse.class);
        when(responseGenerator.generateConfirmationResponse(any(), any(), any(), any()))
                .thenReturn(confirmationResponse);

        Method handleValidationFailure = RichTextPdfHelper.class.getDeclaredMethod(
                "handleValidationFailure", List.class, String.class, String.class, String.class,
                String.class, String.class, File.class, String.class, Boolean.class);
        handleValidationFailure.setAccessible(true);

        assertDoesNotThrow(() -> handleValidationFailure.invoke(
                helper, validationResults, "Title", null, null, null, null, null, null, true));

        verify(errorHandlingStateManager).put(anyString(), any());
    }
}

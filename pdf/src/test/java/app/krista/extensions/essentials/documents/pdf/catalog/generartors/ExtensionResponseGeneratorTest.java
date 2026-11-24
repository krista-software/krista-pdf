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

package app.krista.extensions.essentials.documents.pdf.catalog.generartors;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.RemediationAction;
import app.krista.extension.executor.impl.AskAPersonAction;
import app.krista.extension.executor.impl.InformAPersonAction;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.FieldTypes;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.model.field.NamedField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExtensionResponseGenerator covering confirmation, fetch,
 * fetch-deny, and direct validation-error response generation across empty,
 * single, and multiple validation-result inputs.
 */
@DisplayName("ExtensionResponseGenerator Unit Tests")
public class ExtensionResponseGeneratorTest {

    private final ExtensionResponseGenerator generator = new ExtensionResponseGenerator();

    private ValidationOrchestrator.ValidationResult result(String confirmMsg, String fetchFieldName,
                                                             String fetchMsg, String errMsg, String fieldType) {
        return new ValidationOrchestrator.ValidationResult(confirmMsg, fetchFieldName, fetchMsg, errMsg, fieldType);
    }

    // ==================== generateConfirmationResponse ====================

    @Test
    @DisplayName("generateConfirmationResponse with empty results produces an empty ask message and a Reenter switch field")
    void testGenerateConfirmationResponse_EmptyResults() {
        ExtensionResponse response = generator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                List.of(),
                "subReq1",
                Map.of("k", "v"));

        assertNotNull(response);
        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertEquals("Input validation failed. Would you like to re-enter the correct values?",
                response.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
        assertEquals("subReq1", response.getRemediationActions().getSubCatalogRequestName());
        assertEquals(Map.of("k", "v"), response.getState());

        List<RemediationAction> actions = response.getRemediationActions().getRemediationActionList();
        assertEquals(1, actions.size());
        AskAPersonAction action = (AskAPersonAction) actions.get(0);
        assertEquals("", action.getMessage(), "no validation results means an empty step message");
        assertEquals(RemediationAction.RecipientType.ACTIVE_USER, action.getRecipientType());

        List<NamedField> fields = action.getFields();
        assertEquals(1, fields.size());
        assertEquals(SubCatalogConstants.REENTER, fields.get(0).getName());
        assertEquals(FieldTypes.SWITCH_FIELD, fields.get(0).getFieldType());
    }

    @Test
    @DisplayName("generateConfirmationResponse concatenates the confirm-step message for each validation result")
    void testGenerateConfirmationResponse_MultipleResults() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm msg1", "field1", "Fetch msg1", "Error msg1", FieldTypes.TEXT_FIELD),
                result("Confirm msg2", "field2", "Fetch msg2", "Error msg2", FieldTypes.TEXT_FIELD));

        ExtensionResponse response = generator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.LOGIC_ERROR,
                results,
                "subReq2",
                null);

        assertEquals(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, response.getError().getExceptionType());
        assertNull(response.getState());

        AskAPersonAction action =
                (AskAPersonAction) response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("Confirm msg1\nConfirm msg2\n", action.getMessage());
        // confirmation path only ever produces the single Reenter switch field, regardless of result count
        assertEquals(1, action.getFields().size());
        assertEquals(SubCatalogConstants.REENTER, action.getFields().get(0).getName());
    }

    @ParameterizedTest
    @EnumSource(ExtensionResponse.Error.ExceptionType.class)
    @DisplayName("generateConfirmationResponse propagates every ExceptionType value unchanged")
    void testGenerateConfirmationResponse_AllExceptionTypes(ExtensionResponse.Error.ExceptionType type) {
        ExtensionResponse response = generator.generateConfirmationResponse(
                type, List.of(), "subReqEnum", Map.of());

        assertEquals(type, response.getError().getExceptionType());
    }

    // ==================== generateFetchResponse ====================

    @Test
    @DisplayName("generateFetchResponse with empty results produces no fields and an empty ask message")
    void testGenerateFetchResponse_EmptyResults() {
        ExtensionResponse response = generator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                List.of(),
                "subReq3",
                Map.of());

        assertEquals("Please provide the correct values for the highlighted fields.",
                response.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, response.getError().getExceptionType());

        AskAPersonAction action =
                (AskAPersonAction) response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("", action.getMessage());
        assertTrue(action.getFields().isEmpty());
    }

    @Test
    @DisplayName("generateFetchResponse builds one field per validation result using the fetch step message")
    void testGenerateFetchResponse_MultipleResultsBuildFields() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm1", "textField", "Fetch msg1", "Error1", FieldTypes.TEXT_FIELD),
                result("Confirm2", "fileField", "Fetch msg2", "Error2", FieldTypes.FILE_FIELD));

        ExtensionResponse response = generator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.AUTHENTICATION_ERROR,
                results,
                "subReq4",
                Map.of("stateId", "abc"));

        assertEquals(Map.of("stateId", "abc"), response.getState());
        assertEquals("subReq4", response.getRemediationActions().getSubCatalogRequestName());

        AskAPersonAction action =
                (AskAPersonAction) response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("Fetch msg1\nFetch msg2\n", action.getMessage());

        List<NamedField> fields = action.getFields();
        assertEquals(2, fields.size());

        assertEquals("textField", fields.get(0).getName());
        assertEquals(FieldTypes.TEXT_FIELD, fields.get(0).getFieldType());

        assertEquals("fileField", fields.get(1).getName());
        assertEquals(FieldTypes.FILE_FIELD, fields.get(1).getFieldType());
        // the FILE_FIELD branch of NamedFieldFactory.createField attaches file-upload attributes
        assertTrue(fields.get(1).getAttributeValues().containsKey("multipleFileUpload"));
    }

    @Test
    @DisplayName("generateFetchResponse builds a List field with the composite attribute payload")
    void testGenerateFetchResponse_ListFieldType() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm", "listField", "Fetch msg", "Error", FieldTypes.LIST_FIELD));

        ExtensionResponse response = generator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.UNAVAILABILITY_ERROR,
                results,
                "subReq5",
                Map.of());

        AskAPersonAction action =
                (AskAPersonAction) response.getRemediationActions().getRemediationActionList().get(0);
        NamedField field = action.getFields().get(0);
        assertEquals(FieldTypes.LIST_FIELD, field.getFieldType());
        assertTrue(field.getAttributeValues().containsKey("field"));
    }

    // ==================== generateFetchDenyResponse ====================

    @Test
    @DisplayName("generateFetchDenyResponse with one result lists the single field name")
    void testGenerateFetchDenyResponse_SingleResult() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm", "Email", "Fetch", "bad email", FieldTypes.TEXT_FIELD));

        ExtensionResponse response = generator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.AUTHENTICATION_ERROR,
                results,
                "subReq6",
                Map.of("x", 1));

        assertEquals("Operation cancelled. No values were updated.", response.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.AUTHENTICATION_ERROR,
                response.getError().getExceptionType());
        assertEquals(Map.of("x", 1), response.getState());

        InformAPersonAction action =
                (InformAPersonAction) response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("Updated value for 'Email' were not provided.", action.getMessage());
        assertEquals(RemediationAction.RecipientType.ALL_PARTICIPANTS, action.getRecipientType());
        assertTrue(action.getValuedFields().isEmpty());
    }

    @Test
    @DisplayName("generateFetchDenyResponse with multiple results joins field names with commas")
    void testGenerateFetchDenyResponse_MultipleResults() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm1", "Email", "Fetch1", "bad email", FieldTypes.TEXT_FIELD),
                result("Confirm2", "Phone", "Fetch2", "bad phone", FieldTypes.TEXT_FIELD));

        ExtensionResponse response = generator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.LOGIC_ERROR,
                results,
                "subReq7",
                Map.of());

        InformAPersonAction action =
                (InformAPersonAction) response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("Updated value for 'Email', 'Phone' were not provided.", action.getMessage());
    }

    @Test
    @DisplayName("generateFetchDenyResponse with no results still produces the surrounding prefix/suffix text")
    void testGenerateFetchDenyResponse_EmptyResults() {
        ExtensionResponse response = generator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                List.of(),
                "subReq8",
                Map.of());

        InformAPersonAction action =
                (InformAPersonAction) response.getRemediationActions().getRemediationActionList().get(0);
        assertEquals("Updated value for  were not provided.", action.getMessage());
    }

    // ==================== generateValidationErrorResponse ====================

    @Test
    @DisplayName("generateValidationErrorResponse with empty results yields an empty trimmed error message")
    void testGenerateValidationErrorResponse_EmptyResults() {
        ExtensionResponse response = generator.generateValidationErrorResponse(List.of());

        assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
        assertNull(response.getResponseValue());
        assertNull(response.getRemediationActions());
        assertNull(response.getState());

        ExtensionResponse.Error error = response.getError();
        assertNotNull(error);
        assertEquals("", error.getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, error.getExceptionType());
        assertEquals("", error.getStackTrace());
        assertTrue(error.getTimestamp() > 0);
    }

    @Test
    @DisplayName("generateValidationErrorResponse joins multiple error messages with newlines and trims the result")
    void testGenerateValidationErrorResponse_MultipleResults() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm1", "field1", "Fetch1", "Error A", FieldTypes.TEXT_FIELD),
                result("Confirm2", "field2", "Fetch2", "Error B", FieldTypes.TEXT_FIELD));

        ExtensionResponse response = generator.generateValidationErrorResponse(results);

        assertEquals("Error A\nError B", response.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR, response.getError().getExceptionType());
    }

    @Test
    @DisplayName("generateValidationErrorResponse with a single result has no stray trailing newline")
    void testGenerateValidationErrorResponse_SingleResult() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm", "field1", "Fetch", "Only error", FieldTypes.TEXT_FIELD));

        ExtensionResponse response = generator.generateValidationErrorResponse(results);

        assertEquals("Only error", response.getError().getErrorMessage());
    }

    @Test
    @DisplayName("generateValidationErrorResponse renders a null error message as the literal 'null'")
    void testGenerateValidationErrorResponse_NullErrMessage() {
        List<ValidationOrchestrator.ValidationResult> results = List.of(
                result("Confirm", "field1", "Fetch", null, FieldTypes.TEXT_FIELD));

        ExtensionResponse response = generator.generateValidationErrorResponse(results);

        assertEquals("null", response.getError().getErrorMessage());
    }
}

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

import app.krista.extensions.essentials.documents.pdf.catalog.factories.ExtensionResponseFactory;
import app.krista.extensions.essentials.documents.pdf.catalog.factories.NamedFieldFactory;
import app.krista.extensions.essentials.documents.pdf.catalog.factories.RemediationActionFactory;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.model.field.NamedField;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class ExtensionResponseGenerator {

    public static final String ERROR_MESSAGE = "ErrorMessage";
    public static final String STEP_MESSAGE = "StepMessage";
    public static final String FIELD = "Field";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionResponseGenerator.class);

    public ExtensionResponse generateConfirmationResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state) {

        LOGGER.info("ExtensionResponseGenerator.generateConfirmationResponse() called with {} validation results for subCatalogRequest: {}",
                   validationResults.size(), subCatalogRequestName);

        List<NamedField> fields = List.of(NamedFieldFactory.createSwitchField(SubCatalogConstants.REENTER));
        LOGGER.info("Fields: {}", fields);
        Map<String, Object> stringStringMap = generateResponse(validationResults, false);
        LOGGER.info("Generated response: {}", stringStringMap);

        // Use the new askAPersonToReenter method for better user experience
        String confirmationMessage = "Input validation failed. Would you like to re-enter the correct values?";
        LOGGER.info("Confirmation message: {}", confirmationMessage);
        ExtensionResponse response = ExtensionResponseFactory.create(confirmationMessage, exceptionType,
                List.of(RemediationActionFactory.createAskAction((String) stringStringMap.get(STEP_MESSAGE), fields)),
                subCatalogRequestName, state);

        // Enhanced logging for detailed response information
        LOGGER.debug("ExtensionResponseGenerator.generateConfirmationResponse() completed successfully - Confirmation response generated");
        LOGGER.info("Generated ExtensionResponse: {}", response);
        LOGGER.debug("Response result: {}", response.getResult());
        LOGGER.debug("Response error: {}", response.getError());
        LOGGER.debug("Response error message: {}", response.getError() != null ? response.getError().getErrorMessage() : "null");
        LOGGER.debug("Remediation Actions: {}", response.getRemediationActions());
        LOGGER.debug("State: {}", response.getState());

        // Add detailed JSON representation of the response
        try {
            LOGGER.info("Full response JSON: {}", SubCatalogConstants.GSON.toJson(response));
        } catch (Exception e) {
            LOGGER.warn("Could not serialize response to JSON: {}", e.getMessage());
        }

        return response;
    }

    private Map<String, Object> generateResponse(List<ValidationOrchestrator.ValidationResult> validationResults, boolean fetchResponse) {
        StringBuilder stepMessage = new StringBuilder();
        StringBuilder errMessage = new StringBuilder();
        List<NamedField> fields = new ArrayList<>();
        LOGGER.info("Generating response for {} validation results", validationResults.size());
        for (ValidationOrchestrator.ValidationResult validationResult : validationResults) {
            stepMessage.append(!fetchResponse ? validationResult.getConfirmStepMessage() : validationResult.getFetchStepMessage()).append("\n");
            errMessage.append(validationResult.getErrMessage()).append("\n");
            if (fetchResponse) {
                fields.add(NamedFieldFactory.createField(validationResult.getFetchFieldName(), validationResult.getFieldType()));
            }
        }
        LOGGER.info("Generated response: stepMessage={}, errMessage={}, fields={}", stepMessage, errMessage, fields);
        return Map.of(STEP_MESSAGE, stepMessage.toString(), ERROR_MESSAGE, errMessage.toString(), FIELD, fields);
    }

    @SuppressWarnings("unchecked")
    public ExtensionResponse generateFetchResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state) {

        LOGGER.info("ExtensionResponseGenerator.generateFetchResponse() called with {} validation results for subCatalogRequest: {}",
                validationResults.size(), subCatalogRequestName);

        Map<String, Object> stringStringMap = generateResponse(validationResults, true);

        // Use a user-friendly message for fetch responses
        String fetchMessage = "Please provide the correct values for the highlighted fields.";
        ExtensionResponse response = ExtensionResponseFactory.create(fetchMessage, exceptionType,
                List.of(RemediationActionFactory.createAskAction((String) stringStringMap.get(STEP_MESSAGE), (List<NamedField>) stringStringMap.get(FIELD))),
                subCatalogRequestName, state);

        LOGGER.debug("ExtensionResponseGenerator.generateFetchResponse() completed successfully - Fetch response generated");

        return response;
    }

    public ExtensionResponse generateFetchDenyResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state
    ) {
        LOGGER.info("ExtensionResponseGenerator.generateFetchDenyResponse() called with {} validation results for subCatalogRequest: {}",
                   validationResults.size(), subCatalogRequestName);

        StringJoiner stepMessage = new StringJoiner(", ", "Updated value for ", " were not provided.");
        StringBuilder errMessage = new StringBuilder();
        for (ValidationOrchestrator.ValidationResult validationResult : validationResults) {
            StringBuilder fieldNameBuilder = new StringBuilder();
            fieldNameBuilder.append("'").append(validationResult.getFetchFieldName()).append("'");
            stepMessage.add(fieldNameBuilder.toString());
            errMessage.append(validationResult.getErrMessage()).append("\n");
        }

        // Use a user-friendly message for deny responses
        String denyMessage = "Operation cancelled. No values were updated.";
        ExtensionResponse response = ExtensionResponseFactory.create(denyMessage, exceptionType,
                List.of(RemediationActionFactory.createInformActionALLParticipants(stepMessage.toString(), List.of())),
                subCatalogRequestName, state);

        LOGGER.debug("ExtensionResponseGenerator.generateFetchDenyResponse() completed successfully - Deny response generated");

        return response;
    }

    /**
     * Generates a direct validation error response (WITHOUT retry).
     * Returns a simple error message without remediation actions.
     * This is used when Allow Retry is disabled.
     *
     * @param validationResults the validation results containing error messages
     * @return ExtensionResponse with direct error message
     */
    public ExtensionResponse generateValidationErrorResponse(
            List<ValidationOrchestrator.ValidationResult> validationResults) {

        LOGGER.info("ExtensionResponseGenerator.generateValidationErrorResponse() called with {} validation results",
                validationResults.size());

        // Build error message from validation results
        StringBuilder errMessage = new StringBuilder();
        for (ValidationOrchestrator.ValidationResult result : validationResults) {
            errMessage.append(result.getErrMessage()).append("\n");
        }

        String errorMessage = errMessage.toString().trim();
        LOGGER.debug("Generated direct error message: {}", errorMessage);

        // Create error response without remediation actions
        ExtensionResponse.Error error = new ExtensionResponse.Error(
                errorMessage,
                System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                ""
        );

        ExtensionResponse response = new ExtensionResponse(
                ExtensionResponse.Result.FAILURE,
                null,
                error,
                null,
                null
        );

        LOGGER.debug("ExtensionResponseGenerator.generateValidationErrorResponse() completed - Direct error response generated");

        return response;
    }
}


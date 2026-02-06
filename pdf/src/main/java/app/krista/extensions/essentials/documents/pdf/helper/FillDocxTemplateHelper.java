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

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.TemplateFileFormatValidator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.extensions.essentials.documents.pdf.service.DocxManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import app.krista.model.base.FreeForm;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for filling docx templates and handling validation.
 */
@Service
public class FillDocxTemplateHelper {
    private static final Logger logger = LoggerFactory.getLogger(FillDocxTemplateHelper.class);

    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final DocxManager docxManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final ExtensionResponseGenerator responseGenerator;

    /**
     * Constructor for FillDocxTemplateHelper.
     */
    @Inject
    public FillDocxTemplateHelper(
            ValidationOrchestrator validationOrchestrator,
            ErrorHandlingStateManager internalStateManager,
            DocxManager docxManager,
            KristaMediaClientUtil kristaMediaClientUtil,
            TelemetryHelper telemetryHelper,
            ExtensionResponseGenerator responseGenerator) {
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.docxManager = docxManager;
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.telemetryHelper = telemetryHelper;
        this.responseGenerator = responseGenerator;
        
        // Register the template file format validator
        this.validationOrchestrator.registerValidator(
                Validator.ValidationResource.TEMPLATE_FILE_FORMAT,
                new TemplateFileFormatValidator()
        );
        logger.debug("TemplateFileFormatValidator registered");
    }

    /**
     * Fill a docx template with the given fields and convert to PDF.
     *
     * @param templateFile The docx template file
     * @param templateFields The fields to fill in the template
     * @param allowRetry whether to allow retry on validation failure
     * @return An ExtensionResponse containing the filled PDF file or error details
     */
    public ExtensionResponse fillTheDocxTemplate(File templateFile, FreeForm templateFields, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        logger.info("FillDocxTemplateHelper.fillTheDocxTemplate() - Starting with template file: {}, allowRetry={}",
                templateFile != null ? templateFile.getFileName() : "null", allowRetry);
        logger.debug("FillDocxTemplateHelper.fillTheDocxTemplate() - Template fields: {}", templateFields);
        telemetryHelper.incrementCount("pdf.fillTheDocxTemplate");
        
        try {
            // Validate file format first
            if (templateFile != null) {
                logger.debug("Validating DOCX format: {}", templateFile.getFileName());

                // Create validation context
                Map<Validator.ValidationResource, String> context = new HashMap<>();
                context.put(Validator.ValidationResource.TEMPLATE_FILE_FORMAT, templateFile.getFileName());

                // Validate
                List<ValidationOrchestrator.ValidationResult> validationResults =
                        validationOrchestrator.validate(context);

                if (!validationResults.isEmpty()) {
                    logger.warn("DOCX format validation failed with {} errors", validationResults.size());
                    return handleValidationFailure(validationResults, templateFile, templateFields, allowRetry);
                }
            } else {
                logger.error("No template file provided");
                throw new IllegalArgumentException("No template file provided");
            }
            
            // Process the template
            logger.info("Validation passed, filling docx template");
            java.io.File filledTemplate = docxManager.fillTheDocTemplate(
                    kristaMediaClientUtil.downloadFileHandler(templateFile), 
                    templateFields
            );
            
            // Upload the result
            File result = kristaMediaClientUtil.uploadFileHandler(filledTemplate);
            logger.info("Successfully filled docx template and converted to PDF");

            telemetryHelper.recordSuccess("pdf.fillTheDocxTemplate", startTime,
                    TelemetryHelper.safeTagMap("file_name", templateFile.getFileName()));

            return new ExtensionResponseBuilder()
                    .success(Map.of("Docx Template PDF File", result))
                    .build();
            
        } catch (Exception e) {
            telemetryHelper.recordError("pdf.fillTheDocxTemplate", startTime, e,
                    TelemetryHelper.safeTagMap("file_name", templateFile != null ? templateFile.getFileName() : "N/A"));
            logger.error("Failed to fill docx template: {}", e.getMessage(), e);

            return new ExtensionResponseBuilder()
                    .failure(new ErrorBuilder()
                            .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                            .errorMessage("Failed to fill docx template: " + e.getMessage())
                            .build())
                    .build();
        }
    }
    
    /**
     * Handle validation failure by generating a confirmation response.
     *
     * @param validationResults The validation results
     * @param templateFile The template file that failed validation
     * @param templateFields The template fields
     * @return An ExtensionResponse for the validation failure
     */
    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            File templateFile,
            FreeForm templateFields,
            Boolean allowRetry) {

        logger.warn("Validation failed within handleValidationFailure() with {} errors, allowRetry={}",
                validationResults.size(), allowRetry);

        // Check if retry is allowed
        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Allow Retry is enabled, generating confirmation response for retry flow");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            logger.debug("FillDocxTemplateHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            // Create state map with all parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (templateFile != null) state.put(SubCatalogConstants.TEMPLATE_FILE, List.of(templateFile));

            if (templateFields != null) {
                state.put("Template Fields", templateFields);
            }

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_TEMPLATE_FORMAT_VALIDATION,
                    state
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }
}
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
import app.krista.extensions.essentials.documents.pdf.catalog.validators.RichTextValidator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * Helper class for Rich Text to PDF conversion operations.
 * Provides centralized functionality for converting Rich Text to PDF format with validation.
 */
@Service
public class RichTextPdfHelper {

    private static final Logger logger = LoggerFactory.getLogger(RichTextPdfHelper.class);
    
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final PDFManager pdfManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final ExtensionResponseGenerator responseGenerator;

    /**
     * Constructor for RichTextPdfHelper.
     */
    @Inject
    public RichTextPdfHelper(
            ValidationOrchestrator validationOrchestrator,
            ErrorHandlingStateManager internalStateManager,
            PDFManager pdfManager,
            KristaMediaClientUtil kristaMediaClientUtil,
            TelemetryHelper telemetryHelper,
            ExtensionResponseGenerator responseGenerator) {
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.pdfManager = pdfManager;
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.telemetryHelper = telemetryHelper;
        this.responseGenerator = responseGenerator;
        
        // Register the validator
        this.validationOrchestrator.registerValidator(
                Validator.ValidationResource.RICH_TEXT_CONTENT,
                new RichTextValidator()
        );
        logger.debug("RichTextValidator registered");
    }

    /**
     * Converts Rich Text to PDF format with validation.
     *
     * @param title the title of the document
     * @param subTitle the subtitle of the document
     * @param description optional description
     * @param header header text
     * @param footer footer text
     * @param logo optional logo file
     * @param richTextData the rich text content to convert
     * @param allowRetry whether to allow retry on validation failure
     * @return ExtensionResponse containing the converted PDF file or error details
     */
    public ExtensionResponse getRichTextPDF(String title, String subTitle, String description,
                                           String header, String footer, File logo, String richTextData, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("RichTextPdfHelper.getRichTextPDF() called with allowRetry={}", allowRetry);
            telemetryHelper.incrementCount("pdf.getRichTextPDF");
            
            // Validate rich text content
            if (richTextData != null) {
                logger.debug("Validating rich text content");
                
                // Create validation context
                Map<Validator.ValidationResource, String> context = new HashMap<>();
                context.put(Validator.ValidationResource.RICH_TEXT_CONTENT, richTextData);
                
                // Validate
                List<ValidationOrchestrator.ValidationResult> validationResults = 
                        validationOrchestrator.validate(context);
                
                if (!validationResults.isEmpty()) {
                    logger.warn("Rich text validation failed with {} errors", validationResults.size());
                    return handleValidationFailure(validationResults, title, subTitle, description,
                                                  header, footer, logo, richTextData, allowRetry);
                }
            } else {
                logger.error("No rich text content provided");
                throw new IllegalArgumentException("Please provide rich text data.");
            }
            
            // Convert to PDF
            java.io.File javaFile = pdfManager.getRichTextPDF(
                title, subTitle, description, header, footer, 
                kristaMediaClientUtil.downloadFileHandler(logo), richTextData
            );
            
            // Upload the result
            File result = kristaMediaClientUtil.uploadFileHandler(javaFile);
            
            // Create response map with the resulting PDF
            Map<String, Object> responseValue = new HashMap<>();
            responseValue.put("RichText PDF File", result);
            

            telemetryHelper.recordSuccess("pdf.getRichTextPDF", startTime,
                    TelemetryHelper.safeTagMap("title", title != null ? title : "N/A",
                            "has_rich_text", richTextData != null ? "true" : "false"));
            return new ExtensionResponseBuilder().success(responseValue).build();
            
        } catch (IllegalArgumentException cause) {

            telemetryHelper.recordError("pdf.getRichTextPDF", startTime, cause,
                    TelemetryHelper.safeTagMap("title", title != null ? title : "N/A"));
            logger.error("Invalid input: {}", cause.getMessage(), cause);
            
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage(cause.getMessage())
                    .build();
                    
            return new ExtensionResponseBuilder().failure(error).build();
            
        } catch (Exception cause) {

            telemetryHelper.recordError("pdf.getRichTextPDF", startTime, cause,
                    TelemetryHelper.safeTagMap("title", title != null ? title : "N/A"));
            logger.error("Error converting rich text to PDF: {}", cause.getMessage(), cause);
            
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                    .errorMessage("Failed to convert rich text to PDF: " + cause.getMessage())
                    .build();
                    
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }
    
    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String title, String subTitle, String description,
            String header, String footer, File logo, String richTextData, Boolean allowRetry) {

        logger.warn("Validation failed within handleValidationFailure() with {} errors, allowRetry={}",
                validationResults.size(), allowRetry);

        // Check if retry is allowed
        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Allow Retry is enabled, generating confirmation response for retry flow");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            logger.debug("RichTextPdfHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results and state
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            // Create state map with all parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (title != null) state.put("Title", title);
            if (subTitle != null) state.put("Sub Title", subTitle);
            if (description != null) state.put("Description", description);
            if (header != null) state.put("Header", header);
            if (footer != null) state.put("Footer", footer);
            if (logo != null) state.put("Logo", logo);
            if (richTextData != null) state.put("Rich Text Data", richTextData);

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_RICH_TEXT_VALIDATION,
                    state
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }
}
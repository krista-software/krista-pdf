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
import app.krista.extensions.essentials.documents.pdf.catalog.validators.DocxFileFormatValidator;
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
 * Helper class for DOCX to PDF conversion operations.
 * Provides centralized functionality for converting DOCX to PDF format with validation.
 */
@Service
public class ConvertDocxToPdfHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConvertDocxToPdfHelper.class);
    
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final PDFManager pdfManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final ExtensionResponseGenerator responseGenerator;

    /**
     * Constructor for ConvertDocxToPdfHelper.
     */
    @Inject
    public ConvertDocxToPdfHelper(
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
                Validator.ValidationResource.DOCX_FILE_FORMAT,
                new DocxFileFormatValidator()
        );
        logger.debug("DocxFileFormatValidator registered");
    }

    /**
     * Converts DOCX to PDF format with validation.
     *
     * @param documentFile the DOCX file to convert
     * @param allowRetry whether to allow retry on validation failure
     * @return File containing the converted PDF or ExtensionResponse with error details
     */
    public ExtensionResponse convertDocxToPDF(File documentFile, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("ConvertDocxToPdfHelper.convertDocxToPDF() called with documentFile={}, allowRetry={}",
                    documentFile != null ? documentFile.getFileName() : "null", allowRetry);
            telemetryHelper.incrementCount("pdf.convertDocxToPdf");
            
            // Validate file format first
            if (documentFile != null) {
                logger.debug("Validating DOCX format: {}", documentFile.getFileName());

                // Create validation context
                Map<Validator.ValidationResource, String> context = new HashMap<>();
                context.put(Validator.ValidationResource.DOCX_FILE_FORMAT, documentFile.getFileName());

                // Validate
                List<ValidationOrchestrator.ValidationResult> validationResults =
                        validationOrchestrator.validate(context);

                if (!validationResults.isEmpty()) {
                    logger.warn("DOCX format validation failed with {} errors", validationResults.size());
                    return handleValidationFailure(validationResults, documentFile, allowRetry);
                }
            } else {
                logger.error("No document file provided");
                throw new IllegalArgumentException("No document file provided");
            }
            
            // Download and convert file
            java.io.File javaFile = pdfManager.convertDocToPDF(kristaMediaClientUtil.downloadFileHandler(documentFile));
            File result = kristaMediaClientUtil.uploadFileHandler(javaFile);
            

            telemetryHelper.recordSuccess("pdf.convertDocxToPdf", startTime,
                    TelemetryHelper.safeTagMap("file_name", documentFile.getFileName()));

            // Create response map with the resulting PDF
            Map<String, Object> responseValue = new HashMap<>();
            responseValue.put("Docx PDF File", result);

            return new ExtensionResponseBuilder().success(responseValue).build();
            
        } catch (IllegalArgumentException cause) {

            telemetryHelper.recordError("pdf.convertDocxToPdf", startTime, cause,
                    TelemetryHelper.safeTagMap("file_name", documentFile != null ? documentFile.getFileName() : "N/A"));
            logger.error("Invalid input file: {}", cause.getMessage(), cause);
            
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The uploaded file is not supported. Please upload a valid Microsoft Word document (.docx file).")
                    .build();
                    
            return new ExtensionResponseBuilder().failure(error).build();
            
        } catch (Exception cause) {

            telemetryHelper.recordError("pdf.convertDocxToPdf", startTime, cause,
                    TelemetryHelper.safeTagMap("file_name", documentFile != null ? documentFile.getFileName() : "N/A"));
            logger.error("Error converting DOCX to PDF: {}", cause.getMessage(), cause);
            
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                    .errorMessage("Failed to convert document to PDF: " + cause.getMessage())
                    .build();
                    
            return new ExtensionResponseBuilder().failure(error).build();
        }
    }
    
    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            File documentFile,
            Boolean allowRetry) {

        logger.warn("Validation failed within handleValidationFailure() with {} errors, allowRetry={}",
                validationResults.size(), allowRetry);

        // Check if retry is allowed
        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Allow Retry is enabled, generating confirmation response for retry flow");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            logger.debug("ConvertDocxToPdfHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            // Create state map with all parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (documentFile != null) state.put(SubCatalogConstants.DOCUMENT_FILE, List.of(documentFile));

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_DOCX_FORMAT_VALIDATION,
                    state
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }
}

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
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ImageFileFormatValidator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.extensions.essentials.documents.pdf.client.KristaMediaClient;
import app.krista.extensions.essentials.documents.pdf.util.PDFUtils;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * Helper class for converting image files to PDF format.
 * Encapsulates the business logic for image to PDF conversion.
 */
@Service
public class ConvertImageToPdfHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConvertImageToPdfHelper.class);
    
    private final KristaMediaClient kristaMediaClient;
    private final TelemetryHelper telemetryHelper;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public ConvertImageToPdfHelper(
            KristaMediaClient kristaMediaClient,
            TelemetryHelper telemetryHelper,
            ValidationOrchestrator validationOrchestrator,
            ErrorHandlingStateManager internalStateManager,
            ExtensionResponseGenerator responseGenerator) {
        this.kristaMediaClient = kristaMediaClient;
        this.telemetryHelper = telemetryHelper;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;

        // Register the validator
        this.validationOrchestrator.registerValidator(
                Validator.ValidationResource.IMAGE_FILE_FORMAT,
                new ImageFileFormatValidator()
        );
        logger.debug("ImageFileFormatValidator registered");
    }

    /**
     * Converts an image file (JPG, JPEG, PNG) into a PDF document.
     * This method accepts a single image file and converts it into a PDF.
     * The resulting PDF file is returned as part of the extension response.
     *
     * @param documentFile The input image file to convert. Must be JPG or PNG or JPEG.
     * @param allowRetry whether to allow retry on validation failure
     * @return An ExtensionResponse containing the generated PDF file if successful,
     * or error details if the conversion fails.
     */
    public ExtensionResponse convertImageToPdf(File documentFile, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        try {
            telemetryHelper.incrementCount("pdf.convertImageToPdf");
            logger.info("ConvertImageToPdfHelper.convertImageToPdf() called with documentFile={}, allowRetry={}",
                    documentFile != null ? documentFile.getFileName() : "null", allowRetry);

            // Collect all validation results
            List<ValidationOrchestrator.ValidationResult> validationResults = new ArrayList<>();

            // Validate file format
            if (documentFile != null) {
                logger.debug("Validating image format: {}", documentFile.getFileName());

                // Create validation context
                Map<Validator.ValidationResource, String> context = new HashMap<>();
                context.put(Validator.ValidationResource.IMAGE_FILE_FORMAT, documentFile.getFileName());

                // Validate
                validationResults = validationOrchestrator.validate(context);

                if (!validationResults.isEmpty()) {
                    logger.warn("Image format validation failed with {} errors", validationResults.size());
                    return handleValidationFailure(validationResults, documentFile, allowRetry);
                }
            } else {
                logger.error("No document file provided");
                throw new IllegalArgumentException("No document file provided");
            }

            // Convert input Krista File to Java File
            java.io.File sourceFile = kristaMediaClient.toJavaFile(documentFile);

            // Convert to PDF
            java.io.File pdfFile = PDFUtils.convertImageToPdf(sourceFile);

            // Convert Java File back to Krista File
            File kristaPdfFile = kristaMediaClient.toKristaFile(pdfFile);

            // Create response map with the resulting PDF
            Map<String, Object> responseValue = new HashMap<>();
            responseValue.put("PDF File", kristaPdfFile);


            telemetryHelper.recordSuccess("pdf.convertImageToPdf", startTime,
                    TelemetryHelper.safeTagMap("file_name", documentFile.getFileName()));
            return new ExtensionResponseBuilder().success(responseValue).build();
        } catch (IllegalArgumentException cause) {

            telemetryHelper.recordError("pdf.convertImageToPdf", startTime, cause,
                    TelemetryHelper.safeTagMap("file_name", documentFile != null ? documentFile.getFileName() : "N/A"));
            logger.error("Invalid input file: {}", cause.getMessage(), cause);

            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    "The uploaded file is not supported. Please upload a valid PNG, JPG, JPEG image.", 
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR, 
                    Arrays.toString(cause.getStackTrace()));

            return new ExtensionResponseBuilder().failure(error).build();
        } catch (Exception cause) {

            telemetryHelper.recordError("pdf.convertImageToPdf", startTime, cause,
                    TelemetryHelper.safeTagMap("file_name", documentFile != null ? documentFile.getFileName() : "N/A"));
            logger.error("Error converting file to PDF: {}", cause.getMessage(), cause);

            ExtensionResponse.Error error = new ExtensionResponse.Error(
                    "Failed to convert image to pdf: " + cause.getMessage(), 
                    System.currentTimeMillis(),
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR, 
                    Arrays.toString(cause.getStackTrace()));

            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    /**
     * Handles validation failure by generating a response with error details.
     *
     * @param validationResults the validation results
     * @param documentFile the original document file
     * @param allowRetry whether to allow retry on validation failure
     * @return ExtensionResponse with validation error details
     */
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
            logger.debug("ConvertImageToPdfHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            logger.debug("ConvertImageToPdfHelper.handleValidationFailure() - Stored validation results: {}", validationResults);

            // Create state map with all parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (documentFile != null) state.put(SubCatalogConstants.DOCUMENT_FILE, List.of(documentFile));

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_IMAGE_FORMAT_VALIDATION,
                    state
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }
}
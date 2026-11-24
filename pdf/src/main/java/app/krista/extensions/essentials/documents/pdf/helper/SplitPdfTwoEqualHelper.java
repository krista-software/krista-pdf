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

import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.catalog.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.ErrorBuilder;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Split PDF Into Two Equal Parts operations with validation support.
 * Handles PDF file validation and splitting functionality.
 */
@Service
public class SplitPdfTwoEqualHelper {

    private static final Logger logger = LoggerFactory.getLogger(SplitPdfTwoEqualHelper.class);

    private final PDFManager pdfManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public SplitPdfTwoEqualHelper(PDFManager pdfManager,
                                 KristaMediaClientUtil kristaMediaClientUtil,
                                 TelemetryHelper telemetryHelper,
                                 ValidationOrchestrator validationOrchestrator,
                                 ErrorHandlingStateManager internalStateManager,
                                 ExtensionResponseGenerator responseGenerator) {
        this.pdfManager = pdfManager;
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.telemetryHelper = telemetryHelper;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;

        logger.debug("SplitPdfTwoEqualHelper initialized with validation support");
    }

    /**
     * Splits a PDF file into two equal parts with validation.
     *
     * @param inputFile The PDF file to split
     * @param pageThreshHold Maximum pages per split file (default: 40 if not specified)
     * @param allowRetry whether to allow retry on validation failure
     * @return ExtensionResponse containing the list of split PDF files or error details
     */
    public ExtensionResponse splitPDFIntoTwoEqualPart(File inputFile, Double pageThreshHold, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("SplitPdfTwoEqualHelper.splitPDFIntoTwoEqualPart() called with file={}, allowRetry={}",
                    inputFile != null ? inputFile.getFileName() : "null", allowRetry);
            telemetryHelper.incrementCount("pdf.splitPDFIntoTwoEqualPart");

            // Validate file format first
            if (inputFile != null) {
                logger.debug("Validating PDF format: {}", inputFile.getFileName());

                // Create validation context
                Map<Validator.ValidationResource, String> context = new HashMap<>();
                context.put(Validator.ValidationResource.SPLIT_PDF_TWO_EQUAL_FILE_FORMAT, inputFile.getFileName());

                // Validate
                List<ValidationOrchestrator.ValidationResult> validationResults =
                        validationOrchestrator.validate(context);

                if (!validationResults.isEmpty()) {
                    logger.warn("PDF format validation failed with {} errors", validationResults.size());
                    return handleValidationFailure(validationResults, inputFile, pageThreshHold, allowRetry);
                }
            } else {
                logger.error("No PDF file provided");
                throw new IllegalArgumentException("No PDF file provided");
            }

            // Download and split file
            java.io.File javaFile = kristaMediaClientUtil.downloadFileHandler(inputFile);
            List<java.io.File> javaFiles = pdfManager.splitPDFIntoParts(javaFile, 
                    pageThreshHold != null ? pageThreshHold.intValue() : 40);

            if (javaFiles == null || javaFiles.isEmpty()) {
                logger.warn("PDF splitting returned no files");
                return new ExtensionResponseBuilder()
                        .success(Map.of("Two Equal Files", new ArrayList<>()))
                        .build();
            }

            logger.info("Successfully split PDF into {} files", javaFiles.size());

            List<File> kristaFiles = new ArrayList<>();
            for (java.io.File javaSplitFile : javaFiles) {
                File kristaFile = kristaMediaClientUtil.uploadFileHandler(javaSplitFile);
                kristaFiles.add(kristaFile);
            }

            logger.info("Successfully uploaded {} split files", kristaFiles.size());
            logger.info("Returning {} split PDF files", kristaFiles.size());

            telemetryHelper.recordSuccess("pdf.splitPDFIntoTwoEqualPart", startTime,
                    TelemetryHelper.safeTagMap("file_name", inputFile.getFileName(),
                            "split_count", String.valueOf(kristaFiles.size()),
                            "page_threshold", pageThreshHold != null ? String.valueOf(pageThreshHold.intValue()) : "40"));

            // Return the result as ExtensionResponse
            return new ExtensionResponseBuilder()
                    .success(Map.of("Two Equal Files", kristaFiles))
                    .build();

        } catch (IOException e) {
            telemetryHelper.recordError("pdf.splitPDFIntoTwoEqualPart", startTime, e,
                    TelemetryHelper.safeTagMap("file_name", inputFile != null ? inputFile.getFileName() : "N/A",
                            "page_threshold", pageThreshHold != null ? String.valueOf(pageThreshHold.intValue()) : "40"));
            logger.error("Failed to split PDF file '{}': {}", inputFile != null ? inputFile.getFileName() : "null", e.getMessage(), e);

            // Check for specific PDF corruption errors
            if (e.getMessage().contains("Missing root object specification") ||
                e.getMessage().contains("Header doesn't contain versioninfo") ||
                e.getMessage().contains("Error: Header doesn't contain versioninfo")) {

                ExtensionResponse.Error error = new ErrorBuilder()
                        .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                        .errorMessage("The uploaded PDF file appears to be corrupted or invalid. Please ensure the file is a valid PDF document and try again.")
                        .build();

                return new ExtensionResponseBuilder().failure(error).build();
            } else {
                ExtensionResponse.Error error = new ErrorBuilder()
                        .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                        .errorMessage("Failed to process the PDF file: " + e.getMessage())
                        .build();

                return new ExtensionResponseBuilder().failure(error).build();
            }
        } catch (Exception cause) {
            logger.error("Error splitting PDF: {}", cause.getMessage(), cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                    .errorMessage("An unexpected error occurred while processing the PDF: " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            File inputFile, Double pageThreshHold, Boolean allowRetry) {

        logger.warn("Validation failed within handleValidationFailure() with {} errors, allowRetry={}",
                validationResults.size(), allowRetry);

        // Check if retry is allowed
        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Allow Retry is enabled, generating confirmation response for retry flow");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            logger.debug("SplitPdfTwoEqualHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            // Create state map with all parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (inputFile != null) {
                state.put(SubCatalogConstants.SPLIT_PDF_TWO_EQUAL_INPUT_FILE, List.of(inputFile));
            }
            if (pageThreshHold != null) {
                state.put("Page Thresh Hold", pageThreshHold);
            }

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_SPLIT_PDF_TWO_EQUAL_VALIDATION,
                    state
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }
}

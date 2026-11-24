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
import app.krista.extensions.essentials.documents.pdf.catalog.utils.PdfSplitter;
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
 * Helper class for Split PDF operations with validation support.
 * Handles PDF file validation and splitting functionality.
 */
@Service
public class SplitPdfHelper {

    private static final Logger logger = LoggerFactory.getLogger(SplitPdfHelper.class);

    private final PDFManager pdfManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public SplitPdfHelper(PDFManager pdfManager,
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

        logger.debug("SplitPdfFileFormatValidator registered");
    }

    /**
     * Splits a PDF file into individual pages with validation.
     *
     * @param file The PDF file to split
     * @param category Optional list of categories for filtering
     * @param singleCategory Optional category for custom splitting logic
     * @param allowRetry whether to allow retry on validation failure
     * @return ExtensionResponse containing the list of split PDF files or error details
     */
    public ExtensionResponse splitPDFIntoPages(File file, List<String> category, String singleCategory, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("SplitPdfHelper.splitPDFIntoPages() called with file={}, allowRetry={}",
                    file != null ? file.getFileName() : "null", allowRetry);
            telemetryHelper.incrementCount("pdf.splitPDFIntoPages");

            // Validate file format first
            if (file != null) {
                logger.debug("Validating PDF format: {}", file.getFileName());

                // Create validation context
                Map<Validator.ValidationResource, String> context = new HashMap<>();
                context.put(Validator.ValidationResource.SPLIT_PDF_FILE_FORMAT, file.getFileName());

                // Validate
                List<ValidationOrchestrator.ValidationResult> validationResults =
                        validationOrchestrator.validate(context);

                if (!validationResults.isEmpty()) {
                    logger.warn("PDF format validation failed with {} errors", validationResults.size());
                    return handleValidationFailure(validationResults, file, category, singleCategory, allowRetry);
                }
            } else {
                logger.error("No PDF file provided");
                throw new IllegalArgumentException("No PDF file provided");
            }

            // Download and split file
            java.io.File javaFile = kristaMediaClientUtil.downloadFileHandler(file);

            List<java.io.File> javaFiles = null;
            if (singleCategory != null && singleCategory.equals("NET&SYN Talent Report")) {
                logger.info("Using custom splitting logic for NET&SYN Talent Report");
                javaFiles = PdfSplitter.splitPdfByCriteria(javaFile);
            } else {
                logger.info("Using standard PDF splitting logic");
                javaFiles = pdfManager.splitPDF(javaFile);
            }

            if (javaFiles == null || javaFiles.isEmpty()) {
                logger.warn("PDF splitting returned no files");
                return new ExtensionResponseBuilder()
                        .success(Map.of("Split Files", new ArrayList<>()))
                        .build();
            }

            logger.info("Successfully split PDF into {} files", javaFiles.size());

            List<File> kristaFiles = new ArrayList<>();
            for (java.io.File javaSplitFile : javaFiles) {
                File kristaFile = kristaMediaClientUtil.uploadFileHandler(javaSplitFile);
                kristaFiles.add(kristaFile);
            }

            logger.info("Successfully uploaded {} split files", kristaFiles.size());

            // Check if both category and singleCategory are present and if category contains singleCategory
            List<File> resultFiles = kristaFiles;
            if (category != null && singleCategory != null && category.contains(singleCategory)) {
                logger.debug("Filtering out first file based on category criteria");
                // Return kristaFiles without the first file
                resultFiles = kristaFiles.subList(1, kristaFiles.size());
            }

            logger.info("Returning {} split PDF files", resultFiles.size());

            telemetryHelper.recordSuccess("pdf.splitPDFIntoPages", startTime,
                    TelemetryHelper.safeTagMap("file_name", file.getFileName(),
                            "split_count", String.valueOf(resultFiles.size()),
                            "single_category", singleCategory != null ? singleCategory : "N/A"));

            // Return the result as ExtensionResponse
            return new ExtensionResponseBuilder()
                    .success(Map.of("Split Files", resultFiles))
                    .build();

        } catch (IOException e) {
            telemetryHelper.recordError("pdf.splitPDFIntoPages", startTime, e,
                    TelemetryHelper.safeTagMap("file_name", file != null ? file.getFileName() : "N/A"));
            logger.error("Failed to split PDF file '{}': {}", file != null ? file.getFileName() : "null", e.getMessage(), e);

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
            File file, List<String> category, String singleCategory, Boolean allowRetry) {

        logger.warn("Validation failed within handleValidationFailure() with {} errors, allowRetry={}",
                validationResults.size(), allowRetry);

        // Check if retry is allowed
        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Allow Retry is enabled, generating confirmation response for retry flow");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            logger.debug("SplitPdfHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            // Create state map with all parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (file != null) {
                state.put(SubCatalogConstants.SPLIT_PDF_FILE, List.of(file));
            }
            if (category != null) {
                state.put("Category", category);
            }
            if (singleCategory != null) {
                state.put("Single Category", singleCategory);
            }

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_SPLIT_PDF_VALIDATION,
                    state
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }
}

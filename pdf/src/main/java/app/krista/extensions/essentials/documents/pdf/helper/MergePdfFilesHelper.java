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
 * Helper class for PDF file merging operations.
 */
@Service
public class MergePdfFilesHelper {

    private static final Logger logger = LoggerFactory.getLogger(MergePdfFilesHelper.class);
    
    private final PDFManager pdfManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    /**
     * Constructor for MergePdfFilesHelper.
     */
    @Inject
    public MergePdfFilesHelper(
            PDFManager pdfManager,
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

        logger.debug("MergePdfFilesHelper initialized - using existing PdfFileFormatValidator");
    }

    /**
     * Merges multiple PDF files into a single PDF.
     *
     * @param title the title of the document
     * @param subTitle the subtitle of the document
     * @param description the description of the document
     * @param header the header text
     * @param footer the footer text
     * @param logo the logo file
     * @param inputFiles the list of PDF files to merge
     * @return the merged PDF file
     * @throws Exception if an error occurs during merging
     */
    public ExtensionResponse mergePdfFiles(
            String title,
            String subTitle,
            String description,
            String header,
            String footer,
            File logo,
            List<File> inputFiles,
            Boolean allowRetry) {

        long startTime = System.currentTimeMillis();
        try {
            logger.info("MergePdfFilesHelper.mergePdfFiles() called with allowRetry={}", allowRetry);
            logger.info("MergePdfFilesHelper.mergePdfFiles() called with title={}, inputFiles size={}",
                    title, inputFiles != null ? inputFiles.size() : 0);
            telemetryHelper.incrementCount("pdf.mergePdfFiles");
            
            // Validate input files
            if (inputFiles == null || inputFiles.isEmpty()) {
                logger.error("No PDF files provided for merging");
                return new ExtensionResponseBuilder()
                        .failure(new ErrorBuilder()
                                .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                                .errorMessage("Please provide at least one PDF file to merge")
                                .build())
                        .build();
            }

            // First, validate file count
            int validFileCount = (int) inputFiles.stream().filter(Objects::nonNull).count();

            // Create validation context for file count
            Map<Validator.ValidationResource, String> fileCountContext = new HashMap<>();
            fileCountContext.put(Validator.ValidationResource.PDF_MERGE_FILE_COUNT, "FILE_COUNT:" + validFileCount);

            // Validate file count
            List<ValidationOrchestrator.ValidationResult> fileCountResults =
                    validationOrchestrator.validate(fileCountContext);

            if (!fileCountResults.isEmpty()) {
                logger.warn("File count validation failed: {} files provided, need at least 2", validFileCount);
                return handleValidationFailure(fileCountResults, inputFiles, allowRetry);
            }

            // Second, validate each file format
            for (File inputFile : inputFiles) {
                if (inputFile == null) {
                    continue;
                }

                // Create validation context for file format
                Map<Validator.ValidationResource, String> fileFormatContext = new HashMap<>();
                fileFormatContext.put(Validator.ValidationResource.PDF_MERGE_FILE_FORMAT, inputFile.getFileName());

                // Validate file format
                List<ValidationOrchestrator.ValidationResult> formatResults =
                        validationOrchestrator.validate(fileFormatContext);

                if (!formatResults.isEmpty()) {
                    logger.warn("File format validation failed for file: {}", inputFile.getFileName());
                    return handleValidationFailure(formatResults, inputFiles, allowRetry);
                }
            }
            
            // Process the files
            List<java.io.File> mergeFiles = new ArrayList<>();
            for (File inputFile : inputFiles) {
                logger.debug("Downloading file for merge: {}", inputFile.getFileName());
                java.io.File downloadedFile = kristaMediaClientUtil.downloadFileHandler(inputFile);
                logger.debug("Downloaded file to local path: {}", downloadedFile.getAbsolutePath());
                mergeFiles.add(downloadedFile);
            }
            
            java.io.File javaPdfFile = pdfManager.mergePdfFiles(
                    title, subTitle, description, header, footer, 
                    kristaMediaClientUtil.downloadFileHandler(logo), mergeFiles);
            
            File result = kristaMediaClientUtil.uploadFileHandler(javaPdfFile);

            telemetryHelper.recordSuccess("pdf.mergePdfFiles", startTime,
                    TelemetryHelper.safeTagMap("file_count", String.valueOf(inputFiles.size()),
                            "title", title != null ? title : "N/A"));

            return new ExtensionResponseBuilder()
                    .success(Map.of("Merged PDF", result))
                    .build();
            
        } catch (Exception e) {
            telemetryHelper.recordError("pdf.mergePdfFiles", startTime, e,
                    TelemetryHelper.safeTagMap("file_count", inputFiles != null ? String.valueOf(inputFiles.size()) : "0",
                            "title", title != null ? title : "N/A"));
            logger.error("Error merging PDF files: {}", e.getMessage(), e);
            
            return new ExtensionResponseBuilder()
                    .failure(new ErrorBuilder()
                            .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                            .errorMessage("Failed to merge PDF files format: " + e.getMessage())
                            .build())
                    .build();
        }
    }
    
    /**
     * Handles validation failure by generating a confirmation response.
     *
     * @param validationResults the validation results
     * @param inputFiles the input files
     * @return an extension response for validation failure
     */
    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            List<File> inputFiles,
            Boolean allowRetry) {

        logger.warn("Validation failed within handleValidationFailure() with {} errors, allowRetry={}",
                validationResults.size(), allowRetry);

        // Check if retry is allowed
        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Allow Retry is enabled, generating confirmation response for retry flow");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            logger.debug("MergePdfFilesHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            // Create state map with all parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (inputFiles != null) state.put(SubCatalogConstants.DOCUMENT_FILE, List.of(inputFiles));


            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_PDF_MERGED_FORMAT_VALIDATION,
                    state
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }
}
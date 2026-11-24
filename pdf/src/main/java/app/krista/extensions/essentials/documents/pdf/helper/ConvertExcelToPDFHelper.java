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
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ExcelFileFormatValidator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.SheetNameValidator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.extensions.essentials.documents.pdf.client.KristaMediaClient;
import app.krista.extensions.essentials.documents.pdf.service.ExcelManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * Helper class for Excel to PDF conversion operations.
 * Provides centralized functionality for converting Excel files to PDF format.
 */
@Service
public class ConvertExcelToPDFHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConvertExcelToPDFHelper.class);
    private static final String STATE_ID = "stateId";
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalstateManager;
    private final ExcelManager excelManager;
    private final KristaMediaClient kristaMediaClient;
    private final TelemetryHelper telemetryHelper;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final ExtensionResponseGenerator responseGenerator;


    /**
     * Constructor for ConvertExcelToPDFHelper.
     *
     * @param validationOrchestrator for validating inputs
     * @param excelManager           for Excel operations
     * @param kristaMediaClient      for file operations
     * @param telemetryHelper        for telemetry tracking
     * @param kristaMediaClientUtil  for file handling utilities
     */
    @Inject
    public ConvertExcelToPDFHelper(
            ValidationOrchestrator validationOrchestrator,
            ErrorHandlingStateManager internalstateManager,
            ExcelManager excelManager,
            KristaMediaClient kristaMediaClient,
            TelemetryHelper telemetryHelper,
            KristaMediaClientUtil kristaMediaClientUtil, ExtensionResponseGenerator responseGenerator) {
        this.validationOrchestrator = validationOrchestrator;
        this.internalstateManager = internalstateManager;
        this.excelManager = excelManager;
        this.kristaMediaClient = kristaMediaClient;
        this.telemetryHelper = telemetryHelper;
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.responseGenerator = responseGenerator;

        // Register the Excel file format validator
        this.validationOrchestrator.registerValidator(
                Validator.ValidationResource.EXCEL_FILE_FORMAT,
                new ExcelFileFormatValidator()
        );

        // Register the sheet name validator (will be updated with workbook when needed)
        this.validationOrchestrator.registerValidator(
                Validator.ValidationResource.SHEET_NAME,
                new SheetNameValidator(null)
        );

        logger.info("ConvertExcelToPDFHelper initialized successfully");
    }

    /**
     * Converts an Excel file to PDF format with validation.
     *
     * @param excelFile the Excel file to convert
     * @param sheetName optional specific sheet name to convert
     * @param allowRetry whether to allow retry on validation failure
     * @return ExtensionResponse containing the converted PDF file or error details
     */
    public ExtensionResponse convertExcelToPDF(File excelFile, String sheetName, Boolean allowRetry) {

        long startTime = System.currentTimeMillis();
        try {
            logger.info("ConvertExcelToPDFHelper.convertExcelToPDF() called with excelFile={}, sheetName={}, allowRetry={}",
                    excelFile != null ? excelFile.getFileName() : "null", sheetName, allowRetry);
            telemetryHelper.incrementCount("pdf.convertExcelToPDF");

            // Collect all validation results
            List<ValidationOrchestrator.ValidationResult> allValidationResults = new ArrayList<>();

            // Validate file format first
            if (excelFile != null) {
                logger.debug("Validating Excel format: {}", excelFile.getFileName());

                // Create validation context for file format
                Map<Validator.ValidationResource, String> fileFormatContext = new HashMap<>();
                fileFormatContext.put(Validator.ValidationResource.EXCEL_FILE_FORMAT, excelFile.getFileName());

                // Validate file format
                List<ValidationOrchestrator.ValidationResult> fileFormatResults =
                        validationOrchestrator.validate(fileFormatContext);

                // Add file format validation results to the collection
                allValidationResults.addAll(fileFormatResults);

                // Only proceed with sheet name validation if file format is valid
                if (fileFormatResults.isEmpty()) {
                    // Download file for sheet name validation
                    java.io.File excelJavaFile = kristaMediaClientUtil.downloadFileHandler(excelFile);
                    logger.debug("File downloaded successfully");

                    // If sheet name is provided, validate it
                    if (sheetName != null && !sheetName.isEmpty()) {
                        logger.debug("Validating sheet name: {}", sheetName);

                        try {
                            // Load workbook
                            org.apache.poi.ss.usermodel.Workbook workbook = excelManager.getWorkbook(excelJavaFile);

                            // Register validator with workbook
                            this.validationOrchestrator.registerValidator(
                                    Validator.ValidationResource.SHEET_NAME,
                                    new SheetNameValidator(workbook)
                            );
                            logger.debug("SheetNameValidator registered with workbook");

                            // Create validation context for sheet name
                            Map<Validator.ValidationResource, String> sheetNameContext = new HashMap<>();
                            sheetNameContext.put(Validator.ValidationResource.SHEET_NAME, sheetName);
                            sheetNameContext.put(Validator.ValidationResource.DOCUMENT_FILE, excelFile.getFileName());

                            // Validate sheet name
                            List<ValidationOrchestrator.ValidationResult> sheetNameResults =
                                    validationOrchestrator.validate(sheetNameContext);

                            // Add sheet name validation results to the collection
                            allValidationResults.addAll(sheetNameResults);
                        } catch (Exception e) {
                            logger.warn("Error validating sheet name: {}", e.getMessage(), e);
                            // Create a validation result for the sheet name error
                            ValidationOrchestrator.ValidationResult sheetNameError = new ValidationOrchestrator.ValidationResult(
                                    "The sheet name could not be validated due to an error: " + e.getMessage(),
                                    SubCatalogConstants.SHEET_NAME,
                                    "Please enter a valid sheet name.",
                                    "Failed to validate sheet name: " + e.getMessage(),
                                    "Text"
                            );
                            allValidationResults.add(sheetNameError);
                        }
                    }

                    // If there are any validation errors, handle them
                    if (!allValidationResults.isEmpty()) {
                        logger.warn("Validation failed with {} errors", allValidationResults.size());
                        return handleValidationFailure(allValidationResults, excelFile, sheetName, allowRetry);
                    }
                    // If we get here, all validations passed
                    // We already downloaded the file above, so we can proceed with conversion
                    java.io.File pdfFile = excelManager.convertExcelToPDF(excelJavaFile, sheetName);
                    File result = kristaMediaClientUtil.uploadFileHandler(pdfFile);

                    telemetryHelper.recordSuccess("pdf.convertExcelToPdf", startTime,
                            TelemetryHelper.safeTagMap("file_name", excelFile.getFileName()));
                    return new ExtensionResponseBuilder()
                            .success(Map.of("Excel PDF File", result))
                            .build();
                } else {
                    // File format validation failed, return all validation results
                    logger.warn("Excel format validation failed with {} errors", fileFormatResults.size());
                    return handleValidationFailure(allValidationResults, excelFile, sheetName, allowRetry);
                }
            } else {
                logger.error("No Excel file provided");
                throw new IllegalArgumentException("No Excel file provided");
            }

        } catch (Exception cause) {
            assert excelFile != null;
            telemetryHelper.recordError("pdf.convertExcelToPdf", startTime, cause,
                    TelemetryHelper.safeTagMap("file_name", excelFile.getFileName()));
            logger.error("Error converting Excel to PDF: {}", cause.getMessage(), cause);
            return generateErrorResponse("Failed to convert Excel to PDF: " + cause.getMessage());
        }
    }

    /**
     * Handles validation failure by generating a response with error details.
     *
     * @param validationResults the validation results
     * @param excelFile         the original document file
     * @param sheetName         the original sheet name
     * @param allowRetry        whether to allow retry on validation failure
     * @return ExtensionResponse with validation error details
     */
    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            File excelFile,
            String sheetName,
            Boolean allowRetry) {
        logger.warn("Validation failed within handleValidationFailure() with {} errors, allowRetry={}",
                validationResults.size(), allowRetry);

        // Check if retry is allowed
        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Allow Retry is enabled, generating confirmation response for retry flow");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            logger.debug("ExcelToPdfHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results in a map with the expected key
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalstateManager.put(stateId, stateMap);

            logger.debug("ExcelToPdfHelper.handleValidationFailure() - Stored validation results: {}", validationResults);

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_SHEET_NAME_VALIDATION,
                    Map.of(
                            STATE_ID, stateId,
                            SubCatalogConstants.DOCUMENT_FILE, List.of(excelFile),
                            SubCatalogConstants.SHEET_NAME, sheetName
                    )
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }

    /**
     * Generates a standard error response.
     *
     * @param message the error message
     * @return ExtensionResponse with error details
     */
    private ExtensionResponse generateErrorResponse(String message) {
        logger.error("Generating error response: {}", message);
        ExtensionResponse.Error error = new ErrorBuilder()
                .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                .errorMessage(message)
                .build();
        return new ExtensionResponseBuilder().failure(error).build();
    }
}

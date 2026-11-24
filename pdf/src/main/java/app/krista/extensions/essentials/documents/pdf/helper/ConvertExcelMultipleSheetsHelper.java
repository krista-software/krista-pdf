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
import app.krista.extensions.essentials.documents.pdf.catalog.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.utils.ExtensionResponseWrapper;
import app.krista.extensions.essentials.documents.pdf.service.ExcelConverterService;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.ErrorBuilder;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Convert Excel File with Multiple Sheets to PDF operations with validation support.
 * Handles Excel file validation and conversion functionality.
 */
@Service
public class ConvertExcelMultipleSheetsHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConvertExcelMultipleSheetsHelper.class);

    private final ExcelConverterService excelConverterService;
    private final TelemetryHelper telemetryHelper;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public ConvertExcelMultipleSheetsHelper(ExcelConverterService excelConverterService,
                                           TelemetryHelper telemetryHelper,
                                           ValidationOrchestrator validationOrchestrator,
                                           ErrorHandlingStateManager internalStateManager,
                                           ExtensionResponseGenerator responseGenerator) {
        this.excelConverterService = excelConverterService;
        this.telemetryHelper = telemetryHelper;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;

        logger.debug("ConvertExcelMultipleSheetsHelper initialized with validation support");
    }

    /**
     * Converts an Excel file with multiple sheets to PDF with validation.
     *
     * @param excelFile The Excel file to convert to PDF
     * @param allowRetry whether to allow retry on validation failure
     * @return ExtensionResponse containing the generated PDF file or error details
     */
    public ExtensionResponse convertExcelFileWithMultipleSheetsToPDF(File excelFile, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("ConvertExcelMultipleSheetsHelper.convertExcelFileWithMultipleSheetsToPDF() called with file={}, allowRetry={}",
                    excelFile != null ? excelFile.getFileName() : "null", allowRetry);
            telemetryHelper.incrementCount("pdf.convertExcelFileWithMultipleSheetsToPDF");

            // Validate file format first
            if (excelFile != null) {
                logger.debug("Validating Excel format: {}", excelFile.getFileName());

                // Create validation context
                Map<Validator.ValidationResource, String> context = new HashMap<>();
                context.put(Validator.ValidationResource.EXCEL_MULTIPLE_SHEETS_FILE_FORMAT, excelFile.getFileName());

                // Validate
                List<ValidationOrchestrator.ValidationResult> validationResults =
                        validationOrchestrator.validate(context);

                if (!validationResults.isEmpty()) {
                    logger.warn("Excel format validation failed with {} errors", validationResults.size());
                    return handleValidationFailure(validationResults, excelFile, allowRetry);
                }
            } else {
                logger.error("No Excel file provided");
                throw new IllegalArgumentException("No Excel file provided");
            }

            // Convert Excel file to PDF
            logger.info("Starting Excel to PDF conversion for file: {}", excelFile.getFileName());
            
            final var extensionResponse = ExtensionResponseWrapper.wrapResponse(() -> {
                final var pdfFile = excelConverterService.convertExcelFileWithMultipleSheetsToPDF(excelFile);
                return Map.of("PDF File", pdfFile);
            });

            logger.info("Successfully converted Excel file to PDF");

            telemetryHelper.recordSuccess("pdf.convertExcelFileWithMultipleSheetsToPDF", startTime,
                    TelemetryHelper.safeTagMap("file_name", excelFile.getFileName()));

            return extensionResponse;

        } catch (Exception cause) {
            telemetryHelper.recordError("pdf.convertExcelFileWithMultipleSheetsToPDF", startTime, cause,
                    TelemetryHelper.safeTagMap("file_name", excelFile != null ? excelFile.getFileName() : "N/A"));
            logger.error("Error converting Excel file to PDF: {}", cause.getMessage(), cause);

            // Check for specific Excel processing errors
            if (cause.getMessage().contains("Invalid Excel file") ||
                cause.getMessage().contains("Unsupported Excel format") ||
                cause.getMessage().contains("Cannot read Excel file")) {

                ExtensionResponse.Error error = new ErrorBuilder()
                        .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                        .errorMessage("The uploaded Excel file appears to be corrupted or invalid. Please ensure the file is a valid Excel document (.xlsx format) and try again.")
                        .build();

                return new ExtensionResponseBuilder().failure(error).build();
            } else {
                ExtensionResponse.Error error = new ErrorBuilder()
                        .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                        .errorMessage("An unexpected error occurred while converting the Excel file: " + cause.getMessage())
                        .build();

                return new ExtensionResponseBuilder().failure(error).build();
            }
        }
    }

    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            File excelFile, Boolean allowRetry) {

        logger.warn("Validation failed within handleValidationFailure() with {} errors, allowRetry={}",
                validationResults.size(), allowRetry);

        // Check if retry is allowed
        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Allow Retry is enabled, generating confirmation response for retry flow");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            logger.debug("ConvertExcelMultipleSheetsHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            // Create state map with all parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (excelFile != null) {
                state.put(SubCatalogConstants.EXCEL_MULTIPLE_SHEETS_FILE, List.of(excelFile));
            }

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_EXCEL_MULTIPLE_SHEETS_VALIDATION,
                    state
            );
        } else {
            logger.debug("Allow Retry is disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }
}

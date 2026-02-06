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

package app.krista.extensions.essentials.documents.pdf.helper.strategy;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.model.base.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validation strategy with retry support.
 * 
 * <p>This strategy performs comprehensive validation and provides retry mechanism
 * when validation fails.</p>
 * 
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Single Responsibility:</b> Only handles validation with retry logic</li>
 *   <li><b>Liskov Substitution:</b> Can be substituted for ValidationStrategy interface</li>
 * </ul>
 */
public class WithRetryValidationStrategy implements ValidationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(WithRetryValidationStrategy.class);
    
    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;
    
    public WithRetryValidationStrategy(ValidationOrchestrator validationOrchestrator,
                                      ExtensionResponseGenerator responseGenerator) {
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
    }
    
    @Override
    public ValidationResult validate(File inputFile, Double splitPageNumber) {
        // Validate file is provided
        if (inputFile == null) {
            logger.error("No PDF file provided");
            return ValidationResult.failure(
                "No PDF file provided",
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR
            );
        }
        
        // Validate file format
        logger.debug("Validating PDF format: {}", inputFile.getFileName());
        Map<Validator.ValidationResource, String> context = new HashMap<>();
        context.put(Validator.ValidationResource.SPLIT_PDF_AT_PAGE_NUMBER_FILE_FORMAT, 
                   inputFile.getFileName());
        
        List<ValidationOrchestrator.ValidationResult> validationResults = 
            validationOrchestrator.validate(context);
        
        if (!validationResults.isEmpty()) {
            logger.warn("PDF format validation failed with {} errors", validationResults.size());
            return ValidationResult.failure(
                "PDF format validation failed",
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults
            );
        }
        
        // Validate split page number
        if (splitPageNumber == null) {
            logger.error("Split page number is required");
            return ValidationResult.failure(
                "Split page number is required",
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR
            );
        }
        
        int pageNum = splitPageNumber.intValue();
        if (pageNum < 2) {
            logger.error("Split page number must be at least 2, provided: {}", pageNum);
            return ValidationResult.failure(
                "Split page number must be at least 2. Provided: " + pageNum,
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR
            );
        }
        
        return ValidationResult.success();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleValidationFailure(ValidationResult result, 
                                                     File inputFile, 
                                                     Double splitPageNumber) {
        logger.debug("Allow Retry is enabled, generating confirmation response");
        
        // Generate unique state ID
        String stateId = UUID.randomUUID().toString();
        
        // Create state map with all parameters
        Map<String, Object> state = new HashMap<>();
        state.put(SubCatalogConstants.STATE_ID, stateId);
        if (inputFile != null) {
            state.put(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE, List.of(inputFile));
        }
        if (splitPageNumber != null) {
            state.put(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_SPLIT_PAGE, splitPageNumber);
        }
        
        // If we have validation results from orchestrator, use them
        if (result.getAdditionalData() instanceof List) {
            List<ValidationOrchestrator.ValidationResult> validationResults = 
                (List<ValidationOrchestrator.ValidationResult>) result.getAdditionalData();
            
            return responseGenerator.generateConfirmationResponse(
                result.getExceptionType(),
                validationResults,
                SubCatalogConstants.CONFIRM_SPLIT_PDF_AT_PAGE_NUMBER_VALIDATION,
                state
            );
        }
        
        // Otherwise, create a simple validation result with proper constructor parameters
        List<ValidationOrchestrator.ValidationResult> validationResults = List.of(
            new ValidationOrchestrator.ValidationResult(
                "Do you want to re-enter the file?",  // confirmStepMessage
                SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE,  // fetchFieldName
                "Please upload a valid PDF file",  // fetchStepMessage
                result.getErrorMessage(),  // errMessage
                "File"  // fieldType
            )
        );

        return responseGenerator.generateConfirmationResponse(
            result.getExceptionType(),
            validationResults,
            SubCatalogConstants.CONFIRM_SPLIT_PDF_AT_PAGE_NUMBER_VALIDATION,
            state
        );
    }
}


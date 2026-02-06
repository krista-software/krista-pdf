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

package app.krista.extensions.essentials.documents.pdf.catalog.validators;

import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.FieldTypes;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Validator for PDF file count.
 * Validates that there are at least 2 files provided for merging.
 */
public class PdfFileCountValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(PdfFileCountValidator.class);
    private static final int MIN_FILES_FOR_MERGE = 2;

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        logger.debug("PdfFileCountValidator.validate() - Validating file count for merging, resourceId: {}", resourceId);

        // Check if resourceId contains file count information
        if (resourceId != null && resourceId.startsWith("FILE_COUNT:")) {
            try {
                String countStr = resourceId.substring("FILE_COUNT:".length());
                int fileCount = Integer.parseInt(countStr);

                if (fileCount < MIN_FILES_FOR_MERGE) {
                    logger.warn("Validation failed: Insufficient files for merging. Provided: {}, Required: {}", 
                        fileCount, MIN_FILES_FOR_MERGE);
                    return false;
                }
                
                logger.debug("File count validation passed: {} files provided", fileCount);
                return true;
            } catch (NumberFormatException e) {
                logger.warn("Invalid file count format in resourceId: {}", resourceId);
                return false;
            }
        }

        // If no file count information is provided, assume validation passes
        // (this allows the validator to be used in other contexts)
        logger.debug("No file count information in resourceId, skipping validation");
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return SubCatalogConstants.PDF_FILES;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.FILE_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please upload at least two PDF files to merge them into a single document.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        int fileCount = extractFileCount(resourceId);

        if (fileCount >= 0 && fileCount < MIN_FILES_FOR_MERGE) {
            if (fileCount == 0) {
                return "No PDF files were uploaded. Please select at least two PDF files to merge.";
            } else if (fileCount == 1) {
                return "Only one PDF file was uploaded. Please select at least one more PDF file to merge (minimum 2 files required).";
            }
        }

        return "Please upload at least two PDF files to merge.";
    }

    @Override
    public String getErrMessage(String resourceId) {
        int fileCount = extractFileCount(resourceId);

        if (fileCount >= 0 && fileCount < MIN_FILES_FOR_MERGE) {
            if (fileCount == 0) {
                return "No PDF files provided. Please upload at least two PDF files to merge.";
            } else if (fileCount == 1) {
                return "Only one PDF file provided. PDF merging requires at least two files.";
            }
        }

        return "Insufficient PDF files provided. At least two PDF files are required for merging.";
    }

    /**
     * Extracts file count from resourceId if it contains file count information.
     * 
     * @param resourceId the resource ID that may contain file count
     * @return the file count, or -1 if not found or invalid
     */
    private int extractFileCount(String resourceId) {
        if (resourceId != null && resourceId.startsWith("FILE_COUNT:")) {
            try {
                String countStr = resourceId.substring("FILE_COUNT:".length());
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid file count format in resourceId: {}", resourceId);
            }
        }
        return -1;
    }
}

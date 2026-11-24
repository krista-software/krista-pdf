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

import java.util.Arrays;
import java.util.Map;

/**
 * Validator for PDF file formats specifically for Split PDF Into Two Equal Parts operation.
 * Validates that the provided file has a supported PDF format (.pdf).
 */
public class SplitPdfTwoEqualFileFormatValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(SplitPdfTwoEqualFileFormatValidator.class);
    private static final String[] VALID_PDF_EXTENSIONS = {".pdf"};

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        return isValidPdfFormat(resourceId);
    }

    private boolean isValidPdfFormat(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("Validation failed: File name is null or blank for Split PDF Into Two Equal Parts operation.");
            return false;
        }

        String lowerCaseFileName = fileName.toLowerCase();
        boolean isValid = Arrays.stream(VALID_PDF_EXTENSIONS).anyMatch(lowerCaseFileName::endsWith);
        
        if (!isValid) {
            logger.warn("File '{}' has an unsupported format for Split PDF Into Two Equal Parts. Supported formats: {}", 
                    fileName, String.join(", ", VALID_PDF_EXTENSIONS));
        } else {
            logger.debug("File '{}' passed PDF format validation for Split PDF Into Two Equal Parts", fileName);
        }
        
        return isValid;
    }

    @Override
    public String getFetchFieldName() {
        return SubCatalogConstants.SPLIT_PDF_TWO_EQUAL_INPUT_FILE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.FILE_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please upload a valid PDF file (.pdf format) for splitting into two equal parts.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided file '%s' is not a valid PDF format for splitting into two equal parts. Only .pdf files are supported.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return "The uploaded file is not a PDF document. Please upload a file with .pdf extension to split into equal parts.";
    }
}

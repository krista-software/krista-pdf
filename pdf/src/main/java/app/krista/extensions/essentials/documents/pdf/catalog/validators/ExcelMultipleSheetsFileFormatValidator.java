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
 * Validator for Excel file formats specifically for Convert Excel File with Multiple Sheets to PDF operation.
 * Validates that the provided file has a supported Excel format (.xlsx).
 */
public class ExcelMultipleSheetsFileFormatValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(ExcelMultipleSheetsFileFormatValidator.class);
    private static final String[] VALID_EXCEL_EXTENSIONS = {".xlsx"};

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        return isValidExcelFormat(resourceId);
    }

    private boolean isValidExcelFormat(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("Validation failed: File name is null or blank for Convert Excel File with Multiple Sheets to PDF operation.");
            return false;
        }

        String lowerCaseFileName = fileName.toLowerCase();
        boolean isValid = Arrays.stream(VALID_EXCEL_EXTENSIONS).anyMatch(lowerCaseFileName::endsWith);
        
        if (!isValid) {
            logger.warn("File '{}' has an unsupported format for Convert Excel File with Multiple Sheets to PDF. Supported formats: {}", 
                    fileName, String.join(", ", VALID_EXCEL_EXTENSIONS));
        } else {
            logger.debug("File '{}' passed Excel format validation for Convert Excel File with Multiple Sheets to PDF", fileName);
        }
        
        return isValid;
    }

    @Override
    public String getFetchFieldName() {
        return SubCatalogConstants.EXCEL_MULTIPLE_SHEETS_FILE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.FILE_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please upload a valid Excel file (.xlsx format) for converting multiple sheets to PDF.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided file '%s' is not a valid Excel format for converting multiple sheets to PDF. Only .xlsx files are supported.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return "The uploaded file is not a supported Excel format. Please upload an Excel file with .xlsx extension to convert multiple sheets to PDF.";
    }
}

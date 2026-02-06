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
 * Validator for DOCX file formats.
 * Validates that the provided file has a supported DOCX format (.docx).
 */
public class DocxFileFormatValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(DocxFileFormatValidator.class);
    private static final String[] VALID_DOCX_EXTENSIONS = {".docx"};

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        return isValidDocxFormat(resourceId);
    }

    private boolean isValidDocxFormat(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("Validation failed: File name is null or blank.");
            return false;
        }

        String lowerCaseFileName = fileName.toLowerCase();
        boolean isValid = Arrays.stream(VALID_DOCX_EXTENSIONS).anyMatch(lowerCaseFileName::endsWith);
        
        if (!isValid) {
            logger.warn("File '{}' has an unsupported format. Supported formats: {}", 
                    fileName, String.join(", ", VALID_DOCX_EXTENSIONS));
        }
        
        return isValid;
    }

    @Override
    public String getFetchFieldName() {
        return SubCatalogConstants.DOCUMENT_FILE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.FILE_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please upload a valid Microsoft Word document (.docx file).";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided file '%s' is not a valid Word document format. Only .docx files are supported.", resourceId);
    }

    public String getErrMessage(String resourceId) {
        return "The uploaded file is not a supported Word document format. Please upload a Microsoft Word document with .docx extension.";
    }
}
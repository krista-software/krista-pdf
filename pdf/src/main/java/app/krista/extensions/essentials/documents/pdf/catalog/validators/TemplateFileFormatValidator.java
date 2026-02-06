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
 * Validator for template file formats.
 * Validates that the provided file has a supported template format (.docx).
 */
public class TemplateFileFormatValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(TemplateFileFormatValidator.class);
    private static final String[] VALID_TEMPLATE_EXTENSIONS = {".docx"};

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        return isValidTemplateFormat(resourceId);
    }

    private boolean isValidTemplateFormat(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("Validation failed: Template file name is null or blank.");
            return false;
        }

        String lowerCaseFileName = fileName.toLowerCase();
        boolean isValid = Arrays.stream(VALID_TEMPLATE_EXTENSIONS).anyMatch(lowerCaseFileName::endsWith);

        if (!isValid) {
            logger.warn("Template file '{}' has an unsupported format. Supported formats: {}",
                    fileName, String.join(", ", VALID_TEMPLATE_EXTENSIONS));
        }

        return isValid;
    }

    @Override
    public String getFetchFieldName() {
        return SubCatalogConstants.TEMPLATE_FILE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.FILE_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please upload a valid template file (.docx format).";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided template file '%s' is not in a valid format. Only .docx files are supported for templates.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return "The uploaded template file is not a supported format. Please upload a Microsoft Word template with .docx extension.";
    }
}
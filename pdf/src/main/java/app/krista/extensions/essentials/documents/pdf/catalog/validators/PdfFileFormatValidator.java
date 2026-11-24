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
 * Validator for PDF file format.
 * Validates that the provided files are in PDF format.
 */
public class PdfFileFormatValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(PdfFileFormatValidator.class);

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        logger.debug("PdfFileFormatValidator.validate() - Validating PDF file format: {}", resourceId);

        if (resourceId == null || resourceId.isBlank()) {
            logger.warn("Validation failed: PDF file name is null or blank");
            return false;
        }

        String lowerCaseFileName = resourceId.toLowerCase();
        boolean isPdfFile = lowerCaseFileName.endsWith(".pdf");

        if (!isPdfFile) {
            logger.warn("Validation failed: File is not a PDF: {}", resourceId);
            return false;
        }

        logger.debug("PDF file format validation passed: {}", resourceId);
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
        return "Please provide at least two PDF files to merge.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        if (resourceId == null || resourceId.isBlank()) {
            return "No file was provided. Please upload a valid PDF file.";
        }

        return "The file '" + resourceId + "' is not in PDF format. Please provide valid PDF files.";
    }

    @Override
    public String getErrMessage(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return "No file was provided. Please upload a valid PDF file.";
        }

        return "The file '" + resourceId + "' is not in PDF format. Only PDF files can be merged.";
    }
}
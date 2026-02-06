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
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SheetNameValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(SheetNameValidator.class);
    private final Workbook workbook;

    public SheetNameValidator(Workbook workbook) {
        this.workbook = workbook;
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        return isSheetNamePresent(resourceId);
    }

    private boolean isSheetNamePresent(String sheetName) {
        // If sheet name is null or blank, it's valid (means use first sheet)
        if (sheetName == null || sheetName.isBlank()) {
            logger.debug("Sheet name is null or blank - will use first sheet by default");
            return true;
        }

        // If workbook is null, we can't validate specific sheet names
        if (workbook == null) {
            logger.warn("Cannot validate sheet name '{}' - workbook is null", sheetName);
            return false;
        }

        int sheetIndex = workbook.getSheetIndex(sheetName);
        if (sheetIndex == -1) {
            logger.warn("Sheet '{}' does not exist in the workbook.", sheetName);
            return false;
        }

        logger.debug("Sheet name '{}' validation passed", sheetName);
        return true;
    }

    @Override
    public String getFetchFieldName() {
        return SubCatalogConstants.SHEET_NAME;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid sheet name from your Excel file (leave empty to convert the first sheet).";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        if (resourceId == null || resourceId.isBlank()) {
            return "No sheet name provided. The first sheet will be converted.";
        }
        return String.format("The sheet name '%s' does not exist in your Excel file. Please check the sheet name and try again.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return "No sheet name provided. The first sheet will be converted.";
        }
        return String.format("Sheet '%s' not found in the Excel file. Please verify the sheet name is correct (names are case-sensitive).", resourceId);
    }
}
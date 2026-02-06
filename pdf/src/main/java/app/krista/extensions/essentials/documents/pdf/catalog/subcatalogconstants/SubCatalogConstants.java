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

package app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Constants for subcatalog request names.
 */
public class SubCatalogConstants {
    public static final String VALIDATION_RESULTS = "validationResults";
    public static final String REENTER = "Reenter";

    // SubCatalog request names
    public static final String CONFIRM_SHEET_NAME_VALIDATION = "confirmSheetNameValidation";
    public static final String HANDLE_SHEET_NAME_VALIDATION = "handleSheetNameValidation";

    public static final String CONFIRM_IMAGE_FORMAT_VALIDATION = "confirmImageFormatValidation";
    public static final String HANDLE_IMAGE_FORMAT_VALIDATION = "handleImageFormatValidation";

    // SubCatalog request names for Rich Text validation
    public static final String CONFIRM_RICH_TEXT_VALIDATION = "confirmRichTextValidation";
    public static final String HANDLE_RICH_TEXT_VALIDATION = "handleRichTextValidation";

    // SubCatalog request names for DOCX validation
    public static final String CONFIRM_DOCX_FORMAT_VALIDATION = "confirmDocxFormatValidation";
    public static final String HANDLE_DOCX_FORMAT_VALIDATION = "handleDocxFormatValidation";

    // Add new constants for PDF file format validation
    public static final String CONFIRM_PDF_MERGED_FORMAT_VALIDATION = "confirmPdfFormatValidation";
    public static final String HANDLE_PDF_MERGED_FORMAT_VALIDATION = "handlePdfFormatValidation";
    public static final String PDF_FILES = "Pdf Files";

    // SubCatalog request names for Template validation
    public static final String CONFIRM_TEMPLATE_FORMAT_VALIDATION = "confirmTemplateFormatValidation";
    public static final String HANDLE_TEMPLATE_FORMAT_VALIDATION = "handleTemplateFormatValidation";
    public static final String TEMPLATE_FILE = "Template File";

    // SubCatalog request names for Split PDF validation
    public static final String CONFIRM_SPLIT_PDF_VALIDATION = "confirmSplitPdfValidation";
    public static final String HANDLE_SPLIT_PDF_VALIDATION = "handleSplitPdfValidation";
    public static final String SPLIT_PDF_FILE = "File";

    // SubCatalog request names for Split PDF Into Two Equal Parts validation
    public static final String CONFIRM_SPLIT_PDF_TWO_EQUAL_VALIDATION = "confirmSplitPdfTwoEqualValidation";
    public static final String HANDLE_SPLIT_PDF_TWO_EQUAL_VALIDATION = "handleSplitPdfTwoEqualValidation";
    public static final String SPLIT_PDF_TWO_EQUAL_INPUT_FILE = "Input File";

    // SubCatalog request names for Split PDF Based On Condition validation
    public static final String CONFIRM_SPLIT_PDF_CONDITION_VALIDATION = "confirmSplitPdfConditionValidation";
    public static final String HANDLE_SPLIT_PDF_CONDITION_VALIDATION = "handleSplitPdfConditionValidation";
    public static final String SPLIT_PDF_CONDITION_INPUT_FILE = "Input File";

    // SubCatalog request names for Split PDF at Page Number validation
    public static final String CONFIRM_SPLIT_PDF_AT_PAGE_NUMBER_VALIDATION = "confirmSplitPdfAtPageNumberValidation";
    public static final String HANDLE_SPLIT_PDF_AT_PAGE_NUMBER_VALIDATION = "handleSplitPdfAtPageNumberValidation";
    public static final String SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE = "Input File";
    public static final String SPLIT_PDF_AT_PAGE_NUMBER_SPLIT_PAGE = "Split at Page Number";

    // SubCatalog request names for Convert Excel File with Multiple Sheets to PDF validation
    public static final String CONFIRM_EXCEL_MULTIPLE_SHEETS_VALIDATION = "confirmExcelMultipleSheetsValidation";
    public static final String HANDLE_EXCEL_MULTIPLE_SHEETS_VALIDATION = "handleExcelMultipleSheetsValidation";
    public static final String EXCEL_MULTIPLE_SHEETS_FILE = "Excel File";

    // SubCatalog request names for Convert PDF to HTML validation
    public static final String CONFIRM_PDF_TO_HTML_VALIDATION = "confirmPdfToHtmlValidation";
    public static final String HANDLE_PDF_TO_HTML_VALIDATION = "handlePdfToHtmlValidation";
    public static final String PDF_TO_HTML_FILE = "PDF File";

    // SubCatalog request names for PDF to HTML Conversion (Complex Layout Support) validation
    public static final String CONFIRM_PDF_TO_HTML_PYTHON_VALIDATION = "confirmPdfToHtmlPythonValidation";
    public static final String HANDLE_PDF_TO_HTML_PYTHON_VALIDATION = "handlePdfToHtmlPythonValidation";
    public static final String PDF_TO_HTML_PYTHON_FILE = "PDF File";
    public static final String SAVE_AS_TEXT_FILE = "Save as Text File";

    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()  // Prevent HTML escaping of quotes and special chars
            .serializeNulls()       // Include null fields
            .create();

    public static final String SHEET_NAME = "Sheet Name";
    public static final String STATE_ID = "stateId";
    public static final String DOCUMENT_FILE = "Document File";

    // Prevent instantiation
    private SubCatalogConstants() {
    }
}

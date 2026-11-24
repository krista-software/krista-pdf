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

package app.krista.extensions.essentials.documents.pdf.catalog;

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.SubCatalogRequest;
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.extensions.essentials.documents.pdf.helper.*;
import app.krista.extensions.essentials.documents.pdf.service.DocxManager;
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.service.PdfToHtmlConversionService;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import app.krista.model.base.FreeForm;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

@Service
public class PdfExtensionAreaSubCatalogRequests {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExtensionAreaSubCatalogRequests.class);

    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final ConvertExcelToPDFHelper convertExcelToPDFHelper;
    private final ConvertImageToPdfHelper convertImageToPdfHelper;
    private final RichTextPdfHelper richTextPdfHelper;
    private final ConvertDocxToPdfHelper convertDocxToPdfHelper;
    private final MergePdfFilesHelper mergePdfFilesHelper;
    private final FillDocxTemplateHelper fillDocxTemplateHelper;
    private final PDFManager pdfManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final DocxManager docxManager;
    private final SplitPdfHelper splitPdfHelper;
    private final SplitPdfTwoEqualHelper splitPdfTwoEqualHelper;
    private final SplitPdfAtPageNumberHelper splitPdfAtPageNumberHelper;
    private final ConvertExcelMultipleSheetsHelper convertExcelMultipleSheetsHelper;
    private final ConvertPdfToHtmlHelper convertPdfToHtmlHelper;
    private final PdfToHtmlConversionService pdfToHtmlConversionService;

    @Inject
    public PdfExtensionAreaSubCatalogRequests(
            ValidationOrchestrator validationOrchestrator,
            ErrorHandlingStateManager internalStateManager,
            ExtensionResponseGenerator responseGenerator,
            ConvertExcelToPDFHelper convertExcelToPDFHelper,
            ConvertImageToPdfHelper convertImageToPdfHelper,
            RichTextPdfHelper richTextPdfHelper,
            ConvertDocxToPdfHelper convertDocxToPdfHelper,
            MergePdfFilesHelper mergePdfFilesHelper, FillDocxTemplateHelper fillDocxTemplateHelper,
            PDFManager pdfManager,
            KristaMediaClientUtil kristaMediaClientUtil,
            TelemetryHelper telemetryHelper,
            DocxManager docxManager,
            SplitPdfHelper splitPdfHelper,
            SplitPdfTwoEqualHelper splitPdfTwoEqualHelper,
            ConvertPdfToHtmlHelper convertPdfToHtmlHelper,
            ConvertExcelMultipleSheetsHelper convertExcelMultipleSheetsHelper,
            PdfToHtmlConversionService pdfToHtmlConversionService,SplitPdfAtPageNumberHelper splitPdfAtPageNumberHelper) {
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        this.convertExcelToPDFHelper = convertExcelToPDFHelper;
        this.convertImageToPdfHelper = convertImageToPdfHelper;
        this.richTextPdfHelper = richTextPdfHelper;
        this.convertDocxToPdfHelper = convertDocxToPdfHelper;
        this.mergePdfFilesHelper = mergePdfFilesHelper;
        this.fillDocxTemplateHelper = fillDocxTemplateHelper;
        this.pdfManager = pdfManager;
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.telemetryHelper = telemetryHelper;
        this.docxManager = docxManager;
        this.splitPdfHelper = splitPdfHelper;
        this.splitPdfTwoEqualHelper = splitPdfTwoEqualHelper;
        this.splitPdfAtPageNumberHelper = splitPdfAtPageNumberHelper;
        this.convertExcelMultipleSheetsHelper = convertExcelMultipleSheetsHelper;
        this.convertPdfToHtmlHelper = convertPdfToHtmlHelper;
        this.pdfToHtmlConversionService = pdfToHtmlConversionService;
    }

    @SuppressWarnings("unchecked")
    private List<ValidationOrchestrator.ValidationResult> getValidationResults(String stateId) {
        try {
            Map<String, Object> stateMap = internalStateManager.get(stateId);
            LOGGER.debug("getValidationResults() - State map: {}", stateMap);

            // Get the validation results as a list of objects
            List<Object> rawResults = (List<Object>) stateMap.get(SubCatalogConstants.VALIDATION_RESULTS);

            // Convert each object back to ValidationResult using GSON
            List<ValidationOrchestrator.ValidationResult> results = new ArrayList<>();
            for (Object rawResult : rawResults) {
                String json = SubCatalogConstants.GSON.toJson(rawResult);
                ValidationOrchestrator.ValidationResult result = SubCatalogConstants.GSON.fromJson(json, ValidationOrchestrator.ValidationResult.class);
                results.add(result);
            }
            LOGGER.debug("getValidationResults() - Successfully parsed validation results: {}", results);
            return results;
        } catch (Exception e) {
            LOGGER.error("Error parsing validation results: {}", e.getMessage(), e);
            // Return an empty list as fallback to avoid breaking the flow
            return new ArrayList<>();
        }
    }

    private LinkedHashMap<String, Object> addMetaDataToLinkedHashMap(Map<String, Object> map, String stateId) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(map);
        LOGGER.debug("addMetaDataToLinkedHashMap() - Adding stateId: {}", stateId);
        result.put(SubCatalogConstants.STATE_ID, stateId);
        return result;
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_SHEET_NAME_VALIDATION,
            description = "Checks if user wants to re-enter sheet name for Excel to PDF conversion",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmSheetNameValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Sheet Name: Text, Document File: File }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_SHEET_NAME_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_SHEET_NAME_VALIDATION,
            description = "Handle sheet name validation re-entry for Excel to PDF conversion",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Excel PDF File", type = "File", required = false)
    public ExtensionResponse handleSheetNameValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Sheet Name: Text, Document File: File }") Map<String, Object> map) {

        try {
            String sheetName = (String) map.get(SubCatalogConstants.SHEET_NAME);

            // Extract file using the helper method
            app.krista.model.base.File excelFile = extractFileFromMap(map, SubCatalogConstants.DOCUMENT_FILE);

            LOGGER.debug("handleSheetNameValidation() - Extracted parameters: sheetName={}, excelFile={}",
                    sheetName, excelFile != null ? excelFile.getFileName() : "null");

            // Validate the re-entered data
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(
                            Validator.ValidationResource.SHEET_NAME, sheetName != null ? sheetName : "",
                            Validator.ValidationResource.DOCUMENT_FILE, excelFile != null ? excelFile.getFileName() : ""
                    )
            );

            // If validation still fails after re-entry, return a final error response
            if (!validationResults.isEmpty()) {
                LOGGER.error("Validation failed again after re-entry for file: {}, sheet: {}",
                        excelFile != null ? excelFile.getFileName() : "null", sheetName);

                ExtensionResponse.Error error = new ErrorBuilder()
                        .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                        .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid Excel file (.xlsx format) and a valid sheet name.")
                        .build();

                return new ExtensionResponseBuilder()
                        .failure(error)
                        .build();
            }
            // Call the main method with corrected data
            LOGGER.info("Calling convertExcelToPDF with corrected data");
            return convertExcelToPDFHelper.convertExcelToPDF(excelFile, sheetName, true);
        } catch (Exception e) {
            LOGGER.error("Error processing Excel to PDF conversion: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to convert Excel to PDF: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_IMAGE_FORMAT_VALIDATION,
            description = "Checks if user wants to re-enter image file for PDF conversion",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmImageFormatValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Document File: File }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmImageFormatValidation() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_IMAGE_FORMAT_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_IMAGE_FORMAT_VALIDATION,
            description = "Handle image format validation re-entry for image to PDF conversion",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "PDF File", type = "File", required = false)
    public ExtensionResponse handleImageFormatValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Document File: File}") Map<String, Object> map) {

        // Extract file using the helper method
        app.krista.model.base.File documentFile = extractFileFromMap(map, SubCatalogConstants.DOCUMENT_FILE);

        LOGGER.debug("handleImageFormatValidation() - Extracted parameters: documentFile={}",
                documentFile != null ? documentFile.getFileName() : "null");

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.IMAGE_FILE_FORMAT,
                        documentFile != null ? documentFile.getFileName() : ""
                )
        );

        // If validation still fails after re-entry, return a final error response
        if (!validationResults.isEmpty()) {
            LOGGER.error("Image format validation failed again after re-entry for file: {}",
                    documentFile != null ? documentFile.getFileName() : "null");

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid image file (PNG, JPG, or JPEG format).")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        // Check if we have a valid file
        if (documentFile == null) {
            LOGGER.error("No valid image file found in the request");
            return new ExtensionResponseBuilder()
                    .failure(new ErrorBuilder()
                            .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                            .errorMessage("No valid image file found in the request")
                            .build())
                    .build();
        }

        // Call the main method with corrected data
        LOGGER.info("Calling convertImageToPdf with corrected data");
        return convertImageToPdfHelper.convertImageToPdf(documentFile, true);
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_RICH_TEXT_VALIDATION,
            description = "Checks if user wants to re-enter rich text content for PDF conversion",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmRichTextValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Rich Text Data: RichText, Title: Text, Sub Title: Text, Description: RichText, Header: Text, Footer: Text, Logo: File }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        LOGGER.debug("confirmRichTextValidation() - Reenter: {}, stateId: {}", reenter, stateId);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmRichTextValidation() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_RICH_TEXT_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_RICH_TEXT_VALIDATION,
            description = "Handle rich text validation re-entry for PDF conversion",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "RichText PDF File", type = "File", required = false)
    public ExtensionResponse handleRichTextValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Rich Text Data: RichText, Title: Text, Sub Title: Text, Description: RichText, Header: Text, Footer: Text, Logo: File}")
            Map<String, Object> map) {

        LOGGER.debug("handleRichTextValidation() - Starting with input map: {}", map);

        // Extract parameters from the map
        String richTextData = (String) map.get("Rich Text Data");
        String title = (String) map.get("Title");
        String subTitle = (String) map.get("Sub Title");
        String description = (String) map.get("Description");
        String header = (String) map.get("Header");
        String footer = (String) map.get("Footer");
        app.krista.model.base.File logo = extractFileFromMap(map, "Logo");

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.RICH_TEXT_CONTENT,
                        richTextData != null ? richTextData : ""
                )
        );

        // If validation still fails after re-entry, return a final error response
        if (!validationResults.isEmpty()) {
            LOGGER.error("Rich text validation failed again after re-entry");

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate rich text content after re-entry. Please ensure you are providing valid rich text with sufficient content.")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        try {
            // Call the main method with corrected data
            LOGGER.info("Calling getRichTextPDF with corrected data");
            return richTextPdfHelper.getRichTextPDF(title, subTitle, description, header, footer, logo, richTextData, true);
        } catch (Exception e) {
            LOGGER.error("Error processing rich text to PDF: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                    .errorMessage("Failed to convert rich text to PDF: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_DOCX_FORMAT_VALIDATION,
            description = "Checks if user wants to re-enter document file for PDF conversion",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmDocxFormatValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Document File: File }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            LOGGER.info("User chose not to re-enter data");
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        LOGGER.info("User chose to re-enter data");
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_DOCX_FORMAT_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_DOCX_FORMAT_VALIDATION,
            description = "Handle document format validation re-entry for DOCX to PDF conversion",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Docx PDF File", type = "File", required = false)
    public ExtensionResponse handleDocxFormatValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Document File: File}") Map<String, Object> map) {

        // Extract file using the helper method
        app.krista.model.base.File documentFile = extractFileFromMap(map, SubCatalogConstants.DOCUMENT_FILE);

        LOGGER.debug("handleDocxFormatValidation() - Extracted parameters: documentFile={}",
                documentFile != null ? documentFile.getFileName() : "null");

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.DOCX_FILE_FORMAT,
                        documentFile != null ? documentFile.getFileName() : ""
                )
        );

        // If validation still fails after re-entry, return a final error response
        if (!validationResults.isEmpty()) {
            LOGGER.error("Validation failed again after re-entry for file: {}",
                    documentFile != null ? documentFile.getFileName() : "null");

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid Microsoft Word document (.docx file).")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        // Check if we have a valid file
        if (documentFile == null) {
            LOGGER.error("No valid document file found in the request");
            return new ExtensionResponseBuilder()
                    .failure(new ErrorBuilder()
                            .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                            .errorMessage("No valid document file found in the request")
                            .build())
                    .build();
        }

        // Call the main method with corrected data
        LOGGER.info("Calling convertDocxToPDF with corrected data");
        return convertDocxToPdfHelper.convertDocxToPDF(documentFile, true);
    }


    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_PDF_MERGED_FORMAT_VALIDATION,
            description = "Checks if user wants to re-enter PDF files for merging",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmPdfFormatValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Pdf Files: File }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmPdfFormatValidation() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_PDF_MERGED_FORMAT_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_PDF_MERGED_FORMAT_VALIDATION,
            description = "Handle PDF format validation re-entry for PDF merging",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Merged PDF", type = "File", required = false)
    public ExtensionResponse handlePdfFormatValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Title: Text, Sub Title: Text, Description: Text, Header: Text, Footer: Text, Logo: File, Pdf Files: File}") Map<String, Object> map) {

        // Extract parameters from the map
        String title = (String) map.get("Title");
        String subTitle = (String) map.get("Sub Title");
        String description = (String) map.get("Description");
        String header = (String) map.get("Header");
        String footer = (String) map.get("Footer");
        app.krista.model.base.File logo = extractFileFromMap(map, "Logo");

        // Extract files from the map - for PDF Files, we need to handle multiple files
        List<File> pdfFiles = new ArrayList<>();
        Object fileObj = map.get(SubCatalogConstants.PDF_FILES);

        // Handle case where fileObj is a List
        if (fileObj instanceof List) {
            List<?> files = (List<?>) fileObj;
            for (Object item : files) {
                File file = null;
                if (item instanceof File) {
                    file = (File) item;
                } else {
                    try {
                        file = SubCatalogConstants.GSON.fromJson(SubCatalogConstants.GSON.toJson(item), File.class);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse file from list item", e);
                    }
                }
                if (file != null) {
                    pdfFiles.add(file);
                }
            }
            LOGGER.debug("handlePdfFormatValidation() - Extracted {} files from list", pdfFiles.size());
        } else {
            // Handle case where fileObj is a single File or needs extraction
            File singleFile = extractFileFromMap(map, SubCatalogConstants.PDF_FILES);
            if (singleFile != null) {
                pdfFiles.add(singleFile);
                LOGGER.debug("handlePdfFormatValidation() - Extracted single file");
            } else {
                LOGGER.warn("handlePdfFormatValidation() - No valid files found in the input map");
            }
        }

        // Check if we have any files to validate
        if (pdfFiles.isEmpty()) {
            LOGGER.error("No PDF files found in the request");
            return new ExtensionResponseBuilder()
                    .failure(new ErrorBuilder()
                            .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                            .errorMessage("No PDF files found in the request. Please provide at least one PDF file to merge.")
                            .build())
                    .build();
        }

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = new ArrayList<>();
        List<String> invalidFileNames = new ArrayList<>();

        for (File pdfFile : pdfFiles) {
            if (pdfFile != null) {
                Map<Validator.ValidationResource, String> validationContext = Map.of(
                        Validator.ValidationResource.PDF_MERGE_FILE_FORMAT, pdfFile.getFileName()
                );

                List<ValidationOrchestrator.ValidationResult> fileResults =
                        validationOrchestrator.validate(validationContext);

                if (!fileResults.isEmpty()) {
                    validationResults.addAll(fileResults);
                    invalidFileNames.add(pdfFile.getFileName());
                }
            }
        }

        // If validation still fails, return an error response
        if (!validationResults.isEmpty()) {
            LOGGER.error("Validation failed again after re-entry for files: {}",
                    invalidFileNames.isEmpty() ? "none" : String.join(", ", invalidFileNames));

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing valid PDF files.")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        try {
            // Call the main method with corrected data
            LOGGER.info("Calling mergePdfFiles with corrected data");
            return mergePdfFilesHelper.mergePdfFiles(title, subTitle, description, header, footer, logo, pdfFiles, true);
        } catch (Exception e) {
            LOGGER.error("Error processing PDF merging: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                    .errorMessage("Failed to merge PDF files: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_TEMPLATE_FORMAT_VALIDATION,
            description = "Checks if user wants to re-enter template file for docx template filling",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmTemplateFormatValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Template File: File , Template Fields: FreeForm }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmTemplateFormatValidation() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_TEMPLATE_FORMAT_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_TEMPLATE_FORMAT_VALIDATION,
            description = "Handle template format validation re-entry for docx template filling",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Docx Template PDF File", type = "File", required = false)
    public ExtensionResponse handleTemplateFormatValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Template File: File, Template Fields: FreeForm}") Map<String, Object> map) {

        LOGGER.debug("handleTemplateFormatValidation() - Starting with input map: {}", map);

        // Extract file and state from the map
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);
        File templateFile = extractFileFromMap(map, SubCatalogConstants.TEMPLATE_FILE);
        FreeForm templateFields = (FreeForm) map.get("Template Fields");

        LOGGER.debug("handleTemplateFormatValidation() - Extracted parameters: templateFile={}, stateId={}",
                templateFile != null ? templateFile.getFileName() : "null", stateId);

        // Check if we have a valid file
        if (templateFile == null) {
            LOGGER.error("No valid template file found in the request");
            return new ExtensionResponseBuilder()
                    .failure(new ErrorBuilder()
                            .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                            .errorMessage("No valid template file found in the request")
                            .build())
                    .build();
        }

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.TEMPLATE_FILE_FORMAT,
                        templateFile.getFileName()
                )
        );

        LOGGER.debug("handleTemplateFormatValidation() - Validation results: {} errors", validationResults.size());

        // If validation still fails after re-entry, return a final error response
        if (!validationResults.isEmpty()) {
            LOGGER.error("Validation failed again after re-entry for template file: {}",
                    templateFile != null ? templateFile.getFileName() : "null");

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid Microsoft Word template document (.docx file).")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        // Validation passed, proceed with template filling
        LOGGER.info("Template file validation passed, proceeding with template filling");
        try {
            LOGGER.debug("handleTemplateFormatValidation() - Processing template file: {}", templateFile.getFileName());

            java.io.File javaFile = docxManager.fillTheDocTemplate(kristaMediaClientUtil.downloadFileHandler(templateFile), templateFields);
            File result = kristaMediaClientUtil.uploadFileHandler(javaFile);

            LOGGER.debug("handleTemplateFormatValidation() - Successfully processed template, result file: {}", result.getFileName());

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("Docx Template PDF File", result);
            return new ExtensionResponseBuilder().success(responseMap).build();
        } catch (Exception e) {
            LOGGER.error("Error processing template: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to process template: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder().failure(error).build();
        }
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_SPLIT_PDF_VALIDATION,
            description = "Checks if user wants to re-enter PDF file for splitting",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    @Field.Desc(name = "inputMap", type = "{ Reenter: Boolean, stateId: Text, File: File, Category: [ Text ], Single Category: Text }", required = true)
    public ExtensionResponse confirmSplitPdfValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, File: File, Category: [ Text ], Single Category: Text }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            LOGGER.info("User choose not to re-enter data");
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_SPLIT_PDF_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_SPLIT_PDF_VALIDATION,
            description = "Handles re-entered PDF file for splitting",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Split Files", type = "[ File ]", required = false)
    public ExtensionResponse handleSplitPdfValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, File: File, Category: [ Text ], Single Category: Text}") Map<String, Object> map) {

        // Extract parameters using the helper method
        app.krista.model.base.File file = extractFileFromMap(map, SubCatalogConstants.SPLIT_PDF_FILE);
        @SuppressWarnings("unchecked")
        List<String> category = (List<String>) map.get("Category");
        String singleCategory = (String) map.get("Single Category");

        LOGGER.debug("handleSplitPdfValidation() - Extracted parameters: file={}",
                file != null ? file.getFileName() : "null");

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.SPLIT_PDF_FILE_FORMAT,
                        file != null ? file.getFileName() : ""
                )
        );

        if (!validationResults.isEmpty()) {
            LOGGER.error("Validation failed again after re-entry for file :Re-entered PDF file validation failed: {}",
                    file != null ? file.getFileName() : "null");
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid PDF document (.pdf file).")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        try {
            // Call the SplitPdfHelper method directly to avoid validation loop
            LOGGER.info("Calling SplitPdfHelper.splitPDFIntoPages with corrected data");
            ExtensionResponse response = splitPdfHelper.splitPDFIntoPages(file, category, singleCategory, true);
            return response;

        } catch (Exception e) {
            LOGGER.error("Error processing PDF splitting: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to split PDF: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_SPLIT_PDF_TWO_EQUAL_VALIDATION,
            description = "Checks if user wants to re-enter PDF file for splitting into two equal parts",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    @Field.Desc(name = "inputMap", type = "{ Reenter: Boolean, stateId: Text, Input File: File, Page Thresh Hold: Number }", required = true)
    public ExtensionResponse confirmSplitPdfTwoEqualValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Input File: File, Page Thresh Hold: Number }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            LOGGER.info("User choose not to re-enter data for split PDF into two equal parts");
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_SPLIT_PDF_TWO_EQUAL_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_SPLIT_PDF_TWO_EQUAL_VALIDATION,
            description = "Handles re-entered PDF file for splitting into two equal parts",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Two Equal Files", type = "[ File ]", required = false)
    public ExtensionResponse handleSplitPdfTwoEqualValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Input File: File, Page Thresh Hold: Number}") Map<String, Object> map) {

        // Extract parameters using the helper method
        app.krista.model.base.File inputFile = extractFileFromMap(map, SubCatalogConstants.SPLIT_PDF_TWO_EQUAL_INPUT_FILE);
        Double pageThreshHold = (Double) map.get("Page Thresh Hold");

        LOGGER.debug("handleSplitPdfTwoEqualValidation() - Extracted parameters: inputFile={}",
                inputFile != null ? inputFile.getFileName() : "null");

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.SPLIT_PDF_TWO_EQUAL_FILE_FORMAT,
                        inputFile != null ? inputFile.getFileName() : ""
                )
        );

        if (!validationResults.isEmpty()) {
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid PDF document (.pdf file).")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        try {
            // Call the SplitPdfTwoEqualHelper method directly to avoid validation loop
            LOGGER.info("Calling SplitPdfTwoEqualHelper.splitPDFIntoTwoEqualPart with corrected data");
            ExtensionResponse response = splitPdfTwoEqualHelper.splitPDFIntoTwoEqualPart(inputFile, pageThreshHold, true);
            return response;

        } catch (Exception e) {
            LOGGER.error("Error processing PDF splitting into two equal parts: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to split PDF into two equal parts: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_SPLIT_PDF_AT_PAGE_NUMBER_VALIDATION,
            description = "Checks if user wants to re-enter PDF file for splitting at page number",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    @Field.Desc(name = "inputMap", type = "{ Reenter: Boolean, stateId: Text, Input File: File, Split at Page Number: Number }", required = true)
    public ExtensionResponse confirmSplitPdfAtPageNumberValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Input File: File, Split at Page Number: Number }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            LOGGER.info("User chose not to re-enter data for split PDF at page number");
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_SPLIT_PDF_AT_PAGE_NUMBER_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_SPLIT_PDF_AT_PAGE_NUMBER_VALIDATION,
            description = "Handles re-entered PDF file for splitting at page number",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Split Files", type = "[ File ]", required = false)
    public ExtensionResponse handleSplitPdfAtPageNumberValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Input File: File, Split at Page Number: Number}") Map<String, Object> map) {

        // Extract parameters using the helper method
        app.krista.model.base.File inputFile = extractFileFromMap(map, SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_INPUT_FILE);
        Double splitPageNumber = (Double) map.get(SubCatalogConstants.SPLIT_PDF_AT_PAGE_NUMBER_SPLIT_PAGE);

        LOGGER.debug("handleSplitPdfAtPageNumberValidation() - Extracted parameters: inputFile={}, splitPage={}",
                inputFile != null ? inputFile.getFileName() : "null", splitPageNumber);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.SPLIT_PDF_AT_PAGE_NUMBER_FILE_FORMAT,
                        inputFile != null ? inputFile.getFileName() : ""
                )
        );

        if (!validationResults.isEmpty()) {
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid PDF document (.pdf file).")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        try {
            // Call the SplitPdfAtPageNumberHelper method directly to avoid validation loop
            LOGGER.info("Calling SplitPdfAtPageNumberHelper.splitPDFAtPageNumber with corrected data");
            ExtensionResponse response = splitPdfAtPageNumberHelper.splitPDFAtPageNumber(inputFile, splitPageNumber, true);
            return response;

        } catch (Exception e) {
            LOGGER.error("Error processing PDF splitting at page number: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to split PDF at page number: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_EXCEL_MULTIPLE_SHEETS_VALIDATION,
            description = "Checks if user wants to re-enter Excel file for multiple sheets to PDF conversion",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    @Field.Desc(name = "inputMap", type = "{ Reenter: Boolean, stateId: Text, Excel File: File }", required = true)
    public ExtensionResponse confirmExcelMultipleSheetsValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Excel File: File }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            LOGGER.info("User choose not to re-enter data for Excel multiple sheets to PDF conversion");
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_EXCEL_MULTIPLE_SHEETS_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_EXCEL_MULTIPLE_SHEETS_VALIDATION,
            description = "Handles re-entered Excel file for multiple sheets to PDF conversion",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.File(name = "PDF File", multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Generated PDF file containing all sheets from the Excel workbook'")}, options = {})
    public ExtensionResponse handleExcelMultipleSheetsValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Excel File: File}") Map<String, Object> map) {

        // Extract parameters using the helper method
        app.krista.model.base.File excelFile = extractFileFromMap(map, SubCatalogConstants.EXCEL_MULTIPLE_SHEETS_FILE);

        LOGGER.debug("handleExcelMultipleSheetsValidation() - Extracted parameters: excelFile={}",
                excelFile != null ? excelFile.getFileName() : "null");

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.EXCEL_MULTIPLE_SHEETS_FILE_FORMAT,
                        excelFile != null ? excelFile.getFileName() : ""
                )
        );

        if (!validationResults.isEmpty()) {
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid Excel document (.xlsx format).")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        try {
            // Call the ConvertExcelMultipleSheetsHelper method directly to avoid validation loop
            LOGGER.info("Calling ConvertExcelMultipleSheetsHelper.convertExcelFileWithMultipleSheetsToPDF with corrected data");
            ExtensionResponse response = convertExcelMultipleSheetsHelper.convertExcelFileWithMultipleSheetsToPDF(excelFile, true);
            return response;

        } catch (Exception e) {
            LOGGER.error("Error processing Excel multiple sheets to PDF conversion: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to convert Excel file with multiple sheets to PDF: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }


    /**
     * Extracts a File object from the map using the specified key.
     * Handles both single File objects and Lists containing File objects (for backward compatibility).
     *
     * @param map the input map containing the file data
     * @param key the key to look for in the map
     * @return the extracted File object, or null if not found
     */
    private File extractFileFromMap(Map<String, Object> map, String key) {
        Object fileObj = map.get(key);

        switch (fileObj) {
            case null -> {
                return null;
            }

            // Handle case where fileObj is directly a File object (expected case)
            case File file1 -> {
                return file1;
            }

            // Handle case where fileObj is a List containing File objects (backward compatibility)
            case List<?> fileList -> {
                if (!fileList.isEmpty()) {
                    Object firstItem = fileList.get(0);

                    if (firstItem instanceof File) {
                        return (File) firstItem;
                    } else {
                        // Try to deserialize using Gson
                        try {
                            File file = SubCatalogConstants.GSON.fromJson(SubCatalogConstants.GSON.toJson(firstItem), File.class);
                            return file;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            }
            default -> {
            }
        }

        // Try to deserialize using Gson as fallback
        try {
            return SubCatalogConstants.GSON.fromJson(SubCatalogConstants.GSON.toJson(fileObj), File.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Confirms if the user wants to re-enter the PDF file for PDF to HTML conversion.
     * This subcatalog request is triggered when PDF file format validation fails.
     *
     * <p>Related Jira Ticket: <a href="https://antbrains.atlassian.net/browse/KE-2783">KE-2783</a></p>
     *
     * @param map Input map containing Reenter flag, stateId, PDF File, and Include Formatting flag
     * @return ExtensionResponse with either a fetch response to re-enter data or a deny response
     */
    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_PDF_TO_HTML_VALIDATION,
            description = "Checks if user wants to re-enter PDF file for PDF to HTML conversion",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    @Field.Desc(name = "inputMap", type = "{ Reenter: Boolean, stateId: Text, PDF File: File, Include Formatting: Boolean }", required = true)
    public ExtensionResponse confirmPdfToHtmlValidation(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, PDF File: File, Include Formatting: Boolean }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(SubCatalogConstants.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmPdfToHtmlValidation() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.remove(stateId);
            LOGGER.info("User chose not to re-enter data for PDF to HTML conversion");
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        LOGGER.info("User chose to re-enter data for PDF to HTML conversion");
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_PDF_TO_HTML_VALIDATION,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    /**
     * Handles the re-entered PDF file for PDF to HTML conversion.
     * This subcatalog request validates the re-entered file and processes the conversion if valid.
     *
     * <p>Related Jira Ticket: <a href="https://antbrains.atlassian.net/browse/KE-2783">KE-2783</a></p>
     *
     * @param map Input map containing stateId, PDF File, and Include Formatting flag
     * @return ExtensionResponse with HTML file if successful, or error if validation fails
     */
    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_PDF_TO_HTML_VALIDATION,
            description = "Handles re-entered PDF file for PDF to HTML conversion",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Output File", type = "File", required = false)
    public ExtensionResponse handlePdfToHtmlValidation(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, PDF File: File, Include Formatting: Boolean, Save as Text File: Boolean}") Map<String, Object> map) {
        // Extract parameters using the helper method
        File pdfFile = extractFileFromMap(map, SubCatalogConstants.PDF_TO_HTML_FILE);
        Boolean includeFormatting = (Boolean) map.get("Include Formatting");
        Boolean saveAsTextFile = (Boolean) map.get("Save as Text File");
        LOGGER.debug("handlePdfToHtmlValidation() - Extracted parameters: pdfFile={}, includeFormatting={}, saveAsTextFile={}",
                pdfFile != null ? pdfFile.getFileName() : "null", includeFormatting, saveAsTextFile);
        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.PDF_TO_HTML_FILE_FORMAT,
                        pdfFile != null ? pdfFile.getFileName() : ""
                )
        );
        // If validation still fails after re-entry, return a final error response
        if (!validationResults.isEmpty()) {
            LOGGER.error("Validation failed again after re-entry for file: {}",
                    pdfFile != null ? pdfFile.getFileName() : "null");

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("Failed to validate input after re-entry. Please ensure you are providing a valid PDF document (.pdf file).")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
        // Check if we have a valid file
        if (pdfFile == null) {
            LOGGER.error("No valid PDF file found in the request");
            return new ExtensionResponseBuilder()
                    .failure(new ErrorBuilder()
                            .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                            .errorMessage("No valid PDF file found in the request")
                            .build())
                    .build();
        }
        try {
            // Call the ConvertPdfToHtmlHelper method with corrected data
            LOGGER.info("Calling ConvertPdfToHtmlHelper.convertPdfToHtml with corrected data");
            ExtensionResponse response = convertPdfToHtmlHelper.convertPdfToHtml(pdfFile, includeFormatting, saveAsTextFile, true);
            return response;
        } catch (Exception e) {
            LOGGER.error("Error processing PDF to HTML conversion: {}", e.getMessage(), e);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to convert PDF to HTML: " + e.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }
}

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

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.documents.pdf.client.KristaMediaClient;
import app.krista.extensions.essentials.documents.pdf.helper.*;
import app.krista.extensions.essentials.documents.pdf.service.*;
import app.krista.extensions.essentials.documents.pdf.util.Constants;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.extensions.essentials.documents.pdf.util.PDFUtils;
import app.krista.model.base.EntityValue;
import app.krista.model.base.File;
import app.krista.model.base.FreeForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DefaultAnnotationParam")
@Domain(id = "catEntryDomain_49c18799-c16b-4d33-98ac-f048f446a152",
        name = "Documents",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "f64cabc3-d5f3-487e-be36-5a96af5dddb9")
public final class PDFArea {

    private final PDFManager pdfManager;
    private final ExcelManager excelManager;
    private final DocxManager docxManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final KristaMediaClient kristaMediaClient;
    private final TelemetryHelper telemetryHelper;
    private final ConvertExcelToPDFHelper convertExcelToPDFHelper;
    private final ConvertImageToPdfHelper convertImageToPdfHelper;
    private final RichTextPdfHelper richTextPdfHelper;
    private final ConvertDocxToPdfHelper convertDocxToPdfHelper;
    private final MergePdfFilesHelper mergePdfFilesHelper;
    private final FillDocxTemplateHelper fillDocxTemplateHelper;
    private final TableDataPdfHelper tableDataPdfHelper;
    private final ExcelConverterService excelConverterService;
    private final SplitPdfHelper splitPdfHelper;
    private final SplitPdfTwoEqualHelper splitPdfTwoEqualHelper;
    private final SplitPdfConditionHelper splitPdfConditionHelper;
    private final ConvertExcelMultipleSheetsHelper convertExcelMultipleSheetsHelper;
    private final ConvertPdfToHtmlHelper convertPdfToHtmlHelper;
    private final PdfToHtmlConversionService pdfToHtmlConversionService;
    private final SplitPdfAtPageNumberHelper splitPdfAtPageNumberHelper;
    private final Invoker invoker;
    private static final Logger logger = LoggerFactory.getLogger(PDFArea.class);


    @Inject
    public PDFArea(PDFManager pdfManager, ExcelManager excelManager, DocxManager docxManager,
                   KristaMediaClientUtil kristaMediaClientUtil, KristaMediaClient kristaMediaClient,
                   TelemetryHelper telemetryHelper, ConvertExcelToPDFHelper convertExcelToPDFHelper,
                   ConvertImageToPdfHelper convertImageToPdfHelper, RichTextPdfHelper richTextPdfHelper,
                   ConvertDocxToPdfHelper convertDocxToPdfHelper,
                   MergePdfFilesHelper mergePdfFilesHelper, FillDocxTemplateHelper fillDocxTemplateHelper,
                   TableDataPdfHelper tableDataPdfHelper, ExcelConverterService excelConverterService,
                   SplitPdfHelper splitPdfHelper, SplitPdfTwoEqualHelper splitPdfTwoEqualHelper,
                   SplitPdfConditionHelper splitPdfConditionHelper,
                   ConvertExcelMultipleSheetsHelper convertExcelMultipleSheetsHelper,
                   ConvertPdfToHtmlHelper convertPdfToHtmlHelper,
                   PdfToHtmlConversionService pdfToHtmlConversionService, SplitPdfAtPageNumberHelper splitPdfAtPageNumberHelper, Invoker invoker) {
        this.pdfManager = pdfManager;
        this.excelManager = excelManager;
        this.docxManager = docxManager;
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.kristaMediaClient = kristaMediaClient;
        this.telemetryHelper = telemetryHelper;
        this.convertExcelToPDFHelper = convertExcelToPDFHelper;
        this.convertImageToPdfHelper = convertImageToPdfHelper;
        this.richTextPdfHelper = richTextPdfHelper;
        this.convertDocxToPdfHelper = convertDocxToPdfHelper;
        this.mergePdfFilesHelper = mergePdfFilesHelper;
        this.fillDocxTemplateHelper = fillDocxTemplateHelper;
        this.tableDataPdfHelper = tableDataPdfHelper;
        this.excelConverterService = excelConverterService;
        this.splitPdfHelper = splitPdfHelper;
        this.splitPdfTwoEqualHelper = splitPdfTwoEqualHelper;
        this.splitPdfConditionHelper = splitPdfConditionHelper;
        this.convertExcelMultipleSheetsHelper = convertExcelMultipleSheetsHelper;
        this.convertPdfToHtmlHelper = convertPdfToHtmlHelper;
        this.pdfToHtmlConversionService = pdfToHtmlConversionService;
        this.splitPdfAtPageNumberHelper = splitPdfAtPageNumberHelper;
        this.invoker = invoker;
    }

    /**
     * Converts an Excel file to PDF format.
     * This method takes an Excel file and an optional sheet name, then converts it to PDF.
     * If a sheet name is provided, only that specific sheet will be converted.
     *
     * @param excelFile The Excel file to convert (.xlsx or .xls format)
     * @param sheetName Optional name of the specific sheet to convert
     * @return An ExtensionResponse containing the generated PDF file if successful,
     * or error details if the conversion fails
     */
    @CatalogRequest(
            id = "localDomainRequest_5136d0a6-ffdc-4f22-993b-e049947fc46e",
            name = "Convert Excel to PDF",
            description = "Convert excel file data to pdf",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Excel PDF File", required = false, attributes = {@Attribute(name = "toolTip", value = "'Generated PDF File'")}, options = {})
    public ExtensionResponse convertExcelToPDF(
            @Field.File(name = "Excel File", multipleFileUpload = false, required = true, attributes = {@Attribute(name = "toolTip", value = "'The Excel file to convert to PDF'")}, options = {}) File excelFile,
            @Field.Text(name = "Sheet Name", required = false, attributes = {@Attribute(name = "toolTip", value = "'optional: The specific sheet name to convert'")}, options = {}) String sheetName,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using ConvertExcelToPDFHelper with validation");
            response = convertExcelToPDFHelper.convertExcelToPDF(excelFile, sheetName, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct ExcelManager conversion");
            try {
                // Download the file for processing
                java.io.File excelJavaFile = kristaMediaClientUtil.downloadFileHandler(excelFile);

                // Convert Excel to PDF using ExcelManager directly
                java.io.File pdfFile = excelManager.convertExcelToPDF(excelJavaFile, sheetName);

                // Upload the result
                File result = kristaMediaClientUtil.uploadFileHandler(pdfFile);

                // Create success response
                Map<String, Object> responseMap = Map.of("Excel PDF File", result);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct Excel to PDF conversion: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to convert Excel to PDF: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }

    /**
     * Fills a docx template with provided data and converts it to PDF.
     * This method takes a Word document template with placeholders (e.g., $$VARIABLE)
     * and replaces them with values from the provided template fields.
     *
     * @param templateFile   The Word document template file (.docx format)
     * @param templateFields Key-value pairs where keys match placeholders in the template
     * @return An ExtensionResponse containing the generated PDF file if successful,
     * or error details if the process fails
     */
    @CatalogRequest(
            id = "localDomainRequest_b1ae80a9-56e7-496f-9417-67f69aa66820",
            name = "Fill The Docx Template",
            description = "Fill the .docx file template with given keywords and convert to PDF",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Docx Template PDF File", required = false, attributes = {@Attribute(name = "toolTip", value = "'Generated PDF file with template fields filled in'")}, options = {})
    public ExtensionResponse fillTheDocxTemplate(
            @Field.File(name = "Template File", attributes = {@Attribute(name = "toolTip", value = "'Upload a .docx template file with placeholders (e.g., $$VARIABLE)'")}, options = {}) File templateFile,
            @Field(name = "Template Fields", type = "FreeForm", attributes = {@Attribute(name = "toolTip", value = "'Key-value pairs to replace placeholders in the template'")}, options = {}) FreeForm templateFields,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {
        logger.info("Fill The Docx Template");
        logger.info("PDFArea.fillTheDocxTemplate() called with templateFile={}",
                templateFile != null ? templateFile.getFileName() : "null" + " and templateFields=" + templateFields);

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using FillDocxTemplateHelper with validation");
            response = fillDocxTemplateHelper.fillTheDocxTemplate(templateFile, templateFields, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct DocxManager conversion");
            try {
                // Download the template file for processing
                java.io.File templateJavaFile = kristaMediaClientUtil.downloadFileHandler(templateFile);

                // Fill the template using DocxManager directly
                java.io.File filledTemplate = docxManager.fillTheDocTemplate(templateJavaFile, templateFields);

                // Upload the result
                File result = kristaMediaClientUtil.uploadFileHandler(filledTemplate);

                // Create success response
                Map<String, Object> responseMap = Map.of("Docx Template PDF File", result);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct docx template filling: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to fill docx template: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }

    /**
     * Converts a Word document (.docx) file to PDF format.
     * This method takes a Word document and converts it to PDF while preserving
     * formatting, images, and other content.
     *
     * @param documentFile The Word document file to convert (.docx format)
     * @return An ExtensionResponse containing the generated PDF file if successful,
     * or error details if the conversion fails
     */
    @CatalogRequest(
            id = "localDomainRequest_1539be88-d479-4184-a767-c368abfbd99e",
            name = "Convert Docx File to PDF File",
            description = "Converting microsoft office document file with .docx format to pdf file.",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Docx PDF File", required = false, attributes = {@Attribute(name = "toolTip", value = "'PDF File'")}, options = {})
    public ExtensionResponse convertDocxFileToPDFFile(
            @Field.File(name = "Document File", attributes = {@Attribute(name = "toolTip", value = "'Upload the .docx file to convert to PDF'")}, options = {}) File documentFile,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {
        logger.info("Convert Docx File to PDF File");
        logger.info("PDFArea.convertDocxFileToPDFFile() called with documentFile={}",
                documentFile != null ? documentFile.getFileName() : "null");

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using ConvertDocxToPdfHelper with validation");
            response = convertDocxToPdfHelper.convertDocxToPDF(documentFile, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct PDFManager conversion");
            try {
                // Download the document file for processing
                java.io.File docxJavaFile = kristaMediaClientUtil.downloadFileHandler(documentFile);

                // Convert docx to PDF using PDFManager directly
                java.io.File pdfJavaFile = pdfManager.convertDocToPDF(docxJavaFile);

                // Upload the result
                File result = kristaMediaClientUtil.uploadFileHandler(pdfJavaFile);

                // Create success response
                Map<String, Object> responseMap = Map.of("Docx PDF File", result);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct docx to PDF conversion: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to convert docx to PDF: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }

    /**
     * Generates a PDF file from rich text content.
     * This method takes rich text data along with optional formatting parameters
     * like title, subtitle, header, footer, and logo to create a professionally
     * formatted PDF document.
     *
     * @param title        Optional title for the document
     * @param subTitle     Optional subtitle for the document
     * @param description  Optional rich text description
     * @param header       Optional header text to appear on all pages
     * @param footer       Optional footer text to appear on all pages
     * @param logo         Optional logo image to include in the document
     * @param richTextData The main rich text content for the PDF
     * @return An ExtensionResponse containing the generated PDF file if successful,
     * or error details if the generation fails
     */
    @CatalogRequest(
            id = "localDomainRequest_0236225f-ebee-45c4-b0c1-b00dada3a096",
            name = "Get Rich Text PDF File",
            description = "This request would take Rich Text as input and generate a pdf file for it.",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "RichText PDF File", required = false, attributes = {@Attribute(name = "toolTip", value = "'Rich Text PDF File'")}, options = {})
    public ExtensionResponse getRichTextPDFFile(
            @Field.Text(name = "Title", required = false, attributes = {@Attribute(name = "toolTip", value = "'Title of the document'")}, options = {}) String title,
            @Field.Text(name = "Sub Title", required = false, attributes = {@Attribute(name = "toolTip", value = "'Sub-title of the document'")}, options = {}) String subTitle,
            @Field(name = "Description", type = "RichText", required = false, attributes = {@Attribute(name = "toolTip", value = "'Optional rich text description'")}, options = {}) String description,
            @Field.Text(name = "Header", required = false, attributes = {@Attribute(name = "toolTip", value = "'Header text to appear on pages'")}, options = {}) String header,
            @Field.Text(name = "Footer", required = false, attributes = {@Attribute(name = "toolTip", value = "'Footer text to appear on page'")}, options = {}) String footer,
            @Field.File(name = "Logo", required = false, attributes = {@Attribute(name = "toolTip", value = "'Optional logo image file'")}, options = {}) File logo,
            @Field(name = "Rich Text Data", type = "RichText", attributes = {@Attribute(name = "toolTip", value = "'Main rich text content for PDF'")}, options = {}) String richTextData,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {

        logger.info("Get Rich Text PDF File");
        logger.info("PDFArea.getRichTextPDFFile() called with title={}, richTextData length={}",
                title, richTextData != null ? richTextData.length() : 0);

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using RichTextPdfHelper with validation");
            response = richTextPdfHelper.getRichTextPDF(title, subTitle, description, header, footer, logo, richTextData, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct PDFManager conversion");
            try {
                // Download logo file if provided
                java.io.File logoJavaFile = null;
                if (logo != null) {
                    logoJavaFile = kristaMediaClientUtil.downloadFileHandler(logo);
                }

                // Generate rich text PDF using PDFManager directly
                java.io.File pdfFile = pdfManager.getRichTextPDF(title, subTitle, description, header, footer, logoJavaFile, richTextData);

                // Upload the result
                File result = kristaMediaClientUtil.uploadFileHandler(pdfFile);

                // Create success response
                Map<String, Object> responseMap = Map.of("RichText PDF File", result);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct rich text PDF generation: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to generate rich text PDF: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }

    /**
     * Generates a PDF file from a list of entity values.
     * This method takes a list of entity objects and converts them into a formatted PDF document
     * with optional title, subtitle, header, footer, and logo. The entity data is rendered as a table
     * in the PDF with each entity's properties displayed as columns.
     *
     * @param title        Optional title for the document
     * @param subTitle     Optional subtitle for the document
     * @param description  Optional rich text description
     * @param header       Optional header text to appear on all pages
     * @param footer       Optional footer text to appear on all pages
     * @param logo         Optional logo image to include in the document
     * @param entityValues List of entity values to include in the PDF (must not be null or empty)
     * @return A File object containing the generated PDF with entity data
     * @throws Exception if entity values are null/empty or if PDF generation fails
     */
    @CatalogRequest(
            id = "localDomainRequest_363f33f7-b5c0-43be-bc31-93a2a9f2b0da",
            name = "Get Entity List PDF",
            description = "This request would take List of Entity Values as input and generate a pdf file for it.",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Entity List PDF File", attributes = {@Attribute(name = "toolTip", value = "'Generated PDF file containing entity data'")})
    public File getEntityListPDF(
            @Field.Text(name = "Title", required = false, attributes = {@Attribute(name = "toolTip", value = "'Title of the document'")}, options = {}) String title,
            @Field.Text(name = "Sub Title", required = false, attributes = {@Attribute(name = "toolTip", value = "'Sub-title of the document'")}, options = {}) String subTitle,
            @Field(name = "Description", type = "RichText", required = false, attributes = {@Attribute(name = "toolTip", value = "'Optional rich text description'")}, options = {}) String description,
            @Field.Text(name = "Header", required = false, attributes = {@Attribute(name = "toolTip", value = "'Header text to appear on pages'")}, options = {}) String header,
            @Field.Text(name = "Footer", required = false, attributes = {@Attribute(name = "toolTip", value = "'Footer text to appear on pages'")}, options = {}) String footer,
            @Field.File(name = "Logo", required = false, attributes = {@Attribute(name = "toolTip", value = "'Optional logo image file'")}, options = {}) File logo,
            @Field.Desc(name = "Entity Values", type = "[ Entity ]") List<EntityValue> entityValues,
            @Field.Text(name = "Timezone", required = false, attributes = {@Attribute(name = "toolTip", value = "'Timezone for date formatting (e.g., America/New_York, Asia/Kolkata, UTC). If not provided, uses server timezone.'")}, options = {}) String timezone) throws Exception {

        long startTime = System.currentTimeMillis();
        try {
            telemetryHelper.incrementCount("pdf.getEntityListPDF");
            if (entityValues == null || entityValues.isEmpty()) {
                throw new IllegalArgumentException("Please provide list of Entity Values");
            }

            java.io.File javaFile = pdfManager.getEntityListPdf(title, subTitle, description, header, footer, kristaMediaClientUtil.downloadFileHandler(logo), entityValues, timezone);
            File result = kristaMediaClientUtil.uploadFileHandler(javaFile);
            telemetryHelper.recordSuccess("pdf.getEntityListPDF", startTime,
                    TelemetryHelper.safeTagMap("title", title != null ? title : "N/A",
                            "entity_count", String.valueOf(entityValues.size()),
                            "timezone", timezone != null ? timezone : "server_default"));
            return result;
        } catch (Exception e) {
            telemetryHelper.recordError("pdf.getEntityListPDF", startTime, e,
                    TelemetryHelper.safeTagMap("title", title != null ? title : "N/A",
                            "entity_count", entityValues != null ? String.valueOf(entityValues.size()) : "0"));
            throw e;
        }
    }

    /**
     * Generates a PDF file from table data.
     * This method takes a list of maps representing table rows and converts them into a formatted
     * PDF document with optional title, subtitle, header, footer, and logo. The table data is rendered
     * with column headers derived from the map keys and rows from the map values.
     *
     * @param title       Optional title for the document
     * @param subTitle    Optional subtitle for the document
     * @param description Optional paragraph description
     * @param header      Optional header text to appear on all pages
     * @param footer      Optional footer text to appear on all pages
     * @param logo        Optional logo image to include in the document
     * @param tableData   List of maps where each map represents a table row with column-value pairs
     * @return A File object containing the generated PDF with table data
     * @throws Exception if table data is invalid or if PDF generation fails
     */
    @CatalogRequest(
            id = "localDomainRequest_95a22d87-eaa3-4b6a-bd97-f97a44717201",
            name = "Get Table PDF",
            description = "Get PDF file for the given table format data",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Table PDF File", required = false)
    public File getTableDataPDF(
            @Field(name = "Title", type = "Text", required = false) String title,
            @Field(name = "Sub Title", type = "Text", required = false) String subTitle,
            @Field(name = "Description", type = "Paragraph", required = false) String description,
            @Field(name = "Header", type = "Text", required = false) String header,
            @Field(name = "Footer", type = "Text", required = false) String footer,
            @Field.File(name = "Logo", required = false) File logo,
            @Field.Desc(name = "Table Data", type = "[ Composite ]") List<Map<String, Object>> tableData) throws Exception {
        logger.info("Get Table PDF");
        logger.info("PDFArea.getTableDataPDF() called with title={}, tableData size={}",
                title, tableData != null ? tableData.size() : 0);

        return tableDataPdfHelper.getTableDataPDF(title, subTitle, description, header, footer, logo, tableData);
    }

    /**
     * Merges multiple PDF files into a single PDF document.
     * This method takes a list of PDF files and combines them into one PDF document with optional
     * title page, header, footer, and logo. The files are merged in the order they appear in the list.
     * Supports validation to ensure all files are valid PDFs and at least 2 files are provided.
     *
     * @param title       Title for the merged document
     * @param subTitle    Optional subtitle for the document
     * @param description Optional paragraph description
     * @param header      Optional header text to appear on all pages
     * @param footer      Optional footer text to appear on all pages
     * @param logo        Optional logo image to include in the document
     * @param inputFiles  List of PDF files to merge (must contain at least 2 valid PDF files)
     * @param allowRetry  If true, enables validation and retry flow; if false, performs direct merge
     * @return An ExtensionResponse containing the merged PDF file if successful,
     * or error details if the merge fails
     */
    @CatalogRequest(
            id = "localDomainRequest_4d81e456-f8e1-409f-a3e1-0c1cf16cb69b",
            name = "Merge PDF Files",
            description = "This request would take list of Files and merge the files to a pdf file",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Merged PDF", required = false, attributes = {@Attribute(name = "toolTip", value = "'Merged PDF File'")}, options = {})
    public ExtensionResponse mergePDFFiles(
            @Field.Text(name = "Title", attributes = {@Attribute(name = "toolTip", value = "'Title of the document'")}, options = {}) String title,
            @Field.Text(name = "Sub Title", required = false, attributes = {@Attribute(name = "toolTip", value = "'Sub-title of the document'")}, options = {}) String subTitle,
            @Field(name = "Description", type = "Paragraph", required = false, attributes = {@Attribute(name = "toolTip", value = "'Optional rich text description'")}, options = {}) String description,
            @Field.Text(name = "Header", required = false, attributes = {@Attribute(name = "toolTip", value = "'Header text to appear on pages'")}, options = {}) String header,
            @Field.Text(name = "Footer", required = false, attributes = {@Attribute(name = "toolTip", value = "'Footer text to appear on pages'")}, options = {}) String footer,
            @Field.File(name = "Logo", required = false, attributes = {@Attribute(name = "toolTip", value = "'Optional logo image file'")}, options = {}) File logo,
            @Field.File(name = "Pdf Files", multipleFileUpload = true, attributes = {@Attribute(name = "toolTip", value = "'Select multiple PDF files to merge'")}, options = {}) List<File> inputFiles,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {

        logger.info("Merge PDF Files");
        logger.info("PDFArea.mergePDFFiles() called with title={}, inputFiles size={}",
                title, inputFiles != null ? inputFiles.size() : 0);

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using MergePdfFilesHelper with validation");
            response = mergePdfFilesHelper.mergePdfFiles(title, subTitle, description, header, footer, logo, inputFiles, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct PDFManager conversion for merge PDF");
            try {
                // Download logo file if provided
                java.io.File logoJavaFile = null;
                if (logo != null) {
                    logoJavaFile = kristaMediaClientUtil.downloadFileHandler(logo);
                }

                // Download all input files
                List<java.io.File> mergeFiles = new ArrayList<>();
                for (File inputFile : inputFiles) {
                    mergeFiles.add(kristaMediaClientUtil.downloadFileHandler(inputFile));
                }

                // Merge PDFs using PDFManager directly
                java.io.File javaPdfFile = pdfManager.mergePdfFiles(title, subTitle, description, header, footer, logoJavaFile, mergeFiles);

                // Upload the result
                File result = kristaMediaClientUtil.uploadFileHandler(javaPdfFile);

                // Create success response
                Map<String, Object> responseMap = Map.of("Merged PDF", result);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct PDF merge: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to merge PDF files: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }

    /**
     * Converts an image file (JPG, JPEG, PNG) into a PDF document.
     * This method accepts a single image file and converts it into a PDF.
     * The resulting PDF file is returned as part of the extension response.
     *
     * @param documentFile The input image file to convert. Must be JPG or PNG or JPEG.
     * @return An ExtensionResponse containing the generated PDF file if successful,
     * or error details if the conversion fails.
     */
    @CatalogRequest(
            id = "localDomainRequest_d2e6068c-14ed-4390-90cb-18e11ef70002",
            name = "Convert image to pdf",
            description = "Converts one or more image files (JPG, PNG, JPEG.) into a single PDF document.",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "PDF File", multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'PDF File'")}, options = {})
    public ExtensionResponse convertImageToPdf(
            @Field.File(name = "Document File", multipleFileUpload = false, required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Upload an image (PNG, JPG, JPEG) to convert'")}, options = {}) File documentFile,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {
        logger.info("Convert Image to PDF");
        logger.info("PDFArea.convertImageToPdf() called with documentFile={}",
                documentFile != null ? documentFile.getFileName() : "null");

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using ConvertImageToPdfHelper with validation");
            response = convertImageToPdfHelper.convertImageToPdf(documentFile, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct PDFUtils conversion");
            try {
                // Convert input Krista File to Java File
                java.io.File sourceFile = kristaMediaClient.toJavaFile(documentFile);

                // Convert to PDF using PDFUtils directly
                java.io.File pdfFile = PDFUtils.convertImageToPdf(sourceFile);

                // Convert Java File back to Krista File
                File kristaPdfFile = kristaMediaClient.toKristaFile(pdfFile);

                // Create success response
                Map<String, Object> responseMap = Map.of("PDF File", kristaPdfFile);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct image to PDF conversion: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to convert image to PDF: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }

    /**
     * Splits a PDF file into individual pages, with each page saved as a separate PDF file.
     * This method provides two splitting strategies: standard page-by-page splitting and
     * custom criteria-based splitting for specific document types like "NET&SYN Talent Report".
     * Includes validation to ensure only valid PDF files are processed.
     *
     * @param file           The PDF file to be split into individual pages. Must be a valid PDF document.
     * @param category       Optional list of categories used for output filtering when combined with singleCategory.
     * @param singleCategory Optional category that triggers custom splitting logic and output filtering.
     * @return ExtensionResponse containing the list of split PDF files or error details if validation fails.
     */
    @CatalogRequest(
            id = "localDomainRequest_ac1eb24f-e87e-413e-b936-05a34c67bc88",
            name = "Split PDF into Pages",
            description = "This request will split the uploaded PDF file, returning each page as a separate file.",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Split Files", type = "[ File ]", required = false)
    public ExtensionResponse splitPDFIntoPages(
            @Field.File(name = "File", multipleFileUpload = false, required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Upload the PDF file to split into individual pages'")}, options = {}) File file,
            @Field.Desc(name = "Category", type = "[ Text ]", required = false) List<String> category,
            @Field.Text(name = "Single Category", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Optional category for custom splitting logic (e.g., NET&SYN Talent Report)'")}, options = {}) String singleCategory,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {


        logger.info("Starting PDF split operation for file: {}", file != null ? file.getFileName() : "null");

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using SplitPdfHelper with validation");
            response = splitPdfHelper.splitPDFIntoPages(file, category, singleCategory, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct PdfSplitter conversion");
            try {
                // Download the PDF file for processing
                java.io.File pdfJavaFile = kristaMediaClientUtil.downloadFileHandler(file);

                // Split PDF using PDFManager directly
                List<java.io.File> splitFiles = pdfManager.splitPDF(pdfJavaFile);

                // Upload all result files
                List<File> resultFiles = new ArrayList<>();
                for (java.io.File splitFile : splitFiles) {
                    resultFiles.add(kristaMediaClientUtil.uploadFileHandler(splitFile));
                }

                // Create success response
                Map<String, Object> responseMap = Map.of("Split Files", resultFiles);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct PDF split: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to split PDF into pages: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }

    /**
     * Splits a PDF file into multiple parts based on a configurable page threshold.
     * This method divides a PDF document into smaller chunks, with each chunk containing
     * a maximum number of pages as specified by the page threshold parameter.
     *
     * @param inputFile      The PDF file to be split into multiple parts. Must be a valid PDF document.
     * @param pageThreshHold The maximum number of pages per split file. Defaults to 40 if not provided.
     * @return A list of File objects, each containing a portion of the original PDF.
     */
    @CatalogRequest(
            id = "localDomainRequest_d02e13c0-7b7b-49f8-a356-76d072ec83d8",
            name = "Split PDF Into Two Equal Part",
            description = "Split PDF Into Two Equal Part with configurable page threshold",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Two Equal Files", type = "[ File ]", required = false)
    public ExtensionResponse splitPDFIntoTwoEqualPart(
            @Field.File(name = "Input File", multipleFileUpload = false, required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Upload the PDF file to split into multiple parts'")}, options = {}) File inputFile,
            @Field(name = "Page Thresh Hold", type = "Number", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Maximum pages per split file (default: 40 if not specified)'")}, options = {}) Double pageThreshHold,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {

        logger.info("Starting PDF split into two equal parts operation for file: {}", inputFile != null ? inputFile.getFileName() : "null");

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using SplitPdfTwoEqualHelper with validation");
            response = splitPdfTwoEqualHelper.splitPDFIntoTwoEqualPart(inputFile, pageThreshHold, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct PdfSplitter conversion");
            try {
                // Download the PDF file for processing
                java.io.File pdfJavaFile = kristaMediaClientUtil.downloadFileHandler(inputFile);

                // Split PDF using PDFManager directly
                List<java.io.File> splitFiles = pdfManager.splitPDFIntoParts(pdfJavaFile, pageThreshHold != null ? pageThreshHold.intValue() : 40);

                // Upload all result files
                List<File> resultFiles = new ArrayList<>();
                for (java.io.File splitFile : splitFiles) {
                    resultFiles.add(kristaMediaClientUtil.uploadFileHandler(splitFile));
                }

                // Create success response
                Map<String, Object> responseMap = Map.of("Two Equal Files", resultFiles);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct PDF split into two equal parts: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to split PDF into two equal parts: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }

    /**
     * Converts an Excel file with multiple sheets to a single PDF document.
     * This method processes all sheets within the Excel file and combines them
     * into one comprehensive PDF document, preserving the content and formatting of each sheet.
     *
     * @param excelFile The Excel file to convert (.xlsx or .xls format) containing multiple sheets.
     * @return An ExtensionResponse containing the generated PDF file if successful,
     * or error details if the conversion fails.
     */
    @CatalogRequest(
            id = "localDomainRequest_f02f9c46-a4d4-495e-ba63-409ce3b5e37b",
            name = "Convert Excel File with Multiple Sheets to PDF",
            description = "Convert Excel File with Multiple Sheets to PDF",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "PDF File", multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Generated PDF file containing all sheets from the Excel workbook'")}, options = {})
    public ExtensionResponse convertExcelFileWithMultipleSheetsToPDF(
            @Field.File(name = "Excel File", multipleFileUpload = false, required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Upload an Excel file (.xlsx or .xls) with multiple sheets to convert to PDF'")}, options = {}) File excelFile,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {

        logger.info("Starting Excel file with multiple sheets to PDF conversion for file: {}", excelFile != null ? excelFile.getFileName() : "null");

        ExtensionResponse response;
        logger.debug("Allow Retry setting: {}", allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Error handling is enabled, using ConvertExcelMultipleSheetsHelper with validation");
            response = convertExcelMultipleSheetsHelper.convertExcelFileWithMultipleSheetsToPDF(excelFile, allowRetry);
        } else {
            logger.debug("Error handling is disabled, using direct ExcelConverterService conversion");
            try {
                // Convert Excel file with multiple sheets to PDF using ExcelConverterService directly
                File result = excelConverterService.convertExcelFileWithMultipleSheetsToPDF(excelFile);

                // Create success response
                Map<String, Object> responseMap = Map.of("PDF File", result);
                response = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseMap, null, null, null);

            } catch (Exception e) {
                logger.error("Error in direct Excel multiple sheets to PDF conversion: {}", e.getMessage(), e);
                ExtensionResponse.Error error = new ExtensionResponse.Error(
                        "Failed to convert Excel file with multiple sheets to PDF: " + e.getMessage(),
                        System.currentTimeMillis(),
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                        Arrays.toString(e.getStackTrace())
                );
                response = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
            }
        }

        return response;
    }


    /**
     * Converts a PDF file to high-fidelity HTML with complex layout support using Python script (PyMuPDF/fitz).
     *
     * <p>This catalog request is specifically designed for PDFs with <strong>complex layouts</strong> such as
     * multi-column documents, forms, tables, embedded graphics, and mixed content. It uses the PyMuPDF (fitz)
     * library to convert PDF files to HTML with exceptional fidelity, preserving exact text positioning, fonts,
     * embedded images, and SVG graphics. This method provides superior layout preservation compared to
     * Java-based PDF libraries, making it ideal for documents where visual accuracy is critical.</p>
     *
     * <p><strong>Best Use Cases:</strong></p>
     * <ul>
     *   <li><strong>Complex Forms:</strong> Employment applications, tax forms, insurance documents with precise field positioning</li>
     *   <li><strong>Multi-Column Layouts:</strong> Newsletters, brochures, magazines with complex column structures</li>
     *   <li><strong>Technical Documents:</strong> Engineering drawings, architectural plans, diagrams with exact positioning</li>
     *   <li><strong>Financial Reports:</strong> Statements, invoices, reports with tables and formatted data</li>
     *   <li><strong>Marketing Materials:</strong> Flyers, posters, presentations with graphics and custom fonts</li>
     * </ul>
     *
     * <p><strong>Key Features:</strong></p>
     * <ul>
     *   <li><strong>Pixel-Perfect Layout Preservation:</strong> Maintains exact text positioning, spacing, and page structure</li>
     *   <li><strong>Font Fidelity:</strong> Preserves original fonts, sizes, colors, weights, and styling</li>
     *   <li><strong>Embedded Images:</strong> Includes images as base64-encoded data URIs (no external files needed)</li>
     *   <li><strong>SVG Graphics Support:</strong> Renders vector graphics, drawings, and diagrams with precision</li>
     *   <li><strong>Table Structure:</strong> Maintains complex table layouts with borders, spacing, and alignment</li>
     *   <li><strong>Multi-Page Support:</strong> Converts all pages while preserving individual page layouts</li>
     *   <li><strong>Security Compliance:</strong> Returns ZIP file containing HTML (HTML files cannot be uploaded directly to Krista Media)</li>
     *   <li><strong>Automatic Python Management:</strong> Uses PythonEnvironmentManager to handle Python installation and dependencies</li>
     * </ul>
     *
     * <p><strong>Output Formats:</strong></p>
     * <ul>
     *   <li><strong>ZIP File (default, saveAsTextFile=false):</strong> Contains .html file with all resources embedded.
     *       Users must extract the ZIP file to access the .html file and view it in a browser. This is the recommended
     *       format for security compliance as HTML files cannot be uploaded directly to Krista Media. The HTML file
     *       is self-contained with all images and fonts embedded.</li>
     *   <li><strong>TXT File (saveAsTextFile=true):</strong> Contains HTML source code as plain text. Can be uploaded
     *       directly to Krista Media, but users must manually save the content as .html to view it in a browser.
     *       Useful for inspecting the HTML structure or integrating into other systems.</li>
     * </ul>
     *
     * <p><strong>Technical Requirements:</strong></p>
     * <ul>
     *   <li>Python 3.x (automatically managed by PythonEnvironmentManager in deployed environments)</li>
     *   <li>PyMuPDF library (automatically installed via PythonEnvironmentManager)</li>
     *   <li>Python script: {@code src/main/resources/convert_pdf_to_html.py} (extracted from JAR at runtime)</li>
     *   <li>Sufficient memory for processing large PDFs with embedded images</li>
     * </ul>
     *
     * <p><strong>How It Works:</strong></p>
     * <ol>
     *   <li>Downloads the PDF file from Krista Media to temporary storage</li>
     *   <li>Extracts the Python conversion script from JAR to temporary location ({@code /tmp/alliance_hcm_scripts/})</li>
     *   <li>Executes Python script using PyMuPDF to convert PDF to HTML with exact positioning</li>
     *   <li>Packages the HTML output as ZIP (default) or TXT file with timestamp</li>
     *   <li>Uploads the result to Krista Media and returns the file reference</li>
     *   <li>Cleans up temporary files to free disk space</li>
     * </ol>
     *
     * <p><strong>Usage Example:</strong></p>
     * <pre>
     * Input: complex_enrollment_form.pdf (multi-column form with tables and images)
     *
     * Output (saveAsTextFile = false): html_1234567890_complex_enrollment_form.zip
     *   → Download the ZIP file
     *   → Extract ZIP to get complex_enrollment_form.html
     *   → Open complex_enrollment_form.html in browser to view
     *   → Layout matches original PDF exactly
     *
     * Output (saveAsTextFile = true): html_1234567890_complex_enrollment_form.txt
     *   → Download the .txt file
     *   → Rename to complex_enrollment_form.html
     *   → Open in browser to view
     *   → Or use the HTML source code in other applications
     * </pre>
     *
     * <p><strong>Comparison with Other PDF to HTML Methods:</strong></p>
     * <ul>
     *   <li><strong>Python/PyMuPDF (this request):</strong> Highest fidelity, exact positioning, best for complex layouts,
     *       forms, and documents where visual accuracy is critical. Slower but more accurate.</li>
     *   <li><strong>Java/PDFBox ("Convert PDF to HTML"):</strong> Semantic HTML structure, better for text extraction,
     *       accessibility, and simple documents. Faster but less accurate for complex layouts.</li>
     * </ul>
     *
     * <p><strong>Performance Considerations:</strong></p>
     * <ul>
     *   <li>Processing time increases with PDF size and complexity</li>
     *   <li>Large PDFs with many images may produce large HTML files due to base64 encoding</li>
     *   <li>Recommended for PDFs under 50 pages for optimal performance</li>
     *   <li>First execution may be slower due to Python environment setup</li>
     * </ul>
     *
     * <p><strong>Troubleshooting:</strong></p>
     * <ul>
     *   <li><strong>Python not found:</strong> PythonEnvironmentManager will auto-install Python in deployed environments</li>
     *   <li><strong>PyMuPDF not installed:</strong> Library is auto-installed on first use</li>
     *   <li><strong>Large file size:</strong> Use saveAsTextFile=false (ZIP) to compress output</li>
     *   <li><strong>Layout issues:</strong> Ensure PDF is not corrupted or password-protected</li>
     * </ul>
     *
     * <p>Related Jira Ticket: <a href="https://antbrains.atlassian.net/browse/KE-2783">KE-2783</a></p>
     *
     * @param pdfFile        The PDF file to convert to HTML format (required). Supports complex layouts, forms,
     *                       tables, multi-column documents, and embedded graphics.
     * @param saveAsTextFile True: Returns .txt file with HTML source code (can upload directly to Krista Media).
     *                       False: Returns .zip file containing .html file (extract ZIP to access HTML - required for security).
     *                       Default: false (ZIP format recommended)
     * @param allowRetry     Enable error handling and retry logic (default: false). When enabled, provides detailed
     *                       error messages for troubleshooting.
     * @return ExtensionResponse containing the output file (ZIP or TXT) and conversion status.
     * Success: Returns file reference with metadata (filename, size, media ID, output format).
     * Failure: Returns error details with exception type, message, and troubleshooting information.
     */
    @CatalogRequest(
            id = "localDomainRequest_a7f3c8d9-2e4b-4f1a-9c3d-8b5e6f7a8c9d",
            name = "Convert PDF to HTML",
            description = "Convert PDF file content to HTML-tagged format with proper structure and formatting. Returns ZIP file containing HTML (for security) or TXT file with HTML source code.",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Output File", multipleFileUpload = false, required = false,
            attributes = {@Attribute(name = "toolTip", value = "'ZIP file containing .html file (extract to view) OR .txt file with HTML source code. HTML files cannot be uploaded directly for security reasons.'")},
            options = {})
    public ExtensionResponse convertPdfToHtml(
            @Field.File(name = "PDF File", multipleFileUpload = false, required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "S"),
                            @Attribute(name = "toolTip", value = "'Upload the PDF file to convert to HTML format'")},
                    options = {}) File pdfFile,
            @Field.Boolean(name = "Include Formatting", required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'Enable to detect and preserve headings, lists, and formatting (default: true)'")},
                    options = {}) Boolean includeFormatting,
            @Field.Boolean(name = "Save as Text File", required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'True: Returns .txt file with HTML source code (can upload directly). False: Returns .zip file containing .html file (extract ZIP to access HTML - required for security)'")},
                    options = {}) Boolean saveAsTextFile,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {
        logger.info("Convert PDF to HTML (Python) - pdfFile={}, saveAsTextFile={}, allowRetry={}",
                pdfFile != null ? pdfFile.getFileName() : "null", saveAsTextFile, allowRetry);

        // Always use validation flow with retry enabled by default
        // This ensures users get a chance to correct invalid files
        Boolean effectiveAllowRetry = (allowRetry != null) ? allowRetry : true;
        logger.debug("Effective allowRetry value: {}", effectiveAllowRetry);

        return pdfToHtmlConversionService.convertPdfToHtml(pdfFile, saveAsTextFile, effectiveAllowRetry);
    }

    /**
     * Converts a PDF file to HTML format with optional interactive form elements.
     *
     * @param pdfFile        The PDF file to convert to HTML format (required). Supports forms, tables,
     *                       multi-column documents, and embedded graphics.
     * @param interactive    True: Preserves PDF form fields as interactive HTML inputs (checkboxes, text fields, etc.).
     *                       False: Renders form fields as static text (default: false).
     * @param saveAsTextFile True: Returns .txt file with HTML source code (can upload directly to Krista Media).
     *                       False: Returns .zip file containing .html file (extract ZIP to access HTML - required for security).
     *                       Default: false (ZIP format recommended)
     * @param allowRetry     Enable error handling and retry logic (default: true). When enabled, provides detailed
     *                       error messages for troubleshooting.
     * @return ExtensionResponse containing the output file (ZIP or TXT) and conversion status.
     * Success: Returns file reference with metadata (filename, size, media ID, output format).
     * Failure: Returns error details with exception type, message, and troubleshooting information.
     */
    @CatalogRequest(
            id = "localDomainRequest_c1d2e3f4-a5b6-4c7d-8e9f-0a1b2c3d4e5f",
            name = "Convert PDF to HTML with Interactive Forms",
            description = "Convert PDF file to HTML with optional interactive form elements. Choose between static HTML (read-only) or interactive HTML (with functional form fields). Returns ZIP file containing HTML or TXT file with HTML source code.",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "Output File", multipleFileUpload = false, required = false,
            attributes = {@Attribute(name = "toolTip", value = "'ZIP file containing .html file (extract to view) OR .txt file with HTML source code. HTML files cannot be uploaded directly for security reasons.'")},
            options = {})
    public ExtensionResponse convertPdfToHtmlWithInteractiveForms(
            @Field.File(name = "PDF File", multipleFileUpload = false, required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "S"),
                            @Attribute(name = "toolTip", value = "'Upload the PDF file to convert to HTML format'")},
                    options = {}) File pdfFile,
            @Field.Boolean(name = "Interactive", required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'True: Preserve PDF form fields as interactive HTML inputs (checkboxes, text fields). False: Render as static text (default: false)'")},
                    options = {}) Boolean interactive,
            @Field.Boolean(name = "Save as Text File", required = false,
                    attributes = {@Attribute(name = "toolTip", value = "'True: Returns .txt file with HTML source code (can upload directly). False: Returns .zip file containing .html file (extract ZIP to access HTML - required for security). Default: false'")},
                    options = {}) Boolean saveAsTextFile,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {
        logger.info("Convert PDF to HTML with Interactive Forms - pdfFile={}, interactive={}, saveAsTextFile={}, allowRetry={}",
                pdfFile != null ? pdfFile.getFileName() : "null", interactive, saveAsTextFile, allowRetry);

        // Default interactive to false if not provided
        Boolean effectiveInteractive = (interactive != null) ? interactive : false;

        // Always use validation flow with retry enabled by default
        Boolean effectiveAllowRetry = (allowRetry != null) ? allowRetry : true;
        logger.debug("Effective interactive value: {}, Effective allowRetry value: {}", effectiveInteractive, effectiveAllowRetry);

        // Pass interactive flag to conversion service
        return pdfToHtmlConversionService.convertPdfToHtml(pdfFile, saveAsTextFile, effectiveAllowRetry, effectiveInteractive);
    }

    /**
     * Splits a PDF file at a specific page number into two separate files.
     *
     * <p>This method takes a PDF file and a page number, then splits the document into two parts:</p>
     * <ul>
     *   <li><strong>First Segment:</strong> Contains pages from the beginning up to (but not including) the split page</li>
     *   <li><strong>Second Segment:</strong> Contains pages from the split page to the end of the document</li>
     * </ul>
     *
     * <p><strong>Example:</strong> A 10-page PDF with split page number 6 will produce:</p>
     * <ul>
     *   <li><strong>First Segment:</strong> Pages 1-5</li>
     *   <li><strong>Second Segment:</strong> Pages 6-10</li>
     * </ul>
     *
     * <p><strong>Output Format:</strong> Returns two separate file variables (not a list) for easier use in subsequent steps in Krista Studio.</p>
     *
     * @param inputFile       The PDF file to split. Must be a valid PDF document.
     * @param splitPageNumber The page number where the split should occur (1-based index).
     *                        This page will become the first page of the second file.
     *                        Must be between 2 and the total number of pages in the PDF.
     * @param allowRetry      If true, enables validation and retry flow; if false, performs direct split
     * @return An ExtensionResponse containing two separate PDF file variables if successful,
     * or error details if the split fails
     */
    @CatalogRequest(
            id = "localDomainRequest_b8c9d0e1-f2a3-4b5c-6d7e-8f9a0b1c2d3e",
            name = "Split PDF Based on Condition",
            description = "Split a PDF file at a specific page number into two separate files. Returns two file variables (First Segment and Second Segment) instead of a list for easier use in Studio.",
            area = "PDF",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.File(name = "First Segment", required = false,
            attributes = {@Attribute(name = "toolTip", value = "'PDF file containing pages from the beginning up to (but not including) the split page'")},
            options = {})
    @Field.File(name = "Second Segment", required = false,
            attributes = {@Attribute(name = "toolTip", value = "'PDF file containing pages from the split page to the end of the document'")},
            options = {})
    public ExtensionResponse splitPDFAtPageNumber(
            @Field.File(name = "Input File", multipleFileUpload = false, required = true,
                    attributes = {
                            @Attribute(name = "visualWidth", value = "S"),
                            @Attribute(name = "toolTip", value = "'Upload the PDF file to split at a specific page number'")
                    }, options = {}) File inputFile,
            @Field(name = "Split at Page Number", type = "Number", required = true,
                    attributes = {
                            @Attribute(name = "visualWidth", value = "S"),
                            @Attribute(name = "toolTip", value = "'Page number where split should occur (this page will be the first page of the second file). Must be between 2 and total pages.'")
                    }, options = {}) Double splitPageNumber,
            @Field.Boolean(name = Constants.ENABLE_ERROR_HANDLING, required = false) Boolean allowRetry) {

        logger.info("Starting PDF split based on condition operation for file: {}, split at page: {}",
                inputFile != null ? inputFile.getFileName() : "null", splitPageNumber);

        // Delegate all logic to the helper class
        return splitPdfAtPageNumberHelper.splitPDFAtPageNumber(inputFile, splitPageNumber, allowRetry);
    }
}
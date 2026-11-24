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

package app.krista.extensions.essentials.documents.pdf.helper;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.documents.pdf.service.PDF2DOMConverter;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for converting PDF files to HTML-tagged content using PDF2DOM.
 */
@Service
public class ConvertPdfToHtmlHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConvertPdfToHtmlHelper.class);
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final PDF2DOMConverter pdf2DOMConverter;

    @Inject
    public ConvertPdfToHtmlHelper(KristaMediaClientUtil kristaMediaClientUtil,
                                  TelemetryHelper telemetryHelper) {
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.telemetryHelper = telemetryHelper;
        this.pdf2DOMConverter = new PDF2DOMConverter();
    }

    /**
     * Converts a PDF file to HTML-tagged content with error handling and retry logic.
     *
     * <p>This method extracts text from the PDF and wraps it in appropriate HTML tags
     * to preserve document structure including headings, paragraphs, lists, and formatting.</p>
     *
     * @param pdfFile           The PDF file to convert to HTML
     * @param includeFormatting Whether to include formatting tags (bold, italic, etc.)
     * @param saveAsTextFile    True: save as .txt file, False: save as .html file
     * @param allowRetry        Whether to enable error handling and retry logic
     * @return ExtensionResponse containing the HTML file
     */
    public ExtensionResponse convertPdfToHtml(File pdfFile, Boolean includeFormatting, Boolean saveAsTextFile, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("Starting PDF to HTML conversion for file: {}", pdfFile != null ? pdfFile.getFileName() : "null");
            telemetryHelper.incrementCount("pdf.convertPdfToHtml");
            validatePdfFile(pdfFile);
            java.io.File pdfJavaFile = downloadPdfFile(pdfFile);
            boolean includeFormat = Boolean.TRUE.equals(includeFormatting);
            String htmlContent = convertToHtmlContent(pdfJavaFile, includeFormat);
            boolean saveAsTxt = Boolean.TRUE.equals(saveAsTextFile);
            File htmlFile = saveAndUploadHtmlFile(htmlContent, pdfFile.getFileName(), saveAsTxt);
            recordSuccessTelemetry(startTime, pdfFile, includeFormat, saveAsTxt, htmlContent, htmlFile);
            return createSuccessResponse(htmlFile);
        } catch (IllegalArgumentException cause) {
            return handleValidationError(cause, startTime, pdfFile);
        } catch (Exception cause) {
            return handleUnexpectedError(cause, startTime, pdfFile);
        }
    }

    /**
     * Validates that the PDF file is not null.
     *
     * @param pdfFile The PDF file to validate
     * @throws IllegalArgumentException if the PDF file is null
     */
    private void validatePdfFile(File pdfFile) {
        if (pdfFile == null) {
            logger.error("PDF file is null");
            throw new IllegalArgumentException("PDF file is required");
        }
    }

    /**
     * Downloads the PDF file for processing.
     *
     * @param pdfFile The Krista PDF file to download
     * @return The downloaded Java file
     */
    private java.io.File downloadPdfFile(File pdfFile) {
        java.io.File pdfJavaFile = kristaMediaClientUtil.downloadFileHandler(pdfFile);
        logger.debug("Downloaded PDF file: {}", pdfJavaFile.getAbsolutePath());
        return pdfJavaFile;
    }

    /**
     * Converts PDF file to HTML content using PDF2DOM converter.
     *
     * @param pdfJavaFile   The Java file to convert
     * @param includeFormat Whether to include formatting
     * @return The HTML content as a string
     * @throws Exception if conversion fails
     */
    private String convertToHtmlContent(java.io.File pdfJavaFile, boolean includeFormat) throws Exception {
        String htmlContent = pdf2DOMConverter.convertPdfToHtml(pdfJavaFile, includeFormat);
        logger.info("Successfully converted PDF to HTML. Content length: {} characters",
                htmlContent != null ? htmlContent.length() : 0);
        return htmlContent;
    }

    /**
     * Saves HTML content to a temporary file and uploads it.
     *
     * @param htmlContent      The HTML content to save
     * @param originalFileName The original PDF file name
     * @param saveAsTxt        Whether to save as .txt file (true) or .html file (false)
     * @return The uploaded Krista file
     * @throws Exception if file operations fail
     */
    private File saveAndUploadHtmlFile(String htmlContent, String originalFileName, boolean saveAsTxt) throws Exception {
        String outputFileName = generateOutputFileName(originalFileName, saveAsTxt);
        java.io.File htmlTempFile = createTemporaryHtmlFile(htmlContent, outputFileName);
        try {
            File htmlFile = kristaMediaClientUtil.uploadFileHandler(htmlTempFile);
            logger.info("HTML file uploaded successfully: {} (type: {})",
                    htmlFile.getFileName(), saveAsTxt ? "text" : "html");
            return htmlFile;
        } finally {
            cleanupTemporaryFile(htmlTempFile);
        }
    }

    /**
     * Generates the output file name based on the original file name and save type.
     *
     * @param originalFileName The original PDF file name
     * @param saveAsTxt        Whether to save as .txt file
     * @return The generated output file name
     */
    private String generateOutputFileName(String originalFileName, boolean saveAsTxt) {
        if (saveAsTxt) {
            // Save as .txt file (HTML source code)
            return originalFileName.replaceFirst("\\.[pP][dD][fF]$", "_html_source.txt");
        } else {
            // Save as .html file (renderable)
            return originalFileName.replaceFirst("\\.[pP][dD][fF]$", ".html");
        }
    }

    /**
     * Creates a temporary HTML file with the given content.
     *
     * @param htmlContent    The HTML content to write
     * @param outputFileName The output file name
     * @return The created temporary file
     * @throws Exception if file creation fails
     */
    private java.io.File createTemporaryHtmlFile(String htmlContent, String outputFileName) throws Exception {
        java.io.File htmlTempFile = new java.io.File(System.getProperty("java.io.tmpdir"), "html_" + System.currentTimeMillis() + "_" + outputFileName);
        try (java.io.FileWriter writer = new java.io.FileWriter(htmlTempFile)) {
            writer.write(htmlContent);
        }
        logger.debug("HTML content saved to temporary file: {}", htmlTempFile.getAbsolutePath());
        return htmlTempFile;
    }

    /**
     * Cleans up the temporary file.
     *
     * @param tempFile The temporary file to delete
     */
    private void cleanupTemporaryFile(java.io.File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            boolean deleted = tempFile.delete();
            if (!deleted) {
                logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    /**
     * Records success telemetry for the conversion operation.
     *
     * @param startTime     The operation start time
     * @param pdfFile       The original PDF file
     * @param includeFormat Whether formatting was included
     * @param saveAsTxt     Whether saved as text file
     * @param htmlContent   The generated HTML content
     * @param htmlFile      The uploaded HTML file
     */
    private void recordSuccessTelemetry(long startTime, File pdfFile, boolean includeFormat,
                                        boolean saveAsTxt, String htmlContent, File htmlFile) {
        telemetryHelper.recordSuccess("pdf.convertPdfToHtml", startTime, TelemetryHelper.safeTagMap(
                "file_name", pdfFile.getFileName(),
                "include_formatting", String.valueOf(includeFormat),
                "save_as_text", String.valueOf(saveAsTxt),
                "content_length", String.valueOf(htmlContent != null ? htmlContent.length() : 0),
                "output_file", htmlFile.getFileName()));
    }

    /**
     * Creates a success response with the HTML file.
     *
     * @param htmlFile The uploaded HTML file
     * @return The extension response
     */
    private ExtensionResponse createSuccessResponse(File htmlFile) {
        Map<String, Object> responseValue = new HashMap<>();
        responseValue.put("Output File", htmlFile);
        return new ExtensionResponseBuilder().success(responseValue).build();
    }

    /**
     * Handles validation errors during PDF to HTML conversion.
     *
     * @param cause     The validation exception
     * @param startTime The operation start time
     * @param pdfFile   The PDF file being processed
     * @return The error response
     */
    private ExtensionResponse handleValidationError(IllegalArgumentException cause, long startTime, File pdfFile) {
        logger.error("Validation error during PDF to HTML conversion: {}", cause.getMessage(), cause);
        telemetryHelper.recordError("pdf.convertPdfToHtml", startTime, cause,
                TelemetryHelper.safeTagMap("file_name", pdfFile != null ? pdfFile.getFileName() : "N/A"));

        ExtensionResponse.Error error = new ExtensionResponse.Error(
                cause.getMessage(),
                System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                Arrays.toString(cause.getStackTrace()));
        return new ExtensionResponseBuilder().failure(error).build();
    }

    /**
     * Handles unexpected errors during PDF to HTML conversion.
     *
     * @param cause     The exception that occurred
     * @param startTime The operation start time
     * @param pdfFile   The PDF file being processed
     * @return The error response
     */
    private ExtensionResponse handleUnexpectedError(Exception cause, long startTime, File pdfFile) {
        logger.error("Unexpected error during PDF to HTML conversion: {}", cause.getMessage(), cause);
        telemetryHelper.recordError("pdf.convertPdfToHtml", startTime, cause,
                TelemetryHelper.safeTagMap("file_name", pdfFile != null ? pdfFile.getFileName() : "N/A"));

        ExtensionResponse.Error error = new ExtensionResponse.Error("Failed to convert PDF to HTML: " + cause.getMessage(),
                System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                Arrays.toString(cause.getStackTrace()));

        return new ExtensionResponseBuilder().failure(error).build();
    }
}
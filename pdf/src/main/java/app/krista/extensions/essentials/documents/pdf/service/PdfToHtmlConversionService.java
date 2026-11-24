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

package app.krista.extensions.essentials.documents.pdf.service;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.documents.pdf.catalog.generartors.ExtensionResponseGenerator;
import app.krista.extensions.essentials.documents.pdf.catalog.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.documents.pdf.catalog.subcatalogconstants.SubCatalogConstants;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.documents.pdf.catalog.validators.Validator;
import app.krista.extensions.essentials.documents.pdf.helper.TelemetryHelper;
import app.krista.extensions.essentials.documents.pdf.service.converter.HtmlOutputStrategy;
import app.krista.extensions.essentials.documents.pdf.service.converter.TextHtmlOutputStrategy;
import app.krista.extensions.essentials.documents.pdf.service.converter.ZipHtmlOutputStrategy;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.extensions.essentials.documents.pdf.util.PdfToHtmlEnvironmentManager;
import app.krista.model.base.File;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Service for converting PDF files to HTML using Script (PyMuPDF/fitz).
 *
 * <p>This service orchestrates the complete PDF to HTML conversion workflow using
 * the Template Method design pattern. It leverages PyMuPDF (fitz) library for
 * high-fidelity conversion with exact text positioning, fonts, and embedded images.</p>
 *
 * <p><b>Design Patterns Used:</b></p>
 * <ul>
 *   <li><b>Template Method:</b> Defines the conversion workflow skeleton in {@link #convertPdfToHtml}</li>
 *   <li><b>Strategy:</b> Uses {@link HtmlOutputStrategy} for flexible output packaging (ZIP/TXT)</li>
 *   <li><b>Dependency Injection:</b> All dependencies injected via HK2 framework</li>
 * </ul>
 *
 * <p><b>Conversion Workflow (Template Method):</b></p>
 * <ol>
 *   <li>Validate PDF file format</li>
 *   <li>Download PDF from Krista Media</li>
 *   <li>Execute Script conversion</li>
 *   <li>Package HTML output (ZIP or TXT)</li>
 *   <li>Upload result to Krista Media</li>
 *   <li>Record telemetry and cleanup</li>
 * </ol>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>High-fidelity conversion preserving exact text positioning</li>
 *   <li>Embedded images as base64 data URIs</li>
 *   <li>Font preservation and styling</li>
 *   <li>SVG graphics support</li>
 *   <li>Security-compliant ZIP packaging for HTML output</li>
 *   <li>Validation with retry flow for error recovery</li>
 * </ul>
 *
 * @see HtmlOutputStrategy
 * @see ZipHtmlOutputStrategy
 * @see TextHtmlOutputStrategy
 */
@Service
public class PdfToHtmlConversionService {

    private static final Logger logger = LoggerFactory.getLogger(PdfToHtmlConversionService.class);
    private static final String PYTHON_SCRIPT_RESOURCE = "/convert_pdf_to_html.py";
    private static final String TELEMETRY_METRIC = "pdf.convertPdfToHtml";
    private static final String TEMP_DIR_NAME = "alliance_hcm_scripts";

    private volatile Path extractedScriptPath; // Cached extracted script path (volatile for thread safety)
    private final ObjectMapper objectMapper = new ObjectMapper(); // For parsing JSON metrics

    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final ZipHtmlOutputStrategy zipOutputStrategy;
    private final TextHtmlOutputStrategy textOutputStrategy;

    @Inject
    public PdfToHtmlConversionService(
            KristaMediaClientUtil kristaMediaClientUtil,
            TelemetryHelper telemetryHelper,
            ValidationOrchestrator validationOrchestrator,
            ErrorHandlingStateManager internalStateManager,
            ExtensionResponseGenerator responseGenerator,
            ZipHtmlOutputStrategy zipOutputStrategy,
            TextHtmlOutputStrategy textOutputStrategy) {
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.telemetryHelper = telemetryHelper;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        this.zipOutputStrategy = zipOutputStrategy;
        this.textOutputStrategy = textOutputStrategy;
    }

    /**
     * Converts a PDF file to HTML using Script with PyMuPDF.
     *
     * <p>This is the main template method that orchestrates the entire conversion workflow.
     * It follows the Template Method design pattern, defining the algorithm structure while
     * delegating specific steps to helper methods.</p>
     *
     * @param pdfFile        The PDF file to convert
     * @param saveAsTextFile True: save as .txt file, False: save as .zip file containing .html
     * @param allowRetry     Whether to enable error handling and retry logic
     * @return ExtensionResponse containing the HTML file (as ZIP or TXT)
     */
    public ExtensionResponse convertPdfToHtml(File pdfFile, Boolean saveAsTextFile, Boolean allowRetry) {
        return convertPdfToHtml(pdfFile, saveAsTextFile, allowRetry, false);
    }

    /**
     * Converts a PDF file to HTML using Script with PyMuPDF with optional interactive form support.
     *
     * <p>This is the main template method that orchestrates the entire conversion workflow.
     * It follows the Template Method design pattern, defining the algorithm structure while
     * delegating specific steps to helper methods.</p>
     *
     * @param pdfFile        The PDF file to convert
     * @param saveAsTextFile True: save as .txt file, False: save as .zip file containing .html
     * @param allowRetry     Whether to enable error handling and retry logic
     * @param interactive    True: preserve PDF form fields as interactive HTML inputs, False: render as static text
     * @return ExtensionResponse containing the HTML file (as ZIP or TXT)
     */
    public ExtensionResponse convertPdfToHtml(File pdfFile, Boolean saveAsTextFile, Boolean allowRetry, Boolean interactive) {
        long startTime = System.currentTimeMillis();

        try {
            logger.info("PdfToHtmlConversionService.convertPdfToHtml() called with file={}, saveAsTextFile={}, allowRetry={}, interactive={}",
                    pdfFile != null ? pdfFile.getFileName() : "null", saveAsTextFile, allowRetry, interactive);
            telemetryHelper.incrementCount(TELEMETRY_METRIC);

            // Step 1: Validate PDF file format
            List<ValidationOrchestrator.ValidationResult> validationResults = validatePdfFormat(pdfFile);
            if (!validationResults.isEmpty()) {
                return handleValidationFailure(validationResults, pdfFile, saveAsTextFile, allowRetry);
            }

            // Step 2: Download PDF file
            java.io.File pdfJavaFile = downloadPdfFile(pdfFile);

            // Step 3: Execute Script conversion with interactive flag
            java.io.File htmlFile = executeScriptConversion(pdfJavaFile, interactive);

            // Step 3.5: Read and log conversion metrics from JSON file
            logConversionMetrics(htmlFile);

            // Step 4: Package and upload result (Strategy Pattern)
            HtmlOutputStrategy strategy = selectOutputStrategy(saveAsTextFile);
            File outputFile = strategy.packageAndUpload(htmlFile, pdfFile.getFileName());

            // Step 5: Record success telemetry
            recordSuccessTelemetry(startTime, pdfFile, saveAsTextFile, outputFile);

            // Step 6: Clean up temporary files (including metrics JSON file)
            java.io.File metricsFile = getMetricsFile(htmlFile);
            cleanupTempFiles(pdfJavaFile, htmlFile, metricsFile);

            return createSuccessResponse(outputFile);
        } catch (Exception e) {
            return handleUnexpectedError(e, startTime, pdfFile);
        }
    }

    /**
     * Validates the PDF file format.
     *
     * @param pdfFile The PDF file to validate
     * @return List of validation errors (empty if valid)
     */
    private List<ValidationOrchestrator.ValidationResult> validatePdfFormat(File pdfFile) {
        logger.debug("Validating PDF format: {}", pdfFile != null ? pdfFile.getFileName() : "null");

        Map<Validator.ValidationResource, String> context = new HashMap<>();
        context.put(Validator.ValidationResource.PDF_TO_HTML_FILE_FORMAT,
                pdfFile != null ? pdfFile.getFileName() : "");

        return validationOrchestrator.validate(context);
    }

    /**
     * Downloads the PDF file from Krista Media.
     *
     * @param pdfFile The Krista File reference
     * @return Downloaded Java File
     */
    private java.io.File downloadPdfFile(File pdfFile) {
        logger.info("Downloading PDF file: {}", pdfFile.getFileName());
        java.io.File pdfJavaFile = kristaMediaClientUtil.downloadFileHandler(pdfFile);
        logger.debug("Downloaded PDF file to: {}", pdfJavaFile.getAbsolutePath());
        return pdfJavaFile;
    }

    /**
     * Executes the Script to convert PDF to HTML.
     *
     * @param pdfFile The PDF file to convert
     * @return The generated HTML file
     * @throws IOException If file I/O fails
     * @throws InterruptedException If process execution is interrupted
     */
    /**
     * Executes Script-based PDF to HTML conversion.
     * This method orchestrates the conversion process by:
     * 1. Getting and validating the Script executable
     * 2. Extracting the script from JAR
     * 3. Running the conversion process
     * 4. Verifying the output
     *
     * @param pdfFile     The PDF file to convert
     * @param interactive True: preserve PDF form fields as interactive HTML inputs, False: render as static text
     * @return The generated HTML file
     * @throws IOException          If conversion fails or output file is not created
     * @throws InterruptedException If the process is interrupted
     */
    private java.io.File executeScriptConversion(java.io.File pdfFile, Boolean interactive) throws IOException, InterruptedException {
        logger.info("Executing Script PDF to HTML conversion for: {} (interactive={})", pdfFile.getName(), interactive);

        String scriptExecutable = getScriptExecutable();
        Path scriptPath = getOrExtractScript();
        java.io.File htmlFile = runScriptConversionProcess(scriptExecutable, scriptPath, pdfFile, interactive);

        logger.info("Conversion completed successfully");
        return htmlFile;
    }

    /**
     * Gets and validates the Python executable path.
     *
     * @return The validated Python executable path
     * @throws IOException If Python executable is not found or invalid
     */
    private String getScriptExecutable() throws IOException {
        String scriptExecutable = PdfToHtmlEnvironmentManager.getScriptPath();
        validateScriptExecutable(scriptExecutable);
        return scriptExecutable;
    }

    /**
     * Gets the Script path, extracting it from JAR if necessary.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return The path to the Script
     * @throws IOException If script extraction fails
     */
    private Path getOrExtractScript() throws IOException {
        if (extractedScriptPath == null || !Files.exists(extractedScriptPath)) {
            synchronized (this) {
                // Double-check inside synchronized block
                if (extractedScriptPath == null || !Files.exists(extractedScriptPath)) {
                    extractedScriptPath = extractScriptFromJar();
                    logger.info("Extracted Script to: {}", extractedScriptPath);
                }
            }
        }
        return extractedScriptPath;
    }

    /**
     * Runs the script conversion process and verifies the output.
     *
     * @param scriptExecutable The script executable path
     * @param scriptPath       The script path
     * @param pdfFile          The PDF file to convert
     * @param interactive      True: preserve PDF form fields as interactive HTML inputs, False: render as static text
     * @return The generated HTML file
     * @throws IOException          If the process fails or output is not created
     * @throws InterruptedException If the process is interrupted
     */
    private java.io.File runScriptConversionProcess(String scriptExecutable, Path scriptPath, java.io.File pdfFile, Boolean interactive)
            throws IOException, InterruptedException {
        String[] command = buildConversionCommand(scriptExecutable, scriptPath, pdfFile, interactive);
        logger.info("Executing command: {}", String.join(" ", command));

        Process process = startConversionProcess(command);
        int exitCode = waitForProcessCompletion(process);

        return verifyHtmlOutput(pdfFile);
    }

    /**
     * Builds the command array for Script conversion.
     *
     * @param scriptExecutable The Script executable path
     * @param scriptPath       The Script path
     * @param pdfFile          The PDF file to convert
     * @param interactive      True: preserve PDF form fields as interactive HTML inputs, False: render as static text
     * @return The command array
     */
    private String[] buildConversionCommand(String scriptExecutable, Path scriptPath, java.io.File pdfFile, Boolean interactive) {
        if (Boolean.TRUE.equals(interactive)) {
            return new String[]{
                    scriptExecutable,
                    scriptPath.toString(),
                    pdfFile.getAbsolutePath(),
                    "--interactive"
            };
        } else {
            return new String[]{
                    scriptExecutable,
                    scriptPath.toString(),
                    pdfFile.getAbsolutePath()
            };
        }
    }

    /**
     * Starts the conversion process.
     *
     * @param command The command to execute
     * @return The started process
     * @throws IOException If process cannot be started
     */
    private Process startConversionProcess(String[] command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        return processBuilder.start();
    }

    /**
     * Waits for process completion and handles errors.
     *
     * @param process The process to wait for
     * @return The exit code
     * @throws IOException          If the process fails
     * @throws InterruptedException If waiting is interrupted
     */
    private int waitForProcessCompletion(Process process) throws IOException, InterruptedException {
        String output = captureProcessOutput(process);
        String errors = captureProcessErrors(process);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            logger.error("Conversion failed with exit code: {}. Errors: {}", exitCode, errors);
            throw new IOException("conversion failed (exit code " + exitCode + "): " + errors);
        }

        return exitCode;
    }

    /**
     * Verifies that the HTML output file was created.
     *
     * @param pdfFile The original PDF file
     * @return The generated HTML file
     * @throws IOException If HTML file was not created
     */
    private java.io.File verifyHtmlOutput(java.io.File pdfFile) throws IOException {
        String htmlPath = pdfFile.getAbsolutePath().replaceFirst("(?i)\\.pdf$", ".html");
        java.io.File htmlFile = new java.io.File(htmlPath);

        if (!htmlFile.exists()) {
            throw new IOException("conversion completed but HTML file was not created: " + htmlPath);
        }

        return htmlFile;
    }

    /**
     * Extracts Script from JAR resources to a temporary file.
     *
     * @return Path to the extracted script
     * @throws IOException If extraction fails
     */
    private Path extractScriptFromJar() throws IOException {
        logger.info("Extracting Script from JAR resource: {}", PYTHON_SCRIPT_RESOURCE);

        try (InputStream scriptStream = getClass().getResourceAsStream(PYTHON_SCRIPT_RESOURCE)) {
            if (scriptStream == null) {
                throw new IOException("Script resource not found in JAR: " + PYTHON_SCRIPT_RESOURCE);
            }

            // Create temp directory
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), TEMP_DIR_NAME);

            // Validate temp directory path (security check)
            Path normalizedTempDir = tempDir.normalize();
            Path systemTempDir = Paths.get(System.getProperty("java.io.tmpdir")).normalize();
            if (!normalizedTempDir.startsWith(systemTempDir)) {
                throw new SecurityException("Invalid temp directory path: " + tempDir);
            }

            Files.createDirectories(tempDir);

            // Extract script
            Path scriptPath = tempDir.resolve("convert_pdf_to_html.py");
            Files.copy(scriptStream, scriptPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Script extracted successfully to: {}", scriptPath);
            return scriptPath;
        }
    }

    /**
     * Validates the Script executable path for security.
     *
     * @param scriptExecutable The Script executable path to validate
     * @throws IOException If validation fails
     */
    private void validateScriptExecutable(String scriptExecutable) throws IOException {
        if (scriptExecutable == null || scriptExecutable.trim().isEmpty()) {
            throw new IOException("Script executable path is null or empty. Script environment may not be initialized.");
        }

        // Validate it's an absolute path or a command name (not a relative path)
        Path scriptPath = Paths.get(scriptExecutable);
        if (!scriptPath.isAbsolute() && scriptExecutable.contains("/")) {
            throw new SecurityException("Script executable must be an absolute path or command name, not a relative path: " + scriptExecutable);
        }

        // If it's an absolute path, verify the file exists
        if (scriptPath.isAbsolute() && !Files.exists(scriptPath)) {
            throw new IOException("Script executable not found at path: " + scriptExecutable);
        }

        logger.debug("Script executable validated: {}", scriptExecutable);
    }

    /**
     * Captures standard output from the Script process.
     */
    private String captureProcessOutput(Process process) throws IOException {
        return captureStream(process.getInputStream());
    }

    /**
     * Captures error output from the Script process.
     */
    private String captureProcessErrors(Process process) throws IOException {
        return captureStream(process.getErrorStream());
    }

    /**
     * Captures output from an input stream.
     * Common utility method to avoid code duplication between stdout and stderr capture.
     *
     * @param stream The input stream to read from
     * @return The captured output as a string
     * @throws IOException If an error occurs while reading the stream
     */
    private String captureStream(InputStream stream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    /**
     * Selects the appropriate output strategy based on user preference.
     * This implements the Strategy Pattern for flexible output formatting.
     *
     * @param saveAsTextFile True for TXT output, False for ZIP output
     * @return The selected output strategy
     */
    private HtmlOutputStrategy selectOutputStrategy(Boolean saveAsTextFile) {
        boolean useTxtFormat = Boolean.TRUE.equals(saveAsTextFile);
        HtmlOutputStrategy strategy = useTxtFormat ? textOutputStrategy : zipOutputStrategy;
        logger.debug("Selected output strategy: {} ({})",
                strategy.getClass().getSimpleName(), strategy.getDescription());
        return strategy;
    }

    /**
     * Records success telemetry metrics.
     */
    private void recordSuccessTelemetry(long startTime, File pdfFile, Boolean saveAsTextFile, File outputFile) {
        telemetryHelper.recordSuccess(TELEMETRY_METRIC, startTime,
                TelemetryHelper.safeTagMap(
                        "file_name", pdfFile.getFileName(),
                        "output_format", Boolean.TRUE.equals(saveAsTextFile) ? "txt" : "zip",
                        "output_file", outputFile.getFileName()
                ));
    }

    /**
     * Cleans up temporary files after processing.
     */
    private void cleanupTempFiles(java.io.File... files) {
        for (java.io.File file : files) {
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    logger.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
                } else {
                    logger.debug("Deleted temporary file: {}", file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Creates a success response with the output file.
     */
    private ExtensionResponse createSuccessResponse(File outputFile) {
        Map<String, Object> data = new HashMap<>();
        data.put("Output File", outputFile);
        return new ExtensionResponseBuilder().success(data).build();
    }

    /**
     * Handles validation failure by either triggering retry flow or returning direct error.
     *
     * @param validationResults List of validation errors
     * @param pdfFile           The PDF file that failed validation
     * @param saveAsTextFile    Whether to save as text file
     * @param allowRetry        Whether to allow retry
     * @return ExtensionResponse with either confirmation response or error response
     */
    private ExtensionResponse handleValidationFailure(
            List<ValidationOrchestrator.ValidationResult> validationResults,
            File pdfFile, Boolean saveAsTextFile, Boolean allowRetry) {

        logger.warn("Validation failed with {} errors, allowRetry={}", validationResults.size(), allowRetry);

        if (Boolean.TRUE.equals(allowRetry)) {
            logger.debug("Retry enabled, generating confirmation response for retry flow");

            // Generate unique state ID
            String stateId = UUID.randomUUID().toString();
            logger.debug("Storing validation state with ID: {}", stateId);

            // Store validation results
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(SubCatalogConstants.VALIDATION_RESULTS, validationResults);
            internalStateManager.put(stateId, stateMap);

            // Create state map with parameters
            Map<String, Object> state = new HashMap<>();
            state.put(SubCatalogConstants.STATE_ID, stateId);
            if (pdfFile != null) {
                state.put(SubCatalogConstants.PDF_TO_HTML_PYTHON_FILE, List.of(pdfFile));
            }
            if (saveAsTextFile != null) {
                state.put(SubCatalogConstants.SAVE_AS_TEXT_FILE, saveAsTextFile);
            }

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_PDF_TO_HTML_PYTHON_VALIDATION,
                    state
            );
        } else {
            logger.debug("Retry disabled, returning direct validation error");
            return responseGenerator.generateValidationErrorResponse(validationResults);
        }
    }

    /**
     * Handles unexpected errors during conversion.
     */
    private ExtensionResponse handleUnexpectedError(Exception e, long startTime, File pdfFile) {
        logger.error("Unexpected error during PDF to HTML conversion", e);

        telemetryHelper.recordError(TELEMETRY_METRIC, startTime, e,
                TelemetryHelper.safeTagMap(
                        "file_name", pdfFile != null ? pdfFile.getFileName() : "N/A"
                ));

        ExtensionResponse.Error error = new ExtensionResponse.Error(
                "Failed to convert PDF to HTML: " + e.getMessage(),
                System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                Arrays.toString(e.getStackTrace()));

        return new ExtensionResponseBuilder().failure(error).build();
    }

    /**
     * Reads and logs the conversion metrics from the JSON file created by the Script.
     * The Script creates a {filename}_metrics.json file with accuracy and statistics.
     *
     * @param htmlFile The HTML file created by the Script
     */
    private void logConversionMetrics(java.io.File htmlFile) {
        try {
            java.io.File metricsFile = getMetricsFile(htmlFile);

            if (!metricsFile.exists()) {
                logger.info("Metrics file not found: {}", metricsFile.getAbsolutePath());
                return;
            }

            // Read and parse JSON metrics
            String metricsJson = Files.readString(metricsFile.toPath());
            Map<String, Object> metrics = objectMapper.readValue(metricsJson, Map.class);

            // Log metrics in JSON format
            logger.info("PDF to HTML Conversion Metrics: {}", objectMapper.writeValueAsString(metrics));

            // Also log individual metrics for easier searching
            logger.info("Conversion Accuracy - Overall: {}%, Character: {}%, Word: {}%, Pages: {}, Text Blocks: {}, Image Blocks: {}",
                    metrics.get("overall_accuracy"),
                    metrics.get("character_accuracy"),
                    metrics.get("word_accuracy"),
                    metrics.get("total_pages"),
                    metrics.get("text_blocks"),
                    metrics.get("image_blocks"));

        } catch (Exception e) {
            logger.warn("Failed to read or parse conversion metrics: {}", e.getMessage());
        }
    }

    /**
     * Gets the metrics JSON file path for a given HTML file.
     * The Script creates metrics files with pattern: {filename}_metrics.json
     *
     * @param htmlFile The HTML file
     * @return The corresponding metrics JSON file
     */
    private java.io.File getMetricsFile(java.io.File htmlFile) {
        String metricsPath = htmlFile.getAbsolutePath().replace(".html", "_metrics.json");
        return new java.io.File(metricsPath);
    }
}

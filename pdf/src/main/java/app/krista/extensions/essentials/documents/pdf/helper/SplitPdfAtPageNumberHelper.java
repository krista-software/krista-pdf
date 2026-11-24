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
import app.krista.extensions.essentials.documents.pdf.helper.errorhandler.PdfSplitErrorHandler;
import app.krista.extensions.essentials.documents.pdf.helper.factory.ValidationStrategyFactory;
import app.krista.extensions.essentials.documents.pdf.helper.processor.PdfSplitProcessor;
import app.krista.extensions.essentials.documents.pdf.helper.strategy.ValidationStrategy;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * Helper class for splitting PDF files at a specific page number with validation support.
 *
 * <p>This helper provides validation and retry logic for the "Split PDF Based on Condition" catalog request.
 * It validates the PDF file format and page number before performing the split operation.</p>
 *
 * <p><b>Design Patterns Applied:</b></p>
 * <ul>
 *   <li><b>Strategy Pattern:</b> Uses ValidationStrategy for different validation approaches</li>
 *   <li><b>Factory Pattern:</b> Uses ValidationStrategyFactory to create strategies</li>
 *   <li><b>Template Method Pattern:</b> Defines workflow template in splitPDFAtPageNumber()</li>
 *   <li><b>Facade Pattern:</b> Provides simplified interface to complex subsystems</li>
 * </ul>
 *
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Single Responsibility:</b> Orchestrates the split workflow, delegates specific tasks</li>
 *   <li><b>Open/Closed:</b> Open for extension via strategies, closed for modification</li>
 *   <li><b>Liskov Substitution:</b> ValidationStrategy implementations are interchangeable</li>
 *   <li><b>Interface Segregation:</b> Uses focused interfaces (ValidationStrategy, etc.)</li>
 *   <li><b>Dependency Inversion:</b> Depends on abstractions (strategies, processors)</li>
 * </ul>
 */
@Service
public class SplitPdfAtPageNumberHelper {

    private static final Logger logger = LoggerFactory.getLogger(SplitPdfAtPageNumberHelper.class);

    private final TelemetryHelper telemetryHelper;
    private final ValidationStrategyFactory validationStrategyFactory;
    private final PdfSplitProcessor processor;
    private final PdfSplitErrorHandler errorHandler;

    @Inject
    public SplitPdfAtPageNumberHelper(TelemetryHelper telemetryHelper,
                                      ValidationStrategyFactory validationStrategyFactory,
                                      PdfSplitProcessor processor,
                                      PdfSplitErrorHandler errorHandler) {
        this.telemetryHelper = telemetryHelper;
        this.validationStrategyFactory = validationStrategyFactory;
        this.processor = processor;
        this.errorHandler = errorHandler;

        logger.debug("SplitPdfAtPageNumberHelper initialized with SOLID principles and design patterns");
    }

    /**
     * Splits a PDF file at a specific page number with optional validation.
     *
     * <p>This method implements the Template Method Pattern, defining the workflow:</p>
     * <ol>
     *   <li>Select validation strategy based on allowRetry flag</li>
     *   <li>Validate input parameters</li>
     *   <li>Execute PDF split if validation passes</li>
     *   <li>Handle errors appropriately</li>
     * </ol>
     *
     * @param inputFile       The PDF file to split
     * @param splitPageNumber The page number where the split should occur (1-based index)
     * @param allowRetry      whether to allow retry on validation failure
     * @return ExtensionResponse containing the list of split PDF files or error details
     */
    public ExtensionResponse splitPDFAtPageNumber(File inputFile, Double splitPageNumber, Boolean allowRetry) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting PDF split operation: file={}, splitPage={}, allowRetry={}",
                inputFile != null ? inputFile.getFileName() : "null", splitPageNumber, allowRetry);
        telemetryHelper.incrementCount("pdf.splitPDFAtPageNumber");
        try {
            ValidationStrategy validationStrategy = validationStrategyFactory.createStrategy(allowRetry);
            logger.debug("Using validation strategy: {}", validationStrategy.getClass().getSimpleName());
            ValidationStrategy.ValidationResult validationResult =
                    validationStrategy.validate(inputFile, splitPageNumber);

            if (!validationResult.isValid()) {
                logger.warn("Validation failed: {}", validationResult.getErrorMessage());
                return validationStrategy.handleValidationFailure(validationResult, inputFile, splitPageNumber);
            }
            int pageNum = splitPageNumber.intValue();
            PdfSplitProcessor.ProcessResult processResult = processor.execute(inputFile, pageNum);

            if (!processResult.isSuccess()) {
                return errorHandler.handleException(processResult.getException(), inputFile, splitPageNumber, Boolean.TRUE.equals(allowRetry));
            }
            List<File> splitFiles = processResult.getSplitFiles();
            telemetryHelper.recordSuccess("pdf.splitPDFAtPageNumber", startTime, TelemetryHelper.safeTagMap(
                    "file_name", inputFile.getFileName(),
                    "split_count", String.valueOf(splitFiles.size()),
                    "split_page", String.valueOf(pageNum)
            ));
            logger.info("PDF split completed successfully: {} files created", splitFiles.size());
            return processor.buildSuccessResponse(splitFiles);
        } catch (Exception e) {
            // Handle unexpected errors
            logger.error("Unexpected error in PDF split workflow: {}", e.getMessage(), e);
            telemetryHelper.recordError("pdf.splitPDFAtPageNumber", startTime, e,
                    TelemetryHelper.safeTagMap(
                            "file_name", inputFile != null ? inputFile.getFileName() : "N/A",
                            "split_page", splitPageNumber != null ? String.valueOf(splitPageNumber.intValue()) : "N/A"
                    ));

            return errorHandler.handleException(e, inputFile, splitPageNumber, Boolean.TRUE.equals(allowRetry));
        }
    }
}

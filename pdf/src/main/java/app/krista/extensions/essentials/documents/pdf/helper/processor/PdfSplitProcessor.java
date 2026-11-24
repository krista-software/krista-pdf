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

package app.krista.extensions.essentials.documents.pdf.helper.processor;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processor responsible for executing PDF split operations.
 *
 * <p>This class follows the Single Responsibility Principle by focusing solely
 * on the PDF splitting logic, delegating validation and error handling to other components.</p>
 *
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Single Responsibility:</b> Only handles PDF splitting execution</li>
 *   <li><b>Dependency Inversion:</b> Depends on abstractions (PDFManager, KristaMediaClientUtil)</li>
 * </ul>
 */
@Service
public class PdfSplitProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PdfSplitProcessor.class);

    private final PDFManager pdfManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;

    @Inject
    public PdfSplitProcessor(PDFManager pdfManager, KristaMediaClientUtil kristaMediaClientUtil) {
        this.pdfManager = pdfManager;
        this.kristaMediaClientUtil = kristaMediaClientUtil;
    }

    /**
     * Executes the PDF split operation.
     *
     * @param inputFile  The PDF file to split
     * @param pageNumber The page number where split should occur
     * @return ProcessResult containing the split files or error information
     */
    public ProcessResult execute(File inputFile, int pageNumber) {
        try {
            logger.debug("Starting PDF split execution for file: {}, page: {}",
                    inputFile.getFileName(), pageNumber);

            // Download the PDF file
            java.io.File javaFile = kristaMediaClientUtil.downloadFileHandler(inputFile);

            // Split the PDF
            List<java.io.File> javaFiles = pdfManager.splitPDFAtPageNumber(javaFile, pageNumber);

            if (javaFiles == null || javaFiles.isEmpty()) {
                logger.warn("PDF split returned no files");
                return ProcessResult.success(new ArrayList<>());
            }

            logger.info("Successfully split PDF into {} files at page {}", javaFiles.size(), pageNumber);

            // Upload the split files
            List<File> kristaFiles = new ArrayList<>();
            for (java.io.File javaSplitFile : javaFiles) {
                File kristaFile = kristaMediaClientUtil.uploadFileHandler(javaSplitFile);
                kristaFiles.add(kristaFile);
            }

            logger.info("Successfully uploaded {} split files", kristaFiles.size());

            return ProcessResult.success(kristaFiles);

        } catch (IOException e) {
            logger.error("IOException during PDF split: {}", e.getMessage(), e);
            return ProcessResult.failure(e);
        } catch (Exception e) {
            logger.error("Unexpected error during PDF split: {}", e.getMessage(), e);
            return ProcessResult.failure(e);
        }
    }

    /**
     * Builds the success response from split files.
     * Returns two separate file variables instead of a list for easier use in Krista Studio.
     *
     * @param splitFiles The list of split PDF files (should always contain exactly 2 files)
     * @return ExtensionResponse with success result containing two separate file variables
     */
    public ExtensionResponse buildSuccessResponse(List<File> splitFiles) {
        if (splitFiles == null || splitFiles.size() != 2) {
            logger.warn("Expected exactly 2 split files, but got: {}",
                    splitFiles != null ? splitFiles.size() : "null");
            // Fallback to list format if not exactly 2 files
            return new ExtensionResponseBuilder()
                    .success(Map.of("Split Files", splitFiles != null ? splitFiles : new ArrayList<>()))
                    .build();
        }

        // Return two separate file variables for easier use in Studio
        return new ExtensionResponseBuilder()
                .success(Map.of(
                        "First Segment", splitFiles.get(0),
                        "Second Segment", splitFiles.get(1)
                ))
                .build();
    }

    /**
     * Result of PDF split processing.
     */
    public static class ProcessResult {
        private final boolean success;
        private final List<File> splitFiles;
        private final Exception exception;

        private ProcessResult(boolean success, List<File> splitFiles, Exception exception) {
            this.success = success;
            this.splitFiles = splitFiles;
            this.exception = exception;
        }

        public static ProcessResult success(List<File> splitFiles) {
            return new ProcessResult(true, splitFiles, null);
        }

        public static ProcessResult failure(Exception exception) {
            return new ProcessResult(false, null, exception);
        }

        public boolean isSuccess() {
            return success;
        }

        public List<File> getSplitFiles() {
            return splitFiles;
        }

        public Exception getException() {
            return exception;
        }
    }
}


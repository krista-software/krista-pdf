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

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.extensions.essentials.documents.pdf.util.PDFHelper;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Helper that renames a PDF file to a new, user-supplied name.
 *
 * <p>Validates the input (must be a PDF, name must be non-empty and contain valid
 * characters), builds a sanitized target file name that preserves the user's case and
 * spacing, then downloads the Krista file, copies its bytes under the new name, and
 * re-uploads it. Fail-fast: invalid input or any I/O error returns an {@link ExtensionResponse}
 * FAILURE.</p>
 */
@Service
public class RenamePdfHelper {

    private static final Logger logger = LoggerFactory.getLogger(RenamePdfHelper.class);
    private static final String OUTPUT_KEY = "Renamed PDF File";
    private static final String METRIC = "pdf.renamePdf";

    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;

    @Inject
    public RenamePdfHelper(KristaMediaClientUtil kristaMediaClientUtil, TelemetryHelper telemetryHelper) {
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.telemetryHelper = telemetryHelper;
    }

    /**
     * Renames the given PDF file to {@code newFileName} (sanitized, ending in ".pdf").
     *
     * @param pdfFile     the PDF file to rename
     * @param newFileName the new name for the file (any extension the user types is removed)
     * @return a SUCCESS response with the renamed file under key "Renamed PDF File",
     * or a FAILURE response describing the problem
     */
    public ExtensionResponse renamePdf(File pdfFile, String newFileName) {
        long startTime = System.currentTimeMillis();
        telemetryHelper.incrementCount(METRIC);

        // 1. Input must be a PDF (extension-based check, matching PdfFileFormatValidator).
        if (pdfFile == null || pdfFile.getFileName() == null
                || !pdfFile.getFileName().toLowerCase().endsWith(".pdf")) {
            logger.warn("Rename PDF failed: input is not a PDF file");
            return failure(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    "The provided file is not a PDF. Please upload a valid PDF file.");
        }

        // 2. New name must be non-empty.
        if (newFileName == null || newFileName.isBlank()) {
            logger.warn("Rename PDF failed: new name is blank");
            return failure(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    "Please provide a new name for the PDF file.");
        }

        // 3. Build the sanitized target name (case + spacing preserved).
        String base = PDFHelper.sanitizeFileName(newFileName);
        if (base.isEmpty()) {
            logger.warn("Rename PDF failed: name has no valid characters");
            return failure(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    "The provided name has no valid characters. "
                            + "Please use letters, numbers, spaces, '-' or '_'.");
        }
        String targetName = base + ".pdf";

        // 4-6. Download, copy under the new name in a fresh temp dir, re-upload.
        try {
            java.io.File downloaded = kristaMediaClientUtil.downloadFileHandler(pdfFile);
            Path tempDir = Files.createTempDirectory("rename-pdf-");
            java.io.File renamed = tempDir.resolve(targetName).toFile();
            Files.copy(downloaded.toPath(), renamed.toPath());

            File result = kristaMediaClientUtil.uploadFileHandler(renamed);

            telemetryHelper.recordSuccess(METRIC, startTime,
                    TelemetryHelper.safeTagMap("new_name", targetName));
            logger.info("Renamed PDF '{}' -> '{}'", pdfFile.getFileName(), targetName);

            return new ExtensionResponseBuilder()
                    .success(Map.of(OUTPUT_KEY, result))
                    .build();
        } catch (Exception e) {
            telemetryHelper.recordError(METRIC, startTime, e,
                    TelemetryHelper.safeTagMap("new_name", targetName));
            logger.error("Error renaming PDF file: {}", e.getMessage(), e);
            return failure(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR,
                    "Failed to rename PDF file: " + e.getMessage());
        }
    }

    private ExtensionResponse failure(ExtensionResponse.Error.ExceptionType type, String message) {
        return new ExtensionResponseBuilder()
                .failure(new ErrorBuilder()
                        .exceptionType(type)
                        .errorMessage(message)
                        .build())
                .build();
    }
}

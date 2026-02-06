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

package app.krista.extensions.essentials.documents.pdf.service.converter;

import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static app.krista.extensions.essentials.documents.pdf.service.converter.HtmlOutputConstants.TIMESTAMP_FORMATTER;

/**
 * Strategy for packaging HTML output as a plain text file.
 *
 * <p>This strategy saves the HTML content as a .txt file and uploads it to Krista Media.
 * Text file packaging allows users to directly upload and view the HTML source code
 * without needing to extract a ZIP archive.</p>
 *
 * <p><b>Use Case:</b> When users need to quickly view or copy the HTML source code
 * without the extra step of extracting a ZIP file.</p>
 */
@Service
public class TextHtmlOutputStrategy implements HtmlOutputStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TextHtmlOutputStrategy.class);

    private final KristaMediaClientUtil kristaMediaClientUtil;

    @Inject
    public TextHtmlOutputStrategy(KristaMediaClientUtil kristaMediaClientUtil) {
        this.kristaMediaClientUtil = kristaMediaClientUtil;
    }

    /**
     * Packages HTML content as a plain text file and uploads it to Krista Media.
     */
    @Override
    public File packageAndUpload(java.io.File htmlFile, String originalFileName) throws IOException {
        logger.info("Packaging HTML as TXT file for: {}", originalFileName);

        // Create TXT file with HTML content using interface helper methods
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String baseFileName = extractBaseFileName(originalFileName);
        String txtFileName = generateOutputFileName(baseFileName, timestamp, "txt");

        Path txtPath = Files.createTempFile("pdf-to-html-", ".txt");
        java.io.File txtFile = txtPath.toFile();

        // Copy HTML content to TXT file efficiently (handles large files)
        Files.copy(htmlFile.toPath(), txtPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        logger.debug("Created TXT file: {} with HTML content", txtFileName);

        // Upload to Krista Media
        logger.info("Uploading TXT file to Krista Media: {}", txtFileName);
        File uploadedFile = kristaMediaClientUtil.uploadFileHandler(txtFile);

        // Clean up temp TXT file
        if (!txtFile.delete()) {
            logger.warn("Failed to delete temporary TXT file: {}", txtFile.getAbsolutePath());
        }

        logger.info("Successfully packaged and uploaded TXT file: {}", uploadedFile.getFileName());
        return uploadedFile;
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }

    @Override
    public String getDescription() {
        return "Text file containing HTML source code (can upload directly)";
    }
}


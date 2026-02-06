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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static app.krista.extensions.essentials.documents.pdf.service.converter.HtmlOutputConstants.TIMESTAMP_FORMATTER;

/**
 * Strategy for packaging HTML output as a ZIP file.
 *
 * <p>This strategy creates a ZIP archive containing the HTML file and uploads it
 * to Krista Media. ZIP packaging is required for security compliance when the HTML
 * file needs to be uploaded to the system.</p>
 *
 * <p><b>Security Note:</b> Krista Media Client blocks direct HTML file uploads for
 * security reasons. ZIP packaging is the workaround to deliver HTML content safely.</p>
 */
@Service
public class ZipHtmlOutputStrategy implements HtmlOutputStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ZipHtmlOutputStrategy.class);

    private final KristaMediaClientUtil kristaMediaClientUtil;

    @Inject
    public ZipHtmlOutputStrategy(KristaMediaClientUtil kristaMediaClientUtil) {
        this.kristaMediaClientUtil = kristaMediaClientUtil;
    }

    /**
     * Packages HTML content as a ZIP archive and uploads it to Krista Media.
     */
    @Override
    public File packageAndUpload(java.io.File htmlFile, String originalFileName) throws IOException {
        logger.info("Packaging HTML as ZIP file for: {}", originalFileName);

        // Create ZIP file using interface helper methods
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String baseFileName = extractBaseFileName(originalFileName);
        String zipFileName = generateOutputFileName(baseFileName, timestamp, "zip");

        Path zipPath = Files.createTempFile("pdf-to-html-", ".zip");
        java.io.File zipFile = zipPath.toFile();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Add HTML file to ZIP
            String htmlFileName = baseFileName + ".html";
            ZipEntry zipEntry = new ZipEntry(htmlFileName);
            zos.putNextEntry(zipEntry);

            // Stream file content for better memory efficiency (handles large files)
            Files.copy(htmlFile.toPath(), zos);
            zos.closeEntry();

            logger.debug("Created ZIP file: {} containing {}", zipFileName, htmlFileName);
        }

        // Upload to Krista Media
        logger.info("Uploading ZIP file to Krista Media: {}", zipFileName);
        File uploadedFile = kristaMediaClientUtil.uploadFileHandler(zipFile);

        // Clean up temp ZIP file
        if (!zipFile.delete()) {
            logger.warn("Failed to delete temporary ZIP file: {}", zipFile.getAbsolutePath());
        }

        logger.info("Successfully packaged and uploaded ZIP file: {}", uploadedFile.getFileName());
        return uploadedFile;
    }

    @Override
    public String getFileExtension() {
        return "zip";
    }

    @Override
    public String getDescription() {
        return "ZIP archive containing HTML file (extract to view)";
    }
}


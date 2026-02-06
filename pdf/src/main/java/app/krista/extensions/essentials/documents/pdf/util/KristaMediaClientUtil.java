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

package app.krista.extensions.essentials.documents.pdf.util;


import app.krista.extensions.essentials.documents.pdf.client.KristaMediaClient;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

@Service
public class KristaMediaClientUtil {

    public static final String TEMP_DIR = "/tmp";
    private final KristaMediaClient kristaMediaClient;
    private static final Logger logger = LoggerFactory.getLogger(KristaMediaClientUtil.class);

    @Inject
    public KristaMediaClientUtil(KristaMediaClient kristaMediaClient) {
        this.kristaMediaClient = kristaMediaClient;
    }

    /**
     * Downloads a Krista File and converts it to a Java File.
     * This method handles the conversion from Krista's file representation to a standard
     * Java File object that can be used for local file operations.
     *
     * @param file The Krista File object to download and convert
     * @return A Java File object representing the downloaded file, or null if input is null
     * @throws RuntimeException if an I/O error occurs during the download or conversion
     */
    public File downloadFileHandler(app.krista.model.base.File file) {
        try {
            return file != null ? kristaMediaClient.toJavaFile(file) : null;
        } catch (IOException cause) {
            logger.error("Error while downloading (toJavaFile) file: {} {}", cause.getMessage(), cause);
            throw new RuntimeException(cause);
        }
    }

    /**
     * Uploads a Java File and converts it to a Krista File.
     * This method handles the conversion from a standard Java File object to Krista's
     * file representation for storage and retrieval within the Krista platform.
     *
     * @param file The Java File object to upload and convert
     * @return A Krista File object representing the uploaded file
     * @throws RuntimeException if an I/O error occurs during the upload or conversion
     */
    public app.krista.model.base.File uploadFileHandler(File file) {
        try {
            return kristaMediaClient.toKristaFile(file);
        } catch (IOException cause) {
            logger.error("Error while downloading (toKristaFile) file: {} {}", cause.getMessage(), cause);
            throw new RuntimeException(cause);
        }
    }
}
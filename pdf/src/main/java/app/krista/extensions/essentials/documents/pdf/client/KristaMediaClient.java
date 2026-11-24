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

package app.krista.extensions.essentials.documents.pdf.client;

import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class KristaMediaClient {

    private static final String zipDir = "/tmp/";
    @Inject
    private FileRepository fileRepository;

    private final List<String> unSupportedFileFormats = Arrays.asList("html", "php5", "pht", "phtml", "shtml", "asa", "cer", "asax", "swf", "xap", "jsp", "exe", "js");

    /**
     * Uploads a file to Krista's media server. it will take java.io.File object as input and returns krista's file object
     *
     * @param file The file to be uploaded.
     * @return The Krista file object.
     * @throws IOException If an I/O error occurs.
     */
    public app.krista.model.base.File toKristaFile(File file) throws IOException {
        if (isUnsupportedFileFormat(file.getName())) {
            String baseFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
            String zipFilePath = Paths.get(zipDir, baseFileName + ".zip").toString();
            compressFile(zipFilePath, file.getAbsolutePath());
            file = new File(zipFilePath);
        }
        try (final FileHandle fileHandle = fileRepository.createNewFileByName(file.getName())) {
            fileHandle.setContent(new FileInputStream(file));
            return fileHandle.getFile();
        }
    }

    /**
     * Downloads a file from Krista's media server. it will take krista's file object as input and returns java.io.File object
     *
     * @param file The Krista file object to be downloaded.
     * @return The downloaded Java file object.
     * @throws IOException If an I/O error occurs.
     */
    public File toJavaFile(app.krista.model.base.File file) throws IOException {
        try (FileHandle fileHandle = fileRepository.getFile(file)) {
            InputStream content = fileHandle.getContent();
            final File input = generateUniqueFile(file.getFileName());
            return convertInputStreamToFile(content, input);
        }
    }

    /**
     * Generates a unique file path to prevent file name collisions.
     * This is critical when multiple files with the same name but different content
     * are downloaded, such as during PDF merge operations.
     *
     * <p>The generated file is marked for deletion on JVM exit as a safety mechanism
     * to prevent temp file accumulation. Callers should still explicitly delete files
     * when no longer needed.</p>
     *
     * <p>Example: Input "document.pdf" might generate
     * "/tmp/document_a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf"</p>
     *
     * @param fileName The original file name (must not be null or empty)
     * @return A File object with a unique path in the temp directory
     * @throws IllegalArgumentException if fileName is null or empty
     */
    private File generateUniqueFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        String baseName = getFileNameWithoutExtension(fileName);
        String extension = getFileExtension(fileName);
        String uniqueId = UUID.randomUUID().toString();
        String uniqueFileName = baseName + "_" + uniqueId + "." + extension;

        // Use Paths.get() for cross-platform compatibility instead of string concatenation
        File tempFile = Paths.get(zipDir, uniqueFileName).toFile();

        // Mark for deletion on JVM exit as a safety net to prevent disk space exhaustion
        tempFile.deleteOnExit();

        return tempFile;
    }

    /**
     * Extracts the file name without extension.
     *
     * @param fileName The file name with extension
     * @return The file name without extension
     */
    private String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }

    /**
     * Checks if the file format is unsupported.
     *
     * @param fileName The name of the file.
     * @return True if the file format is unsupported, otherwise false.
     */
    private boolean isUnsupportedFileFormat(String fileName) {
        String fileExtension = getFileExtension(fileName);
        return unSupportedFileFormats.contains(fileExtension);
    }

    /**
     * Converts an input stream to a file.
     *
     * @param inputStream The input stream to be converted.
     * @param input The file to write the input stream content to.
     * @return The file with the content of the input stream.
     * @throws IOException If an I/O error occurs.
     */
    private File convertInputStreamToFile(InputStream inputStream, File input) throws IOException {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(input))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return input;
    }

    /**
     * Retrieves the file extension from the file name.
     *
     * <p>Handles edge cases:</p>
     * <ul>
     *   <li>Files without extension: throws IllegalArgumentException</li>
     *   <li>Files with multiple dots (e.g., "archive.tar.gz"): returns "gz"</li>
     *   <li>Hidden files (e.g., ".gitignore"): throws IllegalArgumentException</li>
     * </ul>
     *
     * @param fileName The name of the file (must not be null or empty)
     * @return The file extension (without the dot)
     * @throws IllegalArgumentException If the file has no extension or is null/empty
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        int lastDotIndex = fileName.lastIndexOf('.');

        // Check if there's a dot and it's not at the beginning (hidden file) or end
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }

        throw new IllegalArgumentException("Unsupported file format: file has no extension");
    }

    /**
     * Compresses a file into a zip archive.
     *
     * @param zipFilePath The path where the zip file will be created.
     * @param dirPathToZip The path of the directory to be zipped.
     * @throws IOException If an I/O error occurs.
     */
    public static void compressFile(String zipFilePath, String dirPathToZip) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            File fileToZip = new File(dirPathToZip);
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
            zipOut.closeEntry();
        }
    }
}

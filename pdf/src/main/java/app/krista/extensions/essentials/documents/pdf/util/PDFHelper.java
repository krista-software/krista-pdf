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

import app.krista.model.base.EntityValue;
import app.krista.model.base.FreeForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static org.docx4j.openpackaging.contenttype.ContentTypes.*;

public class PDFHelper {

    private static final Logger log = LoggerFactory.getLogger(PDFHelper.class);

    private PDFHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * This function will return {@link FreeForm} data in non-technical form
     *
     * @param templateFields input {@link FreeForm} data from client
     * @return given {@link FreeForm} data with non-technical words
     */
    public static FreeForm getValidatedFreeFormData(FreeForm templateFields) {

        FreeForm newTemplateFields = new FreeForm();

        for (String key : templateFields.keySet()) {
            if (templateFields.getTypeDescription(key).equalsIgnoreCase("Number")) {
                addNumberTypeData(templateFields, newTemplateFields, key);
            } else if (templateFields.getTypeDescription(key).equalsIgnoreCase("Switch")) {
                addSwitchData(templateFields, newTemplateFields, key);
            } else {
                addDataToFreeForm(newTemplateFields, key, templateFields.getTypeDescription(key), templateFields.get(key));
            }
        }
        return newTemplateFields;
    }

    /**
     * This function will return {@link List}<{@link EntityValue}> as {@link List}<{@link Map}>
     *
     * @param entityValues input {@link EntityValue}
     * @return {@link List}<{@link Map}> for the given data
     */
    public static List<Map<String, Object>> getEntityDataAsList(List<EntityValue> entityValues) {
        List<Map<String, Object>> entityDataToMap = new ArrayList<>();
        for (EntityValue entity : entityValues) {
            Map<String, Object> entityMap = entity.getFields();
            entityDataToMap.add(entityMap);
        }
        return entityDataToMap;
    }

    /**
     * This function will add validated free form data to the another free form
     *
     * @param newTemplateFields adding switch type data into another {@link FreeForm}
     * @param key               key of the given input {@link FreeForm} data
     * @param typeDescription   type of the given input
     * @param fieldValue        value of the given input
     */
    private static void addDataToFreeForm(FreeForm newTemplateFields, String key, String typeDescription, Object fieldValue) {
        newTemplateFields.put(key, typeDescription, fieldValue);
    }

    /**
     * This function will add Switch data to another free form in non technical format
     *
     * @param templateFields    given input in {@link FreeForm}
     * @param newTemplateFields adding switch type data into another {@link FreeForm}
     * @param key               key of the input switch data
     */
    private static void addSwitchData(FreeForm templateFields, FreeForm newTemplateFields, String key) {
        String switchValue = (Boolean.TRUE.equals(templateFields.get(key))) ? "Yes" : "No";
        PDFHelper.addDataToFreeForm(newTemplateFields, key, "Text", switchValue);
    }

    /**
     * This function will add number data to another free form
     *
     * @param templateFields    given input in {@link FreeForm}
     * @param newTemplateFields adding number type data into another {@link FreeForm}
     * @param key               key of the input given input
     */
    private static void addNumberTypeData(FreeForm templateFields, FreeForm newTemplateFields, String key) {
        String dateValue = getDateForDoubleValue(key, (double) templateFields.get(key));
        if (dateValue != null && !dateValue.isBlank()) {
            PDFHelper.addDataToFreeForm(newTemplateFields, key, "Text", dateValue);
        } else {
            PDFHelper.addDataToFreeForm(newTemplateFields, key, templateFields.getTypeDescription(key), templateFields.get(key));
        }
    }

    /**
     * This function will add date to another free form if the input is kind of Date
     *
     * @param key         key of the number type data
     * @param doubleValue value of the number type data
     * @return Date string if it is having any of the DATE keys
     */
    // TODO: 20/12/22 This function needs to be removed once client passes date along with time zone to extension
    public static String getDateForDoubleValue(String key, double doubleValue) {

        List<String> validDateKeys = List.of("$$date", "$$from", "$$to", "$$from_date", "$$to_date", "$$end", "$$start", "$$fromDate", "$$toDate", "$$startTime", "$$endTime", "$$start_time", "$$end_time", "$$start_date", "$$end_date", "$$startDate", "$$endDate", "$$approved", "$$approved_on", "$$approvedOn", "$$dob", "$$dateOfBirth", "$$date_of_birth");
        if (validDateKeys.contains(key.toLowerCase()) && String.valueOf(doubleValue).contains("E")) {
            long longValue = (long) doubleValue;
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH.mm.ss");
            return sdf.format(new Date(longValue));
        }
        return null;
    }

    /**
     * This function will remove the temporary files crated for the given path
     *
     * @param path to the temporary files created
     * @throws IOException If an I/O error occurs.
     */
    public static void cleanUp(Path path) throws IOException {
        Files.delete(path);
    }

    /**
     * This function will get Array of bytes for the given {@link InputStream} of logo
     *
     * @param logo            {@link File} to be added as logo to document
     * @param logoInputStream {@link InputStream} of {@param logo}
     * @return return the array of bytes
     * @throws IOException If an I/O error occurs.
     */
    public static byte[] getBytesArrayForLogo(File logo, InputStream logoInputStream) throws IOException {
        long length = logo.length();
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Logo file is too large!!");
        }
        // Add reasonable size limit (10MB for logos)
        if (length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Logo file exceeds maximum size of 10MB");
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead;
        while (offset < bytes.length && (numRead = logoInputStream.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        /* Ensure all the bytes have been read in */
        if (offset < bytes.length) {
            log.error("Could not completely read logo file: {}. Expected {} bytes, got {} bytes",
                     logo.getName(), bytes.length, offset);
            throw new IOException("Incomplete file read: expected " + bytes.length + " bytes, got " + offset);
        }
        return bytes;
    }

    /**
     * Generates a safe PDF file path using the given file name.
     * It removes the original file extension, cleans the name to be safe,
     * and appends ".pdf" in a temporary directory.
     *
     * @param fileName the original file name
     * @return a full path string for the PDF file
     */

    public static String generatePdfFilePath(String fileName) {
        String baseName = getFileNameWithoutExtension(fileName);
        String safeName = cleanFileName(baseName);
        return KristaMediaClientUtil.TEMP_DIR + File.separator + safeName + ".pdf";

    }

    /**
     * This function extracts the file name without its extension.
     *
     * @param fileName the full file name (e.g., "document.docx")
     * @return the file name without the extension (e.g., "document")
     */

    private static String getFileNameWithoutExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(0, (fileName.lastIndexOf(".")));
        }
        return fileName;
    }

    /**
     * Sanitizes a user-supplied name into a safe base file name while PRESERVING the
     * user's capitalization and internal spacing. Strips any extension the user typed,
     * removes special characters (keeping letters, digits, spaces, '_' and '-'), and
     * trims leading/trailing whitespace. Returns the base name WITHOUT an extension;
     * callers append ".pdf". Returns "" if the input is null or has no valid characters.
     *
     * <p>Note: any text after the last '.' is treated as an extension and removed.</p>
     *
     * <p>Unlike {@link #getSafeFileName(String)} this does not lowercase, and unlike
     * {@link #cleanFileName(String)} it does not convert spaces to underscores.</p>
     *
     * @param name the user-supplied name (may be null)
     * @return the sanitized base name without extension, possibly empty
     */
    public static String sanitizeFileName(String name) {
        if (name == null) {
            return "";
        }
        String base = getFileNameWithoutExtension(name);
        return base.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
    }

    /**
     * Cleans the given file name by removing unsafe characters and normalizing it.
     * If the input is null, empty, or results in an empty cleaned name, returns "file".
     * Allowed characters after cleaning are letters, numbers, spaces, dashes, and underscores.
     * Spaces and other whitespace are replaced by underscores.
     *
     * @param fileName the original file name
     * @return a safe, cleaned file name string
     */
    public static String cleanFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "file";
        }
        // Normalize Unicode (optional but good for accents)
        String normalized = java.text.Normalizer.normalize(fileName, java.text.Normalizer.Form.NFKD);

        // Remove emojis and symbols — keep letters, numbers, spaces, dashes, underscores
        // \p{L} = any kind of letter from any language
        // \p{N} = any kind of numeric digit
        // We keep spaces, underscore and dash explicitly
        String cleaned = normalized.replaceAll("[^\\p{L}\\p{N} _-]", "");

        // Replace all whitespace (tabs, multiple spaces) with single underscore
        cleaned = cleaned.trim().replaceAll("\\s+", "_");

        if (cleaned.isEmpty()) {
            return "file";
        }

        return cleaned;
    }

    /**
     * This function will generate file path for the docx file
     *
     * @return path of Docx file to be generated
     */
    public static String generateDocxFilePath() {
        return KristaMediaClientUtil.TEMP_DIR + File.separator + "doc_docx_" + UUID.randomUUID() + "." + "docx";
    }

    /**
     * This function takes image file name and returns the image format value string
     *
     * @param imgFile name of the file
     * @return image format
     */
    public static String getImageFormat(String imgFile) {
        String format;
        if (imgFile.endsWith(Constants.EMF)) {
            format = IMAGE_EMF;
        } else if (imgFile.endsWith(Constants.WMF)) {
            format = IMAGE_WMF;
        } else if (imgFile.endsWith(Constants.PICT)) {
            format = IMAGE_PICT;
        } else if (imgFile.endsWith(Constants.JPEG) || imgFile.endsWith(Constants.JPG)) {
            format = IMAGE_JPEG;
        } else if (imgFile.endsWith(Constants.PNG)) {
            format = IMAGE_PNG;
        } else if (imgFile.endsWith(Constants.GIF)) {
            format = IMAGE_GIF;
        } else if (imgFile.endsWith(Constants.TIFF)) {
            format = IMAGE_TIFF;
        } else if (imgFile.endsWith(Constants.EPS)) {
            format = IMAGE_EPS;
        } else if (imgFile.endsWith(Constants.BMP)) {
            format = IMAGE_BMP;
        } else if (imgFile.endsWith(Constants.SVG)) {
            format = IMAGE_SVG;
        } else {
            throw new IllegalArgumentException("Unsupported picture: " + imgFile + ". Expected emf or wmf or pict or jpeg or png or gif or tiff or eps or bmp or svg");
        }
        return format;
    }

    /**
     * Creates a safe file name by removing invalid characters.
     *
     * @param fileName the original file name
     * @return a safe file name or "file" if input is empty
     */
    public static String getSafeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "file";
        }
        // Regex for allowed characters: letters, numbers, space, underscore, dash, dot
        boolean containsSymbols = !fileName.matches("[\\p{L}\\p{N} _\\-.]+");

        if (containsSymbols) {
            // Clean if contains emojis or symbols
            return cleanFileName(fileName).toLowerCase();
        } else {
            // Otherwise, just lowercase and return original
            return fileName.toLowerCase();
        }
    }

    /**
     * Resizes an image to fit within specified dimensions while maintaining aspect ratio.
     *
     * @param originalImage the image to resize
     * @param maxWidth maximum width for the resized image
     * @param maxHeight maximum height for the resized image
     * @return the resized image
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, float maxWidth, float maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        float scale = Math.min(maxWidth / originalWidth, maxHeight / originalHeight);

        int targetWidth = Math.max(1, Math.round(originalWidth * scale));
        int targetHeight = Math.max(1, Math.round(originalHeight * scale));

        Image scaled = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        return resized;
    }
}

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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;

import static app.krista.extensions.essentials.documents.pdf.util.PDFHelper.*;

/**
 * Utility class for PDF-related operations such as validating image files,
 * converting images to PDF documents, and converting JPEG images to PNG format.
 */
public class PDFUtils {

    private static final Logger logger = LoggerFactory.getLogger(PDFUtils.class);
    private static final LoggerProvider loggerProvider = GlobalOpenTelemetry.get().getLogsBridge();
    private static final io.opentelemetry.api.logs.Logger telemetryLogger = loggerProvider.get("PDFUtils");

    private static final String[] VALID_IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg"};


    /**
     * Checks if the given file is an image with a valid extension (.png, .jpg, .jpeg).
     *
     * @param file the file to check
     * @return true if the file has a valid image extension, false otherwise
     */

    public static boolean isValidImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return Arrays.stream(VALID_IMAGE_EXTENSIONS).anyMatch(fileName::endsWith);
    }

    /**
     * Converts an image file to a PDF file.
     * This method processes the input image file (converting it to PNG if needed),
     * creates a PDF document with a single A4 page, scales and centers the image
     * on the page, saves the PDF to a temporary location, and returns the generated PDF file.
     * Telemetry events are logged for success or failure of the conversion.
     *
     * @param imageFile the image file to convert (supported formats: JPEG, PNG, BMP, etc.)
     * @return the generated PDF file containing the image
     * @throws IOException if an error occurs during image processing or PDF creation
     */
    public static File convertImageToPdf(File imageFile) throws IOException {
        try {
            File processedImageFile = convertToPngIfRequired(imageFile);
            File outputFile = prepareOutputFile(processedImageFile);
            try (PDDocument document = new PDDocument()) {
                PDPage page = createPage();
                document.addPage(page);
                PDImageXObject image = PDImageXObject.createFromFileByContent(processedImageFile, document);
                float[] scaledSize = calculateScaledSize(image, page);
                float x = (page.getMediaBox().getWidth() - scaledSize[0]) / 2;
                float y = (page.getMediaBox().getHeight() - scaledSize[1]) / 2;
                drawImage(document, page, image, x, y, scaledSize[0], scaledSize[1]);
                document.save(outputFile);
                logger.info("Successfully converted image to PDF: {}", outputFile.getAbsolutePath());
                telemetryLogger.logRecordBuilder().setBody("Image to PDF conversion succeeded").setAttribute("fileName", imageFile.getName())
                        .setAttribute("status", "success").emit();
                return outputFile;
            }
        } catch (IOException cause) {
            logger.error("Error converting image to PDF: {}", cause.getMessage(), cause);
            telemetryLogger.logRecordBuilder().setBody("Image to PDF conversion failed").setAttribute("fileName", imageFile.getName()).setAttribute("status", "failure")
                    .setAttribute("errorMessage", cause.getMessage()).emit();
            throw cause;
        }
    }

    /**
     * Prepares the output PDF file in the system's temporary directory.
     * Creates the temporary directory if it does not exist.
     *
     * @param processedImageFile the image file that will be embedded in the PDF
     * @return a File object representing the new PDF file path
     * @throws IOException if the temporary directory cannot be created
     */
    private static File prepareOutputFile(File processedImageFile) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        String outputFilePath = generatePdfFilePath(processedImageFile.getName());
        return new File(outputFilePath);
    }

    /**
     * Creates a new A4-sized PDF page.
     * @return a new PDPage object with A4 dimensions
     */
    private static PDPage createPage() {
        return new PDPage(PDRectangle.A4);
    }

    /**
     * Calculates the scaled width and height of the image so that it fits within
     * the margins of the PDF page while maintaining the original aspect ratio.
     *
     * @param image the PDImageXObject representing the image to be scaled
     * @param page the PDF page where the image will be placed
     * @return a float array where index 0 is the scaled width and index 1 is the scaled height
     */
    private static float[] calculateScaledSize(PDImageXObject image, PDPage page) {
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        PDRectangle mediaBox = page.getMediaBox();
        float pageWidth = mediaBox.getWidth() - 40;
        float pageHeight = mediaBox.getHeight() - 40;
        float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
        float scaledWidth = imageWidth * scale;
        float scaledHeight = imageHeight * scale;

        return new float[]{scaledWidth, scaledHeight};
    }

    /**
     * Draws the image on the given PDF page at the specified coordinates with the given dimensions.
     *
     * @param document the PDDocument containing the page
     * @param page     the PDPage where the image will be drawn
     * @param image    the PDImageXObject to be drawn
     * @param x        the x-coordinate (from the left) where the image should start
     * @param y        the y-coordinate (from the bottom) where the image should start
     * @param width    the width to scale the image to
     * @param height   the height to scale the image to
     * @throws IOException if an error occurs writing to the content stream
     */
    private static void drawImage(PDDocument document, PDPage page, PDImageXObject image,
                                  float x, float y, float width, float height) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(image, x, y, width, height);
        }
    }

    /**
     * Converts a JPEG image file to PNG format.
     * If the input file is not a JPEG, it is returned unchanged.
     *
     * @param imageFile the image file to convert
     * @return a File object representing the PNG image (or original file if no conversion needed)
     * @throws IOException if an error occurs reading or writing image files
     */
    public static File convertToPngIfRequired(File imageFile) throws IOException {
        String fileName = imageFile.getName();
        String cleanedFileName = getSafeFileName(fileName);
        if (cleanedFileName.endsWith(".jpeg") || cleanedFileName.endsWith(".jpg") || cleanedFileName.endsWith(".png")) {
            try (InputStream input = new BufferedInputStream(new FileInputStream(imageFile))) {
                BufferedImage originalImage = ImageIO.read(input);
                if (originalImage == null) {
                    throw new IOException("Failed to decode image: " + fileName);
                }
                PDRectangle a4 = PDRectangle.A4;
                BufferedImage resizedImage = resizeImage(originalImage, a4.getWidth() - 40, a4.getHeight() - 40);
                String outputFileName = cleanedFileName.replaceAll("\\.(jpeg|jpg)$", ".png");
                File pngFile = new File(imageFile.getParent(), outputFileName);
                ImageIO.write(resizedImage, "png", pngFile);
                logger.info("Image converted successfully to: {}", pngFile.getAbsolutePath());
                telemetryLogger.logRecordBuilder().setBody("Image to PNG conversion succeeded")
                        .setAttribute("fileName", fileName)
                        .setAttribute("status", "success")
                        .emit();
                return pngFile;
            }
        }
        return imageFile;
    }
}







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

import app.krista.extensions.essentials.documents.pdf.util.Constants;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.extensions.essentials.documents.pdf.util.PDFHelper;
import app.krista.model.base.EntityValue;
import app.krista.model.entity.EntityDefinition;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import fr.opensagres.poi.xwpf.converter.core.XWPFConverterException;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import jakarta.xml.bind.JAXBElement;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.model.structure.SectionWrapper;
import org.docx4j.model.table.TblFactory;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.*;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.*;
import org.jvnet.hk2.annotations.Service;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static app.krista.extensions.essentials.documents.pdf.util.PDFHelper.generatePdfFilePath;
import static org.docx4j.openpackaging.contenttype.ContentTypes.*;

@Service
public final class PDFManager {

    private static final Logger log = LoggerFactory.getLogger(PDFManager.class);
    public static final String SECOND_HALF = "second_half.pdf";
    public static final String FIRST_HALF = "first_half.pdf";

    private final PdfSplitter pdfSplitter;

    /**
     * Minimum timestamp value for date detection (Jan 1, 2000 00:00:00 UTC).
     * Values below this are treated as regular numbers, not timestamps.
     */
    private static final long TIMESTAMP_MIN_VALUE = 946684800000L;

    /**
     * Maximum timestamp value for date detection (Jan 1, 2100 00:00:00 UTC).
     * Values above this are treated as regular numbers, not timestamps.
     */
    private static final long TIMESTAMP_MAX_VALUE = 4102444800000L;

    // Conversion constants for page size calculations
    /**
     * Conversion factor from TWIPS to millimeters.
     * TWIPS (Twentieth of a Point) is a unit used in Word documents.
     * Formula: inches * 25.4mm/inch / 1440 TWIPS/inch = 0.0254 / 1440
     */
    private static final double TWIPS_TO_MM_CONVERSION = 0.0254 / 1440 * 1000;

    /**
     * Page height adjustment in TWIPS for DOCX to PDF conversion.
     * This adjustment compensates for rendering differences.
     */
    private static final double PAGE_HEIGHT_ADJUSTMENT_TWIPS = 90;

    /**
     * Conversion factor from millimeters to inches.
     */
    private static final double MM_TO_INCH = 25.4;

    /**
     * Table width percentage for HTML table styling (80% of page width).
     */
    private static final int TABLE_WIDTH_PERCENTAGE = 80;

    /**
     * Default page width in millimeters (A4 size).
     */
    private static final double DEFAULT_PAGE_WIDTH_MM = 210;

    /**
     * Default page height in millimeters (A4 size).
     */
    private static final double DEFAULT_PAGE_HEIGHT_MM = 297;

    @Inject
    public PDFManager(PdfSplitter pdfSplitter) {
        this.pdfSplitter = pdfSplitter;
    }

    /**
     * Converts a DOCX file stream to PDF format using Apache POI XWPF.
     * This method uses the PdfConverter to transform XWPF documents into PDF format
     * with default PDF options.
     *
     * @param inputStream The input stream of the DOCX file to be converted to PDF
     * @param pdfFilePath The file path where the generated PDF file will be saved
     * @return A File object representing the generated PDF file
     * @throws IllegalStateException if the conversion fails due to I/O or conversion errors
     */
    public static File getPdfForXWPFDoc(InputStream inputStream, String pdfFilePath) {
        try (OutputStream stream = new FileOutputStream(pdfFilePath)) {
            XWPFDocument document = new XWPFDocument(inputStream);
            PdfOptions options = PdfOptions.getDefault();
            PdfConverter.getInstance().convert(document, stream, options);
            document.close();
        } catch (XWPFConverterException | IOException e) {
            throw new IllegalStateException("Failed to get pdf " + e.getMessage(), e);
        }
        return new File(pdfFilePath);
    }

    /**
     * Converts a DOCX file stream to PDF format using HTML intermediate conversion.
     * This method extracts images, generates HTML content, applies styles, and then
     * converts the HTML to PDF format while preserving document formatting and page size.
     *
     * @param doc     The input stream of the DOCX file to be converted
     * @param pdfPath The file path where the generated PDF file will be saved
     * @return A File object representing the generated PDF file
     * @throws IllegalArgumentException if the conversion fails due to I/O or processing errors
     */
    public static File convertDocxToPdf(InputStream doc, String pdfPath) {
        try (ByteArrayOutputStream htmlOut = new ByteArrayOutputStream()) {
            XWPFDocument document = new XWPFDocument(doc);
            String imageOutputDir = KristaMediaClientUtil.TEMP_DIR;

            extractImages(document, imageOutputDir);
            String htmlContent = generateHtmlContent(document, htmlOut, imageOutputDir);
            htmlContent = applyStyles(htmlContent, getDocxPageSize(document));

            generatePdfFromHtml(htmlContent, pdfPath, imageOutputDir, getDocxPageSize(document));
        } catch (IOException cause) {
            log.error("Error reading File: {}", cause.getMessage());
            throw new IllegalArgumentException("Error reading File: " + cause.getMessage(), cause);
        } catch (Exception cause) {
            log.error("Error converting DOCX to PDF: {}", cause.getMessage());
            throw new IllegalArgumentException("Error converting DOCX to PDF: " + cause.getMessage(), cause);
        }
        return new File(pdfPath);
    }

    private static String generateHtmlContent(XWPFDocument document, ByteArrayOutputStream htmlOut, String imageOutputDir) throws Exception {
        XHTMLOptions options = XHTMLOptions.create();
        XHTMLConverter.getInstance().convert(document, htmlOut, options);

        String htmlContent = htmlOut.toString(StandardCharsets.UTF_8);
        htmlContent = updateHtmlWithImagePaths(htmlContent, imageOutputDir);
        return sanitizeHtml(htmlContent, true); // default to useSanitizer = true
    }

    private static String applyStyles(String htmlContent, double[] pageSize) {
        double width = pageSize[0], height = pageSize[1];
        String styles = "<style>"
                + "div {margin: 0px 0px 0px 0px !important}"
                + "table {width: " + (width / MM_TO_INCH) * TABLE_WIDTH_PERCENTAGE + "px}"
                + "</style>";
        return htmlContent.replace("</head>", styles + "</head>");
    }

    private static void generatePdfFromHtml(String htmlContent, String pdfPath, String imageOutputDir, double[] pageSize) throws IOException {
        try (OutputStream os = new FileOutputStream(pdfPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useDefaultPageSize((float) pageSize[0], (float) pageSize[1], PdfRendererBuilder.PageSizeUnits.MM);
            builder.withHtmlContent(htmlContent, "file://" + imageOutputDir + "/");
            builder.toStream(os);
            builder.run();
        }
    }

    private static String updateHtmlWithImagePaths(String htmlContent, String imageDir) {
        File imageDirectory = new File(imageDir);
        String relativePathPrefix = "file:///" + imageDir.replace("\\", "/") + "/";
        File[] imageFiles = imageDirectory.listFiles();

        if (imageFiles == null) {
            log.error("No images found in the directory: {}", imageDirectory.getAbsolutePath());
            return htmlContent;
        }

        // Use StringBuilder for efficient string manipulation in loop
        StringBuilder htmlBuilder = new StringBuilder(htmlContent);

        try {
            String canonicalImageDir = imageDirectory.getCanonicalPath();
            for (File imageFile : imageFiles) {
                // Security: Validate file is actually in the expected directory to prevent path traversal
                String canonicalImagePath = imageFile.getCanonicalPath();
                if (!canonicalImagePath.startsWith(canonicalImageDir)) {
                    log.warn("Skipping file outside image directory: {}", imageFile.getName());
                    continue;
                }

                String imageName = imageFile.getName();
                // Security: Sanitize filename to prevent path traversal sequences
                if (imageName.contains("..") || imageName.contains("/") || imageName.contains("\\")) {
                    log.warn("Skipping file with suspicious name: {}", imageName);
                    continue;
                }

                // Efficiently replace all occurrences using StringBuilder
                String searchPattern = "src=\"word/media/" + imageName + "\"";
                String replacement = "src=\"" + relativePathPrefix + imageName + "\"";

                int index = 0;
                while ((index = htmlBuilder.indexOf(searchPattern, index)) != -1) {
                    htmlBuilder.replace(index, index + searchPattern.length(), replacement);
                    index += replacement.length();
                }
            }
        } catch (IOException e) {
            log.error("Error validating image file paths: {}", e.getMessage());
        }

        return htmlBuilder.toString();
    }

    /**
     * Extracts all images from a DOCX document and saves them to the specified directory.
     * Images are saved with their original filenames from the document.
     *
     * @param document The DOCX document containing images
     * @param outputDir The directory path where images will be saved
     * @throws IOException If an error occurs while writing image files
     */
    private static void extractImages(XWPFDocument document, String outputDir) throws IOException {
        List<XWPFPictureData> pictures = document.getAllPictures();
        for (XWPFPictureData picture : pictures) {
            String fileName = picture.getFileName();
            File imageFile = new File(outputDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(picture.getData());
            }
        }
    }

    /**
     * Sanitizes HTML content using Jsoup to prevent XSS attacks and ensure valid XHTML.
     * When sanitization is enabled, the HTML is parsed and re-serialized with proper
     * XML syntax and XHTML entity escaping.
     *
     * @param htmlContent The raw HTML content to sanitize
     * @param useSanitizer If true, applies Jsoup sanitization; if false, returns content as-is
     * @return Sanitized HTML content safe for rendering, or original content if sanitization is disabled
     */
    private static String sanitizeHtml(String htmlContent, boolean useSanitizer) {

        String contentToRender = htmlContent;
        if (useSanitizer) {

            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(htmlContent);
            doc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
            doc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
            contentToRender = doc.html();

        }
        return contentToRender;
    }

    /**
     * Extracts page size dimensions from a DOCX document.
     * Converts TWIPS (Twentieth of a Point) units used in Word documents to millimeters.
     * Applies a height adjustment to compensate for rendering differences.
     *
     * @param document The DOCX document to extract page size from
     * @return Array of [widthMM, heightMM] in millimeters. Returns A4 size [210, 297] if extraction fails.
     */
    private static double[] getDocxPageSize(XWPFDocument document) {
        try {
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            if (sectPr != null) {
                CTPageSz pageSz = sectPr.getPgSz();
                if (pageSz != null) {
                    double widthMM = ((BigInteger) pageSz.getW()).doubleValue() * TWIPS_TO_MM_CONVERSION;
                    double heightMM = (((BigInteger) pageSz.getH()).doubleValue() + PAGE_HEIGHT_ADJUSTMENT_TWIPS)
                                      * TWIPS_TO_MM_CONVERSION;
                    return new double[]{widthMM, heightMM};
                }
            }
        } catch (Exception cause) {
            log.error("Error extracting page size: {}", cause.getMessage());
        }
        return new double[]{DEFAULT_PAGE_WIDTH_MM, DEFAULT_PAGE_HEIGHT_MM};
    }

    /**
     * This function will create a {@link P} object for the docx file
     *
     * @param text String value which needs to be added to a paragraph in the document
     * @return returns a wml {@link P} object to be added to the docx file
     */
    public static P makeParagraph(String text) {
        ObjectFactory factory = Context.getWmlObjectFactory();
        P p = factory.createP();
        R r = factory.createR();
        p.getContent().add(r);
        Text t = factory.createText();
        r.getContent().add(t);
        t.setValue(text);
        return p;
    }

    /**
     * This function will return section parameters present in the given docx file like Header, Footer etc.
     *
     * @param wordprocessingMLPackage takes {@link WordprocessingMLPackage} object which contains the given docx file.
     * @param objectFactory           takes {@link ObjectFactory} initialized for {@link WordprocessingMLPackage} to get sections present in the document
     * @return returns {@link SectPr} object for the given docx file.
     */
    private static SectPr getSectPr(WordprocessingMLPackage wordprocessingMLPackage, ObjectFactory objectFactory) {
        List<SectionWrapper> sections = wordprocessingMLPackage.getDocumentModel().getSections();
        SectPr sectPr = sections.get(sections.size() - 1).getSectPr();
        if (sectPr == null) {
            sectPr = objectFactory.createSectPr();
            wordprocessingMLPackage.getMainDocumentPart().addObject(sectPr);
            sections.get(sections.size() - 1).setSectPr(sectPr);
        }
        return sectPr;
    }

    /**
     * This function will add Logo Image to the docx file.
     *
     * @param wordMLPackage takes {@link WordprocessingMLPackage} object which contains the given docx file.
     * @param bytes         takes Array of bytes for the given image file.
     * @param filenameHint  Hint name for the given image file.
     * @param altText       alternative text for the given image file.
     * @param id2           unique id in the docx file for the image file
     * @param cx            Image width in twip
     * @return returns {@link P} paragraph object which adds image as Inline to the paragraph
     * @throws Exception If an error occurs while creating or embedding the image into the DOCX file.
     */
    private static org.docx4j.wml.P addNewImage(WordprocessingMLPackage wordMLPackage, byte[] bytes, String filenameHint, String altText, int id2, long cx) throws Exception {
        BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, bytes);
        Inline inline = imagePart.createImageInline(filenameHint, altText, wordMLPackage.getDrawingPropsIdTracker().generateId(), id2, cx, false);

        org.docx4j.wml.ObjectFactory factory = Context.getWmlObjectFactory();
        org.docx4j.wml.P imagePara = factory.createP();
        org.docx4j.wml.R imageParaRun = factory.createR();
        imagePara.getContent().add(imageParaRun);
        org.docx4j.wml.Drawing drawing = factory.createDrawing();
        imageParaRun.getContent().add(drawing);
        drawing.getAnchorOrInline().add(inline);

        return imagePara;
    }

    /**
     * This function will add given string value to the table cell in the docx file.
     *
     * @param keyValue Value that needs to be added to table cell
     * @param factory  {@link ObjectFactory} object that will create {@link P} paragraph object and {@link R} run object.
     * @return returns {@link P} paragraph object that needs to be added to the docx file.
     */
    private static P getDataToAdd(String keyValue, ObjectFactory factory) {
        P cellData = factory.createP();
        R cellDataRun = factory.createR();
        Text cellDataText = factory.createText();
        cellDataText.setValue(keyValue);
        cellDataRun.getContent().add(cellDataText);
        cellData.getContent().add(cellDataRun);
        return cellData;
    }

    /**
     * This function will convert docx file to pdf file.
     *
     * @param inputFile input docx {@link File} object.
     * @return returns pdf {@link File} object.
     */
    public File convertDocToPDF(File inputFile) {
        try {
            InputStream is = new FileInputStream(inputFile.getAbsolutePath());
            String pdfFilePath = generatePdfFilePath(inputFile.getName());
            return convertDocxToPdf(is, pdfFilePath);
        } catch (IOException cause) {
            throw new IllegalArgumentException("Error thrown during converting to PDF: " + cause.getMessage(), cause);
        }
    }

    /**
     * This function will take required parameters and returns a pdf {@link File} in the pre-determined Krista Rich Text Pdf format
     *
     * @param title        Title of the pdf document
     * @param subTitle     subtitle for the pdf document
     * @param description  description for the pdf document
     * @param header       header of the pdf document
     * @param footer       footer of the pdf document
     * @param logo         logo {@link File} for the pdf document
     * @param richTextData main document text for the pdf document
     * @return pdf {@link File} with Krista Rich Text Pdf format
     * @throws Exception If an error occurs during PDF generation.
     */
    public File getRichTextPDF(String title, String subTitle, String description, String header, String footer, File logo, String richTextData) throws Exception {
        WordprocessingMLPackage wordPackage = addTemplateData(title, subTitle, description, header, footer, logo);
        if (richTextData != null && !richTextData.isBlank()) {
            log.info("getRichTextPDF() - Adding rich text data");
            addRichTextPara(richTextData, wordPackage);
        }
        return getJavaPdfFile(wordPackage);
    }

    /**
     * This function will return a pdf {@link File} object for the given {@link WordprocessingMLPackage} object
     *
     * @param wordPackage input {@link WordprocessingMLPackage} object
     * @return pdf {@link File} object for given input {@param wordPackage} object
     * @throws Docx4JException If an error occurs while saving or processing the DOCX package.
     * @throws IOException     If an I/O error occurs during file operations.
     */
    private File getJavaPdfFile(WordprocessingMLPackage wordPackage) throws Docx4JException, IOException {
        File exportFile = new File(PDFHelper.generateDocxFilePath());
        wordPackage.save(exportFile);
        InputStream inputStream = new FileInputStream(exportFile);
        String pdfFilePath = generatePdfFilePath(exportFile.getName());
        File pdfFile = getPdfForXWPFDoc(inputStream, pdfFilePath);
        inputStream.close();
        PDFHelper.cleanUp(exportFile.toPath());
        return pdfFile;
    }

    /**
     * This function will return {@link WordprocessingMLPackage} object for the given template input.
     *
     * @param title       Title of the pdf document
     * @param subTitle    subtitle for the pdf document
     * @param description description for the pdf document
     * @param header      header of the pdf document
     * @param footer      footer of the pdf document
     * @param logo        logo {@link File} for the pdf document
     * @return {@link WordprocessingMLPackage} object with the template data
     * @throws Exception If an error occurs while creating or populating the WordprocessingMLPackage.
     */
    private WordprocessingMLPackage addTemplateData(String title, String subTitle, String description, String header, String footer, File logo) throws Exception {

        WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
        MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();
        List<String> supportedLogoFormats = List.of(IMAGE_BMP, IMAGE_EMF, IMAGE_EPS, IMAGE_GIF, IMAGE_JPEG, IMAGE_PICT, IMAGE_PNG, IMAGE_SVG, IMAGE_WMF, IMAGE_TIFF);
        if ((logo != null) && supportedLogoFormats.contains(PDFHelper.getImageFormat(logo.getName()))) {
            try (InputStream logoInputStream = new FileInputStream(logo)) {
                byte[] logoBytes = PDFHelper.getBytesArrayForLogo(logo, logoInputStream);
                P imagePara = addNewImage(wordPackage, logoBytes, null, null, 1, 2000);
                mainDocumentPart.addObject(imagePara);
            } catch (FileNotFoundException e) {
                log.debug(e.getMessage());
            }
        }
        if (title != null && !title.isBlank()) {
            mainDocumentPart.addStyledParagraphOfText("Title", title);
        }
        if (subTitle != null && !subTitle.isBlank()) {
            mainDocumentPart.addStyledParagraphOfText("Subtitle", subTitle);
        }
        if (description != null && !description.isBlank()) {
            log.info("addTemplateData() - Adding description data");
            addRichTextPara(description, wordPackage);
        }
        if (header != null && !header.isBlank()) {
            addHeaderToDoc(header, wordPackage, Constants.COVER_HEADER);
            addHeaderToDoc(header, wordPackage, Constants.CONTENT_HEADER);
        }
        if (footer != null && !footer.isBlank()) {
            addFooterToDoc(footer, wordPackage, Constants.COVER_FOOTER);
            addFooterToDoc(footer, wordPackage, Constants.CONTENT_FOOTER);
        }
        return wordPackage;
    }

    /**
     * This function will add footers to the docx file
     *
     * @param footerText  text for the footer section
     * @param wordPackage input {@link WordprocessingMLPackage} object
     * @param footerType  either cover footer or content footer
     * @throws org.docx4j.openpackaging.exceptions.InvalidFormatException
     */
    private void addFooterToDoc(String footerText, WordprocessingMLPackage wordPackage, String footerType) throws org.docx4j.openpackaging.exceptions.InvalidFormatException {
        FooterPart footerPart = createFooterPart(wordPackage, footerType);
        Ftr ftr = getFtr();
        footerPart.setJaxbElement(ftr);
        Relationship footerRel = wordPackage.getMainDocumentPart().addTargetPart(footerPart);
        ftr.getContent().add(makeParagraph(footerText));
        addFooterReference(wordPackage, footerRel, footerType);
    }

    /**
     * This function will add footer references to the docx file
     *
     * @param wordPackage input {@link WordprocessingMLPackage} object
     * @param footerRel   footer style {@link Relationship} object of the docx file
     * @param footerType  type of footer to be added to the docx file
     */
    private void addFooterReference(WordprocessingMLPackage wordPackage, Relationship footerRel, String footerType) {
        ObjectFactory objectFactory = Context.getWmlObjectFactory();
        FooterReference footerReference = objectFactory.createFooterReference();
        footerReference.setId(footerRel.getId());
        if (footerType.equalsIgnoreCase(Constants.COVER_FOOTER)) {
            footerReference.setType(HdrFtrRef.FIRST);
        } else if (footerType.equalsIgnoreCase(Constants.CONTENT_FOOTER)) {
            footerReference.setType(HdrFtrRef.DEFAULT);
        }
        SectPr sectPr = getSectPr(wordPackage, objectFactory);
        sectPr.getEGHdrFtrReferences().add(footerReference);
        sectPr.setTitlePg(new BooleanDefaultTrue());
    }

    /**
     * This function will add headers to the docx file
     *
     * @param header      text for the header section
     * @param wordPackage input {@link WordprocessingMLPackage} object
     * @param headerType  either cover header or content header
     * @throws org.docx4j.openpackaging.exceptions.InvalidFormatException
     */
    private void addHeaderToDoc(String header, WordprocessingMLPackage wordPackage, String headerType) throws org.docx4j.openpackaging.exceptions.InvalidFormatException {
        ObjectFactory objectFactory = Context.getWmlObjectFactory();
        HeaderPart headerPart = createHeaderPart(wordPackage, headerType);
        Hdr hdr = objectFactory.createHdr();
        headerPart.setJaxbElement(hdr);
        Relationship headerRel = wordPackage.getMainDocumentPart().addTargetPart(headerPart);
        hdr.getContent().add(makeParagraph(header));
        addHeaderReference(wordPackage, headerRel, headerType);
    }

    /**
     * This function will add header references to the docx file
     *
     * @param wordPackage input {@link WordprocessingMLPackage} object
     * @param headerRel   footer style {@link Relationship} object of the docx file
     * @param headerType  type of header to be added to the docx file
     */
    private void addHeaderReference(WordprocessingMLPackage wordPackage, Relationship headerRel, String headerType) {

        ObjectFactory objectFactory = Context.getWmlObjectFactory();
        HeaderReference headerReference = objectFactory.createHeaderReference();
        headerReference.setId(headerRel.getId());
        if (headerType.equalsIgnoreCase(Constants.COVER_HEADER)) {
            headerReference.setType(HdrFtrRef.FIRST);
        } else if (headerType.equalsIgnoreCase(Constants.CONTENT_HEADER)) {
            headerReference.setType(HdrFtrRef.DEFAULT);
        }
        SectPr sectPr = getSectPr(wordPackage, objectFactory);
        sectPr.getEGHdrFtrReferences().add(headerReference);
        sectPr.setTitlePg(new BooleanDefaultTrue());
    }

    /**
     * This function will create header part with header text
     *
     * @param wordPackage input {@link WordprocessingMLPackage} object
     * @param type        type of header
     * @return {@link HeaderPart} object to be added to docx file
     * @throws org.docx4j.openpackaging.exceptions.InvalidFormatException
     */
    private HeaderPart createHeaderPart(WordprocessingMLPackage wordPackage, String type) throws org.docx4j.openpackaging.exceptions.InvalidFormatException {
        HeaderPart headerPart = new HeaderPart(new PartName("/word/" + type + ".xml"));
        wordPackage.getParts().put(headerPart);
        return headerPart;
    }

    /**
     * This function will create footer part with footer text
     *
     * @param wordPackage input {@link WordprocessingMLPackage} object
     * @param type        type of footer
     * @return {@link FooterPart} object to be added to docx file
     * @throws org.docx4j.openpackaging.exceptions.InvalidFormatException
     */
    private FooterPart createFooterPart(WordprocessingMLPackage wordPackage, String type) throws org.docx4j.openpackaging.exceptions.InvalidFormatException {
        FooterPart footerPart = new FooterPart(new PartName("/word/" + type + ".xml"));
        wordPackage.getParts().put(footerPart);
        return footerPart;
    }

    /**
     * This function will set footer properties returns the {@link Ftr} object
     *
     * @return {@link Ftr} object to be added to {@link WordprocessingMLPackage} docx file
     */
    private Ftr getFtr() {
        ObjectFactory objectFactory = Context.getWmlObjectFactory();
        CTSimpleField pgnum = objectFactory.createCTSimpleField();
        pgnum.setInstr(" PAGE \\* MERGEFORMAT ");
        RPr RPr = objectFactory.createRPr();
        RPr.setNoProof(new BooleanDefaultTrue());
        PPr ppr = objectFactory.createPPr();
        Jc jc = objectFactory.createJc();
        jc.setVal(JcEnumeration.CENTER);
        ppr.setJc(jc);
        PPrBase.Spacing pprbase = objectFactory.createPPrBaseSpacing();
        pprbase.setBefore(BigInteger.valueOf(240));
        pprbase.setAfter(BigInteger.valueOf(0));
        ppr.setSpacing(pprbase);

        R run = objectFactory.createR();
        run.getContent().add(RPr);
        pgnum.getContent().add(run);
        JAXBElement<CTSimpleField> fldSimple = objectFactory.createPFldSimple(pgnum);
        P para = objectFactory.createP();
        para.getContent().add(fldSimple);
        para.setPPr(ppr);
        Ftr ftr = objectFactory.createFtr();
        ftr.getContent().add(para);
        return ftr;
    }

    /**
     * This function will add html text to the docx file
     *
     * @param htmlParaString sloppy html text received from client
     * @param wordPackage    input {@link WordprocessingMLPackage} object
     * @throws Docx4JException If an error occurs while converting or adding HTML content to the DOCX file.
     */
    private void addRichTextPara(String htmlParaString, WordprocessingMLPackage wordPackage) throws Exception {
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, Level.OFF));

        // Replace newlines with HTML line breaks
        htmlParaString = htmlParaString.replace("\n", "<br/>");
        log.info("addRichTextPara() - Original HTML: {}", htmlParaString);

        // Wrap to ensure valid XHTML
        htmlParaString = "<p>" + htmlParaString + "</p>";
        htmlParaString = sanitizeHtmlForXhtmlCompliance(htmlParaString);
        log.info("addRichTextPara() - Sanitized HTML: {}", htmlParaString);

        // If HTML contains bullet/numbered lists, set up numbering
        if (containsListTags(htmlParaString)) {
            setupBulletNumbering(wordPackage);
        }

        processContentInOrder(htmlParaString, wordPackage);
    }

    /**
     * Detects if HTML contains bullet or numbered list tags.
     * This method checks for both unordered lists (ul) and ordered lists (ol).
     *
     * @param html the HTML string to check
     * @return true if the HTML contains list tags, false otherwise
     */
    private boolean containsListTags(String html) {
        if (html == null) {
            return false;
        }
        return html.contains("<ul") || html.contains("<ol");
    }

    /**
     * Sanitizes HTML content to ensure XHTML compliance for proper processing.
     * This method fixes common HTML issues like unclosed br tags and img tags
     * to make the HTML compatible with XHTML parsers.
     *
     * @param html the HTML string to sanitize
     * @return sanitized HTML string that is XHTML compliant
     */
    private String sanitizeHtmlForXhtmlCompliance(String html) {
        return html
                .replaceAll("<br>", "<br/>")
                .replaceAll("</br>", "")
                .replaceAll("(?i)<img([^>]*)>", "<img$1/>");
    }

    private void setupBulletNumbering(WordprocessingMLPackage wordPackage) throws Exception {
        MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();

        NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
        mainDocumentPart.addTargetPart(ndp);

        String bulletXml =
                "<w:numbering xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                        "  <w:abstractNum w:abstractNumId=\"0\">" +
                        "    <w:multiLevelType w:val=\"singleLevel\"/>" +
                        "    <w:lvl w:ilvl=\"0\">" +
                        "      <w:start w:val=\"1\"/>" +
                        "      <w:numFmt w:val=\"bullet\"/>" +
                        "      <w:lvlText w:val=\"\u2022\"/>" +
                        "      <w:lvlJc w:val=\"left\"/>" +
                        "      <w:pPr><w:ind w:left=\"720\" w:hanging=\"180\"/></w:pPr>" +
                        "      <w:rPr><w:rFonts w:ascii=\"Symbol\" w:hAnsi=\"Symbol\"/></w:rPr>" +
                        "    </w:lvl>" +
                        "  </w:abstractNum>" +
                        "  <w:num w:numId=\"1\">" +
                        "    <w:abstractNumId w:val=\"0\"/>" +
                        "  </w:num>" +
                        "</w:numbering>";

        Numbering numbering = (Numbering) XmlUtils.unmarshalString(bulletXml);
        ndp.setJaxbElement(numbering);
    }

    /**
     * Processes HTML content in the order it appears, maintaining the original sequence
     * of text and bullet points. This method parses the HTML and handles different
     * element types appropriately - bullet lists are converted to Word bullet paragraphs
     * while other content is processed through the XHTML importer.
     *
     * @param htmlParaString the HTML content to process
     * @param wordPackage    the WordprocessingMLPackage to add content to
     * @throws Exception if an error occurs during HTML parsing or content conversion
     */
    private void processContentInOrder(String htmlParaString, WordprocessingMLPackage wordPackage) throws Exception {
        MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();
        ObjectFactory factory = Context.getWmlObjectFactory();

        // Parse HTML
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(htmlParaString);
        org.jsoup.nodes.Element body = doc.body();

        // Process all child elements in order
        for (org.jsoup.nodes.Node node : body.childNodes()) {
            processHtmlNode(node, mainDocumentPart, factory, wordPackage);
        }
    }

    /**
     * Processes a single HTML node and adds appropriate content to the Word document.
     *
     * @param node             the HTML node to process
     * @param mainDocumentPart the main document part to add content to
     * @param factory          the ObjectFactory for creating Word elements
     * @param wordPackage      the WordprocessingMLPackage for XHTML conversion
     * @throws Exception if an error occurs during node processing
     */
    private void processHtmlNode(org.jsoup.nodes.Node node, MainDocumentPart mainDocumentPart,
                                 ObjectFactory factory, WordprocessingMLPackage wordPackage) throws Exception {
        if (node instanceof org.jsoup.nodes.Element) {
            processHtmlElement((org.jsoup.nodes.Element) node, mainDocumentPart, factory, wordPackage);
        } else if (node instanceof org.jsoup.nodes.TextNode) {
            processTextNode((org.jsoup.nodes.TextNode) node, mainDocumentPart);
        }
    }

    /**
     * Processes HTML elements, handling lists and regular content differently.
     *
     * @param element          the HTML element to process
     * @param mainDocumentPart the main document part to add content to
     * @param factory          the ObjectFactory for creating Word elements
     * @param wordPackage      the WordprocessingMLPackage for XHTML conversion
     * @throws Exception if an error occurs during element processing
     */
    private void processHtmlElement(org.jsoup.nodes.Element element, MainDocumentPart mainDocumentPart,
                                    ObjectFactory factory, WordprocessingMLPackage wordPackage) throws Exception {
        if (isListElement(element)) {
            processListElement(element, mainDocumentPart, factory);
        } else {
            processRegularElement(element, mainDocumentPart, wordPackage);
        }
    }

    /**
     * Checks if the element is a list element (ul or ol).
     *
     * @param element the HTML element to check
     * @return true if the element is a list, false otherwise
     */
    private boolean isListElement(org.jsoup.nodes.Element element) {
        String tagName = element.tagName().toLowerCase();
        return "ul".equals(tagName) || "ol".equals(tagName);
    }

    /**
     * Processes list elements by converting each list item to a bullet paragraph.
     *
     * @param listElement      the list element (ul or ol) to process
     * @param mainDocumentPart the main document part to add content to
     * @param factory          the ObjectFactory for creating Word elements
     */
    private void processListElement(org.jsoup.nodes.Element listElement, MainDocumentPart mainDocumentPart,
                                    ObjectFactory factory) {
        log.info("processListElement() - Processing {} element", listElement.tagName());
        org.jsoup.select.Elements liElements = listElement.select("li");

        for (org.jsoup.nodes.Element li : liElements) {
            String bulletText = li.text().trim();
            if (!bulletText.isEmpty()) {
                log.debug("processListElement() - Adding bullet: {}", bulletText);
                P bulletParagraph = createBulletParagraph(factory, bulletText);
                mainDocumentPart.addObject(bulletParagraph);
            }
        }
    }

    /**
     * Processes regular HTML elements by converting them through XHTML importer.
     *
     * @param element          the HTML element to process
     * @param mainDocumentPart the main document part to add content to
     * @param wordPackage      the WordprocessingMLPackage for XHTML conversion
     * @throws Exception if an error occurs during XHTML conversion
     */
    private void processRegularElement(org.jsoup.nodes.Element element, MainDocumentPart mainDocumentPart,
                                       WordprocessingMLPackage wordPackage) throws Exception {
        String elementHtml = element.outerHtml();
        if (hasNonEmptyContent(elementHtml)) {
            log.debug("processRegularElement() - Adding regular content: {}", element.tagName());
            XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordPackage);
            mainDocumentPart.getContent().addAll(
                    XHTMLImporter.convert(elementHtml, null)
            );
        }
    }

    /**
     * Processes text nodes by creating simple paragraphs.
     *
     * @param textNode         the text node to process
     * @param mainDocumentPart the main document part to add content to
     */
    private void processTextNode(org.jsoup.nodes.TextNode textNode, MainDocumentPart mainDocumentPart) {
        String text = textNode.text().trim();
        if (!text.isEmpty()) {
            log.debug("processTextNode() - Adding text node: {}", text);
            P textParagraph = makeParagraph(text);
            mainDocumentPart.addObject(textParagraph);
        }
    }

    private static P createBulletParagraph(ObjectFactory factory, String text) {
        P p = factory.createP();
        R run = factory.createR();
        Text t = factory.createText();
        t.setValue(text);
        run.getContent().add(t);
        p.getContent().add(run);
        PPr ppr = factory.createPPr();
        PPrBase.NumPr numPr = factory.createPPrBaseNumPr();
        PPrBase.NumPr.NumId numId = factory.createPPrBaseNumPrNumId();
        numId.setVal(BigInteger.ONE); // Bullet list id from NumberingDefinitionsPart
        numPr.setNumId(numId);
        PPrBase.NumPr.Ilvl ilvl = factory.createPPrBaseNumPrIlvl();
        ilvl.setVal(BigInteger.ZERO);
        numPr.setIlvl(ilvl);
        ppr.setNumPr(numPr);
        p.setPPr(ppr);
        return p;
    }

    private boolean hasNonEmptyContent(String html) {
        if (html == null || html.trim().isEmpty()) {
            return false;
        }

        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
        String textContent = doc.text().trim();
        return !textContent.isEmpty();
    }

    /**
     * This function return pdf {@link File} for the given {@link List}<{@link Map}> data
     *
     * @param title       Title of the pdf document
     * @param subTitle    subtitle for the pdf document
     * @param description description for the pdf document
     * @param header      header of the pdf document
     * @param footer      footer of the pdf document
     * @param logo        logo {@link File} for the pdf document
     * @param tableData   {@link List} of {@link Map} data
     * @return tabular form pdf {@link File} for the given {@link Map} data
     * @throws Exception If an error occurs during PDF generation.
     */
    public File getTableDataPdf(String title, String subTitle, String description, String header, String footer, File logo, List<Map<String, Object>> tableData) throws Exception {
        WordprocessingMLPackage wordPackage = addTemplateData(title, subTitle, description, header, footer, logo);
        if (tableData != null && !tableData.isEmpty()) {
            addTableData(tableData, wordPackage, null);  // Use server timezone
        }
        return getJavaPdfFile(wordPackage);
    }

    /**
     * This function will return pdf {@link File} for the given {@link List}<{@link EntityValue}>
     *
     * @param title        Title of the pdf document
     * @param subTitle     subtitle for the pdf document
     * @param description  description for the pdf document
     * @param header       header of the pdf document
     * @param footer       footer of the pdf document
     * @param logo         logo {@link File} for the pdf document
     * @param entityValues {@link List<EntityValue>} from the {@link EntityDefinition}
     * @return tabular form pdf {@link File} for the given {@link EntityValue} data
     * @throws Exception If an error occurs during PDF generation.
     */
    public File getEntityListPdf(String title, String subTitle, String description, String header, String footer, File logo, List<EntityValue> entityValues) throws Exception {

        WordprocessingMLPackage wordPackage = addTemplateData(title, subTitle, description, header, footer, logo);

        if (entityValues != null && !entityValues.isEmpty()) {
            List<Map<String, Object>> entitiesData = PDFHelper.getEntityDataAsList(entityValues);
            for (Map<String, Object> entityMap : entitiesData) {
                entityMap.remove("primaryKey");  // remove key before generating pdf
            }
            addTableData(entitiesData, wordPackage, null);  // Use server timezone
        }
        return getJavaPdfFile(wordPackage);
    }

    /**
     * This function will return pdf {@link File} for the given {@link List}<{@link EntityValue}> with timezone support
     *
     * @param title        Title of the pdf document
     * @param subTitle     subtitle for the pdf document
     * @param description  description for the pdf document
     * @param header       header of the pdf document
     * @param footer       footer of the pdf document
     * @param logo         logo {@link File} for the pdf document
     * @param entityValues {@link List<EntityValue>} from the {@link EntityDefinition}
     * @param timezone     Timezone for date formatting (e.g., "America/New_York", "Asia/Kolkata", "UTC"). If null, uses server timezone.
     * @return tabular form pdf {@link File} for the given {@link EntityValue} data
     * @throws Exception If an error occurs during PDF generation.
     */
    public File getEntityListPdf(String title, String subTitle, String description, String header, String footer, File logo, List<EntityValue> entityValues, String timezone) throws Exception {

        WordprocessingMLPackage wordPackage = addTemplateData(title, subTitle, description, header, footer, logo);

        if (entityValues != null && !entityValues.isEmpty()) {
            List<Map<String, Object>> entitiesData = PDFHelper.getEntityDataAsList(entityValues);
            for (Map<String, Object> entityMap : entitiesData) {
                entityMap.remove("primaryKey");  // remove key before generating pdf
            }
            addTableData(entitiesData, wordPackage, timezone);
        }
        return getJavaPdfFile(wordPackage);
    }

    /**
     * This function will add tabular form of {@link List}<{@link Map> data to docx file
     *
     * @param entitiesData input data in the form of {@link List}<{@link Map}>
     * @param wordPackage  {@link WordprocessingMLPackage} of the docx file
     * @param timezone     Timezone for date formatting (e.g., "America/New_York", "Asia/Kolkata", "UTC"). If null, uses server timezone.
     */
    private void addTableData(List<Map<String, Object>> entitiesData, WordprocessingMLPackage wordPackage, String timezone) {
        Tbl table = createTable(entitiesData, wordPackage);
        Tr headerRow = (Tr) table.getContent().get(0);
        List<Object> headerCells = headerRow.getContent();
        List<String> headerKeys = new ArrayList<>(entitiesData.get(0).keySet());
        for (int hdrCell = 0; hdrCell < headerKeys.size(); hdrCell++) {
            Tc td = (Tc) headerCells.get(hdrCell);
            addDataToCell(headerKeys.get(hdrCell), td);
        }
        List<Object> rows = table.getContent();
        for (int i = 0; i < entitiesData.size(); i++) {
            Map<String, Object> rowData = entitiesData.get(i);
            Tr tr = (Tr) rows.get(i + 1);
            List<Object> cells = tr.getContent();
            for (int d = 0; d < cells.size(); d++) {
                Tc td = (Tc) cells.get(d);
                String fieldName = headerKeys.get(d);
                String cellValue = formatCellValue(rowData.get(fieldName), timezone, fieldName);
                addDataToCell(cellValue, td);
            }
        }
        wordPackage.getMainDocumentPart().addObject(table);
    }

    /**
     * This function will add data to table cell
     *
     * @param keyValue text value to be added cell
     * @param td       to add cell data for object {@link Tc}
     */
    private void addDataToCell(String keyValue, Tc td) {
        ObjectFactory factory = Context.getWmlObjectFactory();
        P cellData = getDataToAdd(keyValue, factory);
        td.getContent().add(cellData);
    }

    /**
     * Formats a cell value based on field name and value type, with support for:
     * <ul>
     *   <li>Percentage formatting (field names containing "percent" or "percentage")</li>
     *   <li>Time-only formatting (field names containing "time" but not "date")</li>
     *   <li>Date-only formatting (field names explicitly marked as date-only)</li>
     *   <li>DateTime formatting (default for all timestamp fields)</li>
     * </ul>
     *
     * <p><b>Timestamp Detection:</b></p>
     * Only numeric values in the range [{@link #TIMESTAMP_MIN_VALUE}, {@link #TIMESTAMP_MAX_VALUE}]
     * (Jan 1, 2000 to Jan 1, 2100) are treated as timestamps. This avoids false positives with
     * small numbers like 1, 2, 100.
     *
     * <p><b>Format Patterns:</b></p>
     * <ul>
     *   <li>Time fields (contains "time" but not "date"): "hh:mm a" (e.g., "10:42 AM")</li>
     *   <li>Date-only fields (contains "dateonly" or "date_only"): "MMM dd, yyyy" (e.g., "Jan 01, 2000")</li>
     *   <li>DateTime fields (default): "MMM dd, yyyy hh:mm a" (e.g., "Jan 01, 2000 10:42 AM")</li>
     *   <li>Percentage fields: "XX%" (e.g., "80%")</li>
     * </ul>
     *
     * <p><b>Examples:</b></p>
     * <pre>
     * formatCellValue(0.8, null, "Percentage1")        → "80%"
     * formatCellValue(946684920000L, "UTC", "Time1")   → "10:42 AM"
     * formatCellValue(946684920000L, "UTC", "DateOnly1") → "Jan 01, 2000"
     * formatCellValue(946684920000L, "UTC", "Date1")   → "Jan 01, 2000 10:42 AM"
     * formatCellValue(946684920000L, "UTC", "Created") → "Jan 01, 2000 10:42 AM"
     * formatCellValue(1, null, "Position")             → "1"
     * formatCellValue(null, null, "Field")             → ""
     * </pre>
     *
     * @param value     The value to format. Can be Number (for timestamps/percentages), String, or any other type.
     *                  Null values return empty string.
     * @param timezone  Timezone for date formatting (e.g., "America/New_York", "Asia/Kolkata", "UTC").
     *                  If null or blank, uses server's default timezone.
     *                  Invalid timezones fall back to server timezone with a warning log.
     * @param fieldName The name of the field, used for type detection (case-insensitive):
     *                  <ul>
     *                    <li>Contains "percent" or "percentage" → percentage formatting</li>
     *                    <li>Contains "time" (not "date") → time-only formatting</li>
     *                    <li>Contains "dateonly" or "date_only" → date-only formatting</li>
     *                    <li>Otherwise (including regular "date" fields) → full datetime formatting</li>
     *                  </ul>
     * @return Formatted string representation of the value. Never returns null.
     */
    String formatCellValue(Object value, String timezone, String fieldName) {
        if (value == null) {
            return "";
        }

        // Check if the value is a number
        if (value instanceof Number) {
            double doubleValue = ((Number) value).doubleValue();
            long longValue = ((Number) value).longValue();

            // Calculate lowercase field name once for efficiency
            String fieldNameLower = fieldName != null ? fieldName.toLowerCase() : "";

            // Check if this is a percentage field (field name contains "percent" or "percentage")
            if (fieldNameLower.contains("percent") || fieldNameLower.contains("percentage")) {
                // Format as percentage (multiply by 100 and add % sign)
                int percentValue = (int) Math.round(doubleValue * 100);
                return percentValue + "%";
            }

            // Check if the value is in a valid timestamp range
            // Timestamps are in milliseconds since epoch
            // We use a more realistic range to avoid treating small numbers as timestamps
            if (longValue >= TIMESTAMP_MIN_VALUE && longValue <= TIMESTAMP_MAX_VALUE) {
                try {
                    // Convert timestamp to Instant
                    Instant instant = Instant.ofEpochMilli(longValue);

                    // Determine the timezone to use
                    ZoneId zoneId;
                    if (timezone != null && !timezone.isBlank()) {
                        try {
                            zoneId = ZoneId.of(timezone);
                            log.debug("Formatting timestamp {} with user timezone: {}", longValue, timezone);
                        } catch (DateTimeException e) {
                            log.warn("Invalid timezone '{}', falling back to server timezone: {}", timezone, e.getMessage());
                            zoneId = ZoneId.systemDefault();
                        } catch (Exception e) {
                            log.error("Unexpected error parsing timezone '{}': {}", timezone, e.getMessage(), e);
                            zoneId = ZoneId.systemDefault();
                        }
                    } else {
                        log.debug("No timezone provided, using server timezone: {}", ZoneId.systemDefault());
                        zoneId = ZoneId.systemDefault();
                    }

                    // Convert to user's timezone
                    ZonedDateTime zonedDateTime = instant.atZone(zoneId);

                    // Determine format based on field name (fieldNameLower already calculated above)
                    DateTimeFormatter formatter;

                    if (fieldNameLower.contains("time") && !fieldNameLower.contains("date")) {
                        // Time field: Show only time with AM/PM (e.g., "10:42 AM")
                        formatter = DateTimeFormatter.ofPattern("hh:mm a");
                    } else if (fieldNameLower.contains("dateonly") || fieldNameLower.contains("date_only")) {
                        // Only applies to fields explicitly marked as date-only
                        formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                    } else {
                        // DateTime field (default for all other cases including regular "date" fields)
                        formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
                    }

                    String formattedDate = zonedDateTime.format(formatter);
                    log.debug("Formatted timestamp {} to: {}", longValue, formattedDate);
                    return formattedDate;

                } catch (Exception e) {
                    log.debug("Value {} is not a valid timestamp, treating as regular number: {}", longValue, e.getMessage());
                    return String.valueOf(value);
                }
            }
        }

        // For non-date values, return string representation
        return String.valueOf(value);
    }


    /**
     * This function will create a {@link Tbl} object to be added to {@link WordprocessingMLPackage}
     *
     * @param entitiesData input data for the table to be created to get number of rows and columns
     * @param wordPackage  {@link WordprocessingMLPackage} object to get page dimensions
     * @return {@link Tbl} object of the {@link WordprocessingMLPackage} package
     */
    private Tbl createTable(List<Map<String, Object>> entitiesData, WordprocessingMLPackage wordPackage) {
        int tableRows = entitiesData.size() + 1;
        int tableCols = entitiesData.get(0).size();
        int writableWidthTwips = wordPackage.getDocumentModel().getSections().get(0).getPageDimensions().getWritableWidthTwips();
        int cellWidthTwips = writableWidthTwips / tableCols;
        return TblFactory.createTable(tableRows, tableCols, cellWidthTwips);
    }

    /**
     * This function will merge the given list of pdf {@link File}
     *
     * @param title       Title of the pdf document
     * @param subTitle    subtitle for the pdf document
     * @param description description for the pdf document
     * @param header      header of the pdf document
     * @param footer      footer of the pdf document
     * @param logo        logo {@link File} for the pdf document
     * @param mergeFiles  {@link List<File>} input pdf files to be merged
     * @return single pdf {@link File}
     * @throws Exception If an error occurs during merging.
     */
    public File mergePdfFiles(String title, String subTitle, String description, String header, String footer, File logo, List<File> mergeFiles) throws Exception {
        WordprocessingMLPackage wordPackage = addTemplateData(title, subTitle, description, header, footer, logo);
        File templatePdfFile = getJavaPdfFile(wordPackage);
        if (mergeFiles != null && !mergeFiles.isEmpty()) {
            return addMergeFilesToDoc(mergeFiles, templatePdfFile);
        }
        return templatePdfFile;
    }

    /**
     * This function will do the merge operation on the list of pdf files.
     *
     * @param mergeFiles      input {@link List}<{@link File}>
     * @param templatePdfFile template format pdf {@link File}
     * @return merged pdf {@link File}
     * @throws IOException If an I/O error occurs.
     */
    private File addMergeFilesToDoc(List<File> mergeFiles, File templatePdfFile) throws IOException {
        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        String pdfFile = generatePdfFilePath(templatePdfFile.getName());
        pdfMergerUtility.setDestinationFileName(pdfFile);
        InputStream templateInputStream = new FileInputStream(templatePdfFile);
        List<InputStream> sourcesList = new ArrayList<>();
        sourcesList.add(templateInputStream);
        for (File mergeFile : mergeFiles) {
            InputStream mergeFileInputStream = new FileInputStream(mergeFile);
            sourcesList.add(mergeFileInputStream);
        }
        pdfMergerUtility.addSources(sourcesList);
        pdfMergerUtility.mergeDocuments();
        for (InputStream inputStream : sourcesList) {
            inputStream.close();
        }
        return new File(pdfFile);
    }

    public List<File> splitPDF(File pdfFile) throws IOException {
        List<File> outputFiles = new ArrayList<>();

        PDDocument document = PDDocument.load(pdfFile);
        int numberOfPages = document.getNumberOfPages();

        // Extract base name (e.g., "abc" from "abc.pdf")
        String baseName = pdfFile.getName().replaceFirst("[.][^.]+$", "");

        // Capture epoch timestamp in milliseconds for uniqueness
        long epochMillis = System.currentTimeMillis();

        for (int i = 0; i < numberOfPages; i++) {
            PDDocument newDocument = new PDDocument();
            PDPage page = document.getPage(i);
            // Use importPage to properly copy page with all resources
            newDocument.importPage(page);

            // Build a unique output file name
            String outputFileName = String.format("%s_%d_page_%d.pdf", baseName, epochMillis, i + 1);
            File outputFile = new File(outputFileName);

            newDocument.save(outputFile);
            outputFiles.add(outputFile);
            newDocument.close();
        }

        document.close();
        return outputFiles;
    }


    public List<File> splitPDFIntoParts(File pdfFile, int pageThreshold) throws IOException {
        List<File> outputFiles = new ArrayList<>();

        // Load the PDF document
        PDDocument document = PDDocument.load(pdfFile);
        int numberOfPages = document.getNumberOfPages();

        // Check if the number of pages is greater than the threshold
        if (numberOfPages > pageThreshold) {
            // Determine the middle page index
            int middle = numberOfPages / 2;

            // First half of the document
            PDDocument firstHalfDocument = new PDDocument();
            for (int i = 0; i < middle; i++) {
                PDPage page = document.getPage(i);
                // Use importPage to properly copy page with all resources
                firstHalfDocument.importPage(page);
            }
            File firstHalfFile = new File(FIRST_HALF);
            firstHalfDocument.save(firstHalfFile);
            outputFiles.add(firstHalfFile);
            firstHalfDocument.close();

            // Second half of the document
            PDDocument secondHalfDocument = new PDDocument();
            for (int i = middle; i < numberOfPages; i++) {
                PDPage page = document.getPage(i);
                // Use importPage to properly copy page with all resources
                secondHalfDocument.importPage(page);
            }
            File secondHalfFile = new File(SECOND_HALF);
            secondHalfDocument.save(secondHalfFile);
            outputFiles.add(secondHalfFile);
            secondHalfDocument.close();

        } else {
            // If the number of pages is less than or equal to the threshold, return the whole file
            outputFiles.add(pdfFile);
        }

        // Close the original document
        document.close();

        return outputFiles;
    }

    /**
     * Splits a PDF file at a specific page number into two files.
     *
     * <p>For example, a 10-page PDF with splitAtPage=6 will produce:</p>
     * <ul>
     *   <li>File 1: Pages 1-5</li>
     *   <li>File 2: Pages 6-10</li>
     * </ul>
     *
     * <p><b>Improvements:</b></p>
     * <ul>
     *   <li>Try-with-resources for automatic resource management</li>
     *   <li>Early validation before loading PDF</li>
     *   <li>Extracted helper methods for better readability</li>
     *   <li>Proper exception handling with resource cleanup</li>
     * </ul>
     *
     * @param pdfFile     The PDF file to split
     * @param splitAtPage The page number where the split should occur (1-based index).
     *                    This page will be the first page of the second file.
     * @return A list containing exactly two PDF files (first part and second part)
     * @throws IOException              If an error occurs during PDF processing
     * @throws IllegalArgumentException If splitAtPage is invalid (less than 2 or greater than total pages)
     */
    public List<File> splitPDFAtPageNumber(File pdfFile, int splitAtPage) throws IOException {
        return pdfSplitter.splitAtPageNumber(pdfFile, splitAtPage);
    }
}
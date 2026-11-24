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

import app.krista.extensions.essentials.documents.pdf.util.PDFHelper;
import app.krista.model.base.FreeForm;
import org.apache.poi.xwpf.usermodel.*;
import org.jvnet.hk2.annotations.Service;

import java.io.*;

@Service
public class DocxManager {

    /**
     * This function will perform replace operations on the {@link XWPFParagraph} paragraph object
     *
     * @param findText    String that needs to be replaced
     * @param replaceText String Value that gets replaced with {@param findText}
     * @param split       List fo String to perform Replace operations
     * @param paragraph   {@link XWPFParagraph} object where the replace operations take place
     */
    private static void performReplaceOperationOnParagraph(String findText, String replaceText, String[] split, XWPFParagraph paragraph) {
        String text = paragraph.getText();

        if (text != null && !text.isBlank() && text.contains(findText)) {
            text = text.replace(findText, replaceText);
            XWPFRun run = paragraph.getRuns().getFirst();
            if (split.length > 1) {
                for (String str : split) {
                    run.addCarriageReturn();
                    run.setText(str);
                }
            } else {
                run.setText(text, 0);
            }
            for (int i = split.length > 1 ? 0 : 1; i < paragraph.getRuns().size(); i++) {
                run = paragraph.getRuns().get(i);
                run.setText("", 0);
            }
        }
    }

    /**
     * This function will set the region of text for the given paragraph
     *
     * @param paragraph {@link XWPFParagraph} object to perform replace operations
     * @param text      String Value that needs to be replaced
     */
    private static void updateRuns(XWPFParagraph paragraph, String text) {
        for (int i = 0; i < paragraph.getRuns().size(); i++) {
            XWPFRun run = paragraph.getRuns().get(i);
            if (i == 0) {
                run.setText(text, 0);
            } else {
                run.setText("", 0);
            }
        }
    }

    /**
     * This function will perform conversion of docx file to pdf file
     *
     * @param pdfFilePath path of pdf file to be generated
     * @param doc         {@link XWPFDocument} object of the input file
     * @return pdf {@link File} for the docx file
     * @throws IOException If an I/O error occurs.
     */
    private static File getConvertedPdfFile(String pdfFilePath, XWPFDocument doc) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        doc.write(byteArrayOutputStream); // doc should be a XWPFDocument
        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return PDFManager.getPdfForXWPFDoc(inputStream, pdfFilePath);
    }

    /**
     * This function will get the pdf file for the given template format docx file
     *
     * @param file           input template docx {@link File} to fill
     * @param templateFields template data to fill
     * @return filled docx {@link File}
     */
    public File fillTheDocTemplate(File file, FreeForm templateFields) {
        if (templateFields == null || templateFields.isEmpty()) {
            throw new IllegalStateException("Please provide input for template fields.");
        }
        FreeForm newTemplateFields = PDFHelper.getValidatedFreeFormData(templateFields);
        String pdfFilePath = PDFHelper.generatePdfFilePath(file.getName());
        try (InputStream fileInputStream = new FileInputStream(file)) {
            return getJavaDocFile(fileInputStream, newTemplateFields, pdfFilePath);
        } catch (IOException cause) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Failed to generate PDF ").append(cause.getMessage());
            throw new IllegalStateException(errorMessage.toString(), cause);
        }
    }

    /**
     * This function will return a converted pdf {@link File} for the given document with key value replacement
     *
     * @param fileInputStream given docx file {@link InputStream} object
     * @param keywordMapping  input for key value replacement
     * @param pdfFilePath     path for the pdf file to be generated
     * @return a pdf format {@link File}
     * @throws IOException If an I/O error occurs.
     */
    private File getJavaDocFile(InputStream fileInputStream, FreeForm keywordMapping, String pdfFilePath) throws IOException {

        if (fileInputStream == null) {
            throw new IllegalStateException("Given file is empty.");
        }
        XWPFDocument doc = new XWPFDocument(fileInputStream);
        replaceFreeFormData(keywordMapping, doc);
        replaceTextForFooters(keywordMapping, doc);
        replaceTextForHeaders(keywordMapping, doc);
        return getConvertedPdfFile(pdfFilePath, doc);
    }

    /**
     * This function will replace header text for all headers in the given docx file
     *
     * @param header         {@link XWPFHeader} object to perform replace operation
     * @param keyToReplace   header key to get replaced
     * @param valueToReplace value to be replaced with {@param keyToReplace}
     */
    private void replaceHeaderText(XWPFHeader header, String keyToReplace, String valueToReplace) {
        String[] split = valueToReplace.split("\n");
        for (XWPFParagraph paragraph : header.getParagraphs()) {
            performReplaceOperationOnParagraph(keyToReplace, valueToReplace, split, paragraph);
        }
    }

    /**
     * This function will replace footer text for all footer sections in the given docx file
     *
     * @param keywordMapping key value pairs to be replaced for all {@link XWPFFooter} objects
     * @param doc            {@link XWPFDocument} object of the given input docx file
     */
    private void replaceTextForFooters(FreeForm keywordMapping, XWPFDocument doc) {
        for (XWPFFooter footer : doc.getFooterList()) {
            for (String key : keywordMapping.keySet()) {
                replaceTextForFooters(footer, key, String.valueOf(keywordMapping.get(key)));
            }
        }
    }

    /**
     * This function will perform replace operation on the given {@link XWPFFooter} object
     *
     * @param footer         {@link XWPFFooter} object to perform replace operation
     * @param keyToReplace   footer key to get replaced
     * @param valueToReplace footer value to be replaced with {@param keyToReplace}
     */
    private void replaceTextForFooters(XWPFFooter footer, String keyToReplace, String valueToReplace) {

        String[] split = valueToReplace.split("\n");
        for (XWPFParagraph paragraph : footer.getParagraphs()) {
            performReplaceOperationOnParagraph(keyToReplace, valueToReplace, split, paragraph);
        }
    }

    /**
     * This function will perform replace operation on the given {@link XWPFHeader} object
     *
     * @param keywordMapping key value pairs to be replaced for all {@link XWPFHeader} objects
     * @param doc            {@link XWPFDocument} object of the given input docx file
     */
    private void replaceTextForHeaders(FreeForm keywordMapping, XWPFDocument doc) {
        for (XWPFHeader header : doc.getHeaderList()) {
            for (String key : keywordMapping.keySet()) {
                replaceHeaderText(header, key, String.valueOf(keywordMapping.get(key)));
            }
        }
    }

    /**
     * This function will perform replace text operation on {@link XWPFDocument} of each input key
     *
     * @param keywordMapping key value pairs to be replaced for all variables in docx file
     * @param doc            {@link XWPFDocument} object of the given input docx file
     */
    private void replaceFreeFormData(FreeForm keywordMapping, XWPFDocument doc) {
        for (String key : keywordMapping.keySet()) {
            String valueToReplace = String.valueOf(keywordMapping.get(key));
            replaceText(doc, key, valueToReplace);
            replaceTableTextFor(doc, key, valueToReplace);
        }
    }


    /**
     * This function will perform replace operation on all paragraphs of the given {@link XWPFDocument} object
     *
     * @param doc            {@link XWPFDocument} object of the given input docx file
     * @param keyToReplace   key to replace in the {@link XWPFParagraph} objects
     * @param valueToReplace value to be replaced with {@param keyToReplace}
     */
    private void replaceText(XWPFDocument doc, String keyToReplace, String valueToReplace) {

        String[] split = valueToReplace.split("\n");
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            performReplaceOperationOnParagraph(keyToReplace, valueToReplace, split, paragraph);
        }
    }


    /**
     * This function will perform Replace operations on the Tables of the given docx file.
     *
     * @param doc         {@link XWPFDocument} for the given docx file
     * @param findText    text to replace
     * @param replaceText text to be replaced with {@param findText}
     */
    private void replaceTableTextFor(XWPFDocument doc, String findText, String replaceText) {
        for (XWPFTable tbl : doc.getTables()) {
            performOperationOnTable(findText, replaceText, tbl);
        }
    }

    /**
     * This function will perform Replace operation on {@link XWPFTable} object
     *
     * @param findText    key to replace in the table
     * @param replaceText value to be replaced with {@param findText} in the table
     * @param tbl         {@link XWPFTable} objects in the given docx file
     */
    private void performOperationOnTable(String findText, String replaceText, XWPFTable tbl) {
        for (XWPFTableRow row : tbl.getRows()) {
            performOperationOnRow(findText, replaceText, row);
        }
    }

    /**
     * This function will perform replace operation on all cells in {@link XWPFTableRow} object.
     *
     * @param findText    key to replace in the table
     * @param replaceText value to be replaced with {@param findText} in the table
     * @param row         {@link XWPFTableRow} object in the {@link XWPFTable} for the given docx file
     */
    private void performOperationOnRow(String findText, String replaceText, XWPFTableRow row) {
        for (XWPFTableCell cell : row.getTableCells()) {
            performOperationOnCell(findText, replaceText, cell);
        }
    }

    /**
     * This function will perform text replace operations on the {@link XWPFTableCell} of each row in the table.
     *
     * @param findText    text to replace
     * @param replaceText text to be replaced with {@param findText}
     * @param cell        {@link XWPFTableCell} object to perform replace operation
     */
    private void performOperationOnCell(String findText, String replaceText, XWPFTableCell cell) {
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            performReplaceOperationOnTableParagraph(findText, replaceText, paragraph);
        }
    }

    /**
     * This function will perform text replace operation with the given text.
     *
     * @param findText    text to replace
     * @param replaceText text to be replaced with {@param findText}
     * @param paragraph   {@link XWPFParagraph} object to perform replace operation
     */
    private void performReplaceOperationOnTableParagraph(String findText, String replaceText, XWPFParagraph paragraph) {
        String text = paragraph.getText();
        if (text != null && text.trim().equalsIgnoreCase(findText)) {
            text = text.replace(findText, replaceText);
        }
        if (text != null && !text.isBlank()) {
            updateRuns(paragraph, text);
        }
    }
}

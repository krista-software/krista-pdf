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

import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.util.Arrays;
import java.util.stream.StreamSupport;

@Service
public class ExcelConverterService {

    private static final Logger log = LoggerFactory.getLogger(ExcelConverterService.class);

    private static final Font TABLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED, 8);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED, 14, Font.BOLD);
    private static final float MIN_COLUMN_WIDTH_PERCENTAGE = 10.0f;
    private static final Rectangle PAGE_SIZE = PageSize.A4;

    private final FileRepository fileRepository;

    @Inject
    public ExcelConverterService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public File convertExcelFileWithMultipleSheetsToPDF(File excelFile) {
        final var excelFileName = excelFile.getFileName();
        final var excelFileExtension = excelFile.getExtension();
        final var outputFileName = excelFileName.replace(excelFileExtension, "pdf");

        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

        try (final var excelFileIS = fileRepository.getFile(excelFile).getContent();
             XSSFWorkbook workbook = new XSSFWorkbook(excelFileIS)) {

            Document document = new Document();
            PdfWriter.getInstance(document, pdfOutputStream);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            document.setPageSize(PAGE_SIZE);
            document.open();

            final var sheetIterator = workbook.sheetIterator();
            while (sheetIterator.hasNext()) {
                Sheet sheet = sheetIterator.next();
                log.info("Parsing sheet {}...", sheet.getSheetName());

                addSheetTitle(document, sheet.getSheetName());

                PdfPTable table = sheetToPdfTable(sheet, evaluator, document);
                var columnWidths = calculateColumnWidths(table);
                table.setWidths(columnWidths);
                document.add(table);

                document.newPage();
            }
            document.close();
            log.info("All sheets parsed successfully");

            log.info("Creating PDF file...");
            try (final var pdfFile = fileRepository.createNewFileByName(outputFileName)) {
                pdfFile.setContent(new ByteArrayInputStream(pdfOutputStream.toByteArray()));

                return pdfFile.getFile();
            }
        } catch (IOException | DocumentException e) {
            log.error("Error converting excel file to PDF", e);
            throw new RuntimeException("Error converting excel file to PDF", e);
        }
    }

    private PdfPTable sheetToPdfTable(Sheet sheet, FormulaEvaluator evaluator, Document document) {
        int numOfColumns = calculateMaxNumOfColumns(sheet, evaluator);

        PdfPTable table = new PdfPTable(numOfColumns);
        table.setTotalWidth(PAGE_SIZE.getWidth() - document.leftMargin() - document.rightMargin());
        table.setLockedWidth(true);
        for (Row row : sheet) {
            for (int i = 0; i < numOfColumns; i++) {
                Cell cell = row.getCell(i);
                String cellValue = getCellValue(cell, evaluator);
                table.addCell(new Phrase(cellValue, TABLE_FONT));
            }
        }

        return table;
    }

    private Integer calculateMaxNumOfColumns(Sheet sheet, FormulaEvaluator evaluator) {
        return StreamSupport.stream(sheet.spliterator(), false)
                .map(row -> {
                    final var lastCellIndex = row.getLastCellNum() - 1;
                    final var lastCellValue = getCellValue(row.getCell(lastCellIndex), evaluator);

                    // If the last cell is blank, find the last non-blank cell
                    if (lastCellValue.isBlank()) {
                        for (int i = lastCellIndex - 1; i >= 0; i--) {
                            if (!getCellValue(row.getCell(i), evaluator).isBlank()) {
                                return i + 1;
                            }
                        }
                        return 1; // If all cells are blank, return 1
                    }

                    return (int) row.getLastCellNum();
                })
                .max(Integer::compareTo)
                .orElse(1);
    }

    private float[] calculateColumnWidths(PdfPTable table) {
        int columnCount = table.getNumberOfColumns();
        Float[] columnWidths = new Float[columnCount];
        float minWidthIndex = MIN_COLUMN_WIDTH_PERCENTAGE / 100.0f;

        // Get the maximum width of the content in each column
        for (int col = 0; col < columnCount; col++) {
            float maxWidth = 0;

            for (var row : table.getRows()) {
                final var cell = row.getCells()[col];
                if (cell != null && cell.getPhrase() != null) {
                    String text = cell.getPhrase().getContent();
                    float textWidth = TABLE_FONT.getBaseFont().getWidthPoint(text, TABLE_FONT.getSize());
                    maxWidth = Math.max(maxWidth, textWidth);
                }
            }
            columnWidths[col] = maxWidth;
        }

        // Calculate total columns width
        float totalColumnsWidth = Arrays.stream(columnWidths).reduce(0f, Float::sum);

        // Validate if the column width is less than the minimum width
        float minColumnWith = totalColumnsWidth * minWidthIndex;
        for (int i = 0; i < columnWidths.length; i++) {
            if (columnWidths[i] < minColumnWith) {
                columnWidths[i] = minColumnWith;
            }
        }

        // Recalculate total columns width as minimum width has been adjusted
        float totalNormalizedColumnsWidth = Arrays.stream(columnWidths).reduce(0f, Float::sum);

        // Assign the adjusted column widths as percentage
        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] = (columnWidths[i] / totalNormalizedColumnsWidth) * 100;
        }

        return ArrayUtils.toPrimitive(columnWidths);
    }

    private String getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return Double.toString(cell.getNumericCellValue());
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return getFormulaResult(cell, evaluator);
            default:
                return "";
        }
    }

    private String getFormulaResult(Cell cell, FormulaEvaluator evaluator) {
        CellType resultType = evaluator.evaluateFormulaCell(cell);
        switch (resultType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return Double.toString(cell.getNumericCellValue());
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private void addSheetTitle(Document document, String sheetName) throws DocumentException {

        Paragraph sheetNameParagraph = new Paragraph(sheetName, TITLE_FONT);
        sheetNameParagraph.setSpacingAfter(10f);
        document.add(sheetNameParagraph);
    }
}

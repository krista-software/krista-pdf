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

import app.krista.extensions.essentials.documents.pdf.service.PDFManager;
import app.krista.extensions.essentials.documents.pdf.util.KristaMediaClientUtil;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Helper class for generating PDF from table data.
 * Provides centralized functionality for converting tabular data to PDF format.
 */
@Service
public class TableDataPdfHelper {

    private static final Logger logger = LoggerFactory.getLogger(TableDataPdfHelper.class);
    
    private final PDFManager pdfManager;
    private final KristaMediaClientUtil kristaMediaClientUtil;
    private final TelemetryHelper telemetryHelper;

    /**
     * Constructor for TableDataPdfHelper.
     */
    @Inject
    public TableDataPdfHelper(
            PDFManager pdfManager,
            KristaMediaClientUtil kristaMediaClientUtil,
            TelemetryHelper telemetryHelper) {
        this.pdfManager = pdfManager;
        this.kristaMediaClientUtil = kristaMediaClientUtil;
        this.telemetryHelper = telemetryHelper;
        
        logger.info("TableDataPdfHelper initialized successfully");
    }

    /**
     * Generates a PDF from table data.
     * This method takes tabular data along with optional formatting parameters
     * like title, subtitle, description, header, footer, and logo to create a professionally
     * formatted PDF document containing the table.
     *
     * @param title the title of the PDF
     * @param subTitle the subtitle of the PDF
     * @param description the description of the PDF
     * @param header the header text for the PDF
     * @param footer the footer text for the PDF
     * @param logo the logo file for the PDF
     * @param tableData the table data to include in the PDF
     * @return File containing the generated PDF or throws Exception if generation fails
     */
    public File getTableDataPDF(
            String title, 
            String subTitle, 
            String description, 
            String header, 
            String footer, 
            File logo, 
            List<Map<String, Object>> tableData) throws Exception {
        
        long startTime = System.currentTimeMillis();
        try {
            logger.info("TableDataPdfHelper.getTableDataPDF() called with title={}, tableData size={}",
                    title, tableData != null ? tableData.size() : 0);
            telemetryHelper.incrementCount("pdf.getTableDataPDF");

            if (tableData == null || tableData.isEmpty()) {
                throw new IllegalArgumentException("Please provide table data");
            }
            
            java.io.File tableDataPdf = pdfManager.getTableDataPdf(
                    title, 
                    subTitle, 
                    description, 
                    header, 
                    footer, 
                    kristaMediaClientUtil.downloadFileHandler(logo), 
                    tableData);
            
            File result = kristaMediaClientUtil.uploadFileHandler(tableDataPdf);
            
            telemetryHelper.recordSuccess("pdf.getTableDataPDF", startTime,
                    TelemetryHelper.safeTagMap("title", title != null ? title : "N/A",
                            "row_count", String.valueOf(tableData.size())));

            return result;
        } catch (Exception e) {
            telemetryHelper.recordError("pdf.getTableDataPDF", startTime, e,
                    TelemetryHelper.safeTagMap("title", title != null ? title : "N/A",
                            "row_count", tableData != null ? String.valueOf(tableData.size()) : "0"));
            logger.error("Error generating table PDF: {}", e.getMessage(), e);
            throw e;
        }
    }
}
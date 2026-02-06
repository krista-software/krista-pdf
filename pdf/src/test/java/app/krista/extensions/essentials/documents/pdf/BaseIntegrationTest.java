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

package app.krista.extensions.essentials.documents.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for PDF extension integration tests.
 * Provides common test utilities, fixtures, and cleanup logic.
 */
public abstract class BaseIntegrationTest {

    @TempDir
    protected Path tempDir;

    protected List<java.io.File> createdFiles = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        createdFiles = new ArrayList<>();
    }

    @AfterEach
    public void tearDown() {
        // Clean up created files
        for (java.io.File file : createdFiles) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
        createdFiles.clear();
    }

    /**
     * Creates a sample PDF file for testing.
     */
    protected java.io.File createSamplePdfFile(String fileName, int pageCount) throws IOException {
        java.io.File pdfFile = tempDir.resolve(fileName).toFile();
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            }
            document.save(pdfFile);
        }
        createdFiles.add(pdfFile);
        return pdfFile;
    }

    /**
     * Creates a sample DOCX file for testing with proper structure.
     */
    protected java.io.File createSampleDocxFile(String fileName, String content) throws IOException {
        java.io.File docxFile = tempDir.resolve(fileName).toFile();
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(docxFile)) {

            // Create styles
            document.createStyles();

            // Create paragraph with content
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(content);

            // Add section properties with page size (A4: 11906 x 16838 twips)
            CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
            CTPageSz pageSize = sectPr.addNewPgSz();
            pageSize.setW(BigInteger.valueOf(11906));
            pageSize.setH(BigInteger.valueOf(16838));

            document.write(out);
        }
        createdFiles.add(docxFile);
        return docxFile;
    }

    /**
     * Creates a sample DOCX template file with placeholders and proper structure.
     */
    protected java.io.File createSampleDocxTemplate(String fileName) throws IOException {
        java.io.File docxFile = tempDir.resolve(fileName).toFile();
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(docxFile)) {

            // Create styles
            document.createStyles();

            // Create content
            document.createParagraph().createRun().setText("Name: {{name}}");
            document.createParagraph().createRun().setText("Email: {{email}}");
            document.createParagraph().createRun().setText("Phone: {{phone}}");

            // Add section properties with page size (A4: 11906 x 16838 twips)
            CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
            CTPageSz pageSize = sectPr.addNewPgSz();
            pageSize.setW(BigInteger.valueOf(11906));
            pageSize.setH(BigInteger.valueOf(16838));

            document.write(out);
        }
        createdFiles.add(docxFile);
        return docxFile;
    }

    /**
     * Creates a sample Excel file for testing.
     */
    protected java.io.File createSampleExcelFile(String fileName, int rows, int cols) throws IOException {
        java.io.File excelFile = tempDir.resolve(fileName).toFile();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(excelFile)) {
            var sheet = workbook.createSheet("Sheet1");
            for (int i = 0; i < rows; i++) {
                var row = sheet.createRow(i);
                for (int j = 0; j < cols; j++) {
                    row.createCell(j).setCellValue("Cell " + i + "," + j);
                }
            }
            workbook.write(out);
        }
        createdFiles.add(excelFile);
        return excelFile;
    }

    /**
     * Creates a sample image file for testing.
     */
    protected java.io.File createSampleImageFile(String fileName, int width, int height) throws IOException {
        java.io.File imageFile = tempDir.resolve(fileName).toFile();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        ImageIO.write(image, "png", imageFile);
        createdFiles.add(imageFile);
        return imageFile;
    }

    /**
     * Creates a Krista File object from a java.io.File.
     * Note: In the actual PDF extension, File is just java.io.File
     */
    protected File createKristaFile(java.io.File javaFile) {
        // In the PDF extension, File is just java.io.File
        return javaFile;
    }

    /**
     * Verifies that a file exists and is a valid PDF.
     */
    protected void assertValidPdf(java.io.File file) throws IOException {
        assert file != null : "PDF file is null";
        assert file.exists() : "PDF file does not exist";
        assert file.length() > 0 : "PDF file is empty";
        try (PDDocument doc = PDDocument.load(file)) {
            assert doc.getNumberOfPages() > 0 : "PDF has no pages";
        }
    }
}


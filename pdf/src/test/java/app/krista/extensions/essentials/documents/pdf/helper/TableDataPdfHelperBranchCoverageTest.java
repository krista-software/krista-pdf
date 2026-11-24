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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Branch-coverage focused unit tests for {@link TableDataPdfHelper}.
 *
 * <p>The only remaining uncovered branch is the "title == null" side of the ternary inside
 * the {@code catch (Exception e)} block (used when building the error telemetry tags). The
 * existing integration test suite always passes a non-null title into every exception
 * scenario, so this test supplies a null title alongside a thrown exception to flip that
 * branch.</p>
 */
@DisplayName("TableDataPdfHelper Branch Coverage Tests")
class TableDataPdfHelperBranchCoverageTest {

    private PDFManager pdfManager;
    private KristaMediaClientUtil kristaMediaClientUtil;
    private TelemetryHelper telemetryHelper;
    private TableDataPdfHelper helper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pdfManager = mock(PDFManager.class);
        kristaMediaClientUtil = mock(KristaMediaClientUtil.class);
        telemetryHelper = mock(TelemetryHelper.class);
        helper = new TableDataPdfHelper(pdfManager, kristaMediaClientUtil, telemetryHelper);
    }

    @Test
    @DisplayName("Exception during PDF generation with a null title hits the N/A ternary branch in the error-telemetry tags")
    void testGetTableDataPDF_GenerationException_NullTitle() throws Exception {
        List<Map<String, Object>> tableData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("Name", "Test");
        tableData.add(row);

        when(pdfManager.getTableDataPdf(isNull(), any(), any(), any(), any(), any(), eq(tableData)))
                .thenThrow(new RuntimeException("PDF generation failed"));

        Exception exception = assertThrows(RuntimeException.class, () ->
                helper.getTableDataPDF(null, null, null, null, null, null, tableData));

        assertEquals("PDF generation failed", exception.getMessage());
        verify(telemetryHelper).recordError(eq("pdf.getTableDataPDF"), anyLong(), any(), any());
    }
}

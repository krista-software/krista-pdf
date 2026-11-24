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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PdfTableProcessor service class.
 * Tests table extraction, HTML conversion, and text removal functionality.
 */
@DisplayName("PdfTableProcessor Unit Tests")
public class PdfTableProcessorTest {

    private PdfTableProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new PdfTableProcessor();
    }

    // ==================== removeTableTextFromRawText Tests ====================

    @Test
    @DisplayName("Should remove table text from raw text")
    void testRemoveTableTextFromRawText_RemovesTableContent() {
        // Arrange
        String rawText = "This is a header\nName\nAge\nJohn\n30\nFooter text";
        Table table = createMockTable(
            Arrays.asList("Name", "Age"),
            Arrays.asList("John", "30")
        );
        List<Table> tables = Arrays.asList(table);

        // Act
        String result = processor.removeTableTextFromRawText(rawText, tables);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("This is a header"), "Should preserve non-table text");
        assertTrue(result.contains("Footer text"), "Should preserve non-table text");
        assertFalse(result.contains("Name"), "Should remove table header text");
        assertFalse(result.contains("Age"), "Should remove table header text");
        assertFalse(result.contains("John"), "Should remove table cell text");
        assertFalse(result.contains("30"), "Should remove table cell text");
    }

    @Test
    @DisplayName("Should return original text when no tables provided")
    void testRemoveTableTextFromRawText_NoTables() {
        // Arrange
        String rawText = "This is some text\nWith multiple lines\nNo tables here";
        List<Table> tables = new ArrayList<>();

        // Act
        String result = processor.removeTableTextFromRawText(rawText, tables);

        // Assert
        assertEquals(rawText, result, "Should return original text when no tables");
    }

    @Test
    @DisplayName("Should return original text when tables list is null")
    void testRemoveTableTextFromRawText_NullTables() {
        // Arrange
        String rawText = "This is some text\nWith multiple lines";

        // Act
        String result = processor.removeTableTextFromRawText(rawText, null);

        // Assert
        assertEquals(rawText, result, "Should return original text when tables is null");
    }

    @Test
    @DisplayName("Should preserve empty lines in output")
    void testRemoveTableTextFromRawText_PreservesEmptyLines() {
        // Arrange
        String rawText = "Header\n\nName\n\nFooter";
        Table table = createMockTable(Arrays.asList("Name"));
        List<Table> tables = Arrays.asList(table);

        // Act
        String result = processor.removeTableTextFromRawText(rawText, tables);

        // Assert
        assertTrue(result.contains("\n\n"), "Should preserve empty lines");
        assertTrue(result.contains("Header"), "Should preserve non-table text");
        assertTrue(result.contains("Footer"), "Should preserve non-table text");
    }

    @Test
    @DisplayName("Should handle multi-word cell text")
    void testRemoveTableTextFromRawText_MultiWordCells() {
        // Arrange
        String rawText = "Introduction\nEmployee Name\nJohn Smith\nConclusion";
        Table table = createMockTable(
            Arrays.asList("Employee Name"),
            Arrays.asList("John Smith")
        );
        List<Table> tables = Arrays.asList(table);

        // Act
        String result = processor.removeTableTextFromRawText(rawText, tables);

        // Assert
        assertTrue(result.contains("Introduction"), "Should preserve non-table text");
        assertTrue(result.contains("Conclusion"), "Should preserve non-table text");
        assertFalse(result.contains("Employee Name"), "Should remove multi-word cell text");
        assertFalse(result.contains("John Smith"), "Should remove multi-word cell text");
    }

    @Test
    @DisplayName("Should handle case-insensitive matching")
    void testRemoveTableTextFromRawText_CaseInsensitive() {
        // Arrange
        String rawText = "Header\nNAME\nname\nName\nFooter";
        Table table = createMockTable(Arrays.asList("Name"));
        List<Table> tables = Arrays.asList(table);

        // Act
        String result = processor.removeTableTextFromRawText(rawText, tables);

        // Assert
        assertTrue(result.contains("Header"), "Should preserve non-table text");
        assertTrue(result.contains("Footer"), "Should preserve non-table text");
        assertFalse(result.contains("NAME"), "Should remove case variations");
        assertFalse(result.contains("name"), "Should remove case variations");
        assertFalse(result.contains("Name"), "Should remove case variations");
    }

    @Test
    @DisplayName("Should skip words shorter than 3 characters")
    void testRemoveTableTextFromRawText_SkipsShortWords() {
        // Arrange
        String rawText = "The quick brown fox\nID\nAB\nABC\nFooter";
        Table table = createMockTable(
            Arrays.asList("ID AB ABC")
        );
        List<Table> tables = Arrays.asList(table);

        // Act
        String result = processor.removeTableTextFromRawText(rawText, tables);

        // Assert
        // "ID" and "AB" are <= 2 chars, so lines containing only them might be preserved
        // "ABC" is > 2 chars, so it should be removed
        assertFalse(result.contains("ABC"), "Should remove words longer than 2 chars");
    }

    @Test
    @DisplayName("Should handle multiple tables")
    void testRemoveTableTextFromRawText_MultipleTables() {
        // Arrange
        String rawText = "Header\nName\nAge\nCity\nState\nFooter";
        Table table1 = createMockTable(
            Arrays.asList("Name", "Age")
        );
        Table table2 = createMockTable(
            Arrays.asList("City", "State")
        );
        List<Table> tables = Arrays.asList(table1, table2);

        // Act
        String result = processor.removeTableTextFromRawText(rawText, tables);

        // Assert
        assertTrue(result.contains("Header"), "Should preserve non-table text");
        assertTrue(result.contains("Footer"), "Should preserve non-table text");
        assertFalse(result.contains("Name"), "Should remove text from first table");
        assertFalse(result.contains("City"), "Should remove text from second table");
    }

    @Test
    @DisplayName("Should handle empty cell text")
    void testRemoveTableTextFromRawText_EmptyCells() {
        // Arrange
        String rawText = "Header\nName\n\nFooter";
        Table table = createMockTable(
            Arrays.asList("Name", "")
        );
        List<Table> tables = Arrays.asList(table);

        // Act
        String result = processor.removeTableTextFromRawText(rawText, tables);

        // Assert
        assertTrue(result.contains("Header"), "Should preserve non-table text");
        assertTrue(result.contains("Footer"), "Should preserve non-table text");
        assertFalse(result.contains("Name"), "Should remove non-empty cell text");
    }

    // ==================== tableToHtml Tests ====================

    @Test
    @DisplayName("Should convert table to HTML with thead and tbody")
    void testTableToHtml_BasicTable() {
        // Arrange
        Table table = createMockTable(
            Arrays.asList("Name", "Age"),
            Arrays.asList("John", "30"),
            Arrays.asList("Jane", "25")
        );

        // Act
        String html = processor.tableToHtml(table);

        // Assert
        assertNotNull(html);
        assertTrue(html.contains("<table>"), "Should contain table tag");
        assertTrue(html.contains("<thead>"), "Should contain thead tag");
        assertTrue(html.contains("<tbody>"), "Should contain tbody tag");
        assertTrue(html.contains("</table>"), "Should close table tag");
        assertTrue(html.contains("</thead>"), "Should close thead tag");
        assertTrue(html.contains("</tbody>"), "Should close tbody tag");
    }

    @Test
    @DisplayName("Should render first row as table headers")
    void testTableToHtml_FirstRowAsHeaders() {
        // Arrange
        Table table = createMockTable(
            Arrays.asList("Name", "Age"),
            Arrays.asList("John", "30")
        );

        // Act
        String html = processor.tableToHtml(table);

        // Assert
        assertTrue(html.contains("<th>Name</th>"), "First row should use th tags");
        assertTrue(html.contains("<th>Age</th>"), "First row should use th tags");
        assertTrue(html.contains("<td>John</td>"), "Data rows should use td tags");
        assertTrue(html.contains("<td>30</td>"), "Data rows should use td tags");
    }

    @Test
    @DisplayName("Should escape HTML special characters")
    void testTableToHtml_EscapesHtmlCharacters() {
        // Arrange
        Table table = createMockTable(
            Arrays.asList("Name", "Code"),
            Arrays.asList("John & Jane", "<script>alert('xss')</script>")
        );

        // Act
        String html = processor.tableToHtml(table);

        // Assert
        assertTrue(html.contains("&amp;"), "Should escape ampersand");
        assertTrue(html.contains("&lt;"), "Should escape less than");
        assertTrue(html.contains("&gt;"), "Should escape greater than");
        assertFalse(html.contains("<script>"), "Should not contain unescaped script tag");
    }

    @Test
    @DisplayName("Should handle single row table (header only)")
    void testTableToHtml_SingleRowTable() {
        // Arrange
        Table table = createMockTable(
            Arrays.asList("Header1", "Header2")
        );

        // Act
        String html = processor.tableToHtml(table);

        // Assert
        assertTrue(html.contains("<thead>"), "Should contain thead");
        assertTrue(html.contains("<th>Header1</th>"), "Should contain headers");
        assertFalse(html.contains("<tbody>"), "Should not contain tbody for single row");
    }

    @Test
    @DisplayName("Should handle table with many rows")
    void testTableToHtml_MultipleRows() {
        // Arrange
        Table table = createMockTable(
            Arrays.asList("Name", "Age"),
            Arrays.asList("John", "30"),
            Arrays.asList("Jane", "25"),
            Arrays.asList("Bob", "35"),
            Arrays.asList("Alice", "28")
        );

        // Act
        String html = processor.tableToHtml(table);

        // Assert
        assertTrue(html.contains("<th>Name</th>"), "Should have header row");
        assertTrue(html.contains("<td>John</td>"), "Should have first data row");
        assertTrue(html.contains("<td>Jane</td>"), "Should have second data row");
        assertTrue(html.contains("<td>Bob</td>"), "Should have third data row");
        assertTrue(html.contains("<td>Alice</td>"), "Should have fourth data row");
        
        // Count tr tags in tbody (should be 4 data rows)
        int trCount = html.split("<tr>").length - 1;
        assertEquals(5, trCount, "Should have 1 header row + 4 data rows");
    }

    @Test
    @DisplayName("Should handle empty cell values")
    void testTableToHtml_EmptyCells() {
        // Arrange
        Table table = createMockTable(
            Arrays.asList("Name", "Age"),
            Arrays.asList("John", ""),
            Arrays.asList("", "25")
        );

        // Act
        String html = processor.tableToHtml(table);

        // Assert
        assertTrue(html.contains("<td></td>"), "Should handle empty cells");
        assertTrue(html.contains("<td>John</td>"), "Should handle non-empty cells");
        assertTrue(html.contains("<td>25</td>"), "Should handle non-empty cells");
    }

    @Test
    @DisplayName("Should properly indent HTML output")
    void testTableToHtml_ProperIndentation() {
        // Arrange
        Table table = createMockTable(
            Arrays.asList("Name", "Age"),
            Arrays.asList("John", "30")
        );

        // Act
        String html = processor.tableToHtml(table);

        // Assert
        assertTrue(html.contains("    <table>"), "Table should be indented");
        assertTrue(html.contains("      <thead>"), "Thead should be indented");
        assertTrue(html.contains("        <tr>"), "Tr should be indented");
        assertTrue(html.contains("          <th>"), "Th should be indented");
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a mock Table with the specified rows.
     * Each row is a list of cell values.
     */
    @SafeVarargs
    private Table createMockTable(List<String>... rows) {
        Table table = mock(Table.class);
        List<List<RectangularTextContainer>> tableRows = new ArrayList<>();

        for (List<String> rowData : rows) {
            List<RectangularTextContainer> row = new ArrayList<>();
            for (String cellText : rowData) {
                RectangularTextContainer cell = mock(RectangularTextContainer.class);
                when(cell.getText()).thenReturn(cellText);
                row.add(cell);
            }
            tableRows.add(row);
        }

        when(table.getRows()).thenReturn(tableRows);
        return table;
    }
}


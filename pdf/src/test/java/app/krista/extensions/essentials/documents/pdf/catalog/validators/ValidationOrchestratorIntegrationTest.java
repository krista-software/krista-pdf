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

package app.krista.extensions.essentials.documents.pdf.catalog.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ValidationOrchestrator.
 * Tests the orchestration of multiple validators and validation result handling.
 */
@DisplayName("ValidationOrchestrator Integration Tests")
public class ValidationOrchestratorIntegrationTest {

    private ValidationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new ValidationOrchestrator();
    }

    @Test
    @DisplayName("Should validate all resources successfully")
    void testValidate_AllValid() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.DOCX_FILE_FORMAT, "document.docx");
        resources.put(Validator.ValidationResource.EXCEL_FILE_FORMAT, "spreadsheet.xlsx");
        resources.put(Validator.ValidationResource.PDF_MERGE_FILE_FORMAT, "file.pdf");
        resources.put(Validator.ValidationResource.IMAGE_FILE_FORMAT, "image.png");
        resources.put(Validator.ValidationResource.RICH_TEXT_CONTENT, "Valid content here");

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertTrue(results.isEmpty(), "All validations should pass");
    }

    @Test
    @DisplayName("Should return validation errors for invalid resources")
    void testValidate_SomeInvalid() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.DOCX_FILE_FORMAT, "document.pdf"); // Invalid
        resources.put(Validator.ValidationResource.EXCEL_FILE_FORMAT, "spreadsheet.xlsx"); // Valid
        resources.put(Validator.ValidationResource.PDF_MERGE_FILE_FORMAT, "file.docx"); // Invalid

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertEquals(2, results.size(), "Should have 2 validation errors");
    }

    @Test
    @DisplayName("Should handle empty resources map")
    void testValidate_EmptyResources() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertTrue(results.isEmpty(), "Empty resources should pass validation");
    }

    @Test
    @DisplayName("Should validate PDF file count correctly")
    void testValidate_PdfFileCount() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.PDF_MERGE_FILE_COUNT, "FILE_COUNT:1"); // Invalid

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertEquals(1, results.size(), "Should have 1 validation error for insufficient files");
    }

    @Test
    @DisplayName("Should validate rich text content")
    void testValidate_RichTextContent() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.RICH_TEXT_CONTENT, ""); // Invalid

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertEquals(1, results.size(), "Should have 1 validation error for empty content");
    }

    @Test
    @DisplayName("ValidationResult should contain all required fields")
    void testValidationResult_Fields() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.DOCX_FILE_FORMAT, "invalid.pdf");

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertEquals(1, results.size());
        ValidationOrchestrator.ValidationResult result = results.get(0);

        assertNotNull(result.getConfirmStepMessage());
        assertNotNull(result.getFetchFieldName());
        assertNotNull(result.getFetchStepMessage());
        assertNotNull(result.getErrMessage());
        assertNotNull(result.getFieldType());
    }

    @Test
    @DisplayName("Should register custom validator")
    void testRegisterValidator() {
        Validator customValidator = new DocxFileFormatValidator();
        orchestrator.registerValidator(Validator.ValidationResource.TEMPLATE_FILE_FORMAT, customValidator);

        assertNotNull(orchestrator.getValidators());
        assertTrue(orchestrator.getValidators().size() > 0);
    }

    @Test
    @DisplayName("Should get all registered validators")
    void testGetValidators() {
        var validators = orchestrator.getValidators();

        assertNotNull(validators);
        assertTrue(validators.size() > 0, "Should have registered validators");
    }

    @Test
    @DisplayName("Should validate multiple file formats in one call")
    void testValidate_MultipleFormats() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.DOCX_FILE_FORMAT, "report.docx");
        resources.put(Validator.ValidationResource.EXCEL_FILE_FORMAT, "data.xlsx");
        resources.put(Validator.ValidationResource.PDF_MERGE_FILE_FORMAT, "merge.pdf");
        resources.put(Validator.ValidationResource.IMAGE_FILE_FORMAT, "logo.png");
        resources.put(Validator.ValidationResource.PDF_TO_HTML_FILE_FORMAT, "convert.pdf");

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertTrue(results.isEmpty(), "All file formats should be valid");
    }

    @Test
    @DisplayName("Should handle null values in resources")
    void testValidate_NullValues() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.DOCX_FILE_FORMAT, null);
        resources.put(Validator.ValidationResource.EXCEL_FILE_FORMAT, null);

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertTrue(results.size() >= 1, "Null values should fail validation");
    }

    @Test
    @DisplayName("Should validate split PDF file formats")
    void testValidate_SplitPdfFormats() {
        Map<Validator.ValidationResource, String> resources = new HashMap<>();
        resources.put(Validator.ValidationResource.SPLIT_PDF_FILE_FORMAT, "document.pdf");
        resources.put(Validator.ValidationResource.SPLIT_PDF_TWO_EQUAL_FILE_FORMAT, "split.pdf");
        resources.put(Validator.ValidationResource.SPLIT_PDF_CONDITION_FILE_FORMAT, "conditional.pdf");

        List<ValidationOrchestrator.ValidationResult> results = orchestrator.validate(resources);

        assertTrue(results.isEmpty(), "All split PDF formats should be valid");
    }
}


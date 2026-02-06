# Convert Excel File with Multiple Sheets to PDF

## Overview

Converts an Excel file containing multiple sheets into a single PDF file. All sheets from the Excel file are converted
and included in the output PDF.

## Request Details

- **Area**: PDF
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes - When Allow Retry is enabled, provides comprehensive validation with user prompts for
  invalid file formats

## Input Parameters

| Parameter Name | Type | Required | Description                                                                    | Example              |
|----------------|------|----------|--------------------------------------------------------------------------------|----------------------|
| Excel File     | File | Yes      | The Excel file with multiple sheets to convert to PDF. Must be in .xlsx format | `annual_report.xlsx` |

### Parameter Details

#### Excel File

- **Supported Formats**: `.xlsx`
- **Validation**: File format is validated when Allow Retry is enabled
- **Behavior**: All sheets in the workbook are converted to a single PDF file
- **Sheet Order**: Sheets are converted in the order they appear in the workbook

## Output Parameters

| Parameter Name | Type | Description                                                      |
|----------------|------|------------------------------------------------------------------|
| PDF File       | File | The generated PDF file containing all sheets from the Excel file |

## Validation Rules

**When Allow Retry = true**

| Validation                      | Error Message                                                                                 | Resolution                                |
|---------------------------------|-----------------------------------------------------------------------------------------------|-------------------------------------------|
| Excel file format must be .xlsx | "The provided file '[filename]' is not a valid Excel format. Only .xlsx files are supported." | Upload a valid Excel file in .xlsx format |
| File must not be null or empty  | "No Excel file provided"                                                                      | Upload a valid Excel file                 |

**When Allow Retry = false**

No validation is performed. Invalid inputs will result in SYSTEM_ERROR.

## Error Handling

### Input Errors (INPUT_ERROR)

**When Allow Retry is enabled**

#### Invalid Excel File Format

- **Cause**: The uploaded file is not in .xlsx format
- **Error Message**: "The provided file '[filename]' is not a valid Excel format. Only .xlsx files are supported."
- **Resolution**:
    1. Verify the file has a .xlsx extension
    2. Upload a valid Excel file in .xlsx format
    3. Convert older .xls files to .xlsx format before uploading

#### Missing Excel File

- **Cause**: No file was provided
- **Error Message**: "No Excel file provided"
- **Resolution**: Upload a valid Excel file

#### Excel File Corruption

- **Cause**: The Excel file is corrupted or cannot be read
- **Error Message**: "Failed to read Excel file: file may be corrupted"
- **Resolution**:
    1. Open the file in Microsoft Excel to verify it's not corrupted
    2. Save a new copy of the file
    3. Try with a different Excel file

### System Errors (SYSTEM_ERROR)

**When Allow Retry is disabled or for unexpected errors**

#### File Processing Failure

- **Cause**: Error during file download, conversion, or upload
- **Error Message**: "Failed to convert Excel file with multiple sheets to PDF: [error details]"
- **Common Scenarios**:
    - Corrupted Excel file
    - File read/write permissions issues
    - Insufficient memory for large files
    - Network issues during file transfer
    - Complex sheet formatting issues
- **Resolution**:
    1. Verify the Excel file is not corrupted
    2. Try with a smaller file to isolate the issue
    3. Simplify sheet formatting if conversion fails
    4. Check system logs for detailed error information
    5. Ensure sufficient system resources are available

## Usage Examples

### Example 1: Convert Multi-Sheet Workbook

**Scenario**: Convert an annual report with multiple sheets to PDF

**Input**:

```
Excel File: "annual_report_2024.xlsx"
(Contains sheets: Summary, Q1, Q2, Q3, Q4, Appendix)
```

**Output**:

```
PDF File: "annual_report_2024.pdf"
(Contains all 6 sheets in order)
```

**Result**: All sheets are converted to a single PDF file in the order they appear in the workbook.

### Example 2: Convert Financial Workbook

**Scenario**: Convert financial workbook with multiple tabs

**Input**:

```
Excel File: "financial_statements.xlsx"
(Contains sheets: Balance Sheet, Income Statement, Cash Flow)
```

**Output**:

```
PDF File: "financial_statements.pdf"
(Contains all 3 sheets)
```

**Result**: Complete financial statements in a single PDF file.

### Example 3: Retry Scenario (Allow Retry Enabled)

**Scenario**: User provides invalid file format, then corrects it

**Interaction**:

```
User: "Convert this workbook to PDF"
File: "report.csv"

System: "The provided file 'report.csv' is not a valid Excel format. Only .xlsx files are supported."
Prompt: "Please upload a valid Excel file (.xlsx format)."

User: *uploads "report.xlsx"*

System: "Success! All sheets converted to PDF."
Output: "report.pdf"
```

## Business Rules

1. **All Sheets Included**: All sheets in the workbook are converted to PDF
2. **Sheet Order Preserved**: Sheets appear in the PDF in the same order as in the Excel file
3. **Single PDF Output**: All sheets are combined into one PDF file
4. **Format Preservation**: The conversion attempts to preserve formatting, but some complex elements may have
   limitations
5. **File Format Restriction**: Only .xlsx format is supported

## Limitations

1. **File Format**: Only .xlsx files are supported. Older .xls files must be converted to .xlsx first
2. **All Sheets Converted**: Cannot selectively convert specific sheets; all sheets are included
3. **Formatting**: Some complex Excel formatting (charts, macros, advanced formulas) may not be perfectly preserved
4. **File Size**: Very large Excel files with many sheets may impact performance or cause memory issues
5. **Sheet Complexity**: Sheets with complex layouts may not convert perfectly

## Best Practices

### 1. Organize Sheets Logically

Arrange sheets in the desired order before conversion, as they will appear in the PDF in the same sequence.

### 2. Simplify Complex Formatting

For best results:

- Remove or simplify charts and graphs
- Use basic formatting (fonts, colors, borders)
- Avoid complex formulas that may not render correctly

### 3. Validate Files Before Conversion

- Open the Excel file to ensure it's not corrupted
- Verify all sheets are needed in the PDF
- Remove unnecessary sheets before conversion

### 4. Handle Large Files Carefully

For Excel files with many sheets:

- Consider splitting into smaller workbooks
- Remove unnecessary data or sheets
- Monitor system resources during conversion

### 5. Enable Allow Retry for User Workflows

When users are providing files interactively, enable Allow Retry to provide helpful validation messages.

## Common Use Cases

### 1. Annual Report Generation

```
Scenario: Convert annual report with multiple sections to PDF
Action: Upload Excel workbook with all report sections
Result: Complete annual report in single PDF file
```

### 2. Financial Statement Distribution

```
Scenario: Distribute financial statements as PDF
Action: Upload workbook with Balance Sheet, Income Statement, Cash Flow
Result: Complete financial package in PDF format
```

### 3. Multi-Tab Dashboard Export

```
Scenario: Export dashboard with multiple views to PDF
Action: Upload Excel file with multiple dashboard tabs
Result: Complete dashboard in PDF format
```

### 4. Data Archive

```
Scenario: Archive multi-sheet data workbooks
Action: Convert Excel workbooks to PDF for immutable archival
Result: PDF files preserving all sheets for long-term storage
```

## Related Catalog Requests

- [Convert Excel to PDF](pages/ConvertExcelToPDF.md) - Convert single sheet or specific sheet
- [Get Table PDF](pages/GetTablePDF.md) - Create PDF from tabular data
- [Merge PDF Files](pages/MergePDFFiles.md) - Combine multiple PDF files

## Technical Implementation

### Helper Class

- **Class**: `ConvertExcelMultipleSheetsHelper`
- **Package**: `app.krista.extensions.essentials.documents.pdf.helper`
- **Validation**: Uses `ValidationOrchestrator` with `ExcelMultipleSheetsFileFormatValidator`
- **Service**: Delegates to `ExcelConverterService.convertExcelToPDF()`

### Telemetry Metrics

- **pdf.convertExcelMultipleSheetsToPDF.count**: Total number of conversion requests
- **pdf.convertExcelMultipleSheetsToPDF.success**: Successful conversions with duration
- **pdf.convertExcelMultipleSheetsToPDF.error**: Failed conversions with error details and duration

### Validation Flow

1. Validate Excel file format (.xlsx)
2. If validation passes, download file
3. Convert all sheets to PDF using ExcelConverterService
4. Upload result and return success response
5. If validation fails, prompt user for correction (when Allow Retry enabled)

## Troubleshooting

### Issue: "File format not supported" error

**Cause**: File is not in .xlsx format

**Solution**:

1. Check file extension is .xlsx
2. Open file in Excel and save as .xlsx format
3. Ensure file is not corrupted

### Issue: Conversion succeeds but some sheets are missing

**Cause**: Hidden sheets or sheets with errors may be skipped

**Solution**:

1. Unhide all sheets in Excel before conversion
2. Verify all sheets are visible and accessible
3. Check for sheet-level errors in Excel

### Issue: Formatting is lost in PDF

**Cause**: Complex Excel formatting may not be fully supported

**Solution**:

1. Simplify Excel formatting before conversion
2. Remove charts or complex elements
3. Use basic formatting (fonts, colors, borders)
4. Test with simplified version

### Issue: Large file conversion fails

**Cause**: Insufficient memory or timeout

**Solution**:

1. Reduce file size by removing unnecessary data
2. Split into smaller workbooks
3. Remove unused sheets
4. Simplify formatting and formulas

### Issue: Sheets appear in wrong order

**Cause**: Sheet order in Excel doesn't match expected order

**Solution**:

1. Rearrange sheets in Excel before conversion
2. Verify sheet order in Excel matches desired PDF order
3. Save Excel file after reordering

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Configure Allow Retry flag
- [Convert Excel to PDF](pages/ConvertExcelToPDF.md) - Convert single or specific sheet
- [Krista PDF Templates](pages/kristaTemplates.md) - Other PDF generation options


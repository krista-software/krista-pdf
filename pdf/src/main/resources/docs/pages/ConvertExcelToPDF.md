# Convert Excel to PDF

## Overview

Converts an Excel file to PDF format. This request can convert either the entire workbook or a specific sheet by name.

## Request Details

- **Area**: PDF
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes - When Allow Retry is enabled, provides comprehensive validation with user prompts for
  invalid file formats or sheet names

## Input Parameters

| Parameter Name | Type | Required | Description                                                                                      | Example             |
|----------------|------|----------|--------------------------------------------------------------------------------------------------|---------------------|
| Excel File     | File | Yes      | The Excel file to convert to PDF. Must be in .xlsx format                                        | `sales_report.xlsx` |
| Sheet Name     | Text | No       | Optional name of a specific sheet to convert. If not provided, the first sheet will be converted | `Q1 Sales`          |

### Parameter Details

#### Excel File

- **Supported Formats**: `.xlsx`
- **Validation**: File format is validated when Allow Retry is enabled
- **Behavior**: The file is downloaded, processed, and converted to PDF format

#### Sheet Name

- **Optional Parameter**: If not provided, the first sheet in the workbook is converted
- **Case Sensitive**: Sheet names are case-sensitive
- **Validation**: When provided, the sheet name is validated against available sheets in the workbook (when Allow Retry
  is enabled)

## Output Parameters

| Parameter Name | Type | Description                                                |
|----------------|------|------------------------------------------------------------|
| Excel PDF File | File | The generated PDF file containing the converted Excel data |

## Validation Rules

**When Allow Retry = true**

| Validation                                      | Error Message                                                                                 | Resolution                                                                                          |
|-------------------------------------------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Excel file format must be .xlsx                 | "The provided file '[filename]' is not a valid Excel format. Only .xlsx files are supported." | Upload a valid Excel file in .xlsx format                                                           |
| Sheet name must exist in workbook (if provided) | "The provided sheet name '[sheetname]' does not exist in the Excel file."                     | Provide a valid sheet name that exists in the Excel file, or leave empty to convert the first sheet |
| File must not be null or empty                  | "No Excel file provided"                                                                      | Upload a valid Excel file                                                                           |

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

#### Sheet Name Not Found

- **Cause**: The specified sheet name does not exist in the Excel file
- **Error Message**: "The provided sheet name '[sheetname]' does not exist in the Excel file."
- **Resolution**:
    1. Verify the sheet name is spelled correctly (case-sensitive)
    2. Open the Excel file to confirm the exact sheet name
    3. Leave the sheet name empty to convert the first sheet

#### Missing Excel File

- **Cause**: No file was provided
- **Error Message**: "No Excel file provided"
- **Resolution**: Upload a valid Excel file

### System Errors (SYSTEM_ERROR)

**When Allow Retry is disabled or for unexpected errors**

#### File Processing Failure

- **Cause**: Error during file download, conversion, or upload
- **Error Message**: "Failed to convert Excel to PDF: [error details]"
- **Common Scenarios**:
    - Corrupted Excel file
    - File read/write permissions issues
    - Insufficient memory for large files
    - Network issues during file transfer
- **Resolution**:
    1. Verify the Excel file is not corrupted
    2. Try with a smaller file to isolate the issue
    3. Check system logs for detailed error information
    4. Ensure sufficient system resources are available

## Usage Examples

### Example 1: Convert First Sheet

**Scenario**: Convert the first sheet of an Excel file to PDF

**Input**:

```
Excel File: "monthly_report.xlsx"
Sheet Name: (empty)
```

**Output**:

```
Excel PDF File: "monthly_report.pdf"
```

**Result**: The first sheet of the Excel file is converted to PDF format.

### Example 2: Convert Specific Sheet

**Scenario**: Convert a specific sheet named "Summary" to PDF

**Input**:

```
Excel File: "annual_report.xlsx"
Sheet Name: "Summary"
```

**Output**:

```
Excel PDF File: "annual_report.pdf"
```

**Result**: Only the "Summary" sheet is converted to PDF format.

### Example 3: Retry Scenario (Allow Retry Enabled)

**Scenario**: User provides invalid file format, then corrects it

**Interaction**:

```
User: "Convert this file to PDF"
File: "report.csv"

System: "The provided file 'report.csv' is not a valid Excel format. Only .xlsx files are supported."
Prompt: "Please upload a valid Excel file (.xlsx format)."

User: *uploads "report.xlsx"*

System: "Success! Excel file converted to PDF."
Output: "report.pdf"
```

## Business Rules

1. **Default Sheet Behavior**: If no sheet name is provided, the first sheet in the workbook is converted
2. **Single Sheet Conversion**: Only one sheet is converted per request, not the entire workbook
3. **Format Preservation**: The conversion attempts to preserve formatting, but some complex elements may have
   limitations
4. **File Format Restriction**: Only .xlsx format is supported; older .xls formats are not supported

## Limitations

1. **File Format**: Only .xlsx files are supported. Older .xls files must be converted to .xlsx first
2. **Single Sheet**: Only one sheet can be converted per request. For multiple sheets,
   use [Convert Excel File with Multiple Sheets to PDF](pages/ConvertExcelMultipleSheetsToPDF.md)
3. **Formatting**: Some complex Excel formatting (charts, macros, advanced formulas) may not be perfectly preserved in
   PDF
4. **File Size**: Very large Excel files may impact performance or cause memory issues
5. **Sheet Name Case Sensitivity**: Sheet names are case-sensitive and must match exactly

## Best Practices

### 1. Use Descriptive Sheet Names

When converting specific sheets, ensure sheet names are clear and descriptive to avoid confusion.

### 2. Validate Files Before Conversion

For automated workflows, ensure files are in .xlsx format before sending to the extension.

### 3. Handle Large Files Carefully

For Excel files with many rows or complex formatting:

- Consider splitting into smaller files
- Convert specific sheets rather than entire workbook
- Monitor system resources during conversion

### 4. Enable Allow Retry for User Workflows

When users are providing files interactively, enable Allow Retry to provide helpful validation messages.

### 5. Disable Allow Retry for Automation

For automated batch processing with pre-validated files, disable Allow Retry for faster processing.

## Common Use Cases

### 1. Report Generation

```
Scenario: Convert monthly sales report to PDF for distribution
Action: Upload Excel file with sales data, leave sheet name empty
Result: PDF report ready for email distribution
```

### 2. Dashboard Export

```
Scenario: Export specific dashboard sheet from multi-sheet workbook
Action: Upload workbook, specify dashboard sheet name
Result: PDF of dashboard only, without other sheets
```

### 3. Data Archival

```
Scenario: Archive Excel data in PDF format for long-term storage
Action: Convert Excel files to PDF for immutable archival
Result: PDF files that preserve data in read-only format
```

## Related Catalog Requests

- [Convert Excel File with Multiple Sheets to PDF](pages/ConvertExcelMultipleSheetsToPDF.md) - Convert all sheets in an
  Excel file
- [Get Table PDF](pages/GetTablePDF.md) - Create PDF from tabular data
- [Get Entity List PDF](pages/GetEntityListPDF.md) - Create PDF from entity data

## Technical Implementation

### Helper Class

- **Class**: `ConvertExcelToPDFHelper`
- **Package**: `app.krista.extensions.essentials.documents.pdf.helper`
- **Validation**: Uses `ValidationOrchestrator` with `ExcelFileFormatValidator` and `SheetNameValidator`
- **Service**: Delegates to `ExcelManager.convertExcelToPDF()`

### Telemetry Metrics

- **pdf.convertExcelToPDF.count**: Total number of conversion requests
- **pdf.convertExcelToPdf.success**: Successful conversions with duration
- **pdf.convertExcelToPdf.error**: Failed conversions with error details and duration

### Validation Flow

1. Validate Excel file format (.xlsx)
2. If sheet name provided, download file and validate sheet exists
3. If validation passes, convert Excel to PDF
4. Upload result and return success response
5. If validation fails, prompt user for correction (when Allow Retry enabled)

## Troubleshooting

### Issue: "File format not supported" error

**Cause**: File is not in .xlsx format

**Solution**:

1. Check file extension is .xlsx
2. Open file in Excel and save as .xlsx format
3. Ensure file is not corrupted

### Issue: "Sheet name not found" error

**Cause**: Specified sheet name doesn't exist or is misspelled

**Solution**:

1. Open Excel file and verify exact sheet name (case-sensitive)
2. Copy sheet name directly from Excel to avoid typos
3. Leave sheet name empty to convert first sheet

### Issue: Conversion succeeds but formatting is lost

**Cause**: Complex Excel formatting may not be fully supported

**Solution**:

1. Simplify Excel formatting before conversion
2. Remove charts or complex elements
3. Use basic formatting (fonts, colors, borders)

### Issue: Large file conversion fails

**Cause**: Insufficient memory or timeout

**Solution**:

1. Reduce file size by removing unnecessary data
2. Convert specific sheets instead of entire workbook
3. Split large files into smaller chunks

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Configure Allow Retry flag
- [Convert Excel File with Multiple Sheets to PDF](pages/ConvertExcelMultipleSheetsToPDF.md) - Convert entire workbook
- [Krista PDF Templates](pages/kristaTemplates.md) - Other PDF generation options


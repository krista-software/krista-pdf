# Convert Docx File to PDF File

## Overview

Converts a Microsoft Word document (.docx) to PDF format while preserving formatting, images, and other content.

## Request Details

- **Area**: PDF
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes - When Allow Retry is enabled, provides comprehensive validation with user prompts for
  invalid file formats

## Input Parameters

| Parameter Name | Type | Required | Description                                                       | Example         |
|----------------|------|----------|-------------------------------------------------------------------|-----------------|
| Document File  | File | Yes      | The Word document file to convert to PDF. Must be in .docx format | `contract.docx` |

### Parameter Details

#### Document File

- **Supported Formats**: `.docx`
- **Validation**: File format is validated when Allow Retry is enabled
- **Content Preservation**: The conversion attempts to preserve formatting, images, tables, and other document elements
- **Limitations**: Some complex elements (graphs, macros) may not be perfectly preserved

## Output Parameters

| Parameter Name | Type | Description                                                   |
|----------------|------|---------------------------------------------------------------|
| Docx PDF File  | File | The generated PDF file containing the converted Word document |

## Validation Rules

**When Allow Retry = true**

| Validation                         | Error Message                                                                                         | Resolution                                   |
|------------------------------------|-------------------------------------------------------------------------------------------------------|----------------------------------------------|
| Document file format must be .docx | "The provided file '[filename]' is not a valid Word document format. Only .docx files are supported." | Upload a valid Word document in .docx format |
| File must not be null or empty     | "No document file provided"                                                                           | Upload a valid Word document                 |

**When Allow Retry = false**

No validation is performed. Invalid inputs will result in SYSTEM_ERROR.

## Error Handling

### Input Errors (INPUT_ERROR)

**When Allow Retry is enabled**

#### Invalid Document File Format

- **Cause**: The uploaded file is not in .docx format
- **Error Message**: "The provided file '[filename]' is not a valid Word document format. Only .docx files are
  supported."
- **Resolution**:
    1. Verify the file has a .docx extension
    2. Upload a valid Word document in .docx format
    3. Convert older .doc files to .docx format before uploading

#### Missing Document File

- **Cause**: No file was provided
- **Error Message**: "No document file provided"
- **Resolution**: Upload a valid Word document

### System Errors (SYSTEM_ERROR)

**When Allow Retry is disabled or for unexpected errors**

#### File Processing Failure

- **Cause**: Error during file download, conversion, or upload
- **Error Message**: "Failed to convert docx to PDF: [error details]"
- **Common Scenarios**:
    - Corrupted Word document
    - File read/write permissions issues
    - Insufficient memory for large documents
    - Network issues during file transfer
    - Unsupported document features
- **Resolution**:
    1. Verify the Word document is not corrupted
    2. Try opening the document in Microsoft Word to check for issues
    3. Remove complex elements (macros, embedded objects) if conversion fails
    4. Check system logs for detailed error information
    5. Ensure sufficient system resources are available

## Usage Examples

### Example 1: Convert Simple Document

**Scenario**: Convert a basic Word document to PDF

**Input**:

```
Document File: "meeting_notes.docx"
```

**Output**:

```
Docx PDF File: "meeting_notes.pdf"
```

**Result**: The Word document is converted to PDF format with formatting preserved.

### Example 2: Convert Formatted Report

**Scenario**: Convert a formatted business report with images and tables

**Input**:

```
Document File: "quarterly_report.docx"
```

**Output**:

```
Docx PDF File: "quarterly_report.pdf"
```

**Result**: The report is converted to PDF with tables, images, and formatting preserved.

### Example 3: Retry Scenario (Allow Retry Enabled)

**Scenario**: User provides invalid file format, then corrects it

**Interaction**:

```
User: "Convert this document to PDF"
File: "document.txt"

System: "The provided file 'document.txt' is not a valid Word document format. Only .docx files are supported."
Prompt: "Please upload a valid Microsoft Word document (.docx file)."

User: *uploads "document.docx"*

System: "Success! Word document converted to PDF."
Output: "document.pdf"
```

## Business Rules

1. **Format Support**: Only .docx format is supported; older .doc formats must be converted first
2. **Content Preservation**: The extension attempts to preserve all formatting, but some complex elements may have
   limitations
3. **Single File Processing**: One document is converted per request
4. **No Modification**: The original document is not modified; a new PDF file is created

## Limitations

1. **File Format**: Only .docx files are supported. Older .doc files must be converted to .docx first
2. **Complex Elements**: Some advanced Word features may not be perfectly preserved:
    - Graphs and charts may be skipped or simplified
    - Macros are not preserved in PDF
    - Some advanced formatting may be simplified
    - Embedded objects may not render correctly
3. **File Size**: Very large documents may impact performance or cause memory issues
4. **Apache POI Limitations**: The conversion uses Apache POI library, which has known limitations with complex
   formatting
5. **Multi-page Documents**: While multi-page conversion works, some formatting issues may persist

## Best Practices

### 1. Simplify Complex Documents

For documents with complex formatting:

- Remove or simplify graphs and charts before conversion
- Use standard fonts and formatting
- Test conversion with a sample before processing large batches

### 2. Validate Documents Before Conversion

- Open documents in Microsoft Word to verify they're not corrupted
- Save documents in .docx format (not .doc)
- Remove macros and embedded objects if not needed

### 3. Handle Large Documents Carefully

For large Word documents:

- Consider splitting into smaller sections
- Remove unnecessary images or compress them
- Monitor system resources during conversion

### 4. Enable Allow Retry for User Workflows

When users are providing files interactively, enable Allow Retry to provide helpful validation messages.

### 5. Test Formatting Preservation

Before batch processing:

- Test conversion with sample documents
- Verify critical formatting is preserved
- Adjust source documents if needed

## Common Use Cases

### 1. Contract Generation

```
Scenario: Convert legal contracts to PDF for signing
Action: Upload Word contract document
Result: PDF contract ready for digital signatures
```

### 2. Report Distribution

```
Scenario: Convert business reports to PDF for distribution
Action: Upload formatted Word report
Result: PDF report ready for email or sharing
```

### 3. Document Archival

```
Scenario: Archive Word documents in PDF format
Action: Convert Word files to PDF for long-term storage
Result: Immutable PDF files for archival
```

### 4. Proposal Submission

```
Scenario: Convert proposal documents to PDF for submission
Action: Upload Word proposal with formatting
Result: Professional PDF proposal ready for submission
```

## Related Catalog Requests

- [Fill The Docx Template](pages/FillTheDocxTemplate.md) - Fill Word templates with data and convert to PDF
- [Convert Excel to PDF](pages/ConvertExcelToPDF.md) - Convert Excel files to PDF
- [Convert Image to PDF](pages/ConvertImageToPdf.md) - Convert images to PDF

## Technical Implementation

### Helper Class

- **Class**: `ConvertDocxToPdfHelper`
- **Package**: `app.krista.extensions.essentials.documents.pdf.helper`
- **Validation**: Uses `ValidationOrchestrator` with `DocxFileFormatValidator`
- **Service**: Delegates to `PDFManager.convertDocToPDF()`

### Telemetry Metrics

- **pdf.convertDocxToPDF.count**: Total number of conversion requests
- **pdf.convertDocxToPdf.success**: Successful conversions with duration
- **pdf.convertDocxToPdf.error**: Failed conversions with error details and duration

### Validation Flow

1. Validate document file format (.docx)
2. If validation passes, download file
3. Convert Word document to PDF using PDFManager
4. Upload result and return success response
5. If validation fails, prompt user for correction (when Allow Retry enabled)

## Troubleshooting

### Issue: "File format not supported" error

**Cause**: File is not in .docx format

**Solution**:

1. Check file extension is .docx
2. Open file in Microsoft Word and save as .docx format
3. Ensure file is not corrupted

### Issue: Conversion succeeds but formatting is lost

**Cause**: Complex Word formatting may not be fully supported by Apache POI

**Solution**:

1. Simplify document formatting before conversion
2. Remove graphs, charts, or complex elements
3. Use standard fonts and basic formatting
4. Test with a simpler version of the document

### Issue: Graphs or charts are missing in PDF

**Cause**: Known limitation of Apache POI library

**Solution**:

1. Take screenshots of graphs and insert as images
2. Use simpler chart types
3. Consider alternative PDF generation methods for graph-heavy documents

### Issue: Large document conversion fails

**Cause**: Insufficient memory or timeout

**Solution**:

1. Reduce document size by removing unnecessary content
2. Compress images in the document
3. Split large documents into smaller sections
4. Increase system memory allocation if possible

### Issue: Formatting alignment issues

**Cause**: Apache POI library limitations with complex formatting

**Solution**:

1. Use simpler formatting in source document
2. Avoid complex table layouts
3. Use standard paragraph alignment
4. Test and adjust source document as needed

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Configure Allow Retry flag
- [Fill The Docx Template](pages/FillTheDocxTemplate.md) - Template-based PDF generation
- [Release Notes](pages/release-notes.md) - Known issues and limitations


# Merge PDF Files

## Overview

Merges multiple PDF files into a single PDF file. This request can optionally add title, subtitle, header, footer, logo,
and description to the merged document.

## Request Details

- **Area**: PDF
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes - When Allow Retry is enabled, provides comprehensive validation with user prompts for
  invalid PDF files

## Input Parameters

| Parameter Name | Type       | Required | Description                                           | Example                                   |
|----------------|------------|----------|-------------------------------------------------------|-------------------------------------------|
| Title          | Text       | No       | Title for the merged PDF document                     | `Combined Reports`                        |
| Sub Title      | Text       | No       | Subtitle for the merged PDF document                  | `Q1 2024`                                 |
| Description    | Rich Text  | No       | Description or introduction for the merged document   | `This document combines...`               |
| Header         | Text       | No       | Header text to appear on each page                    | `Company Name - Confidential`             |
| Footer         | Text       | No       | Footer text to appear on each page                    | `Page {page} of {total}`                  |
| Logo           | File       | No       | Company logo or image to include in the document      | `company_logo.png`                        |
| PDF Files      | List<File> | Yes      | List of PDF files to merge (minimum 2 files required) | `[report1.pdf, report2.pdf, report3.pdf]` |

### Parameter Details

#### PDF Files (Required)

- **Format**: List of PDF files
- **Minimum**: At least 2 PDF files must be provided
- **Validation**: File format and count are validated when Allow Retry is enabled
- **Merge Order**: Files are merged in the order they appear in the list
- **File Format**: All files must be valid PDF files

#### Optional Formatting Parameters

All other parameters are optional and enhance the merged PDF:

- **Title/Sub Title**: Appear at the beginning of the merged document
- **Header/Footer**: Appear on each page
- **Logo**: Displayed in the document header area
- **Description**: Introductory text at the beginning

## Output Parameters

| Parameter Name | Type | Description                                       |
|----------------|------|---------------------------------------------------|
| Merged PDF     | File | The generated PDF file containing all merged PDFs |

## Validation Rules

**When Allow Retry = true**

| Validation                    | Error Message                                                                              | Resolution                   |
|-------------------------------|--------------------------------------------------------------------------------------------|------------------------------|
| PDF Files must be provided    | "Please provide at least one PDF file to merge"                                            | Provide a list of PDF files  |
| At least 2 PDF files required | "Insufficient files for merging. At least two files are required."                         | Provide at least 2 PDF files |
| All files must be valid PDFs  | "The provided file '[filename]' is not a valid PDF format. Only .pdf files are supported." | Upload valid PDF files only  |

**When Allow Retry = false**

No validation is performed. Invalid inputs will result in SYSTEM_ERROR.

## Error Handling

### Input Errors (INPUT_ERROR)

**When Allow Retry is enabled**

#### Missing PDF Files

- **Cause**: PDF Files parameter is not provided or is empty
- **Error Message**: "Please provide at least one PDF file to merge"
- **Resolution**: Provide a list of PDF files to merge

#### Insufficient PDF Files

- **Cause**: Less than 2 PDF files provided
- **Error Message**: "Insufficient files for merging. At least two files are required."
- **Resolution**: Provide at least 2 PDF files for merging

#### Invalid PDF File Format

- **Cause**: One or more files are not valid PDF files
- **Error Message**: "The provided file '[filename]' is not a valid PDF format. Only .pdf files are supported."
- **Resolution**:
    1. Verify all files have .pdf extension
    2. Ensure all files are valid PDF documents
    3. Remove non-PDF files from the list

### System Errors (SYSTEM_ERROR)

**When Allow Retry is disabled or for unexpected errors**

#### PDF Merging Failure

- **Cause**: Error during PDF merging or file processing
- **Error Message**: "Failed to merge PDF files: [error details]"
- **Common Scenarios**:
    - Corrupted PDF files
    - Password-protected PDFs
    - File read/write permissions issues
    - Insufficient memory for large PDFs
    - Network issues during file transfer
- **Resolution**:
    1. Verify all PDF files are not corrupted
    2. Remove password protection from PDFs
    3. Try with smaller PDF files to isolate the issue
    4. Check system logs for detailed error information
    5. Ensure sufficient system resources are available

## Usage Examples

### Example 1: Merge Monthly Reports

**Scenario**: Combine monthly reports into a quarterly report

**Input**:

```
Title: "Quarterly Report"
Sub Title: "Q1 2024"
PDF Files: ["january_report.pdf", "february_report.pdf", "march_report.pdf"]
```

**Output**:

```
Merged PDF: "quarterly_report.pdf"
```

**Result**: All three monthly reports combined into one PDF.

### Example 2: Merge with Branding

**Scenario**: Merge documents with company branding

**Input**:

```
Title: "Annual Report"
Sub Title: "2023"
Description: "This report combines all quarterly reports for 2023"
Header: "ABC Corporation - Confidential"
Footer: "© 2024 ABC Corporation"
Logo: "company_logo.png"
PDF Files: ["q1_report.pdf", "q2_report.pdf", "q3_report.pdf", "q4_report.pdf"]
```

**Output**:

```
Merged PDF: "annual_report.pdf"
```

**Result**: Professional merged PDF with branding and all quarterly reports.

### Example 3: Retry Scenario (Allow Retry Enabled)

**Scenario**: User provides only one PDF file

**Interaction**:

```
User: "Merge these reports"
PDF Files: ["report1.pdf"]

System: "Please upload more than one PDF file for merging"
Prompt: "Provide at least 2 PDF files"

User: *adds "report2.pdf" and "report3.pdf"*

System: "Success! PDFs merged."
Output: "merged.pdf"
```

## Business Rules

1. **Minimum Files Required**: At least 2 PDF files must be provided for merging
2. **Merge Order**: Files are merged in the order they appear in the PDF Files list
3. **All Files Required**: All files in the list must be valid PDF files
4. **Optional Formatting**: Title, header, footer, and logo are optional
5. **Page Numbering**: If footer includes page numbers, they are continuous across all merged PDFs

## Limitations

1. **File Format**: Only PDF files can be merged; other formats must be converted first
2. **Password Protection**: Password-protected PDFs cannot be merged
3. **File Size**: Very large PDF files may impact performance or cause memory issues
4. **Minimum Files**: At least 2 files required; cannot merge a single file
5. **Bookmarks**: PDF bookmarks may not be preserved during merging
6. **Form Fields**: Interactive PDF form fields may not be preserved

## Best Practices

### 1. Organize Files in Desired Order

Arrange PDF files in the list in the order you want them to appear in the merged document.

### 2. Validate PDFs Before Merging

- Ensure all PDFs are not corrupted
- Remove password protection from PDFs
- Verify all files are valid PDF documents

### 3. Handle Large Files Carefully

For large PDF files:

- Consider merging in smaller batches
- Monitor system resources during merging
- Test with smaller files first

### 4. Use Consistent Formatting

- Ensure all PDFs have similar page sizes for best results
- Use consistent orientation (portrait/landscape)
- Consider page numbering across merged documents

### 5. Add Meaningful Metadata

- Use descriptive title and subtitle
- Add header/footer for context
- Include logo for branding

## Common Use Cases

### 1. Quarterly/Annual Reports

```
Scenario: Combine monthly reports into quarterly or annual reports
Action: Merge monthly PDF reports in chronological order
Result: Comprehensive quarterly or annual report
```

### 2. Document Compilation

```
Scenario: Combine multiple documents into a single package
Action: Merge related PDF documents
Result: Single PDF package for distribution
```

### 3. Proposal Assembly

```
Scenario: Assemble proposal from multiple sections
Action: Merge cover letter, proposal, pricing, terms PDFs
Result: Complete proposal document
```

### 4. Report Consolidation

```
Scenario: Consolidate department reports
Action: Merge individual department reports
Result: Organization-wide consolidated report
```

### 5. Archive Creation

```
Scenario: Create archive of related documents
Action: Merge historical documents into single archive
Result: Consolidated archive PDF
```

## Related Catalog Requests

- [Split PDF into Pages](pages/SplitPDFIntoPages.md) - Split PDF into individual pages
- [Split PDF Into Two Equal Part](pages/SplitPDFIntoTwoEqualPart.md) - Split PDF into two parts
- [Convert Docx File to PDF File](pages/ConvertDocxFileToPDFFile.md) - Convert documents before merging

## Technical Implementation

### Helper Class

- **Class**: `MergePdfFilesHelper`
- **Package**: `app.krista.extensions.essentials.documents.pdf.helper`
- **Validation**: Uses `ValidationOrchestrator` with `PdfFileFormatValidator` and `PdfFileCountValidator`
- **Service**: Delegates to `PDFManager.mergePDFFiles()`

### Telemetry Metrics

- **pdf.mergePDFFiles.count**: Total number of merge requests
- **pdf.mergePDFFiles.success**: Successful merges with duration
- **pdf.mergePDFFiles.error**: Failed merges with error details and duration

### Validation Flow

1. Validate PDF Files list is provided and not empty
2. Validate at least 2 PDF files are provided
3. Validate each file is a valid PDF format
4. If logo provided, download and validate logo file
5. Download all PDF files
6. Merge PDFs using PDFManager
7. Upload result and return success response
8. If validation fails, prompt user for correction (when Allow Retry enabled)

## Troubleshooting

### Issue: "Insufficient files for merging" error

**Cause**: Less than 2 PDF files provided

**Solution**:

1. Provide at least 2 PDF files
2. Verify PDF Files list contains multiple files
3. Check that files were uploaded correctly

### Issue: "File format not supported" error

**Cause**: One or more files are not valid PDFs

**Solution**:

1. Check all files have .pdf extension
2. Verify all files are valid PDF documents
3. Convert non-PDF files to PDF before merging
4. Remove corrupted files from the list

### Issue: "Request not working" error

**Cause**: This was a known issue (KE-2007) that has been resolved

**Solution**:

1. Ensure you're using extension version 2.0.7 or later
2. Verify all input parameters are correct
3. Check that PDF files are valid

### Issue: Merge fails with password-protected PDFs

**Cause**: Password-protected PDFs cannot be merged

**Solution**:

1. Remove password protection from PDFs
2. Use PDF editing software to unlock PDFs
3. Merge only unprotected PDFs

### Issue: Large file merge causes timeout

**Cause**: Too many or too large PDF files

**Solution**:

1. Reduce number of PDFs being merged
2. Merge in smaller batches
3. Compress PDFs before merging
4. Increase timeout settings if possible

### Issue: Merged PDF has formatting issues

**Cause**: Source PDFs have different page sizes or orientations

**Solution**:

1. Standardize page sizes in source PDFs
2. Use consistent orientation (all portrait or all landscape)
3. Accept that some formatting differences may occur

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Configure Allow Retry flag
- [Split PDF into Pages](pages/SplitPDFIntoPages.md) - Split merged PDFs
- [Release Notes](pages/release-notes.md) - Known issues and fixes


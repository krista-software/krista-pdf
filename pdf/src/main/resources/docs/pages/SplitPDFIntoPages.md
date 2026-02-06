# Split PDF into Pages

## Overview

Splits a PDF file into individual pages based on specified categories. Each page can be categorized and organized into
separate PDF files.

## Request Details

- **Area**: PDF
- **Type**: CHANGE_SYSTEM
- **Retry Support**:  Yes - When Allow Retry is enabled, provides comprehensive validation with user prompts for
  invalid PDF files

## Input Parameters

| Parameter Name  | Type       | Required | Description                                                | Example                                   |
|-----------------|------------|----------|------------------------------------------------------------|-------------------------------------------|
| File            | File       | Yes      | The PDF file to split into individual pages                | `document.pdf`                            |
| Category        | List<Text> | No       | Optional list of category names for organizing split pages | `["Section A", "Section B", "Section C"]` |
| Single Category | Boolean    | No       | If true, all pages are placed in a single category         | `false`                                   |

### Parameter Details

#### File (Required)

- **Format**: PDF file
- **Validation**: File format is validated when Allow Retry is enabled
- **Behavior**: Each page becomes a separate PDF file

#### Category (Optional)

- **Format**: List of text strings
- **Purpose**: Organize split pages into categories
- **Behavior**: Pages can be grouped by category names

#### Single Category (Optional)

- **Format**: Boolean (true/false)
- **Default**: false
- **Purpose**: When true, all pages are placed in a single category

## Output Parameters

| Parameter Name | Type       | Description                                                   |
|----------------|------------|---------------------------------------------------------------|
| Split Files    | List<File> | List of PDF files, one for each page of the original document |

## Validation Rules

**When Allow Retry = true**

| Validation                | Error Message                                                                              | Resolution                             |
|---------------------------|--------------------------------------------------------------------------------------------|----------------------------------------|
| File must be provided     | "Please provide a PDF file to split"                                                       | Upload a valid PDF file                |
| File format must be PDF   | "The provided file '[filename]' is not a valid PDF format. Only .pdf files are supported." | Upload a valid PDF file                |
| PDF must not be corrupted | "Failed to read PDF file: file may be corrupted"                                           | Upload a valid, non-corrupted PDF file |

**When Allow Retry = false**

No validation is performed. Invalid inputs will result in SYSTEM_ERROR.

## Error Handling

### Input Errors (INPUT_ERROR)

**When Allow Retry is enabled**

#### Missing PDF File

- **Cause**: File parameter is not provided
- **Error Message**: "Please provide a PDF file to split"
- **Resolution**: Upload a valid PDF file

#### Invalid PDF File Format

- **Cause**: The uploaded file is not a valid PDF
- **Error Message**: "The provided file '[filename]' is not a valid PDF format. Only .pdf files are supported."
- **Resolution**:
    1. Verify the file has a .pdf extension
    2. Ensure the file is a valid PDF document
    3. Try opening the file in a PDF viewer to verify it's not corrupted

#### Corrupted PDF File

- **Cause**: The PDF file is corrupted or cannot be read
- **Error Message**: "Failed to read PDF file: file may be corrupted"
- **Resolution**:
    1. Open the file in a PDF viewer to verify it's not corrupted
    2. Try re-saving the PDF
    3. Use a different PDF file

### System Errors (SYSTEM_ERROR)

**When Allow Retry is disabled or for unexpected errors**

#### PDF Splitting Failure

- **Cause**: Error during PDF splitting or file processing
- **Error Message**: "Failed to split PDF: [error details]"
- **Common Scenarios**:
    - Corrupted PDF file
    - Password-protected PDF
    - File read/write permissions issues
    - Insufficient memory for large PDFs
    - Network issues during file transfer
- **Resolution**:
    1. Verify the PDF file is not corrupted
    2. Remove password protection from PDF
    3. Try with a smaller PDF to isolate the issue
    4. Check system logs for detailed error information
    5. Ensure sufficient system resources are available

## Usage Examples

### Example 1: Split Document into Individual Pages

**Scenario**: Split a 10-page document into 10 separate PDF files

**Input**:

```
File: "report.pdf" (10 pages)
Category: (empty)
Single Category: false
```

**Output**:

```
Split Files: [
  "report_page_1.pdf",
  "report_page_2.pdf",
  ...
  "report_page_10.pdf"
]
```

**Result**: 10 separate PDF files, one for each page.

### Example 2: Split with Categories

**Scenario**: Split a document and organize pages by category

**Input**:

```
File: "manual.pdf" (20 pages)
Category: ["Introduction", "Setup", "Usage", "Troubleshooting"]
Single Category: false
```

**Output**:

```
Split Files: [
  "manual_Introduction_page_1.pdf",
  "manual_Setup_page_2.pdf",
  ...
]
```

**Result**: Pages split and organized by category names.

### Example 3: Retry Scenario (Allow Retry Enabled)

**Scenario**: User provides invalid file format

**Interaction**:

```
User: "Split this document"
File: "document.docx"

System: "The provided file 'document.docx' is not a valid PDF format. Only .pdf files are supported."
Prompt: "Please upload a valid PDF file (.pdf)"

User: *uploads "document.pdf"*

System: "Success! PDF split into pages."
Output: ["page_1.pdf", "page_2.pdf", ...]
```

## Business Rules

1. **One Page Per File**: Each page of the original PDF becomes a separate PDF file
2. **Page Order Preserved**: Pages are split in the order they appear in the original PDF
3. **Optional Categories**: Categories are optional; pages can be split without categorization
4. **File Naming**: Split files are named based on original filename and page number

## Limitations

1. **File Format**: Only PDF files can be split
2. **Password Protection**: Password-protected PDFs cannot be split
3. **Large PDFs**: Very large PDF files may impact performance or cause memory issues
4. **Page Content**: Each page is extracted as-is; no content modification
5. **Bookmarks**: PDF bookmarks are not preserved in split files

## Best Practices

### 1. Validate PDF Before Splitting

- Ensure PDF is not corrupted
- Remove password protection if present
- Verify PDF opens correctly in a viewer

### 2. Use Meaningful Categories

- Provide descriptive category names
- Organize categories logically
- Match category count to document structure

### 3. Handle Large PDFs Carefully

For large PDF files:

- Test with smaller PDFs first
- Monitor system resources during splitting
- Consider splitting in batches if very large

### 4. Plan File Organization

- Decide on category structure before splitting
- Use consistent naming conventions
- Consider how split files will be used

### 5. Enable Allow Retry for User Workflows

When users are providing files interactively, enable Allow Retry to provide helpful validation messages.

## Common Use Cases

### 1. Document Distribution

```
Scenario: Distribute individual pages to different recipients
Action: Split multi-page document into individual pages
Result: Separate PDF for each page
```

### 2. Page Extraction

```
Scenario: Extract specific pages from a document
Action: Split document and select needed pages
Result: Individual page PDFs for further use
```

### 3. Manual Organization

```
Scenario: Organize document pages by section
Action: Split with category names for each section
Result: Categorized page PDFs
```

### 4. Archive Management

```
Scenario: Archive individual pages separately
Action: Split document for page-level archival
Result: Individual page archives
```

## Related Catalog Requests

- [Split PDF Into Two Equal Part](/pages/SplitPDFIntoTwoEqualPart.md) - Split PDF into two parts
- [Split PDF Based On Condition](/pages/SplitPDFBasedOnCondition.md) - Split PDF based on criteria
- [Merge PDF Files](/pages/MergePDFFiles.md) - Merge split PDFs back together

## Technical Implementation

### Helper Class

- **Class**: `SplitPdfHelper`
- **Package**: `app.krista.extensions.essentials.documents.pdf.helper`
- **Validation**: Uses `ValidationOrchestrator` with `SplitPdfFileFormatValidator`
- **Service**: Delegates to `PDFManager.splitPDF()`

### Telemetry Metrics

- **pdf.splitPDFIntoPages.count**: Total number of split requests
- **pdf.splitPDFIntoPages.success**: Successful splits with duration
- **pdf.splitPDFIntoPages.error**: Failed splits with error details and duration

### Validation Flow

1. Validate File is provided and is a valid PDF
2. If validation passes, download PDF file
3. Split PDF into individual pages using PDFManager
4. Upload all split files
5. Return list of split files
6. If validation fails, prompt user for correction (when Allow Retry enabled)

## Troubleshooting

### Issue: "File format not supported" error

**Cause**: File is not a valid PDF

**Solution**:

1. Check file extension is .pdf
2. Verify file is a valid PDF document
3. Convert non-PDF files to PDF before splitting

### Issue: "File may be corrupted" error

**Cause**: PDF file is corrupted or cannot be read

**Solution**:

1. Open PDF in a viewer to verify it's not corrupted
2. Try re-saving the PDF
3. Use PDF repair tools if available
4. Use a different PDF file

### Issue: Split fails with password-protected PDF

**Cause**: Password-protected PDFs cannot be split

**Solution**:

1. Remove password protection from PDF
2. Use PDF editing software to unlock PDF
3. Split only unprotected PDFs

### Issue: Large PDF split causes timeout

**Cause**: Too many pages or large file size

**Solution**:

1. Split smaller PDFs first to test
2. Reduce PDF file size if possible
3. Increase timeout settings if possible
4. Consider splitting in batches

### Issue: Split files not organized as expected

**Cause**: Category configuration may not match expectations

**Solution**:

1. Review Category parameter settings
2. Verify Single Category setting
3. Adjust category names as needed
4. Test with sample PDF first

## See Also

- [Extension Configuration](/pages/ExtensionConfiguration.md) - Configure Allow Retry flag
- [Split PDF Into Two Equal Part](/pages/SplitPDFIntoTwoEqualPart.md) - Alternative splitting method
- [Merge PDF Files](/pages/MergePDFFiles.md) - Merge split PDFs


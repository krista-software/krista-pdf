# Split PDF Based on Condition

## Overview

Splits a PDF file at a specific page number into two separate files. This feature provides precise control over where
the PDF is split, making it ideal for separating documents at known page boundaries.

**Key Feature**: Returns two separate file variables (`First Segment` and `Second Segment`) instead
of a list, making it much easier to use in Krista Studio workflows.

## Request Details

- **Area**: PDF
- **Type**: CHANGE_SYSTEM
- **Retry Support**: Yes - When Allow Retry is enabled, provides comprehensive validation with user prompts for invalid
  PDF files

## Input Parameters

| Parameter Name        | Type   | Required | Description                                                      | Example        |
|-----------------------|--------|----------|------------------------------------------------------------------|----------------|
| Input File            | File   | Yes      | The PDF file to split at a specific page number                  | `document.pdf` |
| Split at Page Number  | Number | Yes      | Page number where split occurs (becomes first page of 2nd file)  | `6`            |

### Parameter Details

#### Input File (Required)

- **Format**: PDF file
- **Validation**: File format is validated when Allow Retry is enabled
- **Behavior**: The PDF is split at the specified page number

#### Split at Page Number (Required)

- **Format**: Number (integer)
- **Range**: Must be between 2 and the total number of pages in the PDF
- **Behavior**: This page becomes the first page of the second file
- **Example**: If you specify page 6, the first file contains pages 1-5, and the second file contains pages 6 to the
  end

## Output Parameters

| Parameter Name   | Type | Description                                                                  |
|------------------|------|------------------------------------------------------------------------------|
| First Segment    | File | PDF file containing pages from the beginning up to (but not including) the split page |
| Second Segment   | File | PDF file containing pages from the split page to the end of the document     |

### Output Format Change (December 2025)

**Previous Format**: Returned a list called "Split Files" containing two files.

**New Format**: Returns two separate file variables for easier use in Studio:

- **Pages Before Split Page**: First part of the split PDF
- **Pages After Split Page**: Second part of the split PDF

**Benefits**:

- No need to extract files from a list
- Direct access to each file by name
- Clear semantic meaning
- Type-safe access in workflows

## Validation Rules

**When Allow Retry = true**

| Validation                     | Error Message                                                                              | Resolution                             |
|--------------------------------|--------------------------------------------------------------------------------------------|----------------------------------------|
| Input File must be provided    | "Please provide a PDF file to split"                                                       | Upload a valid PDF file                |
| File format must be PDF        | "The provided file '[filename]' is not a valid PDF format. Only .pdf files are supported." | Upload a valid PDF file                |
| PDF must not be corrupted      | "Failed to read PDF file: file may be corrupted"                                           | Upload a valid, non-corrupted PDF file |
| Split page must be provided    | "Please provide a valid page number for splitting"                                         | Provide a page number between 2 and total pages |
| Split page must be valid range | "Split page must be between 2 and [total pages]"                                           | Provide a page number within valid range |

**When Allow Retry = false**

No validation is performed. Invalid inputs will result in SYSTEM_ERROR.

## Error Handling

### Input Errors (INPUT_ERROR)

**When Allow Retry is enabled**

#### Missing PDF File

- **Cause**: Input File parameter is not provided
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

#### Invalid Split Page Number

- **Cause**: Split page number is out of valid range
- **Error Message**: "Split page must be between 2 and [total pages]"
- **Common Scenarios**:
    - Split page is 1 (must be at least 2)
    - Split page exceeds total pages in PDF
    - Split page is negative or zero
- **Resolution**:
    1. Verify the PDF has enough pages
    2. Choose a split page between 2 and the total number of pages
    3. Remember: page 1 cannot be a split point (would result in empty first file)

### System Errors (SYSTEM_ERROR)

**When Allow Retry is disabled or for unexpected errors**

#### PDF Splitting Failure

- **Cause**: Error during PDF splitting or file processing
- **Error Message**: "Failed to split PDF at page [page number]: [error details]"
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

### Example 1: Split at Middle of Document

**Scenario**: Split a 10-page document at page 6

**Input**:

```
Input File: "manual.pdf" (10 pages)
Split at Page Number: 6
```

**Output**:

```
First Segment: "manual_part1.pdf" (pages 1-5)
Second Segment: "manual_part2.pdf" (pages 6-10)
```

**Result**: Document split into two files - first contains pages 1-5, second contains pages 6-10.

**Usage in Studio**:

```
Step 1: Split PDF Based on Condition
  Input: manual.pdf
  Split at: 6

Step 2: Process First Part
  Input: {{First Segment}}  ← Direct access, no list extraction needed!

Step 3: Process Second Part
  Input: {{Second Segment}}  ← Direct access, no list extraction needed!
```

### Example 2: Split at Beginning (Page 2)

**Scenario**: Separate cover page from rest of document

**Input**:

```
Input File: "report.pdf" (50 pages)
Split at Page Number: 2
```

**Output**:

```
First Segment: "report_part1.pdf" (page 1 - cover page)
Second Segment: "report_part2.pdf" (pages 2-50 - main content)
```

**Result**: Cover page separated from main content.

### Example 3: Split at End (Last Page)

**Scenario**: Separate last page (signature page) from document

**Input**:

```
Input File: "contract.pdf" (20 pages)
Split at Page Number: 20
```

**Output**:

```
First Segment: "contract_part1.pdf" (pages 1-19 - contract terms)
Second Segment: "contract_part2.pdf" (page 20 - signature page)
```

**Result**: Signature page separated from contract terms.

### Example 4: Retry Scenario (Allow Retry Enabled)

**Scenario**: User provides invalid split page number

**Interaction**:

```
User: "Split this document at page 50"
File: "document.pdf" (10 pages)
Split at Page Number: 50

System: "Split page must be between 2 and 10"
Prompt: "Please provide a valid page number between 2 and 10"

User: *enters "6"*

System: "Success! PDF split at page 6."
Output:
  - Pages Before Split Page: pages 1-5
  - Pages After Split Page: pages 6-10
```

## Business Rules

1. **Page-Based Split**: The PDF is split at the exact page number specified
2. **Split Page Inclusion**: The split page becomes the first page of the second file
3. **Minimum Split Page**: Split page must be at least 2 (cannot split before page 1)
4. **Maximum Split Page**: Split page cannot exceed the total number of pages
5. **Always Two Files**: Always produces exactly two files (unless an error occurs)
6. **Separate Variables**: Returns two separate file variables, not a list

## Limitations

1. **File Format**: Only PDF files can be split
2. **Password Protection**: Password-protected PDFs cannot be split
3. **Large PDFs**: Very large PDF files may impact performance or cause memory issues
4. **Two Parts Only**: Always splits into exactly two parts
5. **No Content Analysis**: Splitting is based purely on page number, not content
6. **Page Range**: Cannot split at page 1 (would result in empty first file)

## Best Practices

### 1. Know Your Split Point

- Identify the exact page number where you want to split
- Remember: the split page becomes the first page of the second file
- Use a PDF viewer to verify page numbers before splitting

### 2. Validate PDF Before Splitting

- Ensure PDF is not corrupted
- Remove password protection if present
- Verify PDF opens correctly in a viewer
- Check total page count to ensure split page is valid

### 3. Handle Large PDFs Carefully

For large PDF files:

- Test with smaller PDFs first
- Monitor system resources during splitting
- Consider splitting large PDFs into smaller chunks

### 4. Use Descriptive Variable Names in Studio

The output variables have clear names:

- `Pages Before Split Page` - Use for content before the split
- `Pages After Split Page` - Use for content after the split

No need to extract from lists or use array indices!

### 5. Enable Allow Retry for User Workflows

When users are providing files interactively, enable Allow Retry to provide helpful validation messages and prevent
invalid split page numbers.

## Common Use Cases

### 1. Separate Cover Page from Content

```
Scenario: Remove cover page from document
Action: Split at page 2
Result:
  - First Segment: Cover page only
  - Second Segment: Main content (pages 2-end)
```

### 2. Extract Signature Page

```
Scenario: Separate signature page from contract
Action: Split at last page
Result:
  - First Segment: Contract terms
  - Second Segment: Signature page
```

### 3. Split at Chapter Boundary

```
Scenario: Separate chapters in a manual
Action: Split at page where new chapter starts (e.g., page 25)
Result:
  - First Segment: Chapters 1-3 (pages 1-24)
  - Second Segment: Chapters 4-6 (pages 25-end)
```

### 4. Separate Appendix from Main Document

```
Scenario: Extract appendix from report
Action: Split at page where appendix starts (e.g., page 40)
Result:
  - First Segment: Main report (pages 1-39)
  - Second Segment: Appendix (pages 40-end)
```

### 5. Workflow Processing

```
Scenario: Process different sections with different workflows
Action: Split at logical boundary
Studio Workflow:
  Step 1: Split PDF at page 15
  Step 2: Send {{Pages Before Split Page}} to Workflow A
  Step 3: Send {{Pages After Split Page}} to Workflow B
```

## Related Catalog Requests

- [Split PDF into Pages](pages/SplitPDFIntoPages.md) - Split PDF into individual pages
- [Merge PDF Files](pages/MergePDFFiles.md) - Merge split PDFs back together

## Technical Implementation

### Helper Class

- **Class**: `SplitPdfAtPageNumberHelper`
- **Package**: `app.krista.extensions.essentials.documents.pdf.helper`
- **Processor**: `PdfSplitProcessor`
- **Validation**: Uses `ValidationOrchestrator` with `SplitPdfConditionFileFormatValidator`
- **Service**: Delegates to `PDFManager.splitPDFAtPageNumber()`

### Telemetry Metrics

- **pdf.splitPDFAtPageNumber.count**: Total number of split requests
- **pdf.splitPDFAtPageNumber.success**: Successful splits with duration
- **pdf.splitPDFAtPageNumber.error**: Failed splits with error details and duration

### Validation Flow

1. Validate Input File is provided and is a valid PDF
2. Validate Split Page Number is provided and within valid range (2 to total pages)
3. If validation passes, download PDF file
4. Split PDF at specified page number using PDFManager
5. Upload split files
6. Return two separate file variables (First Segment, Second Segment)
7. If validation fails, prompt user for correction (when Allow Retry enabled)

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

### Issue: "Split page must be between 2 and X" error

**Cause**: Split page number is out of valid range

**Solution**:

1. Verify the PDF has enough pages
2. Choose a split page between 2 and the total number of pages
3. Remember: page 1 cannot be a split point (would result in empty first file)
4. Use a PDF viewer to check the total page count

**Examples**:

- 10-page PDF: Valid split pages are 2-10
- 5-page PDF: Valid split pages are 2-5
- Cannot split at page 1 (no pages before split)
- Cannot split beyond last page

### Issue: Split fails with password-protected PDF

**Cause**: Password-protected PDFs cannot be split

**Solution**:

1. Remove password protection from PDF
2. Use PDF editing software to unlock PDF
3. Split only unprotected PDFs

### Issue: Large PDF split causes timeout

**Cause**: PDF file is too large

**Solution**:

1. Split smaller PDFs first to test
2. Reduce PDF file size if possible
3. Increase timeout settings if possible
4. Consider using [Split PDF into Pages](pages/SplitPDFIntoPages.md) for very large files

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Configure Allow Retry flag
- [Split PDF into Pages](pages/SplitPDFIntoPages.md) - Alternative splitting method
- [Merge PDF Files](pages/MergePDFFiles.md) - Merge split PDFs


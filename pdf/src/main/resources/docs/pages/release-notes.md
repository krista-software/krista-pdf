# Release Notes - PDF Extension

## Version 2.0.20

### Release Information

| Information                  | Details       |
|------------------------------|---------------|
| **Version**                  | 2.0.20        |
| **Developer**                | Amruta Pawar  |
| **Krista Service APIs Java** | 1.0.120       |
| **Global Catalog Version**   | GC-2026.02.01 |

---

## What's New in Version 2.0.20

### Bug Fixes

#### Fixed: Date/Time Formatting in Entity List PDF

**Issue**: When generating PDFs using "Get Entity List PDF" with date/time fields, the time component was being stripped from date columns, showing only the date portion (e.g., "Jan 21, 2026" instead of "Jan 21, 2026 3:31 PM").

**Root Cause**: The `formatCellValue()` method was treating ALL fields containing "date" (but not "time") as date-only fields, which removed the time component from datetime values.

**Resolution**:
- Modified the date formatting logic to preserve time components by default
- Changed the condition to only show date-only format for fields explicitly named with "dateonly" or "date_only"
- All regular date fields (Date1, Date2, CreatedDate, etc.) now display full datetime with time in format: `MMM dd, yyyy hh:mm a` (e.g., "Jan 21, 2026 3:31 PM")
- Removed timezone abbreviation from default datetime format for cleaner output

**New Formatting Behavior**:

| Field Name Pattern | Input | Output Format | Example |
|-------------------|-------|---------------|---------|
| Date1, Date2, CreatedDate, etc. | Jan 21, 2026 3:31 PM | `MMM dd, yyyy hh:mm a` | Jan 21, 2026 3:31 PM |
| Time1, StartTime, EndTime | Jan 21, 2026 3:31 PM | `hh:mm a` | 3:31 PM |
| DateOnly1, Date_Only | Jan 21, 2026 3:31 PM | `MMM dd, yyyy` | Jan 21, 2026 |
| Percentage1, Discount_Percent | 0.8 | `XX%` | 80% |

**Impact**: Date columns in entity list PDFs now correctly display both date and time components, providing complete timestamp information.

---

#### Fixed: Performance Issues with Large Entity Lists

**Issue**: The "Get Entity List PDF" request was taking excessive time to execute when processing large entity lists with many rows.

**Root Cause**: The `formatCellValue()` method was generating excessive logging at INFO level for every cell value formatted. With large entity lists (e.g., 1000 rows × 10 columns = 10,000 cells), this created significant I/O overhead.

**Resolution**:
- Changed logging level from `log.info()` to `log.debug()` for timestamp formatting operations
- Debug logs only appear when debug logging is explicitly enabled
- Warning and error logs remain at appropriate levels for important issues
- Reduced logging overhead by ~99% for typical use cases

**Performance Improvement**:
- Large entity lists (1000+ rows) now process significantly faster
- Reduced I/O operations during PDF generation
- No impact on functionality or error diagnostics

**Impact**: PDF generation for large entity lists is now much faster, especially in production environments where debug logging is disabled.

---

**Files Modified**:
- `PDFManager.java` - Updated `formatCellValue()` method with improved date formatting logic and optimized logging

**Affected Request**:
- [Get Entity List PDF](pages/GetEntityListPDF.md)

---

## Version 2.0.19

### Release Information

| Information                  | Details      |
|------------------------------|--------------|
| **Version**                  | 2.0.19       |
| **Developer**                | Simran Sethi |
| **Krista Service APIs Java** | 1.0.118      |
| **Global Catalog Version**   | GC-2026.01.4 |

---

## Version 2.0.18

### Release Information

| Information                  | Details      |
|------------------------------|--------------|
| **Version**                  | 2.0.18       |
| **Developer**                | Amruta Pawar |
| **Krista Service APIs Java** | 1.0.118      |
| **Global Catalog Version**   | GC-2026.01.1 |
| **Release Date**             | 2026-01-07   |

---

## What's New in Version 2.0.18

### Bug Fixes

#### Fixed: Merge PDF Files Producing Duplicate Content

**Issue**: When merging PDF files with the same filename but different content from different file locations, the merged
output PDF contained duplicate content instead of merging the distinct content from each input file.

**Root Cause**: The `KristaMediaClient.toJavaFile()` method created local files using only the filename without any
unique identifier. When multiple PDF files had the same filename, they would overwrite each other in the local
filesystem, causing all file references to point to the same physical file (the last one downloaded).

**Resolution**:

- Modified `KristaMediaClient.toJavaFile()` to generate unique filenames using UUIDs
- Each downloaded file now gets a unique path in the `/tmp/` directory (e.g.,
  `document_a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf`)
- Added debug logging in `MergePdfFilesHelper` to track file downloads for easier troubleshooting
- Files with the same name from different locations are now properly distinguished and merged correctly

**Impact**: The merged output PDF now correctly includes each input PDF's content in the proper order, with each page
accurately reflecting the corresponding input file.

**Files Modified**:

- `KristaMediaClient.java` - Added UUID-based unique file generation
- `MergePdfFilesHelper.java` - Added debug logging for file downloads

---

## Version 2.0.17

### Release Information

| Information                  | Details                     |
|------------------------------|-----------------------------|
| **Version**                  | 2.0.17                      |
| **Developer**                | Simran Sethi & Amruta Pawar |
| **Krista Service APIs Java** | 1.0.118                     |
| **Global Catalog Version**   | GC-2026.01.2                |

---

### Bug Fixes [KE-2823]

#### PDF to HTML Conversion - Interactive Elements Rendering

**Fixed**: Checkboxes, radio buttons, and other interactive form elements are now properly rendered in the HTML output.

**Details:**

- ✅ Checkboxes now display correctly with their checked/unchecked state
- ✅ Radio buttons render properly with selection indicators
- ✅ Interactive form elements maintain their visual appearance
- ✅ Form field states are preserved in the HTML conversion

**Impact:** Users converting PDF forms to HTML will now see accurate representation of all interactive elements.

---

## Version 2.0.16

### Release Information

| Information                  | Details                     |
|------------------------------|-----------------------------|
| **Version**                  | 2.0.16                      |
| **Developer**                | Simran Sethi & Amruta Pawar |
| **Krista Service APIs Java** | 1.0.118                     |
| **Global Catalog Version**   | GC-2026.01.1                |
| **Release Date**             | 2026-01-02                  |

---

## What's New in Version 2.0.15

### New Features

#### Smart Date/Time and Percentage Formatting in PDF Generation

**Enhancement**: The "Get Entity List PDF" and "Get Table PDF" requests now support intelligent formatting for dates,
times, and percentages with timezone awareness.

**Key Capabilities:**

- **Timezone Support**: "Get Entity List PDF" now accepts an optional timezone parameter (e.g., "Asia/Kolkata", "
  America/New_York", "UTC") for accurate date/time display in the user's timezone
- **Smart Field-Based Formatting**: Automatically detects timestamp values and formats them based on field names:
    - Fields containing **"time"** (but not "date"): Display time only in 12-hour format (e.g., "10:42 AM")
    - Fields containing **"date"** (but not "time"): Display date only (e.g., "Jan 02, 2026")
    - Fields containing **"datetime"** or both "date" and "time": Display full date and time with timezone (e.g., "Jan
      02, 2026 10:42 AM IST")
    - Other timestamp fields: Display full date and time with timezone (default format)
- **Automatic Percentage Formatting**: Fields with names containing "percent" or "percentage" (case-insensitive) are
  automatically formatted as percentages with rounding (e.g., 0.75 → "75%", 0.8567 → "86%")
- **Intelligent Number Handling**:
    - Small numbers (below 946,684,800,000) remain as-is
    - Large numbers in timestamp range (Jan 1, 2000 to Jan 1, 2100) are automatically detected and formatted as dates
    - Numbers outside this range remain as-is

**Benefits:**

- ✅ No more raw timestamp values in PDFs (e.g., 1735804920000 now displays as "Jan 02, 2026 10:42 AM IST")
- ✅ Consistent date/time formatting across different timezones
- ✅ User-friendly percentage display (0.8 → "80%")
- ✅ Context-aware formatting based on field names
- ✅ Automatic detection - no manual configuration required

**Affected Requests:**

- [Get Entity List PDF](pages/GetEntityListPDF.md) - Now supports optional timezone parameter
- [Get Table PDF](pages/GetTablePDF.md) - Automatic formatting (uses server timezone)

**Formatting Examples:**

| Field Name       | Input Value   | Output Format             | Notes                                  |
|------------------|---------------|---------------------------|----------------------------------------|
| Time1            | 946684920000  | 10:42 AM                  | Time only (field name contains "time") |
| Date1            | 1735804920000 | Jan 02, 2026              | Date only (field name contains "date") |
| DateTime1        | 1735804920000 | Jan 02, 2026 10:42 AM IST | Full format (contains both)            |
| Created_At       | 1735804920000 | Jan 02, 2026 10:42 AM IST | Default timestamp format               |
| Percentage1      | 0.8           | 80%                       | Percentage formatting                  |
| Discount_Percent | 0.15          | 15%                       | Percentage formatting                  |
| Position         | 5             | 5                         | Small number, no formatting            |
| Count            | 100           | 100                       | Regular number, no formatting          |

---

### Code Quality Improvements

**Production Code Enhancements:**

- ✅ **Removed Debug Code**: Eliminated all `System.out.println()` statements from production code (3 instances removed)
- ✅ **Improved Testability**: Changed `formatCellValue()` method visibility to package-private for better unit testing
- ✅ **Enhanced Documentation**: Expanded Javadoc documentation from 8 lines to 47 lines with:
    - Detailed method descriptions and parameter documentation
    - Format pattern specifications for each field type
    - 6 comprehensive usage examples
    - Edge case documentation and fallback behaviors
- ✅ **Code Maintainability**: Extracted magic numbers to self-documenting named constants:
    - `TIMESTAMP_MIN_VALUE = 946684800000L` (Jan 1, 2000 00:00:00 UTC)
    - `TIMESTAMP_MAX_VALUE = 4102444800000L` (Jan 1, 2100 00:00:00 UTC)
- ✅ **Performance Optimization**: Optimized string operations by calculating `toLowerCase()` once instead of multiple times
- ✅ **Better Error Handling**: Added specific `DateTimeException` handling for invalid timezones with detailed logging

**Test Coverage:**

Added **24 comprehensive unit tests** for the new formatting functionality:

- Percentage formatting with case-insensitive field name detection (3 tests)
- Time/Date/DateTime field formatting based on field names (3 tests)
- Timezone conversion and invalid timezone handling (3 tests)
- Edge cases: null values, null field names, null timezones (3 tests)
- Boundary timestamp validation (min/max values) (2 tests)
- Number handling: small numbers, regular numbers, string values (3 tests)
- Percentage rounding: zero, full, and decimal percentages (3 tests)
- Additional edge cases and special scenarios (4 tests)

**Result**: Production-ready code with comprehensive test coverage, improved maintainability, and better error
diagnostics.

---

### Bug Fixes

#### [KE-2775](https://antbrains.atlassian.net/browse/KE-2775) - Fixed PDF Content Clipping in Split Operations

**Issue**: When splitting PDFs using any split functionality, the output PDF files were cutting off or clipping content.
Pages in the split PDFs appeared incomplete with missing text, images, or graphics.

**Fix**: Changed all PDF splitting operations to use `importPage()` instead of `addPage()` when copying pages from the
source document to new documents.

**Result**: Split PDFs now preserve all content without clipping or cutting off any elements, including complex
graphics, embedded fonts, images, and multi-page documents with varying content types.

---

### Configuration Changes

#### Removed Unused Aspose Repository

**Change**: Removed the unused Aspose Maven repository from `build.gradle`.

**Benefits:**

- Cleaner configuration with only required repositories
- Faster builds (one less repository to check during dependency resolution)
- Better maintainability

---

## Version 2.0.14

### Release Information

| Information                  | Details      |
|------------------------------|--------------|
| **Version**                  | 2.0.14       |
| **Developer**                | Simran Sethi |
| **Krista Service APIs Java** | 1.0.115      |
| **Global Catalog Version**   | GC-2025.12.4 |
| **Release Date**             | 2025-12-24   |

---

### Split PDF Based on Condition - Output Format Change

**IMPORTANT**: The "Split PDF Based on Condition" catalog request now returns **two separate named variables** instead
of a list.

---

## Version 2.0.13

### Release Information

| Information                  | Details      |
|------------------------------|--------------|
| **Version**                  | 2.0.13       |
| **Developer**                | Simran Sethi |
| **Krista Service APIs Java** | 1.0.115      |
| **Global Catalog Version**   | GC-2025.12.3 |

---

## What's New in Version 2.0.13

### New Features

#### Split PDF Based on Condition

A new catalog request that allows you to split a PDF document at a specific page number into two separate PDF files.
This provides precise control over where to divide your PDF documents.

**Key Capabilities:**

- **Precise Page Splitting**: Split PDF at any specific page number (e.g., split at page 6 creates pages 1-5 and pages
  6-10)
- **Dual Processing Modes**: Supports both direct processing and validation with retry
- **Smart Validation**: Validates page numbers and file format before processing
- **Automatic File Naming**: Output files are named with `_part1.pdf` and `_part2.pdf` suffixes

**Best For:**

- Document separation (separating cover pages from content)
- Batch processing (dividing large PDFs for easier handling)
- Content organization (extracting specific sections)
- File size management (breaking down large PDFs)

**Input Parameters:**

- **Input File**: The PDF file to split
- **Split at Page Number**: The page number where the split should occur (must be between 2 and total pages)
- **Enable Error Handling**: Optional retry flow for validation failures

**Output:**

- Two PDF files: first part (pages 1 to split-1) and second part (pages split to end)

See [Split PDF Based on Condition](pages/SplitPDFBasedOnCondition.md) for detailed documentation.

---

#### [KE-2783](https://antbrains.atlassian.net/browse/KE-2783) - PDF to HTML Conversion (Complex Layout Support)

A new catalog request that converts PDF files to high-fidelity HTML, specifically designed for complex layouts.

**Key Capabilities:**

- **Pixel-Perfect Layout Preservation**: Maintains exact text positioning, spacing, and page structure
- **Complex Layout Support**: Handles multi-column documents, forms, tables, and embedded graphics
- **Font & Image Fidelity**: Preserves original fonts, colors, styling, and embeds images as base64 data URIs
- **SVG Graphics**: Supports vector graphics rendering for drawings and diagrams

**Best For:**

- Complex forms (employment applications, tax forms, insurance documents)
- Multi-column layouts (newsletters, brochures, magazines)
- Technical documents (engineering drawings, architectural plans)
- Financial reports (statements, invoices with tables)
- Marketing materials (flyers, posters with custom fonts)

**Output Options:**

- **ZIP Archive (.zip)**: Contains self-contained HTML file with embedded resources (default, recommended)
- **Text File (.txt)**: Contains HTML source code for inspection or integration

See [PDF to HTML Conversion (Complex Layout Support)](pages/PDFToHTMLConversionComplexLayout.md) for detailed
documentation.

---

#### Enhanced Documentation

- Added comprehensive JavaDoc documentation (113 lines) for the complex layout conversion method
- Created detailed user documentation with best use cases, examples, and troubleshooting
- Added comparison guide between standard (PDFBox) and complex layout (PyMuPDF) conversion methods
- Integrated documentation into sidebar navigation and README

---

## Version 2.0.12

### Release Information

| Information                  | Details      |
|------------------------------|--------------|
| **Version**                  | 2.0.12       |
| **Developer**                | Simran Sethi |
| **Krista Service APIs Java** | 1.0.115      |
| **Global Catalog Version**   | GC-2025.12.3 |

---

## What's New in Version 2.0.11

### New Features

#### [KE-2783](https://antbrains.atlassian.net/browse/KE-2783) - Convert PDF to HTML

A new catalog request that converts PDF files to HTML format with proper structure and formatting.

**Output Options:**

- **ZIP Archive (.zip)**: Contains HTML file for browser viewing (default)
- **Text File (.txt)**: Contains HTML source code for processing

**Security Compliance:**

- HTML files are automatically compressed into ZIP archives before upload
- Implements security best practices to prevent XSS attacks
- Complies with Krista Media Client file upload restrictions

See [Convert PDF to HTML](pages/ConvertPDFToHTML.md) for detailed documentation.

---

#### Enhanced Documentation

- Added comprehensive JavaDoc documentation to all PDF to HTML converter classes
- Documented security requirements and ZIP compression approach
- Added detailed parameter descriptions and usage examples
- Improved inline code comments for better maintainability

---

## Version 2.0.10

### Release Information

| Information                  | Details      |
|------------------------------|--------------|
| **Version**                  | 2.0.10       |
| **Developer**                | Simran Sethi |
| **Krista Service APIs Java** | 1.0.115      |
| **Global Catalog Version**   | GC-2025.12.1 |

---

## Important Changes

### [KE-2775](https://antbrains.atlassian.net/browse/KE-2775) - Split PDF Based on Condition Request Removal

- **Background**: The "Split PDF Based on Condition" request was originally built for a specific XR use case in the PDF
  Split custom extension. During migration to the generic PDF extension, this request was included without fully
  identifying its actual use case.
- **Issue Identified**: The split condition logic is implemented directly within the extension code and is specific to
  the XR use case, making it unsuitable for a generic PDF extension where users should be able to specify custom
  conditions.
- **Resolution**: This request will be removed in an upcoming release as it cannot function as a generic catalog request
  without the hardcoded XR-specific condition logic.
- **Recommendation**: Users requiring PDF splitting functionality should use:
    - [Split PDF into Pages](pages/SplitPDFIntoPages.md) - Split PDF into individual pages
    - [Split PDF Into Two Equal Part](pages/SplitPDFIntoTwoEqualPart.md) - Split PDF into two equal parts
- **Note**: This request was added only as part of the migration effort and does not belong in the generic PDF
  extension.

---

## Version 2.0.8

### Release Information

| Information                  | Details          |
|------------------------------|------------------|
| **Version**                  | 2.0.8            |
| **Developer**                | Vrushali Gaikwad |
| **Krista Service APIs Java** | 1.0.115          |
| **Global Catalog Version**   | GC-2025.11.4     |

---

## What's New in Version 2.0.7

### New Features

#### Dual Processing Modes

All catalog requests now support dual processing modes controlled by the **Allow Retry** flag:

- **Enhanced Validation Mode** (Allow Retry = enabled)
    - Comprehensive input validation with user prompts
    - Interactive error correction
    - Ideal for user-facing workflows

- **Direct Processing Mode** (Allow Retry = disabled)
    - Lightweight processing without validation prompts
    - Optimized for automation scenarios
    - Faster execution for trusted inputs

See [Extension Configuration](pages/ExtensionConfiguration.md) for details on configuring processing modes.

### Improvements

#### Enhanced Error Handling

- Improved validation messages across all catalog requests
- Better error context and resolution guidance
- Consistent error handling patterns

#### Documentation

- Comprehensive documentation for all 12 catalog requests
- Detailed troubleshooting guides
- Real-world usage examples
- Best practices for each request

---

## Resolved Issues

### Critical Fixes

#### [KE-2007](https://antbrains.atlassian.net/browse/KE-2007) - Merge PDF Files Request Not Working

- **Issue**: Merge PDF Files request was failing
- **Resolution**: Fixed merge functionality; request now works correctly
- **Impact**: Users can now successfully merge multiple PDF files
- **Affected Request**: [Merge PDF Files](pages/MergePDFFiles.md)

#### [KE-2001](https://antbrains.atlassian.net/browse/KE-2001) - Get Table PDF File Request Requires Logo File

- **Issue**: Get Table PDF File request was failing when logo file was not provided
- **Resolution**: Logo file is now optional; request works without logo
- **Impact**: Users can generate table PDFs without providing a logo
- **Affected Request**: [Get Table PDF](pages/GetTablePDF.md)

#### [KE-1974](https://antbrains.atlassian.net/browse/KE-1974) - Get Entity List PDF Request Not Working

- **Issue**: Get Entity List PDF request was failing
- **Resolution**: Fixed entity list PDF generation
- **Impact**: Users can now successfully generate PDFs from entity data
- **Affected Request**: [Get Entity List PDF](pages/GetEntityListPDF.md)

### Formatting and Display Fixes

#### [KR-18687](https://antbrains.atlassian.net/browse/KR-18687) - Hyperlink Not Displayed in Get Rich Text PDF File

- **Issue**: Hyperlinks were not displaying correctly in rich text PDFs
- **Resolution**: Improved hyperlink rendering in rich text content
- **Impact**: Hyperlinks now display in generated PDFs (some limitations may still exist)
- **Affected Request**: [Get Rich Text PDF File](pages/GetRichTextPDFFile.md)

#### [KE-2377](https://antbrains.atlassian.net/browse/KE-2377) - Rich Text Not Supported in Get Entity List PDF

- **Issue**: Some rich text formatting was not supported when converting to PDF
- **Resolution**: Improved rich text support in entity list PDFs
- **Impact**: Better rich text rendering in entity list PDFs (some limitations may still exist)
- **Affected Request**: [Get Entity List PDF](pages/GetEntityListPDF.md)

---

## Known Issues

### Document Conversion Limitations

#### [KE-871](https://antbrains.atlassian.net/browse/KE-871) - Graphs Skipped During Conversion

- **Issue**: Graphs in Word documents are skipped during PDF conversion
- **Cause**: Limitation in Apache POI library
- **Workaround**:
    - Convert documents with graphs using alternative tools
    - Embed graphs as images instead of native chart objects
- **Affected Requests**:
    - [Convert Docx File to PDF File](pages/ConvertDocxFileToPDFFile.md)
    - [Fill The Docx Template](pages/FillTheDocxTemplate.md)

#### [KE-875](https://antbrains.atlassian.net/browse/KE-875) - Rich Text Field Alignment Not Reflected

- **Issue**: Rich text field alignment selection is not reflected in generated PDF files
- **Cause**: Limitation in rich text to PDF conversion
- **Workaround**: Use simple text formatting without complex alignment
- **Affected Requests**:
    - [Get Rich Text PDF File](pages/GetRichTextPDFFile.md)
    - [Get Entity List PDF](pages/GetEntityListPDF.md)

#### [KE-1974](https://antbrains.atlassian.net/browse/KE-1974) - Complex Fields Not Working in Entity List PDF

- **Issue**: Get Entity List PDF request does not work with complex fields like selection fields
- **Cause**: Complex field types are not fully supported in entity list conversion
- **Workaround**:
    - Use simple text values in entity fields
    - Convert complex fields to text before generating PDF
- **Affected Request**: [Get Entity List PDF](pages/GetEntityListPDF.md)

### Apache POI Library Limitations

#### Word Document Formatting

- **Issue**: Some formatting issues persist when converting multi-page .docx files to PDF
- **Cause**: Limitations in the Apache POI library
- **Impact**:
    - Complex formatting may not be preserved exactly
    - Advanced Word features may not convert correctly
- **Workaround**:
    - Use simple document formatting
    - Test conversion with sample documents
    - Simplify complex layouts
- **Affected Requests**:
    - [Convert Docx File to PDF File](pages/ConvertDocxFileToPDFFile.md)
    - [Fill The Docx Template](pages/FillTheDocxTemplate.md)

---

## Supported Catalog Requests

The PDF Extension provides 13 catalog requests organized into four categories:

### Document Conversion (6 Requests)

1. **[Convert Excel to PDF](pages/ConvertExcelToPDF.md)** - Convert Excel files to PDF with optional sheet selection
2. **[Convert Docx File to PDF File](pages/ConvertDocxFileToPDFFile.md)** - Convert Word documents to PDF
3. **[Fill The Docx Template](pages/FillTheDocxTemplate.md)** - Fill Word templates with data and generate PDF
4. **[Convert Image to PDF](pages/ConvertImageToPdf.md)** - Convert images (PNG, JPG, JPEG) to PDF
5. **[Convert Excel Multiple Sheets to PDF](pages/ConvertExcelMultipleSheetsToPDF.md)** - Convert all Excel sheets to
   single PDF
6. **[Convert PDF to HTML](pages/ConvertPDFToHTML.md)** - Convert PDF files to HTML format with complex layout support

### PDF Generation (3 Requests)

7. **[Get Rich Text PDF File](pages/GetRichTextPDFFile.md)** - Create PDF from rich text content
8. **[Get Entity List PDF](pages/GetEntityListPDF.md)** - Create PDF with entity data in table format
9. **[Get Table PDF](pages/GetTablePDF.md)** - Create PDF with multi-field data in table format

### PDF Manipulation (4 Requests)

10. **[Merge PDF Files](pages/MergePDFFiles.md)** - Merge multiple PDF files into one
11. **[Split PDF into Pages](pages/SplitPDFIntoPages.md)** - Split PDF into individual pages
12. **[Split PDF Into Two Equal Part](pages/SplitPDFIntoTwoEqualPart.md)** - Split PDF into two equal parts
13. **[Split PDF Based on Condition](pages/SplitPDFBasedOnCondition.md)** - Split PDF at a specific page number into two
    files

---

## Upgrade Instructions

### Upgrading from Previous Versions

1. Navigate to the PDF Extension in your Krista workspace
2. Click **Upgrade** to install version 2.0.7
3. Review the [Extension Configuration](pages/ExtensionConfiguration.md) to configure the Allow Retry flag
4. Test the extension using **Diagnostics** → **Test Connection**
5. Review the updated documentation for any catalog requests you use

### Configuration Changes

- **Allow Retry Flag**: New configuration option to control processing modes
    - Default: Enabled (Enhanced Validation Mode)
    - See [Extension Configuration](pages/ExtensionConfiguration.md) for details

### Breaking Changes

None. Version 2.0.7 is fully backward compatible with previous versions.

---

## Important Notes

### Apache POI Library Limitations

The PDF Extension uses the Apache POI library for document processing. While the conversion of multi-page .docx files to
PDF works correctly, some formatting issues may persist due to library limitations:

- Complex document layouts may not convert exactly
- Graphs and charts may be skipped
- Advanced formatting features may not be preserved
- Rich text alignment may not be reflected

**Recommendation**: Test document conversion with sample files before processing important documents.

### File Size Limitations

- **Image Files**: Maximum 25MB for image to PDF conversion
- **Large PDFs**: Very large PDF files may impact performance during merge/split operations

### Supported File Formats

- **Excel**: .xlsx (recommended), .xls
- **Word**: .docx only
- **Images**: PNG, JPG, JPEG
- **PDF**: .pdf

---

## Getting Help

### Documentation

- [README](../README.md) - Overview and quick start
- [Extension Configuration](pages/ExtensionConfiguration.md) - Setup and configuration
- [Krista PDF Templates](pages/kristaTemplates.md) - Template types and usage

### Support

For issues or questions:

1. Review the troubleshooting section in the relevant catalog request documentation
2. Check the Known Issues section above
3. Contact Krista support with error details and logs

---

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Configure Allow Retry and processing modes
- [Krista PDF Templates](pages/kristaTemplates.md) - Template types and examples
- [README](../README.md) - Extension overview and navigation

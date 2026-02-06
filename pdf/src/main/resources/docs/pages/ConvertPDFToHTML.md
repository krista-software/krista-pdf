# Convert PDF to HTML

## Overview

The **Convert PDF to HTML** catalog request extracts text content from PDF files and converts it into HTML-tagged format
with proper structure and formatting. This request preserves document structure including headings, paragraphs, lists,
and formatting while converting PDF content to web-ready HTML.

**Related Jira Ticket**: [KE-2783](https://antbrains.atlassian.net/browse/KE-2783)

---

## Request Details

| Property          | Value               |
|-------------------|---------------------|
| **Request Name**  | Convert PDF to HTML |
| **Area**          | PDF                 |
| **Type**          | Change System       |
| **Version Added** | 2.0.10              |

---

## Input Parameters

### PDF File (Required)

- **Type**: File
- **Description**: The PDF file to convert to HTML format
- **Supported Formats**: .pdf
- **Validation**: File must be a valid PDF document

### Include Formatting (Optional)

- **Type**: Boolean
- **Default**: true
- **Description**: Enable to detect and preserve headings, lists, and formatting
    - **true**: Converter will detect and tag headings (H1, H2), bullet points (UL/LI), and numbered lists
    - **false**: All content will be wrapped in simple paragraph tags (P)

### Allow Retry (Optional)

- **Type**: Boolean
- **Default**: false
- **Description**: Enable error handling and retry logic for the conversion process

---

## Output

### HTML File (ZIP Archive)

- **Type**: File
- **Description**: ZIP archive containing the HTML file with converted PDF content
- **File Extension**: .zip
- **Contains**: Single .html file inside the ZIP archive
- **HTML Format**: Valid HTML5 document with:
    - DOCTYPE declaration
    - HTML, head, and body tags
    - Embedded CSS styles
    - Properly escaped content
    - Semantic HTML tags (h1, h2, h3, p, ul, ol, li)
- **File Name**: Original PDF filename with .zip extension (e.g., document.pdf → document.zip)
- **Usage**: Download the ZIP file, extract it, and open the .html file in a browser

### Security Note: Why ZIP Format?

️ **Important Security Information**

HTML files are executable in web browsers and can potentially contain malicious scripts. For security reasons, the Krista Media Client has added HTML files (along with other executable web formats) to the rejected file list. These files can run in the browser, and to protect against XSS (Cross-Site Scripting) attacks and other security vulnerabilities, we cannot upload raw HTML files directly.

**Rejected File Types for Security:**
- `.html` - HTML files


**Solution: ZIP Compression**

To safely deliver HTML content while maintaining security:
1. The HTML file is generated with proper content and structure
2. The HTML file is compressed into a ZIP archive
3. The ZIP file is uploaded to Krista Media Client
4. Users download the ZIP file and extract the HTML file locally
5. The extracted HTML file can be opened safely in any browser

This approach ensures that:
- ✅ HTML content cannot execute on the server or during upload
- ✅ Users receive properly formatted HTML files
- ✅ Security policies are maintained
- ✅ Content remains accessible and usable

---

## Validation Rules

1. **PDF File is Required**: A valid PDF file must be provided
2. **File Format**: Only PDF files are accepted
3. **File Size**: Must be within system limits
4. **Content Extraction**: PDF must contain extractable text (not scanned images)

---

## Usage Examples

### Example 1: Basic Conversion with Formatting

```
Input:
- PDF File: "document.pdf"
- Include Formatting: true
- Allow Retry: false

Output:
ZIP File: "document.zip"

After extracting document.zip, you get:
File: "document.html"
File Contents:
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>PDF Content</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; margin: 20px; }
        h1 { font-size: 24px; margin-top: 20px; }
        p { margin: 10px 0; }
    </style>
</head>
<body>
    <h1>DOCUMENT TITLE</h1>
    <p>This is a paragraph from the PDF.</p>
    <ul>
        <li>Bullet point 1</li>
        <li>Bullet point 2</li>
    </ul>
</body>
</html>
```

### Example 2: Simple Conversion without Formatting Detection

```
Input:
- PDF File: "report.pdf"
- Include Formatting: false
- Allow Retry: true

Output:
ZIP File: "report.zip"

After extracting report.zip, you get:
File: "report.html"
(All content wrapped in <p> tags without heading/list detection)
```

### Example 3: Complete Workflow

```
Step 1: Upload PDF
- Upload "quarterly_report.pdf" to Krista

Step 2: Run Conversion
- Select "Convert PDF to HTML" catalog request
- Choose the uploaded PDF file
- Enable "Include Formatting"
- Click Execute

Step 3: Download Result
- Download "quarterly_report.zip" from output

Step 4: Extract and View
- Extract the ZIP file
- Open "quarterly_report.html" in your browser
- View the formatted HTML content
```

---

## Business Rules

1. **Text Extraction**: Only text content is extracted; images and graphics are not included
2. **Structure Preservation**: When formatting is enabled, the converter attempts to preserve document structure
3. **Heading Detection**: Text in ALL CAPS or numbered headings are converted to H1/H2 tags
4. **List Detection**: Lines starting with bullet characters (•, ●, ○) are converted to list items
5. **HTML Escaping**: All special characters are properly escaped to prevent XSS and rendering issues
6. **Multi-Page Support**: Content from all pages is combined into a single HTML document

---

## Limitations

1. **Scanned PDFs**: Cannot extract text from scanned images or image-based PDFs
2. **Complex Layouts**: Multi-column layouts may not preserve exact positioning
3. **Images**: Images embedded in PDFs are not extracted or included in HTML
4. **Fonts**: Font information is not preserved; default web fonts are used
5. **Colors**: Text colors and background colors are not preserved
6. **Tables**: Complex table structures may not be perfectly preserved

---

## Best Practices

1. **Use Formatting Detection**: Enable "Include Formatting" for better structure preservation
2. **Extract ZIP Files**: Always extract the ZIP file to access the HTML content
3. **Test Output**: Review the HTML output to ensure it meets your requirements
4. **Post-Processing**: Consider additional processing for specific formatting needs
5. **Error Handling**: Enable "Allow Retry" for production workflows
6. **File Validation**: Ensure PDFs contain extractable text before conversion
7. **Security Awareness**: Understand that ZIP format is used for security compliance

---

## Common Use Cases

### 1. Content Migration

Convert PDF documents to HTML for web publishing or content management systems.

### 2. Document Archiving

Extract and preserve PDF content in a web-accessible format.

### 3. Text Analysis

Extract PDF content for text analysis, search indexing, or natural language processing.

### 4. Accessibility

Convert PDF content to HTML for improved accessibility and screen reader compatibility.

### 5. Email Templates

Extract PDF content to create HTML email templates.

---

## Related Catalog Requests

- [Convert Docx File to PDF File](/pages/ConvertDocxFileToPDFFile.md) - Convert Word documents to PDF
- [Get Rich Text PDF File](/pages/GetRichTextPDFFile.md) - Create PDF from rich text content
- [Merge PDF Files](/pages/MergePDFFiles.md) - Combine multiple PDFs

---

## Technical Implementation

### Helper Class

- **Class**: ConvertPdfToHtmlHelper
- **Package**: app.krista.extensions.essentials.documents.pdf.helper
- **Service**: PDFToHtmlConverter
- **Library**: Apache PDFBox for text extraction

### Conversion Process

1. Load PDF document using Apache PDFBox
2. Extract text content with position sorting
3. Analyze text structure (headings, lists, paragraphs)
4. Apply HTML tags based on content type
5. Escape special characters for HTML safety
6. Generate complete HTML document with styles

---

## Troubleshooting

### Issue: "PDF file is required"

**Solution**: Ensure a valid PDF file is uploaded

### Issue: "Failed to convert PDF to HTML"

**Solution**:

- Verify the PDF is not corrupted
- Ensure the PDF contains extractable text (not scanned images)
- Try enabling "Allow Retry" option

### Issue: "No text extracted"

**Solution**: The PDF may be image-based. Use OCR software to convert to searchable PDF first

### Issue: "Formatting not preserved"

**Solution**: Enable "Include Formatting" option for better structure detection

---

## See Also

- [PDF Extension Documentation](../index.md)
- [Release Notes](/pages/release-notes.md)
- [Jira Ticket KE-2783](https://antbrains.atlassian.net/browse/KE-2783)


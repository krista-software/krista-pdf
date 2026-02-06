# PDF to HTML Conversion (Complex Layout Support)

## Overview

The **PDF to HTML Conversion (Complex Layout Support)** catalog request converts PDF files to high-fidelity HTML. This
request is specifically designed for PDFs with **complex layouts** such as
multi-column documents, forms, tables, embedded graphics, and mixed content. It preserves exact text positioning, fonts,
embedded images, and SVG graphics, providing superior layout preservation compared to Java-based PDF libraries.

**Related Jira Ticket**: [KE-2783](https://antbrains.atlassian.net/browse/KE-2783)

---

## Request Details

| Property          | Value                                           |
|-------------------|-------------------------------------------------|
| **Request Name**  | PDF to HTML Conversion (Complex Layout Support) |
| **Area**          | PDF                                             |
| **Type**          | Change System                                   |
| **Version Added** | 2.0.13                                          |

---

## When to Use This Request

### Best Use Cases

This catalog request is ideal for PDFs where **visual accuracy and layout preservation** are critical:

#### 1. Complex Forms

- Employment applications with precise field positioning
- Tax forms (W-2, 1099, etc.)
- Insurance documents with checkboxes and signatures
- Government forms with specific layouts

#### 2. Multi-Column Layouts

- Newsletters with 2-3 column designs
- Brochures and marketing materials
- Magazine articles with complex layouts
- Academic journals with multiple columns

#### 3. Technical Documents

- Engineering drawings with annotations
- Architectural plans and blueprints
- Diagrams with exact positioning requirements
- Scientific papers with figures and charts

#### 4. Financial Reports

- Bank statements with tables and formatting
- Invoices with itemized lists
- Financial reports with charts and graphs
- Accounting documents with precise alignment

#### 5. Marketing Materials

- Flyers with custom fonts and graphics
- Posters with layered content
- Presentations with slides and images
- Product catalogs with images and descriptions

---

## Input Parameters

### PDF File (Required)

- **Type**: File
- **Description**: The PDF file to convert to high-fidelity HTML format
- **Supported Formats**: .pdf, .PDF
- **Validation**: File must be a valid PDF document
- **Recommended**: PDFs under 50 pages for optimal performance
- **Supports**: Complex layouts, forms, tables, multi-column documents, embedded graphics

### Save as Text File (Optional)

- **Type**: Boolean
- **Default**: false (ZIP format)
- **Description**: Choose the output format for the HTML content
    - **false (default)**: Returns .zip file containing .html file
        - Recommended for security compliance
        - Must extract ZIP to access HTML file
        - Self-contained with all resources embedded
        - Can be viewed in any browser after extraction
    - **true**: Returns .txt file with HTML source code
        - Can upload directly to Krista Media
        - Must manually save as .html to view in browser
        - Useful for inspecting HTML structure
        - Good for integration with other systems

### Allow Retry (Optional)

- **Type**: Boolean
- **Default**: false
- **Description**: Enable error handling and retry logic
    - When enabled, provides detailed error messages for troubleshooting
    - Recommended for production workflows

---

## Output

### ZIP File (Default - saveAsTextFile=false)

- **Type**: File
- **File Extension**: .zip
- **File Name Format**: `html_<timestamp>_<original_name>.zip`
- **Example**: `html_1234567890_enrollment_form.zip`
- **Contains**: Single .html file with all resources embedded
- **HTML Features**:
    - Pixel-perfect layout preservation
    - Embedded images as base64 data URIs
    - Original fonts and styling
    - SVG graphics for vector content
    - Multi-page support
    - Self-contained (no external dependencies)
- **Usage**:
    1. Download the ZIP file
    2. Extract the ZIP file
    3. Open the .html file in a browser
    4. Layout matches original PDF exactly

### TXT File (saveAsTextFile=true)

- **Type**: File
- **File Extension**: .txt
- **File Name Format**: `html_<timestamp>_<original_name>.txt`
- **Example**: `html_1234567890_enrollment_form.txt`
- **Contains**: HTML source code as plain text
- **Usage**:
    1. Download the .txt file
    2. Rename to .html extension
    3. Open in browser to view
    4. Or use HTML source in other applications

### Security Note: Why ZIP Format?

**Important Security Information**

HTML files can execute JavaScript and potentially contain malicious scripts. For security reasons, the Krista Media
Client blocks direct upload of HTML files to prevent XSS (Cross-Site Scripting) attacks and other security
vulnerabilities.

**Rejected File Types for Security:**

- `.html` - HTML files
- `.htm` - HTML files (alternate extension)

**Solution: ZIP Compression**

To safely deliver HTML content while maintaining security:

1. The HTML file is generated with proper content and embedded resources
2. The HTML file is compressed into a ZIP archive
3. The ZIP file is uploaded to Krista Media Client
4. Users download the ZIP file and extract the HTML file locally
5. The extracted HTML file can be opened safely in any browser

This approach ensures that:

- HTML content cannot execute on the server or during upload
- Users receive properly formatted HTML files with exact layout
- Security policies are maintained
- Content remains accessible and usable
- All resources (images, fonts) are embedded

---

## Key Features

### 1. Pixel-Perfect Layout Preservation

- Maintains exact text positioning and spacing
- Preserves page structure and dimensions
- Accurate multi-column layout rendering
- Precise element alignment

### 2. Font Fidelity

- Preserves original fonts, sizes, and colors
- Maintains font weights (bold, italic)
- Supports custom and embedded fonts
- Accurate text styling

### 3. Embedded Images

- Includes images as base64-encoded data URIs
- No external image files needed
- Self-contained HTML output
- Supports JPEG, PNG, and other formats


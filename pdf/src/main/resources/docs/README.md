[comment]: <> (Home Page)

# PDF Extension

## Overview

The Krista PDF Extension is a comprehensive document processing solution that enables seamless conversion, generation,
and manipulation of PDF files. This extension provides powerful capabilities for working with Microsoft Office
documents (Word, Excel), images, and rich text content, transforming them into professional PDF documents with ease.

The extension supports both interactive user workflows with comprehensive validation and automated processing modes,
making it suitable for a wide range of use cases from manual document generation to automated report creation.

## Key Features

- **Document Conversion**: Convert Word documents (.docx), Excel spreadsheets (.xlsx), and images (PNG, JPG, JPEG) to
  PDF format
- **PDF to HTML Conversion**: Convert PDFs to HTML with two methods:
    - **Standard Conversion**: Text-based extraction for simple documents (PDFBox)
    - **Complex Layout Support**: High-fidelity conversion preserving exact layouts, forms, tables, and graphics (PyMuPDF)
- **Template Processing**: Fill Word document templates with dynamic data and generate customized PDFs
- **Rich Content Generation**: Create PDFs from rich text, entity lists, and tabular data with customizable headers,
  footers, and logos
- **PDF Manipulation**: Merge multiple PDFs, split PDFs into pages or sections, and organize documents efficiently
- **Dual Processing Modes**: Choose between enhanced validation mode for interactive workflows or direct processing
  mode for automation
- **Retry Support**: Built-in retry mechanism with comprehensive error handling and validation
- **Telemetry Integration**: Track operation metrics and performance for monitoring and optimization

## Quick Start Guide

### 1. Add Extension to Workspace

1. Navigate to the **Catalog** section in your Krista workspace
2. Click on **Essentials Ecosystem**
3. Under Essentials, select **Documents** → **Extensions**
4. Choose **PDF Extension** and click **Add to Workspace**
5. Click **Upgrade** to install the latest version

### 2. Configure the Extension

1. Go to the extension **Setup** page
2. Configure the **Allow Retry** flag based on your needs:
    - **Enabled**: Enhanced validation mode with user prompts (recommended for interactive workflows)
    - **Disabled**: Direct processing mode for automation (no user input required)
3. Click **Diagnostics** → **Test Connection** to verify the setup

### 3. Start Using Catalog Requests

Once configured, you can start using any of the available catalog requests to process documents. See
the [Documentation Structure](#documentation-structure) below for detailed information on each request.

## Documentation Structure

### Getting Started

- **[Extension Configuration](pages/ExtensionConfiguration.md)** - Complete setup and configuration guide
- **[Krista PDF Templates](pages/kristaTemplates.md)** - Overview of template types and usage

### Document Conversion

- **[Convert Excel to PDF](pages/ConvertExcelToPDF.md)** - Convert Excel files to PDF format
- **[Convert Docx File to PDF File](pages/ConvertDocxFileToPDFFile.md)** - Convert Word documents to PDF
- **[Convert Image to PDF](pages/ConvertImageToPdf.md)** - Convert images (PNG, JPG, JPEG) to PDF
- **[Convert PDF to HTML](pages/ConvertPDFToHTML.md)** - Convert PDF files to HTML format (text-based extraction)
- **[PDF to HTML Conversion (Complex Layout Support)](pages/PDFToHTMLConversionComplexLayout.md)** - Convert PDFs to
  high-fidelity HTML preserving exact layouts, forms, tables, and graphics using Python/PyMuPDF
- **[Convert Excel File with Multiple Sheets to PDF](pages/ConvertExcelMultipleSheetsToPDF.md)** - Convert multi-sheet
  Excel files to PDF

### PDF Generation

- **[Fill The Docx Template](pages/FillTheDocxTemplate.md)** - Fill Word templates with data and generate PDFs
- **[Get Rich Text PDF File](pages/GetRichTextPDFFile.md)** - Create PDFs from rich text content
- **[Get Entity List PDF](pages/GetEntityListPDF.md)** - Generate PDFs from entity data in table format
- **[Get Table PDF](pages/GetTablePDF.md)** - Create PDFs from multi-field table data

### PDF Manipulation

- **[Merge PDF Files](pages/MergePDFFiles.md)** - Combine multiple PDF files into one
- **[Split PDF into Pages](pages/SplitPDFIntoPages.md)** - Split PDF into individual pages
- **[Split PDF Into Two Equal Part](pages/SplitPDFBasedOnCondition)** - Divide PDF into two equal sections
- **[Split PDF Based On Condition](pages/SplitPDFBasedOnCondition.md)** - Split PDF based on intelligent criteria

### Additional Resources

- **[Release Notes](pages/release-notes.md)** - Version history and updates

## Support & Resources

- **Extension Version**: 2.0.9
- **Krista Service APIs Java**: 1.0.115
- **Global Catalog Version**: GC-2025.10.3
- **Appliance Release Version**: 3.5.1
- **Domain**: Documents (Essentials Ecosystem)

### Need Help?

- Review the detailed documentation for each catalog request
- Check the troubleshooting sections in individual request pages
- Refer to the [Extension Configuration](pages/ExtensionConfiguration.md) guide for setup issues

---

**Note**: This extension uses Apache POI library for document processing. While most formatting is preserved during
conversion, some complex formatting elements (such as graphs in Word documents) may have limitations.

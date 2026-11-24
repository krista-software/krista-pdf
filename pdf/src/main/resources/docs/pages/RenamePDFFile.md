# Rename PDF File

## Overview

Renames a PDF file to a new, user-supplied name and returns the same PDF content under
that name. Useful in workflows that require a specific file name before further processing
(for example, ingesting a document into a Doc set).

## Request Details

- **Area**: PDF
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ❌ No — the request validates and fails fast with a clear error.

## Input Parameters

| Parameter Name | Type | Required | Description                                  | Example          |
|----------------|------|----------|----------------------------------------------|------------------|
| PDF File       | File | Yes      | The PDF file to rename                        | `report.pdf`     |
| New File Name  | Text | Yes      | The new name for the file                     | `Invoice March`  |

### How the new name is handled

- Any extension you type in **New File Name** is removed, and `.pdf` is always appended.
  (`Invoice.docx` → `Invoice.pdf`.)
- Special characters are removed; letters, numbers, spaces, `-` and `_` are kept.
- Your capitalization and spacing are preserved (`Invoice March` → `Invoice March.pdf`).
- Any text after the last `.` is treated as an extension and removed.

| New File Name (input) | Output file name    |
|-----------------------|---------------------|
| `Invoice March`       | `Invoice March.pdf` |
| `Invoice.pdf`         | `Invoice.pdf`       |
| `Invoice.docx`        | `Invoice.pdf`       |
| `Q3 Report: v2`       | `Q3 Report v2.pdf`  |

## Output Parameters

| Parameter Name   | Type | Description                                   |
|------------------|------|-----------------------------------------------|
| Renamed PDF File | File | The same PDF content under the new file name  |

## Validation Rules

| Validation                        | Error Message                                                                                     |
|-----------------------------------|--------------------------------------------------------------------------------------------------|
| Input must be a PDF               | "The provided file is not a PDF. Please upload a valid PDF file."                                 |
| New File Name must be provided    | "Please provide a new name for the PDF file."                                                     |
| New File Name must have valid chars | "The provided name has no valid characters. Please use letters, numbers, spaces, '-' or '_'."   |

## Errors

On any processing error the request returns a failure with the message
`Failed to rename PDF file: <reason>`.

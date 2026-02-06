# Convert Image to PDF

## Overview

Converts an image file (PNG, JPG, JPEG) into a PDF document. The image is embedded as a page in the resulting PDF file.

## Request Details

- **Area**: PDF
- **Type**: CHANGE_SYSTEM
- **Retry Support**: ✅ Yes - When Allow Retry is enabled, provides comprehensive validation with user prompts for
  invalid file formats

## Input Parameters

| Parameter Name | Type | Required | Description                                                         | Example    |
|----------------|------|----------|---------------------------------------------------------------------|------------|
| Document File  | File | Yes      | The image file to convert to PDF. Supported formats: PNG, JPG, JPEG | `logo.png` |

### Parameter Details

#### Document File

- **Supported Formats**: `.png`, `.jpg`, `.jpeg`
- **Maximum File Size**: 25MB (enforced to prevent memory issues)
- **Validation**: File format and size are validated when Allow Retry is enabled
- **Image Features**: Transparency and advanced image features may not be perfectly preserved

## Output Parameters

| Parameter Name | Type | Description                                          |
|----------------|------|------------------------------------------------------|
| PDF File       | File | The generated PDF file containing the embedded image |

## Validation Rules

**When Allow Retry = true**

| Validation                                  | Error Message                                                                                              | Resolution                                            |
|---------------------------------------------|------------------------------------------------------------------------------------------------------------|-------------------------------------------------------|
| Image file format must be PNG, JPG, or JPEG | "The provided file '[filename]' is not a valid image format. Only PNG, JPG, and JPEG files are supported." | Upload a valid image file in PNG, JPG, or JPEG format |
| File size must not exceed 25MB              | "Image file size exceeds the maximum limit of 25MB"                                                        | Reduce image file size or compress the image          |
| File must not be null or empty              | "No image file provided"                                                                                   | Upload a valid image file                             |

**When Allow Retry = false**

No validation is performed. Invalid inputs will result in SYSTEM_ERROR.

## Error Handling

### Input Errors (INPUT_ERROR)

**When Allow Retry is enabled**

#### Invalid Image File Format

- **Cause**: The uploaded file is not in PNG, JPG, or JPEG format
- **Error Message**: "The provided file '[filename]' is not a valid image format. Only PNG, JPG, and JPEG files are
  supported."
- **Resolution**:
    1. Verify the file has a .png, .jpg, or .jpeg extension
    2. Convert the image to a supported format
    3. Upload a valid image file

#### Image File Too Large

- **Cause**: The image file exceeds 25MB
- **Error Message**: "Image file size exceeds the maximum limit of 25MB"
- **Resolution**:
    1. Compress the image using image editing software
    2. Reduce image resolution or dimensions
    3. Convert to a more efficient format (e.g., JPEG for photos)

#### Missing Image File

- **Cause**: No file was provided
- **Error Message**: "No image file provided"
- **Resolution**: Upload a valid image file

### System Errors (SYSTEM_ERROR)

**When Allow Retry is disabled or for unexpected errors**

#### File Processing Failure

- **Cause**: Error during file reading, conversion, or writing
- **Error Message**: "Failed to convert image to PDF: [error details]"
- **Common Scenarios**:
    - Corrupted image file
    - Unsupported image encoding
    - File read/write permissions issues
    - Insufficient memory
    - Network issues during file transfer
- **Resolution**:
    1. Verify the image file is not corrupted
    2. Try opening the image in an image viewer
    3. Convert to a standard format (PNG or JPEG)
    4. Check system logs for detailed error information
    5. Ensure sufficient system resources are available

## Usage Examples

### Example 1: Convert Logo to PDF

**Scenario**: Convert a company logo to PDF format

**Input**:

```
Document File: "company_logo.png"
```

**Output**:

```
PDF File: "company_logo.pdf"
```

**Result**: The logo image is embedded in a PDF file.

### Example 2: Convert Photo to PDF

**Scenario**: Convert a photograph to PDF for archival

**Input**:

```
Document File: "photo_2024.jpg"
```

**Output**:

```
PDF File: "photo_2024.pdf"
```

**Result**: The photograph is converted to PDF format.

### Example 3: Retry Scenario (Allow Retry Enabled)

**Scenario**: User provides unsupported file format, then corrects it

**Interaction**:

```
User: "Convert this image to PDF"
File: "image.gif"

System: "The provided file 'image.gif' is not a valid image format. Only PNG, JPG, and JPEG files are supported."
Prompt: "Please upload a valid image file (PNG, JPG, JPEG)."

User: *uploads "image.png"*

System: "Success! Image converted to PDF."
Output: "image.pdf"
```

## Business Rules

1. **Single Image Per Request**: Only one image file can be converted per request
2. **Supported Formats Only**: Only PNG, JPG, and JPEG formats are supported
3. **File Size Limit**: Maximum file size is 25MB to prevent memory issues
4. **Single Page PDF**: The output PDF contains the image as a single page
5. **No Image Editing**: The image is embedded as-is without modifications

## Limitations

1. **Supported Formats**: Only PNG, JPG, and JPEG are supported. Other formats (GIF, BMP, TIFF, etc.) are not supported
2. **File Size**: Maximum file size is 25MB
3. **Single Image**: Only one image per request; multiple images cannot be combined
4. **Transparency**: PNG transparency may not be perfectly preserved in PDF
5. **Advanced Features**: Some advanced image features (color profiles, metadata) may not be preserved
6. **No Batch Processing**: Cannot convert multiple images in a single request

## Best Practices

### 1. Optimize Image Size

- Compress images before conversion to reduce file size
- Use appropriate resolution for the intended use
- Consider JPEG for photographs, PNG for graphics with transparency

### 2. Use Supported Formats

- Convert unsupported formats (GIF, BMP) to PNG or JPEG before uploading
- Verify file extension matches actual file format

### 3. Validate Image Quality

- Ensure images are not corrupted before conversion
- Test with image viewer before uploading
- Use standard image encoding

### 4. Handle Large Images

For large images:

- Compress or resize before conversion
- Use JPEG format for better compression
- Monitor file size to stay under 25MB limit

### 5. Enable Allow Retry for User Workflows

When users are providing files interactively, enable Allow Retry to provide helpful validation messages.

## Common Use Cases

### 1. Logo Conversion

```
Scenario: Convert company logo to PDF for printing
Action: Upload PNG logo file
Result: PDF logo ready for high-quality printing
```

### 2. Photo Archival

```
Scenario: Archive photographs in PDF format
Action: Upload JPEG photo files one at a time
Result: PDF files for long-term archival
```

### 3. Signature Conversion

```
Scenario: Convert signature image to PDF for document signing
Action: Upload PNG signature image
Result: PDF signature ready for embedding
```

### 4. Diagram Conversion

```
Scenario: Convert technical diagrams to PDF
Action: Upload PNG diagram files
Result: PDF diagrams for documentation
```

## Related Catalog Requests

- [Convert Docx File to PDF File](pages/ConvertDocxFileToPDFFile.md) - Convert Word documents to PDF
- [Convert Excel to PDF](pages/ConvertExcelToPDF.md) - Convert Excel files to PDF
- [Merge PDF Files](pages/MergePDFFiles.md) - Combine multiple PDFs (including converted images)

## Technical Implementation

### Helper Class

- **Class**: `ConvertImageToPdfHelper`
- **Package**: `app.krista.extensions.essentials.documents.pdf.helper`
- **Validation**: Uses `ValidationOrchestrator` with `ImageFileFormatValidator`
- **Service**: Delegates to `PDFUtils.convertImageToPdf()`

### Telemetry Metrics

- **pdf.convertImageToPdf.count**: Total number of conversion requests
- **pdf.convertImageToPdf.success**: Successful conversions with duration
- **pdf.convertImageToPdf.error**: Failed conversions with error details and duration

### Validation Flow

1. Validate image file format (PNG, JPG, JPEG)
2. Validate file size (max 25MB)
3. If validation passes, download file
4. Convert image to PDF using PDFUtils
5. Upload result and return success response
6. If validation fails, prompt user for correction (when Allow Retry enabled)

## Troubleshooting

### Issue: "File format not supported" error

**Cause**: File is not in PNG, JPG, or JPEG format

**Solution**:

1. Check file extension is .png, .jpg, or .jpeg
2. Convert image to supported format using image editing software
3. Ensure file is not corrupted

### Issue: "File size exceeds maximum limit" error

**Cause**: Image file is larger than 25MB

**Solution**:

1. Compress image using image editing software
2. Reduce image resolution or dimensions
3. Convert to JPEG format for better compression
4. Use online image compression tools

### Issue: Conversion succeeds but image quality is poor

**Cause**: Image may have been compressed or resized

**Solution**:

1. Use higher quality source image
2. Ensure image resolution is appropriate for intended use
3. Avoid multiple conversions that degrade quality

### Issue: Transparency is lost in PDF

**Cause**: PDF format may not preserve PNG transparency perfectly

**Solution**:

1. Use white or appropriate background color in source image
2. Accept that transparency may not be preserved
3. Consider alternative PDF generation methods if transparency is critical

### Issue: Corrupted image file error

**Cause**: Image file is corrupted or has invalid encoding

**Solution**:

1. Open image in image viewer to verify it's not corrupted
2. Re-save image in standard format
3. Try converting with different image editing software
4. Use a different source image

## See Also

- [Extension Configuration](pages/ExtensionConfiguration.md) - Configure Allow Retry flag
- [Merge PDF Files](pages/MergePDFFiles.md) - Combine multiple image PDFs
- [Convert Docx File to PDF File](pages/ConvertDocxFileToPDFFile.md) - Convert documents to PDF


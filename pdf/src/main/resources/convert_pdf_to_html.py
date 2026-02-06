
import fitz
import sys
import json
import base64
import argparse
import os
import math
import difflib

def calculate_accuracy_metrics(pdf_text, html_text):
    """Calculate accuracy metrics for PDF to HTML conversion"""

    # Clean and normalize text for comparison
    pdf_clean = ' '.join(pdf_text.split())
    html_clean = ' '.join(html_text.split())

    # Character-level accuracy
    total_chars = len(pdf_clean)
    if total_chars == 0:
        char_accuracy = 0.0
    else:
        matcher = difflib.SequenceMatcher(None, pdf_clean, html_clean)
        char_accuracy = matcher.ratio() * 100

    # Word-level accuracy
    pdf_words = pdf_clean.split()
    html_words = html_clean.split()
    total_words = len(pdf_words)

    if total_words == 0:
        word_accuracy = 0.0
    else:
        word_matcher = difflib.SequenceMatcher(None, pdf_words, html_words)
        word_accuracy = word_matcher.ratio() * 100

    # Calculate metrics
    metrics = {
        "total_characters": total_chars,
        "total_words": total_words,
        "character_accuracy": round(char_accuracy, 2),
        "word_accuracy": round(word_accuracy, 2),
        "overall_accuracy": round((char_accuracy + word_accuracy) / 2, 2)
    }

    return metrics

def convert_pdf_to_html_high_fidelity(pdf_path, output_path, interactive=False):
    if not output_path:
        output_path = pdf_path.replace(".pdf", ".html")

    if not os.path.exists(pdf_path):
        print(f"Error: File not found {pdf_path}")
        return

    doc = fitz.open(pdf_path)

    # Text for accuracy calculation
    full_pdf_text = ""
    extracted_html_text = ""

    # Metrics tracking
    total_text_blocks = 0
    total_image_blocks = 0
    total_pages = len(doc)

    html = "<!DOCTYPE html><html><head><meta charset='utf-8'><style>"
    html += """
    body { margin: 0; padding: 20px; background-color: #525659; font-family: Arial, Helvetica, sans-serif; }
    .page {
        background-color: white;
        margin: 0 auto 20px auto;
        position: relative;
        box-shadow: 0 0 10px rgba(0,0,0,0.5);
        overflow: hidden;
    }
    .text-span {
        position: absolute;
        white-space: nowrap;
        transform-origin: 0 0;
        line-height: 1.2;
    }
    svg {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        pointer-events: none;
    }
    .pdf-checkbox {
        appearance: none;
        -webkit-appearance: none;
        border: 1px solid black;
        border-radius: 0;
        background-color: white;
        cursor: pointer;
        z-index: 1000;
        box-sizing: border-box;
    }
    .pdf-checkbox:checked {
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Cpath d='M1 5 L4 8 L9 1' fill='none' stroke='black' stroke-width='2'/%3E%3C/svg%3E");
        background-size: 80% 80%;
        background-position: center;
        background-repeat: no-repeat;
    }
    .pdf-text-input {
        position: absolute;
        border: none;
        border-bottom: 1px solid black;
        background-color: transparent;
        font-family: Arial, Helvetica, sans-serif;
        font-size: 10pt;
        padding: 0;
        margin: 0;
        z-index: 1000;
        box-sizing: border-box;
    }
    .pdf-text-input:focus {
        outline: none;
        border-bottom: 2px solid #0066cc;
    }
    .pdf-radio {
        appearance: none;
        -webkit-appearance: none;
        border: 1px solid black;
        border-radius: 50%;
        background-color: white;
        cursor: pointer;
        z-index: 1000;
        box-sizing: border-box;
    }
    .pdf-radio:checked {
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'%3E%3Ccircle cx='5' cy='5' r='3' fill='black'/%3E%3C/svg%3E");
        background-size: 60% 60%;
        background-position: center;
        background-repeat: no-repeat;
    }
    """
    html += "</style></head><body>"

    for page_idx in range(len(doc)):
        page = doc[page_idx]
        width = page.rect.width
        height = page.rect.height
        matrix = page.rotation_matrix

        # Ground truth text for this page
        full_pdf_text += page.get_text("text")

        html += f'<div class="page" style="width: {width}pt; height: {height}pt;">'

        # 1. Render drawings (lines, rects) as SVG
        # We transform all points using the page's rotation matrix
        paths = page.get_drawings()
        html += '<svg viewBox="0 0 {} {}" style="width: {}pt; height: {}pt;" xmlns="http://www.w3.org/2000/svg">'.format(width, height, width, height)

        # Interactive form detection (only if interactive=True)
        interactive_checkboxes = []
        interactive_text_inputs = []
        interactive_radios = []

        # Detect checkbox-like rectangles for spacing normalization (always needed for layout)
        checkbox_rects = []

        for path in paths:
            # Interactive form detection logic (only when interactive=True)
            if interactive:
                # Checkbox heuristic
                is_interactive_checkbox = False
                checkbox_rect = None

                # Style check
                has_stroke = bool(path.get('color'))
                fill_val = path.get('fill')
                is_filled_dark = False

                if fill_val:
                    if len(fill_val) >= 3 and (fill_val[0] < 0.9 or fill_val[1] < 0.9 or fill_val[2] < 0.9):
                        is_filled_dark = True

                # Case 1: Simple rectangle checkbox
                if len(path["items"]) == 1 and path["items"][0][0] == "re":
                    item = path["items"][0]
                    if has_stroke and not is_filled_dark:
                        r = item[1] * matrix
                        w = r.width
                        h = r.height
                        if 6 <= w <= 30 and 6 <= h <= 30:
                            ratio = w / h if h > 0 else 0
                            if 0.8 <= ratio <= 1.2:
                                is_interactive_checkbox = True
                                checkbox_rect = r

                # Case 2: Complex path forming checkbox
                if not is_interactive_checkbox and has_stroke and not is_filled_dark:
                    rect = path.get('rect')
                    if rect:
                        r = fitz.Rect(rect) * matrix
                        w = r.width
                        h = r.height
                        if 6 <= w <= 30 and 6 <= h <= 30:
                            ratio = w / h if h > 0 else 0
                            if 0.8 <= ratio <= 1.2:
                                items = path["items"]
                                if len(items) >= 4:
                                    is_interactive_checkbox = True
                                    checkbox_rect = r

                if is_interactive_checkbox:
                    interactive_checkboxes.append({
                        "left": checkbox_rect.x0,
                        "top": checkbox_rect.y0,
                        "width": checkbox_rect.width,
                        "height": checkbox_rect.height
                    })
                    continue

                # Text Input Field Detection
                is_text_input = False
                text_input_rect = None
                if has_stroke and not is_filled_dark:
                    rect = path.get('rect')
                    if rect:
                        r = fitz.Rect(rect) * matrix
                        w = r.width
                        h = r.height
                        if w > 30 and h < 3:
                            is_text_input = True
                            text_input_rect = r

                if is_text_input:
                    interactive_text_inputs.append({
                        "left": text_input_rect.x0,
                        "top": text_input_rect.y0 - 12,
                        "width": text_input_rect.width,
                        "height": 14
                    })
                    continue

                # Radio Button Detection
                is_radio = False
                radio_rect = None
                if has_stroke and not is_filled_dark:
                    rect = path.get('rect')
                    if rect:
                        r = fitz.Rect(rect) * matrix
                        w = r.width
                        h = r.height
                        if 6 <= w <= 20 and 6 <= h <= 20:
                            ratio = w / h if h > 0 else 0
                            if 0.9 <= ratio <= 1.1:
                                has_curves = any(item[0] == 'c' for item in path["items"])
                                if has_curves and len(path["items"]) >= 4:
                                    is_radio = True
                                    radio_rect = r

                if is_radio:
                    interactive_radios.append({
                        "left": radio_rect.x0,
                        "top": radio_rect.y0,
                        "width": radio_rect.width,
                        "height": radio_rect.height
                    })
                    continue

            # Store checkbox rects for spacing (always needed)
            rect_raw = path.get("rect")
            if rect_raw:
                r = fitz.Rect(rect_raw) * matrix
                w = r.width
                h = r.height
                if 6 <= w <= 25 and 6 <= h <= 25 and abs(w - h) < 3:
                    checkbox_rects.append(r)

            # Original rendering logic for all paths (or non-interactive paths)
            fill_val = path.get('fill')
            fill = "none"
            if fill_val:
                f_r = int(max(0, min(255, fill_val[0] * 255)))
                f_g = int(max(0, min(255, fill_val[1] * 255)))
                f_b = int(max(0, min(255, fill_val[2] * 255)))
                fill = "#{:02x}{:02x}{:02x}".format(f_r, f_g, f_b)

            stroke_val = path.get('color')
            stroke = "none"
            if stroke_val:
                s_r = int(max(0, min(255, stroke_val[0] * 255)))
                s_g = int(max(0, min(255, stroke_val[1] * 255)))
                s_b = int(max(0, min(255, stroke_val[2] * 255)))
                stroke = "#{:02x}{:02x}{:02x}".format(s_r, s_g, s_b)

            stroke_width = path.get('width', 1)
            fill_opacity = path.get('fill_opacity', 1)
            stroke_opacity = path.get('stroke_opacity', 1)

            d = ""
            for item in path["items"]:
                if item[0] == "l": # line
                    p1 = item[1] * matrix
                    p2 = item[2] * matrix
                    d += f"M {p1.x} {p1.y} L {p2.x} {p2.y} "
                elif item[0] == "re": # rect
                    r = item[1] * matrix
                    d += f"M {r.x0} {r.y0} H {r.x1} V {r.y1} H {r.x0} Z "
                elif item[0] == "qu": # quad
                    p1 = item[1] * matrix
                    p2 = item[2] * matrix
                    p3 = item[3] * matrix
                    p4 = item[4] * matrix
                    d += f"M {p1.x} {p1.y} L {p2.x} {p2.y} L {p3.x} {p3.y} L {p4.x} {p4.y} Z "
                elif item[0] == "c": # curve
                    p1 = item[1] * matrix
                    p2 = item[2] * matrix
                    p3 = item[3] * matrix
                    p4 = item[4] * matrix
                    d += f"M {p1.x} {p1.y} C {p2.x} {p2.y} {p3.x} {p3.y} {p4.x} {p4.y} "

            if d:
                html += f'<path d="{d}" fill="{fill}" fill-opacity="{fill_opacity}" stroke="{stroke}" stroke-opacity="{stroke_opacity}" stroke-width="{stroke_width}" />'
        html += '</svg>'

        # Render interactive form elements (only if interactive=True)
        if interactive:
            for cb in interactive_checkboxes:
                html += f'<input type="checkbox" class="pdf-checkbox" style="position: absolute; left: {cb["left"]}pt; top: {cb["top"]}pt; width: {cb["width"]}pt; height: {cb["height"]}pt; margin: 0; padding: 0;">'

            for ti in interactive_text_inputs:
                html += f'<input type="text" class="pdf-text-input" style="left: {ti["left"]}pt; top: {ti["top"]}pt; width: {ti["width"]}pt; height: {ti["height"]}pt;">'

            for rb in interactive_radios:
                html += f'<input type="radio" class="pdf-radio" style="position: absolute; left: {rb["left"]}pt; top: {rb["top"]}pt; width: {rb["width"]}pt; height: {rb["height"]}pt; margin: 0; padding: 0;">'

        # 1.5. Render interactive form fields if enabled
        if interactive:
            widgets = page.widgets()
            for widget in widgets:
                field_type = widget.field_type
                field_name = widget.field_name or ""
                field_value = widget.field_value or ""
                rect = widget.rect * matrix

                # Escape HTML attributes
                field_name_escaped = field_name.replace('"', '&quot;').replace("'", '&#39;')
                field_value_escaped = field_value.replace('"', '&quot;').replace("'", '&#39;').replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

                if field_type == fitz.PDF_WIDGET_TYPE_TEXT:
                    # Text input field
                    html += f'<input type="text" name="{field_name_escaped}" value="{field_value_escaped}" style="position: absolute; left: {rect.x0}pt; top: {rect.y0}pt; width: {rect.width}pt; height: {rect.height}pt; z-index: 2; font-size: 10pt; border: 1px solid #999; padding: 2pt;">'

                elif field_type == fitz.PDF_WIDGET_TYPE_CHECKBOX:
                    # Checkbox
                    checked = "checked" if field_value else ""
                    html += f'<input type="checkbox" name="{field_name_escaped}" {checked} style="position: absolute; left: {rect.x0}pt; top: {rect.y0}pt; width: {rect.width}pt; height: {rect.height}pt; z-index: 2;">'

                elif field_type == fitz.PDF_WIDGET_TYPE_RADIOBUTTON:
                    # Radio button
                    checked = "checked" if field_value else ""
                    html += f'<input type="radio" name="{field_name_escaped}" {checked} style="position: absolute; left: {rect.x0}pt; top: {rect.y0}pt; width: {rect.width}pt; height: {rect.height}pt; z-index: 2;">'

                elif field_type == fitz.PDF_WIDGET_TYPE_COMBOBOX or field_type == fitz.PDF_WIDGET_TYPE_LISTBOX:
                    # Dropdown/Listbox
                    options_html = ""
                    if hasattr(widget, 'choice_values') and widget.choice_values:
                        for option in widget.choice_values:
                            option_escaped = option.replace('"', '&quot;').replace("'", '&#39;').replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                            selected = "selected" if option == field_value else ""
                            options_html += f'<option value="{option_escaped}" {selected}>{option_escaped}</option>'
                    html += f'<select name="{field_name_escaped}" style="position: absolute; left: {rect.x0}pt; top: {rect.y0}pt; width: {rect.width}pt; height: {rect.height}pt; z-index: 2; font-size: 10pt; border: 1px solid #999;">{options_html}</select>'

        # 2. Render text and images
        # Use TextPage with matrix to get correctly oriented text and bboxes
        tp = page.get_textpage(matrix=matrix)
        text_dict = tp.extractDICT()

        # Standard extraction for images (since TextPage might not include them)
        full_dict = page.get_text("dict")

        # Count blocks for metrics from BOTH sources
        for block in text_dict["blocks"]:
            if block["type"] == 0:
                total_text_blocks += 1
            elif block["type"] == 1:
                total_image_blocks += 1

        # Image blocks from standard extraction
        for block in full_dict["blocks"]:
            if block["type"] == 1:
                total_image_blocks += 1

        blocks = text_dict["blocks"]
        # Add images from full_dict, but transform their bboxes
        for b in full_dict["blocks"]:
            if b["type"] == 1: # image
                b_copy = b.copy()
                b_copy["bbox"] = fitz.Rect(b["bbox"]) * matrix
                blocks.append(b_copy)

        # Helper functions for collision detection (only used when not in interactive mode)
        def intersects_y(rect_a, rect_b):
            return not (rect_a.y1 < rect_b.y0 or rect_a.y0 > rect_b.y1)

        def is_to_right_of_checkbox(text_bbox, cb_rect):
            return text_bbox.x0 >= (cb_rect.x1 - 2)

        for block in blocks:
            if block["type"] == 0: # text
                for line in block["lines"]:
                    # Calculate line rotation
                    angle = math.atan2(line["dir"][1], line["dir"][0]) * 180 / math.pi
                    rotation_css = f"transform: rotate({angle}deg);" if abs(angle) > 0.01 else ""

                    for span in line["spans"]:
                        x, y = span["origin"]
                        bbox = span["bbox"] # (x0, y0, x1, y1)
                        width_pdf = bbox[2] - bbox[0]
                        height_pdf = bbox[3] - bbox[1]

                        font_size = span["size"]

                        # BASE VISUAL TUNING
                        LEFT_PADDING = 1.5 
                        VERTICAL_NUDGE = 1.0
                        
                        # Adjust coordinates
                        left = bbox[0] + LEFT_PADDING
                        top = bbox[1] + VERTICAL_NUDGE
                        
                        # --- GEOMETRY-AWARE SPACING ---
                        # Check against detected checkboxes
                        text_rect_fitz = fitz.Rect(bbox)
                        for cb in checkbox_rects:
                             if intersects_y(text_rect_fitz, cb) and is_to_right_of_checkbox(text_rect_fitz, cb):
                                 # Force a minimum gutter of 4pt from the checkbox
                                 target_left = cb.x1 + 4
                                 if left < target_left:
                                     left = target_left

                        text = span["text"].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        color = "#{:06x}".format(span["color"])

                        font_weight = "bold" if "Bold" in span.get("font", "") else "normal"
                        font_style = "italic" if "Italic" in span.get("font", "") else "normal"

                        html += f'<div class="text-span" style="left: {left}pt; top: {top}pt; {rotation_css} font-size: {font_size}pt; color: {color}; font-weight: {font_weight}; font-style: {font_style}; overflow: visible;">{text}</div>'
                        extracted_html_text += span["text"] + " "

            elif block["type"] == 1: # image
                bbox = block["bbox"]
                img_data = block["image"]
                img_base64 = base64.b64encode(img_data).decode('utf-8')
                html += f'<img src="data:image/png;base64,{img_base64}" style="position: absolute; left: {bbox[0]}pt; top: {bbox[1]}pt; width: {bbox[2]-bbox[0]}pt; height: {bbox[3]-bbox[1]}pt;">'

        html += "</div>"

    # Calculate comprehensive accuracy metrics
    metrics = calculate_accuracy_metrics(full_pdf_text, extracted_html_text)

    # Close HTML (no metrics banner in HTML output)
    html += "</body></html>"

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html)

    # Print metrics to console
    print(f"Successfully converted {pdf_path} to {output_path}")
    print("\n" + "="*60)
    print("CONVERSION METRICS")
    print("="*60)
    print(f"Overall Accuracy:     {metrics['overall_accuracy']}%")
    print(f"Character Accuracy:   {metrics['character_accuracy']}%")
    print(f"Word Accuracy:        {metrics['word_accuracy']}%")
    print(f"Total Pages:          {total_pages}")
    print(f"Text Blocks:          {total_text_blocks}")
    print(f"Image Blocks:         {total_image_blocks}")
    print(f"Total Characters:     {metrics['total_characters']}")
    print(f"Total Words:          {metrics['total_words']}")
    print("="*60)

    # Detect if PDF is image-based
    if total_image_blocks > 0 and total_text_blocks == 0:
        print("\n⚠️  WARNING: This PDF appears to be image-based (scanned).")
        print("   No text was extracted. Consider using OCR for text extraction.")

    # Save metrics to JSON file
    metrics_file = output_path.replace('.html', '_metrics.json')
    metrics_data = {
        **metrics,
        "total_pages": total_pages,
        "text_blocks": total_text_blocks,
        "image_blocks": total_image_blocks,
        "pdf_file": pdf_path,
        "html_file": output_path
    }

    with open(metrics_file, "w", encoding="utf-8") as f:
        json.dump(metrics_data, f, indent=2)

    print(f"\n📄 Metrics saved to: {metrics_file}")

if __name__ == "__main__":
    try:
        # Debug logging to stderr (so it shows up in logs even if exit code is 1)
        print(f"DEBUG: Script started with args: {sys.argv}", file=sys.stderr)
        print(f"DEBUG: Python version: {sys.version}", file=sys.stderr)

        # Parse command-line arguments
        parser = argparse.ArgumentParser(description='Convert PDF to HTML with optional interactive forms')
        parser.add_argument('input_pdf', help='Input PDF file path')
        parser.add_argument('output_html', nargs='?', help='Output HTML file path (optional)')
        parser.add_argument('--interactive', action='store_true', help='Preserve PDF form fields as interactive HTML inputs')

        args = parser.parse_args()

        input_pdf = args.input_pdf
        interactive = args.interactive

        if args.output_html:
            output_html = args.output_html
        else:
            # Infer output path if not provided
            import os
            base, _ = os.path.splitext(input_pdf)
            output_html = base + ".html"
            print(f"DEBUG: Output path not provided. Inferred: {output_html}", file=sys.stderr)

        print(f"DEBUG: Interactive mode: {interactive}", file=sys.stderr)
        convert_pdf_to_html_high_fidelity(input_pdf, output_html, interactive)

    except Exception as e:
        import traceback
        print("CRITICAL ERROR:", file=sys.stderr)
        traceback.print_exc(file=sys.stderr)
        sys.exit(1)

from __future__ import annotations

import html
import re
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)

DOCS = Path(__file__).resolve().parent
GUIDES = [
    ("USER-GUIDE.md", "USER-GUIDE.docx", "USER-GUIDE.pdf", "CSSMS End-User Guide"),
    ("ADMINISTRATOR-GUIDE.md", "ADMINISTRATOR-GUIDE.docx", "ADMINISTRATOR-GUIDE.pdf", "CSSMS Administrator Guide"),
]


def clean_inline(text: str) -> str:
    text = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", text)
    text = re.sub(r"`([^`]+)`", r"\1", text)
    text = re.sub(r"\*\*(.*?)\*\*", r"\1", text)
    text = re.sub(r"\*(.*?)\*", r"\1", text)
    return text.replace("  ", " ").strip()


def parse_table_row(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def parse_markdown(text: str) -> list[dict]:
    lines = text.splitlines()
    blocks = []
    index = 0
    while index < len(lines):
        line = lines[index]
        if not line.strip():
            index += 1
            continue
        if line.strip() == "<!-- pagebreak -->":
            blocks.append({"type": "pagebreak"})
            index += 1
            continue
        if line.startswith("```"):
            index += 1
            code = []
            while index < len(lines) and not lines[index].startswith("```"):
                code.append(lines[index])
                index += 1
            index += 1
            blocks.append({"type": "code", "text": "\n".join(code)})
            continue
        heading = re.match(r"^(#{1,6})\s+(.+)$", line)
        if heading:
            blocks.append({"type": "heading", "level": len(heading.group(1)), "text": clean_inline(heading.group(2))})
            index += 1
            continue
        if line.lstrip().startswith("|"):
            rows = []
            while index < len(lines) and lines[index].lstrip().startswith("|"):
                row = parse_table_row(lines[index])
                if not all(re.fullmatch(r":?-{3,}:?", cell.replace(" ", "")) for cell in row):
                    rows.append(row)
                index += 1
            if rows:
                width = max(len(row) for row in rows)
                blocks.append({"type": "table", "rows": [row + [""] * (width - len(row)) for row in rows]})
            continue
        if line.startswith(">"):
            quote = []
            while index < len(lines) and lines[index].startswith(">"):
                quote.append(clean_inline(lines[index][1:].strip()))
                index += 1
            blocks.append({"type": "quote", "text": " ".join(quote)})
            continue
        if re.match(r"^\s*[-*]\s+", line) or re.match(r"^\s*\d+[.)]\s+", line):
            items = []
            ordered = bool(re.match(r"^\s*\d+[.)]\s+", line))
            pattern = r"^\s*\d+[.)]\s+" if ordered else r"^\s*[-*]\s+"
            while index < len(lines) and re.match(pattern, lines[index]):
                items.append(clean_inline(re.sub(pattern, "", lines[index])))
                index += 1
            blocks.append({"type": "list", "ordered": ordered, "items": items})
            continue
        paragraph = [line.strip()]
        index += 1
        while index < len(lines) and lines[index].strip() and not lines[index].startswith(("#", ">", "```", "|")):
            if re.match(r"^\s*([-*]|\d+[.)])\s+", lines[index]):
                break
            paragraph.append(lines[index].strip())
            index += 1
        blocks.append({"type": "paragraph", "text": clean_inline(" ".join(paragraph))})
    return blocks


def set_cell_shading(cell, fill: str) -> None:
    properties = cell._tc.get_or_add_tcPr()
    shading = OxmlElement("w:shd")
    shading.set(qn("w:fill"), fill)
    properties.append(shading)


def set_repeat_table_header(row) -> None:
    properties = row._tr.get_or_add_trPr()
    repeat = OxmlElement("w:tblHeader")
    repeat.set(qn("w:val"), "true")
    properties.append(repeat)


def set_page_field(paragraph) -> None:
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instruction = OxmlElement("w:instrText")
    instruction.set(qn("xml:space"), "preserve")
    instruction.text = " PAGE "
    separate = OxmlElement("w:fldChar")
    separate.set(qn("w:fldCharType"), "separate")
    value = OxmlElement("w:t")
    value.text = "1"
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.extend([begin, instruction, separate, value, end])


def make_docx(source: Path, destination: Path, title: str) -> None:
    blocks = parse_markdown(source.read_text(encoding="utf-8"))
    doc = Document()
    section = doc.sections[0]
    section.top_margin = Inches(0.65)
    section.bottom_margin = Inches(0.65)
    section.left_margin = Inches(0.7)
    section.right_margin = Inches(0.7)
    section.header_distance = Inches(0.3)
    section.footer_distance = Inches(0.3)

    normal = doc.styles["Normal"]
    normal.font.name = "Aptos"
    normal.font.size = Pt(10)
    normal.font.color.rgb = RGBColor(35, 54, 65)
    normal.paragraph_format.space_after = Pt(5)
    for style_name, size, color in [("Heading 1", 17, "123C4A"), ("Heading 2", 13, "087E83"), ("Heading 3", 11, "087E83")]:
        style = doc.styles[style_name]
        style.font.name = "Aptos Display"
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor.from_string(color)

    header = section.header.paragraphs[0]
    header.text = title
    header.style = doc.styles["Caption"]
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run("CSSMS  |  ")
    set_page_field(footer)

    cover = True
    for block in blocks:
        kind = block["type"]
        if kind == "pagebreak":
            doc.add_page_break()
            cover = False
        elif kind == "heading":
            level = block["level"]
            text = block["text"]
            if cover and level == 1:
                paragraph = doc.add_paragraph(style="Title")
                paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                run = paragraph.add_run(text)
                run.font.name = "Aptos Display"
                run.font.size = Pt(27)
                run.font.bold = True
                run.font.color.rgb = RGBColor(18, 60, 74)
            elif cover and level == 2:
                paragraph = doc.add_paragraph()
                paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                run = paragraph.add_run(text)
                run.font.size = Pt(18)
                run.font.color.rgb = RGBColor(8, 126, 131)
            else:
                doc.add_heading(text, level=min(max(level - 1, 1), 3))
        elif kind == "paragraph":
            paragraph = doc.add_paragraph(block["text"])
            if cover and (block["text"].startswith("Version:") or block["text"].startswith("Prepared by:") or block["text"].startswith("Date:") or block["text"].startswith("Company:")):
                paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                for run in paragraph.runs:
                    run.font.size = Pt(11)
        elif kind == "quote":
            paragraph = doc.add_paragraph()
            paragraph.paragraph_format.left_indent = Inches(0.2)
            paragraph.paragraph_format.right_indent = Inches(0.2)
            run = paragraph.add_run(block["text"])
            run.italic = True
            run.font.color.rgb = RGBColor(78, 99, 108)
            if "Screenshot placeholder" in block["text"]:
                run.bold = True
                run.font.color.rgb = RGBColor(8, 126, 131)
        elif kind == "code":
            paragraph = doc.add_paragraph()
            paragraph.paragraph_format.left_indent = Inches(0.15)
            paragraph.paragraph_format.right_indent = Inches(0.15)
            paragraph.paragraph_format.space_before = Pt(4)
            paragraph.paragraph_format.space_after = Pt(8)
            run = paragraph.add_run(block["text"])
            run.font.name = "Consolas"
            run.font.size = Pt(8.5)
        elif kind == "list":
            style = "List Number" if block["ordered"] else "List Bullet"
            for item in block["items"]:
                doc.add_paragraph(item, style=style)
        elif kind == "table":
            rows = block["rows"]
            table = doc.add_table(rows=len(rows), cols=len(rows[0]))
            table.style = "Table Grid"
            table.alignment = WD_TABLE_ALIGNMENT.CENTER
            table.autofit = True
            for row_index, row_data in enumerate(rows):
                row = table.rows[row_index]
                if row_index == 0:
                    set_repeat_table_header(row)
                for col_index, value in enumerate(row_data):
                    cell = row.cells[col_index]
                    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.TOP
                    cell.text = value
                    for paragraph in cell.paragraphs:
                        paragraph.paragraph_format.space_after = Pt(2)
                        for run in paragraph.runs:
                            run.font.size = Pt(8.5)
                            if row_index == 0:
                                run.font.bold = True
                                run.font.color.rgb = RGBColor(255, 255, 255)
                    if row_index == 0:
                        set_cell_shading(cell, "123C4A")
            doc.add_paragraph().paragraph_format.space_after = Pt(1)

    doc.core_properties.title = title
    doc.core_properties.subject = "End-user and administrator operating instructions"
    doc.core_properties.author = "CSSMS Documentation Team"
    doc.save(destination)


def pdf_styles():
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name="GuideTitle", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=23, leading=28, textColor=colors.HexColor("#123c4a"), alignment=TA_CENTER, spaceAfter=12))
    styles.add(ParagraphStyle(name="GuideSubtitle", parent=styles["Heading1"], fontName="Helvetica", fontSize=16, leading=20, textColor=colors.HexColor("#087e83"), alignment=TA_CENTER, spaceAfter=18))
    styles.add(ParagraphStyle(name="GuideH1", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=16, leading=19, textColor=colors.HexColor("#123c4a"), spaceBefore=14, spaceAfter=8, keepWithNext=True))
    styles.add(ParagraphStyle(name="GuideH2", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=12, leading=15, textColor=colors.HexColor("#087e83"), spaceBefore=10, spaceAfter=5, keepWithNext=True))
    styles.add(ParagraphStyle(name="GuideH3", parent=styles["Heading3"], fontName="Helvetica-Bold", fontSize=10.5, leading=13, textColor=colors.HexColor("#234d5a"), spaceBefore=8, spaceAfter=4, keepWithNext=True))
    styles.add(ParagraphStyle(name="GuideBody", parent=styles["BodyText"], fontName="Helvetica", fontSize=9.2, leading=13, textColor=colors.HexColor("#233640"), spaceAfter=5))
    styles.add(ParagraphStyle(name="GuideMeta", parent=styles["BodyText"], fontName="Helvetica", fontSize=10, leading=15, alignment=TA_CENTER, textColor=colors.HexColor("#344b56"), spaceAfter=4))
    styles.add(ParagraphStyle(name="GuideQuote", parent=styles["BodyText"], fontName="Helvetica-Oblique", fontSize=8.7, leading=12, textColor=colors.HexColor("#536873"), leftIndent=10, rightIndent=10, borderColor=colors.HexColor("#b9d9d7"), borderWidth=0.8, borderPadding=7, backColor=colors.HexColor("#f0f8f7"), spaceBefore=4, spaceAfter=8))
    styles.add(ParagraphStyle(name="GuideBullet", parent=styles["BodyText"], fontName="Helvetica", fontSize=9.2, leading=13, leftIndent=15, firstLineIndent=-9, spaceAfter=3))
    styles.add(ParagraphStyle(name="GuideCode", parent=styles["Code"], fontName="Courier", fontSize=7.6, leading=10, leftIndent=8, rightIndent=8, borderColor=colors.HexColor("#d7e0e4"), borderWidth=0.5, borderPadding=6, backColor=colors.HexColor("#f4f6f7"), spaceBefore=4, spaceAfter=8))
    styles.add(ParagraphStyle(name="GuideCell", parent=styles["BodyText"], fontName="Helvetica", fontSize=7.4, leading=9.4, textColor=colors.HexColor("#233640")))
    styles.add(ParagraphStyle(name="GuideCellHead", parent=styles["BodyText"], fontName="Helvetica-Bold", fontSize=7.4, leading=9.4, textColor=colors.white))
    return styles


def draw_page(canvas, doc, title):
    canvas.saveState()
    width, height = A4
    canvas.setStrokeColor(colors.HexColor("#d7e0e4"))
    canvas.line(18 * mm, height - 15 * mm, width - 18 * mm, height - 15 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#536873"))
    canvas.drawString(18 * mm, height - 11 * mm, title)
    canvas.drawRightString(width - 18 * mm, 10 * mm, f"Page {doc.page}")
    canvas.restoreState()


def make_pdf(source: Path, destination: Path, title: str) -> None:
    blocks = parse_markdown(source.read_text(encoding="utf-8"))
    styles = pdf_styles()
    page_width, _ = A4
    margin = 17 * mm
    available = page_width - 2 * margin
    story = []
    cover = True
    for block in blocks:
        kind = block["type"]
        if kind == "pagebreak":
            story.append(PageBreak())
        elif kind == "heading":
            if cover and block["level"] == 1:
                story.append(Spacer(1, 38 * mm))
                story.append(Paragraph(html.escape(block["text"]), styles["GuideTitle"]))
            elif cover and block["level"] == 2:
                story.append(Paragraph(html.escape(block["text"]), styles["GuideSubtitle"]))
            else:
                name = "GuideH1" if block["level"] <= 2 else "GuideH2" if block["level"] == 3 else "GuideH3"
                story.append(Paragraph(html.escape(block["text"]), styles[name]))
        elif kind == "paragraph":
            style = styles["GuideMeta"] if cover and block["text"].startswith(("Version:", "Prepared by:", "Date:", "Company:")) else styles["GuideBody"]
            story.append(Paragraph(html.escape(block["text"]), style))
        elif kind == "quote":
            text = html.escape(block["text"])
            if "Screenshot placeholder" in block["text"]:
                text = "[ SCREENSHOT PLACEHOLDER ]<br/>" + text.replace("Screenshot placeholder:", "Insert screenshot:")
            story.append(Paragraph(text, styles["GuideQuote"]))
        elif kind == "code":
            story.append(Preformatted(block["text"], styles["GuideCode"], maxLineLength=100))
        elif kind == "list":
            for number, item in enumerate(block["items"], 1):
                marker = f"{number}." if block["ordered"] else "-"
                story.append(Paragraph(f"{marker} {html.escape(item)}", styles["GuideBullet"]))
        elif kind == "table":
            data = []
            for row_index, row in enumerate(block["rows"]):
                cell_style = styles["GuideCellHead"] if row_index == 0 else styles["GuideCell"]
                data.append([Paragraph(html.escape(value), cell_style) for value in row])
            col_width = available / max(1, len(data[0]))
            table = Table(data, colWidths=[col_width] * len(data[0]), repeatRows=1, hAlign="LEFT")
            commands = [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#123c4a")),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#cbd6da")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
            for row_index in range(2, len(data), 2):
                commands.append(("BACKGROUND", (0, row_index), (-1, row_index), colors.HexColor("#f5f8f9")))
            table.setStyle(TableStyle(commands))
            story.extend([table, Spacer(1, 5)])

    doc = BaseDocTemplate(str(destination), pagesize=A4, rightMargin=margin, leftMargin=margin, topMargin=21 * mm, bottomMargin=18 * mm, title=title, author="CSSMS Documentation Team")
    frame = Frame(margin, 18 * mm, available, A4[1] - 39 * mm, id="normal")
    doc.addPageTemplates([PageTemplate(id="manual", frames=frame, onPage=lambda canvas, instance: draw_page(canvas, instance, title))])
    doc.build(story)


def main() -> None:
    for markdown_name, docx_name, pdf_name, title in GUIDES:
        source = DOCS / markdown_name
        make_docx(source, DOCS / docx_name, title)
        make_pdf(source, DOCS / pdf_name, title)
        print(f"Generated {docx_name} and {pdf_name}")


if __name__ == "__main__":
    main()

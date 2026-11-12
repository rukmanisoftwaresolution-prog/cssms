package com.cssms.service;

import com.cssms.dto.ReportResponse;
import com.cssms.dto.ReportResponse.Column;
import com.cssms.dto.ReportResponse.Summary;
import com.cssms.dto.ReportResponse.Type;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Renders any {@link ReportResponse} as an Excel (Apache POI) or PDF (OpenPDF) document. */
@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public byte[] toExcel(ReportResponse report) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(report.title()));
            CreationHelper helper = workbook.getCreationHelper();

            org.apache.poi.ss.usermodel.Font bold = workbook.createFont();
            bold.setBold(true);
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(bold);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.###"));
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd-mm-yyyy"));
            dateStyle.setAlignment(HorizontalAlignment.LEFT);
            CellStyle summaryLabelStyle = workbook.createCellStyle();
            summaryLabelStyle.setFont(bold);

            int rowIndex = 0;
            Cell titleCell = sheet.createRow(rowIndex++).createCell(0);
            titleCell.setCellValue(report.title());
            titleCell.setCellStyle(titleStyle);
            if (report.subtitle() != null) {
                sheet.createRow(rowIndex++).createCell(0).setCellValue(report.subtitle());
            }
            rowIndex++;

            List<Column> columns = report.columns();
            Row header = sheet.createRow(rowIndex++);
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(columns.get(c).label());
                cell.setCellStyle(headerStyle);
            }
            for (var data : report.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int c = 0; c < columns.size(); c++) {
                    writeCell(row.createCell(c), data.get(columns.get(c).key()), columns.get(c).type(),
                            currencyStyle, numberStyle, dateStyle);
                }
            }
            if (!report.summary().isEmpty()) {
                rowIndex++;
                for (Summary s : report.summary()) {
                    Row row = sheet.createRow(rowIndex++);
                    Cell label = row.createCell(0);
                    label.setCellValue(s.label());
                    label.setCellStyle(summaryLabelStyle);
                    writeCell(row.createCell(1), s.value(), s.type(), currencyStyle, numberStyle, dateStyle);
                }
            }
            for (int c = 0; c < columns.size(); c++) {
                sheet.autoSizeColumn(c);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not generate Excel report", ex);
        }
    }

    private void writeCell(Cell cell, Object value, Type type, CellStyle currency, CellStyle number, CellStyle date) {
        if (value == null) {
            return;
        }
        if (value instanceof LocalDate d) {
            cell.setCellValue(d);
            cell.setCellStyle(date);
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
            cell.setCellStyle(type == Type.CURRENCY ? currency : number);
        } else {
            cell.setCellValue(neutralizeFormula(value.toString()));
        }
    }

    /** Prevents spreadsheet formula injection from user-entered text. */
    private static String neutralizeFormula(String text) {
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text;
    }

    public byte[] toPdf(ReportResponse report) {
        List<Column> columns = report.columns();
        Document document = new Document(columns.size() > 6 ? PageSize.A4.rotate() : PageSize.A4, 28, 28, 32, 32);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(report.title(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            if (report.subtitle() != null) {
                document.add(new Paragraph(report.subtitle(), FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY)));
            }
            document.add(new Paragraph(" "));

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            PdfPTable table = new PdfPTable(Math.max(columns.size(), 1));
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (Column column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column.label(), headerFont));
                cell.setBackgroundColor(new Color(224, 224, 224));
                cell.setPadding(4);
                cell.setHorizontalAlignment(isNumeric(column.type()) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                table.addCell(cell);
            }
            for (var row : report.rows()) {
                for (Column column : columns) {
                    PdfPCell cell = new PdfPCell(new Phrase(format(row.get(column.key()), column.type()), bodyFont));
                    cell.setPadding(3);
                    cell.setHorizontalAlignment(isNumeric(column.type()) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                    table.addCell(cell);
                }
            }
            document.add(table);

            if (!report.summary().isEmpty()) {
                document.add(new Paragraph(" "));
                PdfPTable summary = new PdfPTable(2);
                summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
                summary.setWidthPercentage(45);
                for (Summary s : report.summary()) {
                    summary.addCell(new PdfPCell(new Phrase(s.label(), headerFont)));
                    PdfPCell value = new PdfPCell(new Phrase(format(s.value(), s.type()), bodyFont));
                    value.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    summary.addCell(value);
                }
                document.add(summary);
            }
            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate PDF report", ex);
        }
    }

    private static boolean isNumeric(Type type) {
        return type == Type.CURRENCY || type == Type.NUMBER;
    }

    static String format(Object value, Type type) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate d) {
            return d.format(DATE_FORMAT);
        }
        if (value instanceof Number n) {
            DecimalFormat format = new DecimalFormat(type == Type.CURRENCY ? "#,##0.00" : "#,##0.###",
                    DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            return format.format(n instanceof BigDecimal b ? b : BigDecimal.valueOf(n.doubleValue()));
        }
        return value.toString();
    }
}

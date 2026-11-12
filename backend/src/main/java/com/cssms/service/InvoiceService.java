package com.cssms.service;

import com.cssms.config.AppProperties;
import com.cssms.entity.Customer;
import com.cssms.entity.SalesOrder;
import com.cssms.entity.SalesOrderItem;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.SalesOrderRepository;
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
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);

    private final SalesOrderRepository repository;
    private final AppProperties properties;

    public record Invoice(String fileName, byte[] content) {
    }

    @Transactional(readOnly = true)
    public Invoice generate(Long salesOrderId) {
        SalesOrder order = repository.findById(salesOrderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sales order", salesOrderId));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            writeHeader(document, order);
            writeParties(document, order);
            writeItems(document, order);
            writeTotals(document, order);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("This is a computer generated invoice.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
            document.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate invoice", ex);
        }
        return new Invoice(order.getInvoiceNumber() + ".pdf", out.toByteArray());
    }

    private void writeHeader(Document document, SalesOrder order) throws DocumentException {
        AppProperties.Company company = properties.company();
        Paragraph name = new Paragraph(company != null && company.name() != null ? company.name() : "CSSMS", TITLE);
        name.setAlignment(Element.ALIGN_CENTER);
        document.add(name);
        if (company != null) {
            StringBuilder line = new StringBuilder();
            if (company.address() != null) {
                line.append(company.address());
            }
            if (company.phone() != null) {
                line.append("  |  ").append(company.phone());
            }
            if (company.gstin() != null && !company.gstin().isBlank()) {
                line.append("  |  GSTIN: ").append(company.gstin());
            }
            Paragraph details = new Paragraph(line.toString(), NORMAL);
            details.setAlignment(Element.ALIGN_CENTER);
            document.add(details);
        }
        Paragraph title = new Paragraph("\nTAX INVOICE" + (order.getStatus().name().equals("CANCELLED") ? " (CANCELLED)" : ""), BOLD);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));
    }

    private void writeParties(Document document, SalesOrder order) throws DocumentException {
        Customer customer = order.getCustomer();
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(cell("Invoice No: " + order.getInvoiceNumber() + "\nOrder No: " + order.getSalesOrderNumber()
                + "\nDate: " + order.getOrderDate().format(DATE_FORMAT)
                + "\nVehicle: " + nullToDash(order.getVehicleNumber()), NORMAL));
        String billTo = customer.getCustomerName()
                + (customer.getCompanyName() != null ? "\n" + customer.getCompanyName() : "")
                + "\n" + customer.getAddress()
                + "\nMobile: " + customer.getMobileNumber()
                + (customer.getGstNumber() != null ? "\nGSTIN: " + customer.getGstNumber() : "");
        table.addCell(cell("Bill To:\n" + billTo + "\n\nDeliver To:\n" + nullToDash(order.getDeliveryAddress()), NORMAL));
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void writeItems(Document document, SalesOrder order) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[] {3.2f, 1.4f, 1.4f, 1.4f, 1.6f, 1f, 1.4f, 1.7f});
        table.setWidthPercentage(100);
        for (String heading : new String[] {"Material", "Qty", "Rate", "Discount", "Amount", "GST %", "GST", "Total"}) {
            PdfPCell header = cell(heading, BOLD);
            header.setBackgroundColor(new Color(224, 224, 224));
            header.setHorizontalAlignment(heading.equals("Material") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            table.addCell(header);
        }
        for (SalesOrderItem item : order.getItems()) {
            table.addCell(cell(item.getMaterial().getMaterialName(), NORMAL));
            table.addCell(right(ExportService.format(item.getQuantity(), com.cssms.dto.ReportResponse.Type.NUMBER)));
            table.addCell(right(ExportService.format(item.getRate(), com.cssms.dto.ReportResponse.Type.CURRENCY)));
            table.addCell(right(ExportService.format(item.getDiscount(), com.cssms.dto.ReportResponse.Type.CURRENCY)));
            table.addCell(right(ExportService.format(item.getAmount(), com.cssms.dto.ReportResponse.Type.CURRENCY)));
            table.addCell(right(ExportService.format(item.getGstPercentage(), com.cssms.dto.ReportResponse.Type.NUMBER)));
            table.addCell(right(ExportService.format(item.getGstAmount(), com.cssms.dto.ReportResponse.Type.CURRENCY)));
            table.addCell(right(ExportService.format(item.getTotalAmount(), com.cssms.dto.ReportResponse.Type.CURRENCY)));
        }
        document.add(table);
    }

    private void writeTotals(Document document, SalesOrder order) throws DocumentException {
        document.add(new Paragraph(" "));
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(40);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        row(totals, "Sub Total", order.getSubTotal(), false);
        row(totals, "Discount", order.getDiscountTotal(), false);
        row(totals, "GST", order.getGstAmount(), false);
        row(totals, "Grand Total", order.getTotalAmount(), true);
        row(totals, "Received", order.getReceivedAmount(), false);
        row(totals, "Balance Due", order.getPendingAmount(), true);
        document.add(totals);
        document.add(new Paragraph("Amounts in INR", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
    }

    private void row(PdfPTable table, String label, java.math.BigDecimal amount, boolean bold) {
        table.addCell(cell(label, bold ? BOLD : NORMAL));
        PdfPCell value = cell(ExportService.format(amount, com.cssms.dto.ReportResponse.Type.CURRENCY), bold ? BOLD : NORMAL);
        value.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(value);
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell right(String text) {
        PdfPCell cell = cell(text, NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

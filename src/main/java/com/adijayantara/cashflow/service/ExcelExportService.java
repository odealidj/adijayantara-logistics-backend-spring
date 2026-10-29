package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.dto.CashflowFilterRequest;
import com.adijayantara.cashflow.dto.InvoiceFilterRequest;
import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import com.adijayantara.cashflow.domain.entity.Invoice;
import com.adijayantara.cashflow.domain.enums.InvoiceStatus;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import com.adijayantara.cashflow.repository.CashflowEntryRepository;
import com.adijayantara.cashflow.repository.InvoiceRepository;
import com.adijayantara.cashflow.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private final CashflowEntryRepository cashflowEntryRepository;
    private final InvoiceRepository invoiceRepository;
    private final CashflowService cashflowService;
    private final InvoiceService invoiceService;

    private static final String COMPANY_NAME = "PT ADIJAYANTARA LOGISTIC INDONESIA";
    private static final String COMPANY_TAGLINE = "Freight Forwarding & Logistics Services";
    private static final String COMPANY_ADDRESS = "WISMA SMR JL YOS SUDARSO, Kav. 89 Lantai 9, UNIT 904, Jakarta Utara 14350";
    private static final String COMPANY_EMAIL = "Email: adijantara.logistic@gmail.com";

    private XSSFColor createColor(String hex) {
        byte[] rgb = new byte[]{
                (byte) Integer.parseInt(hex.substring(0, 2), 16),
                (byte) Integer.parseInt(hex.substring(2, 4), 16),
                (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
        return new XSSFColor(rgb, new DefaultIndexedColorMap());
    }

    private void addLogo(Workbook workbook, Sheet sheet, int col1, int row1, double scale) {
        try (InputStream is = getClass().getResourceAsStream("/assets/logo.png")) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                CreationHelper helper = workbook.getCreationHelper();
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(col1);
                anchor.setRow1(row1);
                Picture pict = drawing.createPicture(anchor, pictureIdx);
                pict.resize(scale, scale);
            }
        } catch (Exception e) {
            log.warn("Logo could not be loaded into Excel export: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportCashflow(CashflowFilterRequest filter) {
        Specification<CashflowEntry> spec = cashflowService.buildSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.ASC, "dateOfEntry")
                .and(Sort.by(Sort.Direction.ASC, "sequenceNo"))
                .and(Sort.by(Sort.Direction.ASC, "id"));
        List<CashflowEntry> entries = cashflowEntryRepository.findAll(spec, sort);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            DataFormat dataFormat = workbook.createDataFormat();

            // Font & Styles
            Font companyFont = workbook.createFont();
            companyFont.setFontName("Calibri");
            companyFont.setFontHeightInPoints((short) 14);
            companyFont.setBold(true);
            companyFont.setColor(createColor("1A365D").getIndexed());

            CellStyle companyStyle = workbook.createCellStyle();
            companyStyle.setFont(companyFont);
            companyStyle.setAlignment(HorizontalAlignment.CENTER);
            companyStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font taglineFont = workbook.createFont();
            taglineFont.setFontName("Calibri");
            taglineFont.setFontHeightInPoints((short) 10);
            taglineFont.setBold(true);
            taglineFont.setColor(createColor("4A5568").getIndexed());

            CellStyle taglineStyle = workbook.createCellStyle();
            taglineStyle.setFont(taglineFont);
            taglineStyle.setAlignment(HorizontalAlignment.CENTER);
            taglineStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font addressFont = workbook.createFont();
            addressFont.setFontName("Calibri");
            addressFont.setFontHeightInPoints((short) 9);
            addressFont.setColor(createColor("64748B").getIndexed());

            CellStyle addressStyle = workbook.createCellStyle();
            addressStyle.setFont(addressFont);
            addressStyle.setAlignment(HorizontalAlignment.CENTER);
            addressStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font docTitleFont = workbook.createFont();
            docTitleFont.setFontName("Calibri");
            docTitleFont.setFontHeightInPoints((short) 11);
            docTitleFont.setBold(true);
            docTitleFont.setColor(createColor("1E293B").getIndexed());

            CellStyle docTitleStyle = workbook.createCellStyle();
            docTitleStyle.setFont(docTitleFont);
            docTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            docTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font periodFont = workbook.createFont();
            periodFont.setFontName("Calibri");
            periodFont.setFontHeightInPoints((short) 9);
            periodFont.setBold(true);
            periodFont.setColor(createColor("475569").getIndexed());

            CellStyle periodStyle = workbook.createCellStyle();
            periodStyle.setFont(periodFont);
            periodStyle.setAlignment(HorizontalAlignment.CENTER);
            periodStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            periodStyle.setBorderBottom(BorderStyle.MEDIUM);

            // Table Header Style (Navy Blue #223249, Bold White)
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(createColor("223249"));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Number Style (#,##0)
            CellStyle numStyle = workbook.createCellStyle();
            numStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            numStyle.setAlignment(HorizontalAlignment.RIGHT);
            numStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            numStyle.setBorderTop(BorderStyle.THIN);
            numStyle.setBorderBottom(BorderStyle.THIN);
            numStyle.setBorderLeft(BorderStyle.THIN);
            numStyle.setBorderRight(BorderStyle.THIN);

            // Center Style
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);

            // Text Style
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setAlignment(HorizontalAlignment.LEFT);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            textStyle.setBorderTop(BorderStyle.THIN);
            textStyle.setBorderBottom(BorderStyle.THIN);
            textStyle.setBorderLeft(BorderStyle.THIN);
            textStyle.setBorderRight(BorderStyle.THIN);

            // Total Style
            Font totalFont = workbook.createFont();
            totalFont.setFontName("Calibri");
            totalFont.setFontHeightInPoints((short) 10);
            totalFont.setBold(true);
            totalFont.setColor(createColor("1E293B").getIndexed());

            CellStyle totalNumStyle = workbook.createCellStyle();
            totalNumStyle.setFont(totalFont);
            totalNumStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            totalNumStyle.setFillForegroundColor(createColor("F1F5F9"));
            totalNumStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalNumStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalNumStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalNumStyle.setBorderTop(BorderStyle.THIN);
            totalNumStyle.setBorderBottom(BorderStyle.DOUBLE);
            totalNumStyle.setBorderLeft(BorderStyle.THIN);
            totalNumStyle.setBorderRight(BorderStyle.THIN);

            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setFont(totalFont);
            totalLabelStyle.setFillForegroundColor(createColor("F1F5F9"));
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalLabelStyle.setAlignment(HorizontalAlignment.CENTER);
            totalLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalLabelStyle.setBorderTop(BorderStyle.THIN);
            totalLabelStyle.setBorderBottom(BorderStyle.DOUBLE);
            totalLabelStyle.setBorderLeft(BorderStyle.THIN);
            totalLabelStyle.setBorderRight(BorderStyle.THIN);

            // Set Row Heights
            sheet.createRow(0).setHeightInPoints(24);
            sheet.createRow(1).setHeightInPoints(16);
            sheet.createRow(2).setHeightInPoints(16);
            sheet.createRow(3).setHeightInPoints(20);
            sheet.createRow(4).setHeightInPoints(18);
            sheet.createRow(5).setHeightInPoints(26);

            // Add Logo at A1
            addLogo(workbook, sheet, 0, 0, 0.45);

            // Kop Surat
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 13));
            Row r0 = sheet.getRow(0);
            Cell c0 = r0.createCell(2);
            c0.setCellValue(COMPANY_NAME);
            c0.setCellStyle(companyStyle);

            sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 13));
            Row r1 = sheet.getRow(1);
            Cell c1 = r1.createCell(2);
            c1.setCellValue(COMPANY_TAGLINE);
            c1.setCellStyle(taglineStyle);

            sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 13));
            Row r2 = sheet.getRow(2);
            Cell c2 = r2.createCell(2);
            c2.setCellValue(COMPANY_ADDRESS + " • " + COMPANY_EMAIL);
            c2.setCellStyle(addressStyle);

            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 13));
            Row r3 = sheet.getRow(3);
            Cell c3 = r3.createCell(0);
            c3.setCellValue("CASHFLOW SHIPMENT CONTROL");
            c3.setCellStyle(docTitleStyle);

            // Periode Row 5
            String periodText = "PERIODE: SEMUA TRANSAKSI";
            if (filter != null && filter.getDateFrom() != null && filter.getDateTo() != null) {
                periodText = "PERIODE: " + DateUtil.formatDateIndo(filter.getDateFrom()) + " s/d " + DateUtil.formatDateIndo(filter.getDateTo());
            } else if (filter != null && filter.getDateFrom() != null) {
                periodText = "PERIODE MULAI: " + DateUtil.formatDateIndo(filter.getDateFrom());
            } else if (filter != null && filter.getDateTo() != null) {
                periodText = "PERIODE SAMPAI: " + DateUtil.formatDateIndo(filter.getDateTo());
            }

            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 13));
            Row r4 = sheet.getRow(4);
            for (int col = 0; col < 14; col++) {
                Cell cell = r4.createCell(col);
                cell.setCellStyle(periodStyle);
            }
            r4.getCell(0).setCellValue(periodText);

            // Row 6: Headers (14 Kolom)
            String[] headers = {
                    "KREDIT", "DEBIT", "SALDO", "DATE OF ENTRY", "ACT INFORMATION",
                    "ACT EXPLAINATION", "VENDOR", "T O P", "DUE DATE", "GRAND COST",
                    "GRAND SELLING", "PROFIT", "MARGIN IN %", "REMARKS"
            };
            Row r5 = sheet.getRow(5);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = r5.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows (Row 7 onwards, index 6)
            int rowIndex = 6;
            BigDecimal totalKredit = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal totalSelling = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;
            BigDecimal lastSaldo = BigDecimal.ZERO;

            for (CashflowEntry entry : entries) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);

                BigDecimal kredit = entry.getKredit() != null ? entry.getKredit() : BigDecimal.ZERO;
                BigDecimal debit = entry.getDebit() != null ? entry.getDebit() : BigDecimal.ZERO;
                BigDecimal saldo = entry.getSaldo() != null ? entry.getSaldo() : BigDecimal.ZERO;
                BigDecimal grandCost = entry.getGrandCost() != null ? entry.getGrandCost() : BigDecimal.ZERO;
                BigDecimal grandSelling = entry.getGrandSelling() != null ? entry.getGrandSelling() : BigDecimal.ZERO;
                BigDecimal profit = entry.getProfit() != null ? entry.getProfit() : BigDecimal.ZERO;

                totalKredit = totalKredit.add(kredit);
                totalDebit = totalDebit.add(debit);
                totalCost = totalCost.add(grandCost);
                totalSelling = totalSelling.add(grandSelling);
                totalProfit = totalProfit.add(profit);
                lastSaldo = saldo;

                // A: Kredit
                Cell cA = row.createCell(0);
                cA.setCellValue(kredit.doubleValue());
                cA.setCellStyle(numStyle);

                // B: Debit
                Cell cB = row.createCell(1);
                cB.setCellValue(debit.doubleValue());
                cB.setCellStyle(numStyle);

                // C: Saldo
                Cell cC = row.createCell(2);
                cC.setCellValue(saldo.doubleValue());
                cC.setCellStyle(numStyle);

                // D: Date of Entry
                Cell cD = row.createCell(3);
                cD.setCellValue(DateUtil.formatDateIndo(entry.getDateOfEntry()));
                cD.setCellStyle(centerStyle);

                // E: Act Info
                Cell cE = row.createCell(4);
                cE.setCellValue(entry.getActInformation() != null ? entry.getActInformation() : "");
                cE.setCellStyle(textStyle);

                // F: Act Explanation
                Cell cF = row.createCell(5);
                cF.setCellValue(entry.getActExplaination() != null ? entry.getActExplaination() : "");
                cF.setCellStyle(textStyle);

                // G: Vendor
                Cell cG = row.createCell(6);
                cG.setCellValue(entry.getVendorNameRaw() != null ? entry.getVendorNameRaw() : "");
                cG.setCellStyle(textStyle);

                // H: TOP
                Cell cH = row.createCell(7);
                String topStr = (entry.getTopDays() != null && entry.getTopDays() > 0) ? entry.getTopDays() + " HARI" : "-";
                cH.setCellValue(topStr);
                cH.setCellStyle(centerStyle);

                // I: Due Date
                Cell cI = row.createCell(8);
                cI.setCellValue(entry.getDueDate() != null ? DateUtil.formatDateIndo(entry.getDueDate()) : "-");
                cI.setCellStyle(centerStyle);

                // J: Grand Cost
                Cell cJ = row.createCell(9);
                cJ.setCellValue(grandCost.doubleValue());
                cJ.setCellStyle(numStyle);

                // K: Grand Selling
                Cell cK = row.createCell(10);
                cK.setCellValue(grandSelling.doubleValue());
                cK.setCellStyle(numStyle);

                // L: Profit
                Cell cL = row.createCell(11);
                cL.setCellValue(profit.doubleValue());
                cL.setCellStyle(numStyle);

                // M: Margin %
                Cell cM = row.createCell(12);
                String marginStr = "-";
                if (entry.getMarginPct() != null && grandSelling.compareTo(BigDecimal.ZERO) > 0) {
                    marginStr = String.format("%.2f%%", entry.getMarginPct().multiply(BigDecimal.valueOf(100)).doubleValue());
                }
                cM.setCellValue(marginStr);
                cM.setCellStyle(centerStyle);

                // N: Remarks
                Cell cN = row.createCell(13);
                cN.setCellValue(entry.getRemarks() != null ? entry.getRemarks().name() : "");
                cN.setCellStyle(centerStyle);
            }

            // Total Row
            if (!entries.isEmpty()) {
                Row sumRow = sheet.createRow(rowIndex);
                sumRow.setHeightInPoints(22);

                Cell sA = sumRow.createCell(0);
                sA.setCellValue(totalKredit.doubleValue());
                sA.setCellStyle(totalNumStyle);

                Cell sB = sumRow.createCell(1);
                sB.setCellValue(totalDebit.doubleValue());
                sB.setCellStyle(totalNumStyle);

                Cell sC = sumRow.createCell(2);
                sC.setCellValue(lastSaldo.doubleValue());
                sC.setCellStyle(totalNumStyle);

                for (int col = 3; col <= 8; col++) {
                    Cell c = sumRow.createCell(col);
                    c.setCellStyle(totalLabelStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 3, 8));
                sumRow.getCell(3).setCellValue("TOTAL");

                Cell sJ = sumRow.createCell(9);
                sJ.setCellValue(totalCost.doubleValue());
                sJ.setCellStyle(totalNumStyle);

                Cell sK = sumRow.createCell(10);
                sK.setCellValue(totalSelling.doubleValue());
                sK.setCellStyle(totalNumStyle);

                Cell sL = sumRow.createCell(11);
                sL.setCellValue(totalProfit.doubleValue());
                sL.setCellStyle(totalNumStyle);

                Cell sM = sumRow.createCell(12);
                String avgMargin = "-";
                if (totalSelling.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal avg = totalProfit.divide(totalSelling, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                    avgMargin = String.format("%.2f%%", avg.doubleValue());
                }
                sM.setCellValue(avgMargin);
                sM.setCellStyle(totalLabelStyle);

                Cell sN = sumRow.createCell(13);
                sN.setCellValue("-");
                sN.setCellStyle(totalLabelStyle);
            }

            // Set Column Widths (in 256ths of a character)
            sheet.setColumnWidth(0, 16 * 256);
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            sheet.setColumnWidth(3, 14 * 256);
            sheet.setColumnWidth(4, 28 * 256);
            sheet.setColumnWidth(5, 28 * 256);
            sheet.setColumnWidth(6, 22 * 256);
            sheet.setColumnWidth(7, 12 * 256);
            sheet.setColumnWidth(8, 14 * 256);
            sheet.setColumnWidth(9, 16 * 256);
            sheet.setColumnWidth(10, 16 * 256);
            sheet.setColumnWidth(11, 16 * 256);
            sheet.setColumnWidth(12, 14 * 256);
            sheet.setColumnWidth(13, 14 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to export cashflow excel: {}", e.getMessage(), e);
            throw new RuntimeException("Gagal mengekspor laporan kas ke Excel: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportInvoices(InvoiceFilterRequest filter) {
        Specification<Invoice> spec = invoiceService.buildSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "dueDate").and(Sort.by(Sort.Direction.DESC, "id"));
        List<Invoice> invoices = invoiceRepository.findAll(spec, sort);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;
        int totalCount = 0, paidCount = 0, unpaidCount = 0, overdueCount = 0;

        for (Invoice inv : invoices) {
            BigDecimal amt = inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(amt);
            totalCount++;
            if (inv.getStatus() == InvoiceStatus.PAID) {
                paidAmount = paidAmount.add(amt);
                paidCount++;
            } else if (inv.getStatus() == InvoiceStatus.OVERDUE) {
                overdueAmount = overdueAmount.add(amt);
                overdueCount++;
            } else {
                unpaidAmount = unpaidAmount.add(amt);
                unpaidCount++;
            }
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = "Rekapitulasi Invoice";
            Sheet sheet = workbook.createSheet(sheetName);
            DataFormat dataFormat = workbook.createDataFormat();

            // Setup Fonts & Styles
            Font companyFont = workbook.createFont();
            companyFont.setFontName("Calibri");
            companyFont.setFontHeightInPoints((short) 14);
            companyFont.setBold(true);
            companyFont.setColor(createColor("1A365D").getIndexed());

            CellStyle companyStyle = workbook.createCellStyle();
            companyStyle.setFont(companyFont);
            companyStyle.setAlignment(HorizontalAlignment.CENTER);
            companyStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font taglineFont = workbook.createFont();
            taglineFont.setFontName("Calibri");
            taglineFont.setFontHeightInPoints((short) 10);
            taglineFont.setBold(true);
            taglineFont.setColor(createColor("4A5568").getIndexed());

            CellStyle taglineStyle = workbook.createCellStyle();
            taglineStyle.setFont(taglineFont);
            taglineStyle.setAlignment(HorizontalAlignment.CENTER);
            taglineStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font addressFont = workbook.createFont();
            addressFont.setFontName("Calibri");
            addressFont.setFontHeightInPoints((short) 8);
            addressFont.setColor(createColor("64748B").getIndexed());

            CellStyle addressStyle = workbook.createCellStyle();
            addressStyle.setFont(addressFont);
            addressStyle.setAlignment(HorizontalAlignment.CENTER);
            addressStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle borderDoubleStyle = workbook.createCellStyle();
            borderDoubleStyle.setBorderBottom(BorderStyle.DOUBLE);

            Font docTitleFont = workbook.createFont();
            docTitleFont.setFontName("Calibri");
            docTitleFont.setFontHeightInPoints((short) 13);
            docTitleFont.setBold(true);
            docTitleFont.setColor(createColor("1A365D").getIndexed());

            CellStyle docTitleStyle = workbook.createCellStyle();
            docTitleStyle.setFont(docTitleFont);
            docTitleStyle.setAlignment(HorizontalAlignment.LEFT);
            docTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font docSubtitleFont = workbook.createFont();
            docSubtitleFont.setFontName("Calibri");
            docSubtitleFont.setFontHeightInPoints((short) 9);
            docSubtitleFont.setItalic(true);
            docSubtitleFont.setColor(createColor("4A5568").getIndexed());

            CellStyle docSubtitleStyle = workbook.createCellStyle();
            docSubtitleStyle.setFont(docSubtitleFont);
            docSubtitleStyle.setAlignment(HorizontalAlignment.LEFT);
            docSubtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Metadata row styles
            Font metaFont = workbook.createFont();
            metaFont.setFontName("Calibri");
            metaFont.setFontHeightInPoints((short) 9);
            metaFont.setBold(true);
            metaFont.setColor(createColor("1A365D").getIndexed());

            CellStyle metaLeft = workbook.createCellStyle();
            metaLeft.setFont(metaFont);
            metaLeft.setFillForegroundColor(createColor("F1F5F9"));
            metaLeft.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            metaLeft.setAlignment(HorizontalAlignment.LEFT);
            metaLeft.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle metaCenter = workbook.createCellStyle();
            metaCenter.setFont(metaFont);
            metaCenter.setFillForegroundColor(createColor("F1F5F9"));
            metaCenter.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            metaCenter.setAlignment(HorizontalAlignment.CENTER);
            metaCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle metaRight = workbook.createCellStyle();
            metaRight.setFont(metaFont);
            metaRight.setFillForegroundColor(createColor("F1F5F9"));
            metaRight.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            metaRight.setAlignment(HorizontalAlignment.RIGHT);
            metaRight.setVerticalAlignment(VerticalAlignment.CENTER);

            // Table Header Style (Dark Navy #1A365D)
            Font tblHeaderFont = workbook.createFont();
            tblHeaderFont.setFontName("Calibri");
            tblHeaderFont.setFontHeightInPoints((short) 9);
            tblHeaderFont.setBold(true);
            tblHeaderFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle tblHeaderStyle = workbook.createCellStyle();
            tblHeaderStyle.setFont(tblHeaderFont);
            tblHeaderStyle.setFillForegroundColor(createColor("1A365D"));
            tblHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tblHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            tblHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            tblHeaderStyle.setBorderTop(BorderStyle.THIN);
            tblHeaderStyle.setBorderBottom(BorderStyle.THIN);
            tblHeaderStyle.setBorderLeft(BorderStyle.THIN);
            tblHeaderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle tblHeaderLeftStyle = workbook.createCellStyle();
            tblHeaderLeftStyle.cloneStyleFrom(tblHeaderStyle);
            tblHeaderLeftStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle tblHeaderRightStyle = workbook.createCellStyle();
            tblHeaderRightStyle.cloneStyleFrom(tblHeaderStyle);
            tblHeaderRightStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Data rows styles
            CellStyle numRpStyle = workbook.createCellStyle();
            numRpStyle.setDataFormat(dataFormat.getFormat("\"Rp \"#,##0"));
            numRpStyle.setAlignment(HorizontalAlignment.RIGHT);
            numRpStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            numRpStyle.setBorderTop(BorderStyle.THIN);
            numRpStyle.setBorderBottom(BorderStyle.THIN);
            numRpStyle.setBorderLeft(BorderStyle.THIN);
            numRpStyle.setBorderRight(BorderStyle.THIN);

            CellStyle centerDataStyle = workbook.createCellStyle();
            centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
            centerDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerDataStyle.setBorderTop(BorderStyle.THIN);
            centerDataStyle.setBorderBottom(BorderStyle.THIN);
            centerDataStyle.setBorderLeft(BorderStyle.THIN);
            centerDataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle leftDataStyle = workbook.createCellStyle();
            leftDataStyle.setAlignment(HorizontalAlignment.LEFT);
            leftDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            leftDataStyle.setBorderTop(BorderStyle.THIN);
            leftDataStyle.setBorderBottom(BorderStyle.THIN);
            leftDataStyle.setBorderLeft(BorderStyle.THIN);
            leftDataStyle.setBorderRight(BorderStyle.THIN);

            // Set Row Heights
            sheet.createRow(0).setHeightInPoints(22);
            sheet.createRow(1).setHeightInPoints(16);
            sheet.createRow(2).setHeightInPoints(16);
            sheet.createRow(3).setHeightInPoints(16);
            sheet.createRow(4).setHeightInPoints(8);
            sheet.createRow(5).setHeightInPoints(20);
            sheet.createRow(6).setHeightInPoints(16);
            sheet.createRow(7).setHeightInPoints(22);
            sheet.createRow(8).setHeightInPoints(8);
            sheet.createRow(9).setHeightInPoints(18);
            sheet.createRow(10).setHeightInPoints(24);
            sheet.createRow(11).setHeightInPoints(16);
            sheet.createRow(12).setHeightInPoints(8);
            sheet.createRow(13).setHeightInPoints(24);

            // Add Logo at I1
            addLogo(workbook, sheet, 8, 0, 0.40);

            // Kop Surat
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 7));
            Row r0 = sheet.getRow(0);
            Cell c0 = r0.createCell(1);
            c0.setCellValue(COMPANY_NAME);
            c0.setCellStyle(companyStyle);

            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 7));
            Row r1 = sheet.getRow(1);
            Cell c1 = r1.createCell(1);
            c1.setCellValue(COMPANY_TAGLINE);
            c1.setCellStyle(taglineStyle);

            sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 7));
            Row r2 = sheet.getRow(2);
            Cell c2 = r2.createCell(1);
            c2.setCellValue(COMPANY_ADDRESS);
            c2.setCellStyle(addressStyle);

            sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 7));
            Row r3 = sheet.getRow(3);
            Cell c3 = r3.createCell(1);
            c3.setCellValue(COMPANY_EMAIL);
            c3.setCellStyle(addressStyle);

            // Row 5: Border double bottom
            Row r4 = sheet.getRow(4);
            for (int col = 0; col < 9; col++) {
                Cell cell = r4.createCell(col);
                cell.setCellStyle(borderDoubleStyle);
            }

            // Title & Subtitle (Row 6 & 7)
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, 8));
            Row r5 = sheet.getRow(5);
            Cell c5 = r5.createCell(0);
            c5.setCellValue("DAFTAR REKAPITULASI INVOICE & PIUTANG");
            c5.setCellStyle(docTitleStyle);

            sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, 8));
            Row r6 = sheet.getRow(6);
            Cell c6 = r6.createCell(0);
            c6.setCellValue("Laporan Pengiriman, Syarat Pembayaran (TOP), dan Jatuh Tempo Tagihan");
            c6.setCellStyle(docSubtitleStyle);

            // Metadata Row 8
            String periodText = "Periode: Semua Periode";
            if (filter != null && filter.getDateFrom() != null && filter.getDateTo() != null) {
                periodText = "Periode: " + DateUtil.formatDateIndo(filter.getDateFrom()) + " s/d " + DateUtil.formatDateIndo(filter.getDateTo());
            }
            String statusText = "Status: " + (filter != null && filter.getStatus() != null ? filter.getStatus().name() : "Semua Status");
            String printDateText = "Tanggal Cetak: " + DateUtil.formatDateIndo(LocalDate.now());

            Row r7 = sheet.getRow(7);
            for (int col = 0; col < 9; col++) {
                Cell c = r7.createCell(col);
                if (col <= 2) c.setCellStyle(metaLeft);
                else if (col <= 5) c.setCellStyle(metaCenter);
                else c.setCellStyle(metaRight);
            }
            sheet.addMergedRegion(new CellRangeAddress(7, 7, 0, 2));
            r7.getCell(0).setCellValue(periodText);

            sheet.addMergedRegion(new CellRangeAddress(7, 7, 3, 5));
            r7.getCell(3).setCellValue(statusText);

            sheet.addMergedRegion(new CellRangeAddress(7, 7, 6, 8));
            r7.getCell(6).setCellValue(printDateText);

            // KPI Cards Helper
            createKpiCard(workbook, sheet, 0, 1, "TOTAL TAGIHAN", totalAmount, totalCount + " Invoice tercatat", "F8FAFC", "1A365D");
            createKpiCard(workbook, sheet, 2, 3, "TERBAYAR (LUNAS)", paidAmount, paidCount + " Invoice terselesaikan", "F0FDF4", "166534");
            createKpiCard(workbook, sheet, 4, 5, "MENUNGGU PEMBAYARAN", unpaidAmount, unpaidCount + " Invoice kredit berjalan", "FFFBEB", "C2410C");
            createKpiCard(workbook, sheet, 6, 8, "JATUH TEMPO (OVERDUE)", overdueAmount, overdueCount + " Invoice melewati tempo", "FEF2F2", "B91C1C");

            // Row 14: Table Headers (9 Columns)
            String[] headers = {
                    "No", "No. Invoice", "Nama Klien / Perusahaan", "Tgl Pengiriman",
                    "TOP (Terms)", "Tgl Jatuh Tempo", "Nominal Tagihan", "Status", "Tgl. Pelunasan"
            };
            Row r13 = sheet.getRow(13);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = r13.createCell(i);
                cell.setCellValue(headers[i]);
                if (i == 1 || i == 2) {
                    cell.setCellStyle(tblHeaderLeftStyle);
                } else if (i == 6) {
                    cell.setCellStyle(tblHeaderRightStyle);
                } else {
                    cell.setCellStyle(tblHeaderStyle);
                }
            }

            // Data Rows (Row 15 onwards, index 14)
            int rowIndex = 14;
            for (int idx = 0; idx < invoices.size(); idx++) {
                Invoice inv = invoices.get(idx);
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);

                // No
                Cell cNo = row.createCell(0);
                cNo.setCellValue(idx + 1);
                cNo.setCellStyle(centerDataStyle);

                // No. Invoice
                Cell cInv = row.createCell(1);
                cInv.setCellValue(inv.getInvoiceNo());
                cInv.setCellStyle(leftDataStyle);

                // Nama Klien
                Cell cCli = row.createCell(2);
                String clientDisplay = inv.getClientName() != null ? inv.getClientName() : "";
                if (inv.getNotes() != null && !inv.getNotes().isBlank()) {
                    clientDisplay += " (" + inv.getNotes() + ")";
                }
                cCli.setCellValue(clientDisplay);
                cCli.setCellStyle(leftDataStyle);

                // Tgl Pengiriman
                Cell cShip = row.createCell(3);
                cShip.setCellValue(DateUtil.formatDateIndo(inv.getShipmentDate()));
                cShip.setCellStyle(centerDataStyle);

                // TOP
                Cell cTop = row.createCell(4);
                String topDisplay = (inv.getTopDays() != null && inv.getTopDays() > 0) ? inv.getTopDays() + " Hari" : "Cash / Tunai";
                cTop.setCellValue(topDisplay);
                cTop.setCellStyle(centerDataStyle);

                // Tgl Jatuh Tempo
                Cell cDue = row.createCell(5);
                cDue.setCellValue(DateUtil.formatDateIndo(inv.getDueDate()));
                cDue.setCellStyle(centerDataStyle);

                // Nominal Tagihan
                Cell cAmt = row.createCell(6);
                cAmt.setCellValue(inv.getAmount() != null ? inv.getAmount().doubleValue() : 0.0);
                cAmt.setCellStyle(numRpStyle);

                // Status
                Cell cStatus = row.createCell(7);
                String statusLabel = switch (inv.getStatus()) {
                    case PAID -> "Lunas";
                    case OVERDUE -> "Jatuh Tempo (Overdue)";
                    default -> "Menunggu Pembayaran";
                };
                cStatus.setCellValue(statusLabel);
                cStatus.setCellStyle(centerDataStyle);

                // Tgl Pelunasan
                Cell cPaid = row.createCell(8);
                String paidAtStr = inv.getPaidAt() != null ? DateUtil.formatDateIndo(inv.getPaidAt()) : "-";
                cPaid.setCellValue(paidAtStr);
                cPaid.setCellStyle(centerDataStyle);
            }

            // Total Row
            Row sumRow = sheet.createRow(rowIndex);
            sumRow.setHeightInPoints(22);

            Font totalFont = workbook.createFont();
            totalFont.setFontName("Calibri");
            totalFont.setFontHeightInPoints((short) 9);
            totalFont.setBold(true);
            totalFont.setColor(createColor("1A365D").getIndexed());

            CellStyle totalLblStyle = workbook.createCellStyle();
            totalLblStyle.setFont(totalFont);
            totalLblStyle.setFillForegroundColor(createColor("EDF2F7"));
            totalLblStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalLblStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalLblStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalLblStyle.setBorderTop(BorderStyle.THIN);
            totalLblStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle totalAmtStyle = workbook.createCellStyle();
            totalAmtStyle.cloneStyleFrom(totalLblStyle);
            totalAmtStyle.setDataFormat(dataFormat.getFormat("\"Rp \"#,##0"));

            CellStyle totalCntStyle = workbook.createCellStyle();
            totalCntStyle.cloneStyleFrom(totalLblStyle);
            totalCntStyle.setAlignment(HorizontalAlignment.CENTER);

            for (int col = 0; col < 9; col++) {
                Cell c = sumRow.createCell(col);
                if (col <= 5) c.setCellStyle(totalLblStyle);
                else if (col == 6) c.setCellStyle(totalAmtStyle);
                else c.setCellStyle(totalCntStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 5));
            sumRow.getCell(0).setCellValue("TOTAL KESELURUHAN INVOICE:");

            sumRow.getCell(6).setCellValue(totalAmount.doubleValue());
            sumRow.getCell(7).setCellValue(invoices.size() + " Tagihan");
            sumRow.getCell(8).setCellValue("");

            // Signatures Section (3 rows below total)
            int sigRow = rowIndex + 3;
            sheet.createRow(sigRow).setHeightInPoints(18);
            sheet.createRow(sigRow + 1).setHeightInPoints(20);
            sheet.createRow(sigRow + 2).setHeightInPoints(20);
            sheet.createRow(sigRow + 3).setHeightInPoints(20);
            sheet.createRow(sigRow + 4).setHeightInPoints(20);
            sheet.createRow(sigRow + 5).setHeightInPoints(18);

            CellStyle sigHdr = workbook.createCellStyle();
            sigHdr.setAlignment(HorizontalAlignment.CENTER);
            sigHdr.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle sigLine = workbook.createCellStyle();
            Font lineFont = workbook.createFont();
            lineFont.setBold(true);
            sigLine.setFont(lineFont);
            sigLine.setAlignment(HorizontalAlignment.CENTER);
            sigLine.setVerticalAlignment(VerticalAlignment.BOTTOM);

            CellStyle sigRole = workbook.createCellStyle();
            Font roleFont = workbook.createFont();
            roleFont.setItalic(true);
            roleFont.setColor(createColor("4A5568").getIndexed());
            sigRole.setFont(roleFont);
            sigRole.setAlignment(HorizontalAlignment.CENTER);
            sigRole.setVerticalAlignment(VerticalAlignment.TOP);

            // Headers
            sheet.addMergedRegion(new CellRangeAddress(sigRow, sigRow, 0, 2));
            Row rSigH = sheet.getRow(sigRow);
            Cell cSh1 = rSigH.createCell(0);
            cSh1.setCellValue("Dibuat Oleh,");
            cSh1.setCellStyle(sigHdr);

            sheet.addMergedRegion(new CellRangeAddress(sigRow, sigRow, 3, 5));
            Cell cSh2 = rSigH.createCell(3);
            cSh2.setCellValue("Diperiksa Oleh,");
            cSh2.setCellStyle(sigHdr);

            sheet.addMergedRegion(new CellRangeAddress(sigRow, sigRow, 6, 8));
            Cell cSh3 = rSigH.createCell(6);
            cSh3.setCellValue("Disetujui Oleh,");
            cSh3.setCellStyle(sigHdr);

            // Underlines
            sheet.addMergedRegion(new CellRangeAddress(sigRow + 4, sigRow + 4, 0, 2));
            Row rSigL = sheet.getRow(sigRow + 4);
            Cell cSl1 = rSigL.createCell(0);
            cSl1.setCellValue("( _______________________ )");
            cSl1.setCellStyle(sigLine);

            sheet.addMergedRegion(new CellRangeAddress(sigRow + 4, sigRow + 4, 3, 5));
            Cell cSl2 = rSigL.createCell(3);
            cSl2.setCellValue("( _______________________ )");
            cSl2.setCellStyle(sigLine);

            sheet.addMergedRegion(new CellRangeAddress(sigRow + 4, sigRow + 4, 6, 8));
            Cell cSl3 = rSigL.createCell(6);
            cSl3.setCellValue("( _______________________ )");
            cSl3.setCellStyle(sigLine);

            // Roles
            sheet.addMergedRegion(new CellRangeAddress(sigRow + 5, sigRow + 5, 0, 2));
            Row rSigR = sheet.getRow(sigRow + 5);
            Cell cSr1 = rSigR.createCell(0);
            cSr1.setCellValue("Staff Finance & Billing");
            cSr1.setCellStyle(sigRole);

            sheet.addMergedRegion(new CellRangeAddress(sigRow + 5, sigRow + 5, 3, 5));
            Cell cSr2 = rSigR.createCell(3);
            cSr2.setCellValue("Supervisor Keuangan");
            cSr2.setCellStyle(sigRole);

            sheet.addMergedRegion(new CellRangeAddress(sigRow + 5, sigRow + 5, 6, 8));
            Cell cSr3 = rSigR.createCell(6);
            cSr3.setCellValue("Finance Manager");
            cSr3.setCellStyle(sigRole);

            // Column Widths
            sheet.setColumnWidth(0, 6 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 32 * 256);
            sheet.setColumnWidth(3, 15 * 256);
            sheet.setColumnWidth(4, 16 * 256);
            sheet.setColumnWidth(5, 16 * 256);
            sheet.setColumnWidth(6, 22 * 256);
            sheet.setColumnWidth(7, 20 * 256);
            sheet.setColumnWidth(8, 16 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to export invoices excel: {}", e.getMessage(), e);
            throw new RuntimeException("Gagal mengekspor laporan invoice ke Excel: " + e.getMessage(), e);
        }
    }

    private void createKpiCard(Workbook workbook, Sheet sheet, int startCol, int endCol,
                               String title, BigDecimal amount, String subtitle, String bgHex, String fontHex) {
        DataFormat dataFormat = workbook.createDataFormat();
        XSSFColor bgColor = createColor(bgHex);
        XSSFColor fontColor = createColor(fontHex);

        Font titleFont = workbook.createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeightInPoints((short) 8);
        titleFont.setBold(true);
        titleFont.setColor(fontColor.getIndexed());

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(bgColor);
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setBorderTop(BorderStyle.THIN);
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setBorderRight(BorderStyle.THIN);

        Font valFont = workbook.createFont();
        valFont.setFontName("Calibri");
        valFont.setFontHeightInPoints((short) 12);
        valFont.setBold(true);
        valFont.setColor(fontColor.getIndexed());

        CellStyle valStyle = workbook.createCellStyle();
        valStyle.setFont(valFont);
        valStyle.setDataFormat(dataFormat.getFormat("\"Rp \"#,##0"));
        valStyle.setFillForegroundColor(bgColor);
        valStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        valStyle.setAlignment(HorizontalAlignment.LEFT);
        valStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        valStyle.setBorderLeft(BorderStyle.THIN);
        valStyle.setBorderRight(BorderStyle.THIN);

        Font subFont = workbook.createFont();
        subFont.setFontName("Calibri");
        subFont.setFontHeightInPoints((short) 8);
        subFont.setColor(createColor("64748B").getIndexed());

        CellStyle subStyle = workbook.createCellStyle();
        subStyle.setFont(subFont);
        subStyle.setFillForegroundColor(bgColor);
        subStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        subStyle.setAlignment(HorizontalAlignment.LEFT);
        subStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        subStyle.setBorderBottom(BorderStyle.THIN);
        subStyle.setBorderLeft(BorderStyle.THIN);
        subStyle.setBorderRight(BorderStyle.THIN);

        // Row 10 (index 9)
        Row r9 = sheet.getRow(9);
        for (int c = startCol; c <= endCol; c++) {
            r9.createCell(c).setCellStyle(titleStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(9, 9, startCol, endCol));
        r9.getCell(startCol).setCellValue(title);

        // Row 11 (index 10)
        Row r10 = sheet.getRow(10);
        for (int c = startCol; c <= endCol; c++) {
            r10.createCell(c).setCellStyle(valStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(10, 10, startCol, endCol));
        r10.getCell(startCol).setCellValue(amount.doubleValue());

        // Row 12 (index 11)
        Row r11 = sheet.getRow(11);
        for (int c = startCol; c <= endCol; c++) {
            r11.createCell(c).setCellStyle(subStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(11, 11, startCol, endCol));
        r11.getCell(startCol).setCellValue(subtitle);
    }
}

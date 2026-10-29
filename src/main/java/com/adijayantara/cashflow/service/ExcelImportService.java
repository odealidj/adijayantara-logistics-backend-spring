package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.dto.CreateShipmentRequest;
import com.adijayantara.cashflow.dto.CreateTopUpRequest;
import com.adijayantara.cashflow.dto.UserSessionDto;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final CashflowService cashflowService;

    @Transactional
    public void processExcelImport(InputStream inputStream, UserSessionDto user) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("File Excel tidak memiliki sheet yang valid");
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            BigDecimal previousSaldo = BigDecimal.ZERO;
            boolean isFirstRow = true;

            // Row 7 in Excel is index 6
            for (int r = 6; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                BigDecimal kredit = parseBigDecimal(row.getCell(0));
                BigDecimal debit = parseBigDecimal(row.getCell(1));
                String actInfo = formatter.formatCellValue(row.getCell(4)).trim();
                String actExp = formatter.formatCellValue(row.getCell(5)).trim();
                String vendorName = formatter.formatCellValue(row.getCell(6)).trim();

                if (kredit.compareTo(BigDecimal.ZERO) == 0 && debit.compareTo(BigDecimal.ZERO) == 0
                        && actInfo.isEmpty() && vendorName.isEmpty()) {
                    continue;
                }

                LocalDate dateOfEntry = parseDate(row.getCell(3));
                if (dateOfEntry == null) {
                    dateOfEntry = LocalDate.now();
                }

                Integer topDays = parseTopDays(formatter.formatCellValue(row.getCell(7)));
                LocalDate dueDate = parseDate(row.getCell(8));
                BigDecimal grandCost = parseBigDecimal(row.getCell(9));
                BigDecimal grandSelling = parseBigDecimal(row.getCell(10));

                String remarksStr = formatter.formatCellValue(row.getCell(13)).trim().toUpperCase();
                PaymentStatus remarks = switch (remarksStr) {
                    case "PAID" -> PaymentStatus.PAID;
                    case "UNPAID" -> PaymentStatus.UNPAID;
                    default -> PaymentStatus.PENDING;
                };

                // Carry-over vs modal top-up detection
                if (isFirstRow) {
                    if (kredit.compareTo(BigDecimal.ZERO) > 0) {
                        CreateTopUpRequest topUp = CreateTopUpRequest.builder()
                                .dateOfEntry(dateOfEntry)
                                .kredit(kredit)
                                .actInformation("Modal Awal (Opening Balance)")
                                .actExplaination(actExp)
                                .build();
                        cashflowService.recordTopUp(topUp, user);
                        previousSaldo = kredit;
                    }
                    isFirstRow = false;
                } else {
                    BigDecimal selisih = kredit.subtract(previousSaldo);
                    if (selisih.compareTo(BigDecimal.valueOf(0.01)) > 0) {
                        CreateTopUpRequest topUp = CreateTopUpRequest.builder()
                                .dateOfEntry(dateOfEntry)
                                .kredit(selisih)
                                .actInformation("Penambahan Modal (Injeksi Dana)")
                                .actExplaination(actExp)
                                .build();
                        cashflowService.recordTopUp(topUp, user);
                        previousSaldo = previousSaldo.add(selisih);
                    }
                }

                if (debit.compareTo(BigDecimal.ZERO) > 0 || grandCost.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal cost = grandCost.compareTo(BigDecimal.ZERO) > 0 ? grandCost : debit;

                    CreateShipmentRequest shipment = CreateShipmentRequest.builder()
                            .dateOfEntry(dateOfEntry)
                            .actInformation(actInfo.isEmpty() ? "Pengiriman Kargo" : actInfo)
                            .actExplaination(actExp)
                            .vendorNameRaw(vendorName)
                            .topDays(topDays != null ? topDays : 0)
                            .dueDate(dueDate)
                            .grandCost(cost)
                            .grandSelling(grandSelling)
                            .debit(cost)
                            .remarks(remarks)
                            .build();

                    cashflowService.recordShipment(shipment, user);
                    previousSaldo = previousSaldo.subtract(cost);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process Excel import: {}", e.getMessage(), e);
            throw new RuntimeException("Gagal memproses file Excel: " + e.getMessage(), e);
        }
    }

    private BigDecimal parseBigDecimal(Cell cell) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().replaceAll("[^0-9.-]", "").trim();
                if (val.isEmpty()) return BigDecimal.ZERO;
                return new BigDecimal(val);
            }
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
    }

    private LocalDate parseDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue().trim();
                if (text.isEmpty() || text.equals("-")) return null;

                // Try ISO YYYY-MM-DD
                try {
                    return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception ignored) {}

                // Try dd/MM/yyyy
                try {
                    return LocalDate.parse(text, DateTimeFormatter.ofPattern("d/M/yyyy"));
                } catch (Exception ignored) {}

                // Try Indonesian date format (e.g., 7 Sep 2026, 15 Januari 2026)
                String[] parts = text.split("\\s+");
                if (parts.length == 3) {
                    int day = Integer.parseInt(parts[0]);
                    int month = parseMonthIndo(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    if (month > 0) {
                        return LocalDate.of(year, month, day);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private int parseMonthIndo(String monthStr) {
        String m = monthStr.toLowerCase();
        if (m.startsWith("jan")) return 1;
        if (m.startsWith("feb")) return 2;
        if (m.startsWith("mar")) return 3;
        if (m.startsWith("apr")) return 4;
        if (m.startsWith("mei") || m.startsWith("may")) return 5;
        if (m.startsWith("jun")) return 6;
        if (m.startsWith("jul")) return 7;
        if (m.startsWith("agu") || m.startsWith("aug")) return 8;
        if (m.startsWith("sep")) return 9;
        if (m.startsWith("okt") || m.startsWith("oct")) return 10;
        if (m.startsWith("nov")) return 11;
        if (m.startsWith("des") || m.startsWith("dec")) return 12;
        return 0;
    }

    private Integer parseTopDays(String str) {
        if (str == null || str.isBlank() || str.equals("-")) {
            return 0;
        }
        try {
            String digits = str.replaceAll("[^0-9]", "").trim();
            if (!digits.isEmpty()) {
                return Integer.parseInt(digits);
            }
        } catch (Exception ignored) {}
        return 0;
    }
}

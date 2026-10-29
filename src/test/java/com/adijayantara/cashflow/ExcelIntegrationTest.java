package com.adijayantara.cashflow;

import com.adijayantara.cashflow.dto.ApiResponse;
import com.adijayantara.cashflow.dto.LoginRequest;
import com.adijayantara.cashflow.dto.LoginResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ExcelIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .identifier("finance.ayu@adijayantara.co.id")
                .password("password123")
                .build();
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<LoginResponse> response = objectMapper.readValue(
                res.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        this.adminToken = response.getData().getToken();
    }

    @Test
    @DisplayName("Cashflow Excel Export produces valid XLSX with official PT Adijayantara kop surat")
    void testExportCashflowExcel() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/cashflow/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = wb.getSheet("Sheet1");
            assertThat(sheet).isNotNull();

            // Row 0 should contain PT Adijayantara
            Row r0 = sheet.getRow(0);
            assertThat(r0.getCell(2).getStringCellValue()).contains("PT ADIJAYANTARA LOGISTIC INDONESIA");

            // Row 5 should contain table headers
            Row r5 = sheet.getRow(5);
            assertThat(r5.getCell(0).getStringCellValue()).isEqualTo("KREDIT");
            assertThat(r5.getCell(1).getStringCellValue()).isEqualTo("DEBIT");
            assertThat(r5.getCell(2).getStringCellValue()).isEqualTo("SALDO");
            assertThat(r5.getCell(3).getStringCellValue()).isEqualTo("DATE OF ENTRY");
        }
    }

    @Test
    @DisplayName("Invoice Excel Export produces valid XLSX with KPI summary cards")
    void testExportInvoicesExcel() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/invoices/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = wb.getSheet("Rekapitulasi Invoice");
            assertThat(sheet).isNotNull();

            // Kop Surat
            Row r0 = sheet.getRow(0);
            assertThat(r0.getCell(1).getStringCellValue()).contains("PT ADIJAYANTARA LOGISTIC INDONESIA");

            // Table Header in Row 13
            Row r13 = sheet.getRow(13);
            assertThat(r13.getCell(0).getStringCellValue()).isEqualTo("No");
            assertThat(r13.getCell(1).getStringCellValue()).isEqualTo("No. Invoice");
            assertThat(r13.getCell(2).getStringCellValue()).isEqualTo("Nama Klien / Perusahaan");
        }
    }

    @Test
    @DisplayName("Cashflow Excel Import processes rows and updates rolling balance")
    void testImportCashflowExcel() throws Exception {
        byte[] excelBytes;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sheet1");

            // Create blank rows up to index 5
            for (int r = 0; r < 6; r++) {
                sheet.createRow(r);
            }

            // Row 6 (Index 6) is the first data row: Opening Balance
            Row r6 = sheet.createRow(6);
            r6.createCell(0).setCellValue(15000000); // Kredit: 15 juta
            r6.createCell(1).setCellValue(0);
            r6.createCell(3).setCellValue("2026-10-15");
            r6.createCell(4).setCellValue("Modal Awal Import Test");
            r6.createCell(13).setCellValue("PAID");

            // Row 7 (Index 7) is Shipment
            Row r7 = sheet.createRow(7);
            r7.createCell(0).setCellValue(0);
            r7.createCell(1).setCellValue(4500000); // Debit: 4.5 juta
            r7.createCell(3).setCellValue("2026-10-16");
            r7.createCell(4).setCellValue("Shipment Cargo Surabaya Test");
            r7.createCell(5).setCellValue("Pengiriman Truk CDD");
            r7.createCell(6).setCellValue("Vendor Truk Cepat");
            r7.createCell(7).setCellValue("14 HARI");
            r7.createCell(8).setCellValue("2026-10-30");
            r7.createCell(9).setCellValue(4500000);
            r7.createCell(10).setCellValue(7000000);
            r7.createCell(13).setCellValue("PENDING");

            wb.write(out);
            excelBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cashflow_import_test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        mockMvc.perform(multipart("/api/v1/cashflow/import")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}

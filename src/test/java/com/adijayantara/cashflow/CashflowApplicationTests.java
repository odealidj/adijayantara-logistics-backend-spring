package com.adijayantara.cashflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CashflowApplicationTests {

    @Test
    void contextLoads() {
        // Memvalidasi bahwa Spring Context berhasil di-load
        // dan Hibernate sukses memvalidasi skema PostgreSQL (ddl-auto: validate)
    }
}


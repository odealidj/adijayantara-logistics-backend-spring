package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();
        
        // Coba parsing format standard ISO-8601 YYYY-MM-DD
        if (text.length() == 10 && text.charAt(4) == '-' && text.charAt(7) == '-') {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        }

        // Coba parsing format timestamp ISO (misal 2026-10-30T00:00:00.000Z)
        try {
            return OffsetDateTime.parse(text).toLocalDate();
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(text.substring(0, 10));
        } catch (Exception e) {
            throw new IOException("Gagal membaca format tanggal: " + text, e);
        }
    }
}

package com.adijayantara.cashflow.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DateUtil {

    private static final String[] MONTHS_INDO = {
            "", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    };

    private DateUtil() {}

    public static String formatDateIndo(LocalDate date) {
        if (date == null) {
            return "-";
        }
        int m = date.getMonthValue();
        if (m < 1 || m > 12) {
            return date.toString();
        }
        return date.getDayOfMonth() + " " + MONTHS_INDO[m] + " " + date.getYear();
    }

    public static String formatDateIndo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return formatDateIndo(dateTime.toLocalDate());
    }
}


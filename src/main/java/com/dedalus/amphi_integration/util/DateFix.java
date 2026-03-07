package com.dedalus.amphi_integration.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateFix {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
    private static final DateTimeFormatter OFFSET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter SHORT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter LONG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private DateFix() {
    }

    public static String dateFix(LocalDateTime localDateTime) {
        return format(localDateTime, OFFSET_FORMATTER);
    }

    public static String dateFixShort(LocalDateTime localDateTime) {
        return format(localDateTime, SHORT_FORMATTER);
    }

    /**
     * Returns a string representation of the given localDateTime, formatted as "yyyy-MM-dd'T'HH:mm:ss'Z'",
     * and converted to the Europe/Stockholm time zone.
     *
     * @param localDateTime the LocalDateTime to convert
     * @return a string representation of the given localDateTime, formatted as "yyyy-MM-dd'T'HH:mm:ss'Z'",
     *         and converted to the Europe/Stockholm time zone, or null if localDateTime is null
     */
    public static String dateFixLong(LocalDateTime localDateTime) {
        return format(localDateTime, LONG_FORMATTER);
    }

    public static LocalDateTime toStockholmLocalTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return asStockholm(localDateTime).toLocalDateTime();
    }

    private static String format(LocalDateTime localDateTime, DateTimeFormatter formatter) {
        if (localDateTime == null) {
            return null;
        }

        return asStockholm(localDateTime).format(formatter);
    }

    private static ZonedDateTime asStockholm(LocalDateTime localDateTime) {
        return localDateTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(STOCKHOLM);
    }
}

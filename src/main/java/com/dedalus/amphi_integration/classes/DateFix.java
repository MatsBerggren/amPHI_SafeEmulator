package com.dedalus.amphi_integration.classes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateFix {

    public static String dateFix(LocalDateTime localDateTime) {
        String returnDate;
        if (localDateTime != null) {

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("Europe/Stockholm"));

            returnDate = zonedDateTime.format(dateTimeFormatter);
        } else {
            returnDate = null;
        }

        return returnDate;
    }

    public static String dateFixShort(LocalDateTime localDateTime) {
        String returnDate;
        if (localDateTime != null) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("Europe/Stockholm"));

            returnDate = zonedDateTime.format(dateTimeFormatter);
        } else {
            returnDate = null;
        }

        return returnDate;
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
        String returnDate;
        if (localDateTime != null) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("Europe/Stockholm"));

            returnDate = zonedDateTime.format(dateTimeFormatter);
        } else {
            returnDate = null;
        }

        return returnDate;
    }
}

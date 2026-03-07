package com.dedalus.amphi_integration.util;

import com.dedalus.amphi_integration.util.DateFix;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DateFix utility class.
 * Tests date formatting methods with various inputs including edge cases.
 */
class DateFixTest {

    @Test
    void dateFix_WithNullInput_ReturnsNull() {
        // Arrange
        LocalDateTime nullDateTime = null;

        // Act
        String result = DateFix.dateFix(nullDateTime);

        // Assert
        assertNull(result, "dateFix should return null when input is null");
    }

    @Test
    void dateFix_WithValidDateTime_ReturnsFormattedStringWithTimezone() {
        // Arrange
                LocalDateTime testDateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45);

        // Act
        String result = DateFix.dateFix(testDateTime);

        // Assert
        assertNotNull(result, "dateFix should not return null for valid input");
        // Format is yyyy-MM-dd'T'HH:mm:ssXXX (with timezone offset)
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "Result should match ISO 8601 format with timezone offset: " + result);
        assertTrue(result.startsWith("2024-01-15T15:30:45"),
                "Result should contain correct date and time: " + result);
    }

    @Test
    void dateFix_WithMidnight_ReturnsFormattedString() {
        // Arrange
        LocalDateTime midnight = LocalDateTime.of(2024, 6, 20, 0, 0, 0);

        // Act
        String result = DateFix.dateFix(midnight);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("2024-06-20T02:00:00"),
                "Midnight should be formatted correctly: " + result);
    }

    @Test
    void dateFixShort_WithNullInput_ReturnsNull() {
        // Arrange
        LocalDateTime nullDateTime = null;

        // Act
        String result = DateFix.dateFixShort(nullDateTime);

        // Assert
        assertNull(result, "dateFixShort should return null when input is null");
    }

    @Test
        void dateFixShort_WithValidDateTime_ReturnsFormattedStringWithOffset() {
        // Arrange
        LocalDateTime testDateTime = LocalDateTime.of(2024, 3, 10, 9, 15, 30);

        // Act
        String result = DateFix.dateFixShort(testDateTime);

        // Assert
        assertNotNull(result, "dateFixShort should not return null for valid input");
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "Result should match format yyyy-MM-dd'T'HH:mm:ssXXX: " + result);
        assertEquals("2024-03-10T10:15:30+01:00", result,
                "Result should be formatted correctly in Stockholm time");
    }

    @Test
    void dateFixShort_WithEndOfDay_ReturnsFormattedString() {
        // Arrange
        LocalDateTime endOfDay = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        // Act
        String result = DateFix.dateFixShort(endOfDay);

        // Assert
        assertNotNull(result);
        assertEquals("2025-01-01T00:59:59+01:00", result,
                "End of day should be formatted correctly");
    }

    @Test
    void dateFixLong_WithNullInput_ReturnsNull() {
        // Arrange
        LocalDateTime nullDateTime = null;

        // Act
        String result = DateFix.dateFixLong(nullDateTime);

        // Assert
        assertNull(result, "dateFixLong should return null when input is null");
    }

    @Test
    void dateFixLong_WithValidDateTime_ReturnsFormattedStringWithMilliseconds() {
        // Arrange
        LocalDateTime testDateTime = LocalDateTime.of(2024, 7, 4, 16, 45, 20, 123000000);

        // Act
        String result = DateFix.dateFixLong(testDateTime);

        // Assert
        assertNotNull(result, "dateFixLong should not return null for valid input");
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2}"),
                "Result should match format yyyy-MM-dd'T'HH:mm:ss.SSSXXX: " + result);
        assertEquals("2024-07-04T18:45:20.123+02:00", result,
                "Result should include milliseconds");
    }

    @Test
    void dateFixLong_WithZeroMilliseconds_ReturnsFormattedStringWith000() {
        // Arrange
        LocalDateTime testDateTime = LocalDateTime.of(2024, 2, 28, 12, 0, 0, 0);

        // Act
        String result = DateFix.dateFixLong(testDateTime);

        // Assert
        assertNotNull(result);
        assertEquals("2024-02-28T13:00:00.000+01:00", result,
                "Zero milliseconds should be formatted as .000");
    }

    @Test
    void dateFixLong_WithMaxMilliseconds_ReturnsFormattedString() {
        // Arrange
        // 999 milliseconds = 999000000 nanoseconds
        LocalDateTime testDateTime = LocalDateTime.of(2024, 11, 11, 11, 11, 11, 999000000);

        // Act
        String result = DateFix.dateFixLong(testDateTime);

        // Assert
        assertNotNull(result);
        assertEquals("2024-11-11T12:11:11.999+01:00", result,
                "Max milliseconds should be formatted correctly");
    }

    @Test
    void allMethods_WithSameInput_ProduceDifferentFormats() {
        // Arrange
        LocalDateTime testDateTime = LocalDateTime.of(2024, 5, 20, 10, 30, 45, 500000000);

        // Act
        String resultFix = DateFix.dateFix(testDateTime);
        String resultShort = DateFix.dateFixShort(testDateTime);
        String resultLong = DateFix.dateFixLong(testDateTime);

        // Assert
        assertNotNull(resultFix);
        assertNotNull(resultShort);
        assertNotNull(resultLong);
        
        // dateFix has timezone offset (+XX:XX)
        assertTrue(resultFix.contains("+") || resultFix.contains("-"),
                "dateFix should contain timezone offset");
        
        // dateFixShort has no milliseconds
        assertFalse(resultShort.contains("."),
                "dateFixShort should not contain milliseconds");
        
        // dateFixLong has milliseconds
        assertTrue(resultLong.contains("."),
                "dateFixLong should contain milliseconds");
    }

        @Test
        void toStockholmLocalTime_ConvertsUtcToLocalTime() {
                LocalDateTime utcDateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45);

                LocalDateTime result = DateFix.toStockholmLocalTime(utcDateTime);

                assertEquals(LocalDateTime.of(2024, 1, 15, 15, 30, 45), result);
        }

        @Test
        void localDateTimeJsonSemantics_UseUtcOnReadAndWrite() {
                LocalDateTimeSerializer serializer = new LocalDateTimeSerializer();
                LocalDateTimeDeserializer deserializer = new LocalDateTimeDeserializer();
                LocalDateTime utcDateTime = LocalDateTime.of(2024, 7, 4, 16, 45, 20, 123000000);

                String serialized = serializer.serialize(utcDateTime, LocalDateTime.class, null).getAsString();
                LocalDateTime parsed = deserializer.deserialize(new com.google.gson.JsonPrimitive("2024-07-04T18:45:20.123+02:00"), LocalDateTime.class, null);

                assertEquals("2024-07-04T16:45:20.123Z", serialized);
                assertEquals(utcDateTime, parsed);
                assertEquals(ZoneOffset.UTC, utcDateTime.atOffset(ZoneOffset.UTC).getOffset());
        }
}

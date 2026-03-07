package com.dedalus.amphi_integration.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class LocalDateTimeSerializer extends com.fasterxml.jackson.databind.JsonSerializer<LocalDateTime>
        implements JsonSerializer<LocalDateTime> {

    @Override
    public JsonElement serialize(LocalDateTime localDateTime, Type srcType, JsonSerializationContext context) {
        if (localDateTime == null) {
            return JsonNull.INSTANCE;
        }
        return new JsonPrimitive(format(localDateTime));
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeString(format(value));
    }

    private String format(LocalDateTime localDateTime) {
        return localDateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }
}

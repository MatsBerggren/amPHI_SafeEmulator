package com.dedalus.amphi_integration.util;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class FlexibleStringDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<String>
        implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return parse(json == null ? null : json.getAsString());
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return parse(parser.getValueAsString());
    }

    private String parse(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed;
    }
}
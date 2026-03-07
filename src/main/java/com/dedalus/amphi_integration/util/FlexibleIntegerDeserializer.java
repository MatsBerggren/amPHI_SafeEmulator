package com.dedalus.amphi_integration.util;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class FlexibleIntegerDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Integer>
        implements JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return parse(json == null ? null : json.getAsString());
    }

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() != null && parser.getCurrentToken().isNumeric()) {
            return parser.getIntValue();
        }
        return parse(parser.getValueAsString());
    }

    private Integer parse(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return Integer.valueOf(trimmed);
    }
}
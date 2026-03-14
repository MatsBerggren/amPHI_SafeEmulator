package com.dedalus.amphi_integration.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.Set;

public final class StructuredLogValueParser {

    private static final Set<String> NUMERIC_FIELDS = Set.of(
            "latitude",
            "longitude",
            "etaSeconds",
            "distanceToDestinationMeters",
            "selectedPriority");

    private StructuredLogValueParser() {
    }

    public static JsonElement parse(String input) {
        Parser parser = new Parser(input);
        JsonElement element = parser.parseValue(null);
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("Unexpected trailing content in structured value: " + input);
        }
        return element;
    }

    private static final class Parser {

        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input == null ? "" : input.trim();
        }

        private JsonElement parseValue(String fieldName) {
            skipWhitespace();
            if (isAtEnd()) {
                return JsonNull.INSTANCE;
            }

            char current = input.charAt(index);
            if (current == '[') {
                return parseArray();
            }
            if (looksLikeObjectStart()) {
                return parseObject();
            }
            return parseScalar(fieldName);
        }

        private JsonObject parseObject() {
            readIdentifier();
            expect('(');

            JsonObject object = new JsonObject();
            skipWhitespace();
            while (!isAtEnd() && input.charAt(index) != ')') {
                int fieldStart = index;
                String fieldName = readUntil('=');
                expect('=');
                JsonElement value = parseValue(fieldName.trim());
                if (index <= fieldStart) {
                    throw new IllegalArgumentException("Parser did not advance while reading object field in structured value: " + input);
                }
                object.add(fieldName.trim(), value);
                skipWhitespace();
                if (!isAtEnd() && input.charAt(index) == ',') {
                    index++;
                    skipWhitespace();
                }
            }
            expect(')');
            return object;
        }

        private JsonArray parseArray() {
            expect('[');
            JsonArray array = new JsonArray();
            skipWhitespace();
            while (!isAtEnd() && input.charAt(index) != ']') {
                int elementStart = index;
                array.add(parseValue(null));
                if (index <= elementStart) {
                    throw new IllegalArgumentException("Parser did not advance while reading array element in structured value: " + input);
                }
                skipWhitespace();
                if (!isAtEnd() && input.charAt(index) == ',') {
                    index++;
                    skipWhitespace();
                }
            }
            expect(']');
            return array;
        }

        private JsonElement parseScalar(String fieldName) {
            int start = index;
            while (!isAtEnd()) {
                char current = input.charAt(index);
                if (current == ',' || current == ')' || current == ']') {
                    break;
                }
                index++;
            }

            String token = input.substring(start, index).trim();
            if (token.isEmpty() || "null".equals(token)) {
                return JsonNull.INSTANCE;
            }
            if ("true".equalsIgnoreCase(token) || "false".equalsIgnoreCase(token)) {
                return new JsonPrimitive(Boolean.parseBoolean(token));
            }
            if (shouldTreatAsNumber(fieldName, token)) {
                return new JsonPrimitive(new BigDecimal(token));
            }
            return new JsonPrimitive(token);
        }

        private boolean shouldTreatAsNumber(String fieldName, String token) {
            if (fieldName == null || !NUMERIC_FIELDS.contains(fieldName)) {
                return false;
            }
            if (!token.matches("-?\\d+(\\.\\d+)?")) {
                return false;
            }
            return !(token.length() > 1 && token.startsWith("0") && !token.startsWith("0."));
        }

        private boolean looksLikeObjectStart() {
            int probe = index;
            if (probe >= input.length() || !Character.isJavaIdentifierStart(input.charAt(probe))) {
                return false;
            }
            probe++;
            while (probe < input.length() && Character.isJavaIdentifierPart(input.charAt(probe))) {
                probe++;
            }
            return probe < input.length() && input.charAt(probe) == '(';
        }

        private String readIdentifier() {
            int start = index;
            if (isAtEnd() || !Character.isJavaIdentifierStart(input.charAt(index))) {
                throw new IllegalArgumentException("Expected identifier in structured value: " + input);
            }
            index++;
            while (!isAtEnd() && Character.isJavaIdentifierPart(input.charAt(index))) {
                index++;
            }
            return input.substring(start, index);
        }

        private String readUntil(char expectedDelimiter) {
            int start = index;
            while (!isAtEnd() && input.charAt(index) != expectedDelimiter) {
                index++;
            }
            if (isAtEnd()) {
                throw new IllegalArgumentException("Missing delimiter '" + expectedDelimiter + "' in structured value: " + input);
            }
            return input.substring(start, index);
        }

        private void expect(char expected) {
            skipWhitespace();
            if (isAtEnd() || input.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' in structured value: " + input);
            }
            index++;
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean isAtEnd() {
            return index >= input.length();
        }
    }
}
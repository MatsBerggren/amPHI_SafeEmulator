package com.dedalus.amphi_integration.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class WrappedPayloadParser {

    private WrappedPayloadParser() {
    }

    public static <T> T parseObject(String json, Gson gson, Class<T> targetClass, String... wrapperKeys) {
        JsonElement payload = gson.fromJson(json, JsonElement.class);

        if (payload != null && payload.isJsonObject()) {
            JsonObject jsonObject = payload.getAsJsonObject();
            for (String wrapperKey : wrapperKeys) {
                JsonElement wrappedPayload = jsonObject.get(wrapperKey);
                if (wrappedPayload == null || wrappedPayload.isJsonNull()) {
                    continue;
                }

                if (wrappedPayload.isJsonPrimitive() && wrappedPayload.getAsJsonPrimitive().isString()) {
                    return gson.fromJson(wrappedPayload.getAsString(), targetClass);
                }

                if (wrappedPayload.isJsonObject()) {
                    return gson.fromJson(wrappedPayload, targetClass);
                }
            }
        }

        return gson.fromJson(payload, targetClass);
    }
}
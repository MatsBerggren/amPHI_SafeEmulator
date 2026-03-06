package com.dedalus.amphi_integration.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Generic JSON file-based repository for storing entities.
 * Each entity type is stored in its own JSON file in the data directory.
 */
@Slf4j
@Component
public class JsonFileRepository<T> {

    private final ObjectMapper objectMapper;
    private final Map<String, T> storage = new ConcurrentHashMap<>();
    private final String dataDirectory = "data";
    private final Class<T> entityClass;
    private String fileName;

    public JsonFileRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.entityClass = null;
    }

    /**
     * Initialize repository with entity class
     */
    public void initialize(Class<T> entityClass) {
        this.fileName = entityClass.getSimpleName() + ".json";
        ensureDataDirectoryExists();
        loadFromFile(entityClass);
    }

    private void ensureDataDirectoryExists() {
        try {
            Path path = Paths.get(dataDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("Failed to create data directory", e);
        }
    }

    private void loadFromFile(Class<T> entityClass) {
        File file = new File(dataDirectory, fileName);
        if (file.exists()) {
            try {
                Map<String, T> data = objectMapper.readValue(file,
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, entityClass));
                storage.clear();
                storage.putAll(data);
                log.info("Loaded {} entities from {}", storage.size(), fileName);
            } catch (IOException e) {
                log.error("Failed to load data from file: {}", fileName, e);
            }
        } else {
            log.info("No existing data file found: {}", fileName);
        }
    }

    private void saveToFile() {
        File file = new File(dataDirectory, fileName);
        try {
            objectMapper.writeValue(file, storage);
            log.debug("Saved {} entities to {}", storage.size(), fileName);
        } catch (IOException e) {
            log.error("Failed to save data to file: {}", fileName, e);
        }
    }

    public T save(T entity) {
        String id = extractId(entity);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            setId(entity, id);
        }
        storage.put(id, entity);
        saveToFile();
        return entity;
    }

    public Optional<T> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void deleteById(String id) {
        storage.remove(id);
        saveToFile();
    }

    public void delete(T entity) {
        String id = extractId(entity);
        if (id != null) {
            deleteById(id);
        }
    }

    public void deleteAll() {
        storage.clear();
        saveToFile();
    }

    public long count() {
        return storage.size();
    }

    public boolean existsById(String id) {
        return storage.containsKey(id);
    }

    private String extractId(T entity) {
        try {
            Field idField = findIdField(entity.getClass());
            if (idField != null) {
                idField.setAccessible(true);
                Object value = idField.get(entity);
                return value != null ? value.toString() : null;
            }
        } catch (IllegalAccessException e) {
            log.error("Failed to extract ID from entity", e);
        }
        return null;
    }

    private void setId(T entity, String id) {
        try {
            Field idField = findIdField(entity.getClass());
            if (idField != null) {
                idField.setAccessible(true);
                idField.set(entity, id);
            }
        } catch (IllegalAccessException e) {
            log.error("Failed to set ID on entity", e);
        }
    }

    private Field findIdField(Class<?> clazz) {
        // Look for field named "id" (case-insensitive)
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase("id")) {
                return field;
            }
        }
        return null;
    }

    public List<T> saveAll(Iterable<T> entities) {
        List<T> result = new ArrayList<>();
        entities.forEach(entity -> result.add(save(entity)));
        return result;
    }
}

package com.dedalus.amphi_integration.repository;

import com.dedalus.amphi_integration.util.LocalDateTimeDeserializer;
import com.dedalus.amphi_integration.util.LocalDateTimeSerializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Generic JSON file-based repository for storing entities.
 * Each entity type is stored in its own JSON file in the data directory.
 */
@Slf4j
@Component
public class JsonFileRepository<T> {

    private final ObjectMapper objectMapper;
    private final Map<String, T> storage = Collections.synchronizedMap(new LinkedHashMap<>());
    private final String dataDirectory = "data";
    @Value("${repository.load-on-startup:false}")
    private boolean loadOnStartup;

    private Class<T> entityClass;
    private String fileName;

    public JsonFileRepository() {
        this.objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        this.objectMapper.registerModule(javaTimeModule);
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Initialize repository with entity class
     */
    public void initialize(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.fileName = entityClass.getSimpleName() + ".json";
        ensureDataDirectoryExists();
        if (loadOnStartup) {
            reloadFromDisk();
        }
    }

    public int reloadFromDisk() {
        if (entityClass == null) {
            throw new IllegalStateException("Repository must be initialized before loading from disk");
        }

        loadFromFile(entityClass);
        return storage.size();
    }

    private void ensureDataDirectoryExists() {
        try {
            Path path = getDataDirectoryPath();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("Failed to create data directory", e);
        }
    }

    private void loadFromFile(Class<T> entityClass) {
        File file = getDataDirectoryPath().resolve(fileName).toFile();
        if (file.exists()) {
            try {
                Map<String, T> data = objectMapper.readValue(file,
                        objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, entityClass));
                Map<String, T> normalizedData = normalizeLoadedData(data);
                synchronized (storage) {
                    storage.clear();
                    storage.putAll(normalizedData);
                }
                log.info("Loaded {} entities from {}", storage.size(), fileName);
            } catch (IOException e) {
                log.error("Failed to load data from file: {}", fileName, e);
            }
        } else {
            log.info("No existing data file found: {}", fileName);
        }
    }

    private void saveToFile() {
        File file = getDataDirectoryPath().resolve(fileName).toFile();
        try {
            objectMapper.writeValue(file, createPersistenceSnapshot());
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
        synchronized (storage) {
            storage.put(id, entity);
        }
        saveToFile();
        return entity;
    }

    public Optional<T> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<T> findAll() {
        synchronized (storage) {
            return new ArrayList<>(storage.values());
        }
    }

    public void deleteById(String id) {
        synchronized (storage) {
            storage.remove(id);
        }
        saveToFile();
    }

    public void delete(T entity) {
        String id = extractId(entity);
        if (id != null) {
            deleteById(id);
        }
    }

    public void deleteAll() {
        synchronized (storage) {
            storage.clear();
        }
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

    protected Path getDataDirectoryPath() {
        return Paths.get(dataDirectory);
    }

    protected Map<String, T> normalizeLoadedData(Map<String, T> data) {
        return data;
    }

    protected Map<String, T> createPersistenceSnapshot() {
        synchronized (storage) {
            return new LinkedHashMap<>(storage);
        }
    }
}

package com.telemedicine.common.mockdata;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * In-memory store seeded from JSON. Mutations go to an overlay only — seed files are never modified.
 */
public class JsonStore<T> {
    private final Map<String, T> seeds = new LinkedHashMap<>();
    private final Map<String, T> overlay = new ConcurrentHashMap<>();
    private final Function<T, String> idExtractor;
    private final boolean writable;

    public JsonStore(Function<T, String> idExtractor, boolean writable) {
        this.idExtractor = idExtractor;
        this.writable = writable;
    }

    public void load(List<T> items) {
        seeds.clear();
        overlay.clear();
        for (T item : items) {
            seeds.put(idExtractor.apply(item), item);
        }
    }

    public Optional<T> findById(String id) {
        T value = overlay.get(id);
        if (value != null) {
            return Optional.of(value);
        }
        return Optional.ofNullable(seeds.get(id));
    }

    public List<T> findAll() {
        Map<String, T> merged = new LinkedHashMap<>(seeds);
        merged.putAll(overlay);
        return List.copyOf(merged.values());
    }

    public List<T> findBy(Predicate<T> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    public T save(T item) {
        if (!writable) {
            throw new IllegalStateException("Mock store is read-only");
        }
        String id = idExtractor.apply(item);
        overlay.put(id, item);
        return item;
    }

    public boolean deleteById(String id) {
        if (!writable) {
            throw new IllegalStateException("Mock store is read-only");
        }
        boolean removed = seeds.containsKey(id) || overlay.containsKey(id);
        seeds.remove(id);
        overlay.remove(id);
        return removed;
    }

    public void reset() {
        overlay.clear();
    }

    public static <T> JsonStore<T> fromResource(
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            String location,
            Function<T, String> idExtractor,
            boolean writable,
            Class<T> elementType) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Mock seed file not found: " + location);
        }
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        List<T> items;
        try (InputStream in = resource.getInputStream()) {
            items = objectMapper.readValue(in, listType);
        }
        JsonStore<T> store = new JsonStore<>(idExtractor, writable);
        store.load(items);
        return store;
    }
}

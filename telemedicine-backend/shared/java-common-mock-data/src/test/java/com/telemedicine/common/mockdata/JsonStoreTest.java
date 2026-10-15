package com.telemedicine.common.mockdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonStoreTest {

    record Sample(String id, String name) {}

    @Test
    void loadsAndQueriesById() throws Exception {
        var loader = new DefaultResourceLoader();
        var mapper = new ObjectMapper();
        var store = JsonStore.fromResource(
                loader,
                mapper,
                "classpath:mock-data-samples/items.json",
                Sample::id,
                true,
                Sample.class
        );

        Optional<Sample> found = store.findById("item-1");
        assertTrue(found.isPresent());
        assertEquals("Alpha", found.get().name());
    }

    @Test
    void saveWritesToOverlayOnly() throws Exception {
        var loader = new DefaultResourceLoader();
        var mapper = new ObjectMapper();
        var store = JsonStore.fromResource(
                loader,
                mapper,
                "classpath:mock-data-samples/items.json",
                Sample::id,
                true,
                Sample.class
        );

        store.save(new Sample("item-3", "Gamma"));
        assertEquals(3, store.findAll().size());
        assertEquals("Gamma", store.findById("item-3").orElseThrow().name());
    }
}

package org.jenkinsci.plugins.envinject.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gregory Boissinot
 */
class PropertiesGetterTest {

    private final PropertiesGetter propertiesGetter = new PropertiesGetter();

    @Test
    void getPropertiesContentNullArg() {
        assertNull(propertiesGetter.getPropertiesContentFromMapObject(null));
    }

    @Test
    void getPropertiesContentEmptyMap() {
        String content = propertiesGetter.getPropertiesContentFromMapObject(Collections.emptyMap());
        assertNotNull(content);
        assertEquals(0, content.trim().length());
    }

    @Test
    void getPropertiesContentOneElement() {
        Map<String, String> entryMap = new HashMap<>();
        entryMap.put("someKey", "someValue");
        String content = propertiesGetter.getPropertiesContentFromMapObject(entryMap);
        assertNotNull(content);
        assertEquals("someKey=someValue", content);
    }

    @Test
    void getPropertiesContentThreeElements() {
        Map<String, String> entryMap = new LinkedHashMap<>();
        entryMap.put("key3", "value3");
        entryMap.put("key2", "value2");
        entryMap.put("key1", "value1");
        String content = propertiesGetter.getPropertiesContentFromMapObject(entryMap);
        assertNotNull(content);
        List<String> lines = Arrays.asList(content.split("\n"));
        assertTrue(lines.contains("key1=value1"));
        assertTrue(lines.contains("key2=value2"));
        assertTrue(lines.contains("key3=value3"));
    }

}

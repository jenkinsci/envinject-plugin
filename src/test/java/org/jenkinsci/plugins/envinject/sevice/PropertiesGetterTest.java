package org.jenkinsci.plugins.envinject.sevice;

import org.jenkinsci.plugins.envinject.service.PropertiesGetter;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Gregory Boissinot
 */
public class PropertiesGetterTest {

    private PropertiesGetter propertiesGetter = new PropertiesGetter();

    @Test
    public void getPropertiesContentNullArg() {
        assertNull(propertiesGetter.getPropertiesContentFromMapObject(null));
    }

    @Test
    public void getPropertiesContentEmptyMap() {
        String content = propertiesGetter.getPropertiesContentFromMapObject(Collections.<String, String>emptyMap());
        assertNotNull(content);
        assertTrue(content.trim().length() == 0);
    }

    @Test
    public void getPropertiesContentOneElement() {
        Map<String, String> entryMap = new HashMap<String, String>();
        entryMap.put("someKey", "someValue");
        String content = propertiesGetter.getPropertiesContentFromMapObject(entryMap);
        assertNotNull(content);
        assertEquals("someKey=someValue", content);
    }

    @Test
    public void getPropertiesContentThreeElements() {
        Map<String, String> entryMap = new HashMap<String, String>();
        entryMap.put("key1", "value1");
        entryMap.put("key2", "value2");
        entryMap.put("key3", "value3");
        String content = propertiesGetter.getPropertiesContentFromMapObject(entryMap);
        assertNotNull(content);
        assertEquals("key3=value3\nkey2=value2\nkey1=value1", content);
    }

}
